package com.beyt.jdq.elasticsearch.core;

import com.beyt.jdq.core.deserializer.IDeserializer;
import com.beyt.jdq.core.model.DynamicQuery;
import com.beyt.jdq.core.model.annotation.JdqField;
import com.beyt.jdq.core.model.annotation.JdqModel;
import com.beyt.jdq.core.model.annotation.JdqSubModel;
import com.beyt.jdq.core.model.annotation.JdqIgnoreField;
import com.beyt.jdq.core.model.enums.CriteriaOperator;
import com.beyt.jdq.core.model.exception.DynamicQueryIllegalArgumentException;
import com.beyt.jdq.core.util.field.FieldUtil;
import org.apache.commons.lang3.StringUtils;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.util.CollectionUtils;
import org.springframework.data.util.Pair;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Template class for building and executing Elasticsearch queries from DynamicQuery objects.
 * Provides methods similar to DynamicQueryManager but for Elasticsearch.
 */
public class ElasticsearchSearchQueryTemplate {

    private final ElasticsearchOperations elasticsearchOperations;
    private final IDeserializer deserializer;

    public ElasticsearchSearchQueryTemplate(ElasticsearchOperations elasticsearchOperations, IDeserializer deserializer) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.deserializer = deserializer;
    }

    /**
     * Find all entities matching the criteria list
     */
    public <Entity> List<Entity> findAll(Class<Entity> entityClass, List<com.beyt.jdq.core.model.Criteria> searchCriteriaList) {
        DynamicQuery dynamicQuery = DynamicQuery.of(searchCriteriaList);
        return findAll(entityClass, dynamicQuery);
    }

    /**
     * Find all entities matching the dynamic query
     */
    public <Entity> List<Entity> findAll(Class<Entity> entityClass, DynamicQuery dynamicQuery) {
        // If we have selections, use the projection method with same class
        if (!CollectionUtils.isEmpty(dynamicQuery.getSelect())) {
            return findAll(entityClass, dynamicQuery, entityClass);
        }
        
        NativeQuery query = prepareQuery(entityClass, dynamicQuery);
        SearchHits<Entity> searchHits = elasticsearchOperations.search(query, entityClass);
        List<Entity> results = new ArrayList<>();
        
        // Check if we're querying on nested fields - if so, we need to return duplicates
        // to match JPA behavior (one result per matching nested document)
        boolean hasNestedFieldQuery = hasNestedFieldInWhere(dynamicQuery);
        
        for (SearchHit<Entity> searchHit : searchHits) {
            Entity entity = searchHit.getContent();
            if (hasNestedFieldQuery && !dynamicQuery.isDistinct()) {
                // Count how many nested documents match the criteria
                int matchCount = countMatchingNestedDocuments(entity, dynamicQuery);
                for (int i = 0; i < matchCount; i++) {
                    results.add(entity);
                }
            } else {
                results.add(entity);
            }
        }
        
        // Handle distinct if needed
        if (dynamicQuery.isDistinct()) {
            results = results.stream().distinct().collect(Collectors.toList());
        }
        
        return results;
    }

    /**
     * Find all entities with projection matching the dynamic query
     */
    public <Entity, ResultType> List<ResultType> findAll(Class<Entity> entityClass, DynamicQuery dynamicQuery, Class<ResultType> resultClass) {
        // Extract @JdqModel annotations if present
        extractIfJdqModel(dynamicQuery, resultClass);
        
        // Check if projection is needed
        if (CollectionUtils.isEmpty(dynamicQuery.getSelect())) {
            // No projection, just get entities
            if (entityClass.equals(resultClass)) {
                // No projection, just cast
                List<Entity> results = findAll(entityClass, dynamicQuery);
                return results.stream()
                        .map(entity -> (ResultType) entity)
                        .collect(Collectors.toList());
            }
        }
        
        // Check if we need to apply ordering/pagination after flattening
        boolean needsPostFlatteningOrdering = needsPostFlatteningOrdering(dynamicQuery);
        
        // Only skip ES-level pagination/ordering if we have nested field ordering
        DynamicQuery queryForES = dynamicQuery;
        if (needsPostFlatteningOrdering) {
            queryForES = new DynamicQuery();
            queryForES.setWhere(dynamicQuery.getWhere());
            queryForES.setSelect(dynamicQuery.getSelect());
            queryForES.setDistinct(false); // Will apply distinct after
            // Don't set orderBy - ES can't sort by nested fields, will sort after flattening
            // Set a large page size to get all results for post-processing
            queryForES.setPageNumber(0);
            queryForES.setPageSize(10000); // Large enough to get all results
        }
        
        // Execute query and convert to result type
        NativeQuery query = prepareQuery(entityClass, queryForES);
        SearchHits<Entity> searchHits = elasticsearchOperations.search(query, entityClass);
        
        // For nested field projections, we need to flatten the results
        List<ResultType> results = new ArrayList<>();
        for (SearchHit<Entity> searchHit : searchHits) {
            Entity entity = searchHit.getContent();
            List<ResultType> flattenedResults = convertEntityToResultTypeWithFlattening(entity, resultClass, dynamicQuery);
            results.addAll(flattenedResults);
        }
        
        // Apply ordering after flattening if needed
        if (needsPostFlatteningOrdering && dynamicQuery.getOrderBy() != null && !dynamicQuery.getOrderBy().isEmpty()) {
            results = applyOrdering(results, dynamicQuery);
        }
        
        // Handle distinct if needed
        if (dynamicQuery.isDistinct()) {
            results = results.stream().distinct().collect(Collectors.toList());
        }
        
        // Apply pagination after flattening/ordering if needed
        if (needsPostFlatteningOrdering && dynamicQuery.getPageNumber() != null && dynamicQuery.getPageSize() != null) {
            int pageNumber = dynamicQuery.getPageNumber();
            int pageSize = dynamicQuery.getPageSize();
            int fromIndex = pageNumber * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, results.size());
            
            if (fromIndex < results.size()) {
                results = new ArrayList<>(results.subList(fromIndex, toIndex));
            } else {
                results = new ArrayList<>();
            }
        }
        
        return results;
    }

    /**
     * Find all entities as page matching the criteria list
     */
    public <Entity> Page<Entity> findAllAsPage(Class<Entity> entityClass, List<com.beyt.jdq.core.model.Criteria> searchCriteriaList, Pageable pageable) {
        DynamicQuery dynamicQuery = DynamicQuery.of(searchCriteriaList);
        dynamicQuery.setPageNumber(pageable.getPageNumber());
        dynamicQuery.setPageSize(pageable.getPageSize());
        return findAllAsPage(entityClass, dynamicQuery);
    }

    /**
     * Find all entities as page matching the dynamic query
     */
    public <Entity> Page<Entity> findAllAsPage(Class<Entity> entityClass, DynamicQuery dynamicQuery) {
        // If we have selections, use the projection method with same class
        if (!CollectionUtils.isEmpty(dynamicQuery.getSelect())) {
            return findAllAsPage(entityClass, dynamicQuery, entityClass);
        }

        NativeQuery query = prepareQuery(entityClass, dynamicQuery);
        SearchHits<Entity> searchHits = elasticsearchOperations.search(query, entityClass);
        
        List<Entity> results = searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
        
        // Handle distinct if needed
        if (dynamicQuery.isDistinct()) {
            results = results.stream().distinct().collect(Collectors.toList());
        }
        
        return new org.springframework.data.domain.PageImpl<>(
                results,
                PageRequest.of(
                        dynamicQuery.getPageNumber() != null ? dynamicQuery.getPageNumber() : 0,
                        dynamicQuery.getPageSize() != null ? dynamicQuery.getPageSize() : 20
                ),
                searchHits.getTotalHits()
        );
    }

    /**
     * Find all entities as page with projection matching the dynamic query
     */
    public <Entity, ResultType> Page<ResultType> findAllAsPage(Class<Entity> entityClass, DynamicQuery dynamicQuery, Class<ResultType> resultClass) {
        // Extract @JdqModel annotations if present
        extractIfJdqModel(dynamicQuery, resultClass);
        
        // Check if projection is needed
        if (CollectionUtils.isEmpty(dynamicQuery.getSelect())) {
            // No projection, just get entities
            if (entityClass.equals(resultClass)) {
                // No projection, just cast
                Page<Entity> page = findAllAsPage(entityClass, dynamicQuery);
                return page.map(entity -> (ResultType) entity);
            }
        }
        
        // Check if we need to apply ordering/pagination after flattening
        // This is needed when ordering by nested fields, as flattening changes the result count
        boolean needsPostFlatteningOrdering = needsPostFlatteningOrdering(dynamicQuery);
        
        // Only skip ES-level pagination/ordering if we have nested field ordering
        // Elasticsearch cannot sort parent documents by nested field values properly,
        // so we need to fetch all results, flatten them, and sort in memory
        DynamicQuery queryForES = dynamicQuery;
        if (needsPostFlatteningOrdering) {
            queryForES = new DynamicQuery();
            queryForES.setWhere(dynamicQuery.getWhere());
            queryForES.setSelect(dynamicQuery.getSelect());
            queryForES.setDistinct(false); // Will apply distinct after
            // Don't set orderBy - ES can't sort by nested fields, will sort after flattening
            // Set a large page size to get all results for post-processing
            queryForES.setPageNumber(0);
            queryForES.setPageSize(10000); // Large enough to get all results
        }
        
        // Execute query and convert to result type
        NativeQuery query = prepareQuery(entityClass, queryForES);
        SearchHits<Entity> searchHits = elasticsearchOperations.search(query, entityClass);
        
        // For nested field projections, we need to flatten the results
        List<ResultType> results = new ArrayList<>();
        for (SearchHit<Entity> searchHit : searchHits) {
            Entity entity = searchHit.getContent();
            List<ResultType> flattenedResults = convertEntityToResultTypeWithFlattening(entity, resultClass, dynamicQuery);
            results.addAll(flattenedResults);
        }
        
        // Apply ordering after flattening if needed
        if (needsPostFlatteningOrdering && dynamicQuery.getOrderBy() != null && !dynamicQuery.getOrderBy().isEmpty()) {
            results = applyOrdering(results, dynamicQuery);
        }
        
        // Handle distinct if needed
        if (dynamicQuery.isDistinct()) {
            results = results.stream().distinct().collect(Collectors.toList());
        }
        
        // Build sort object for the PageRequest
        Sort sort = Sort.unsorted();
        if (dynamicQuery.getOrderBy() != null && !dynamicQuery.getOrderBy().isEmpty()) {
            List<Sort.Order> orders = new ArrayList<>();
            for (Pair<String, com.beyt.jdq.core.model.enums.Order> orderPair : dynamicQuery.getOrderBy()) {
                Sort.Direction direction = orderPair.getSecond() == com.beyt.jdq.core.model.enums.Order.ASC 
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
                orders.add(new Sort.Order(direction, orderPair.getFirst()));
            }
            sort = Sort.by(orders);
        }
        
        // Apply pagination after flattening ONLY if we skipped ES-level pagination
        if (needsPostFlatteningOrdering) {
            int pageNumber = dynamicQuery.getPageNumber() != null ? dynamicQuery.getPageNumber() : 0;
            int pageSize = dynamicQuery.getPageSize() != null ? dynamicQuery.getPageSize() : 20;
            long totalElements = results.size();
            
            int fromIndex = pageNumber * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, results.size());
            
            List<ResultType> pagedResults;
            if (fromIndex < results.size()) {
                pagedResults = results.subList(fromIndex, toIndex);
            } else {
                pagedResults = new ArrayList<>();
            }
            
            return new org.springframework.data.domain.PageImpl<>(
                    pagedResults,
                    PageRequest.of(pageNumber, pageSize, sort),
                    totalElements
            );
        } else {
            // Elasticsearch already handled pagination
            int pageNumber = dynamicQuery.getPageNumber() != null ? dynamicQuery.getPageNumber() : 0;
            int pageSize = dynamicQuery.getPageSize() != null ? dynamicQuery.getPageSize() : 20;
            
            return new org.springframework.data.domain.PageImpl<>(
                    results,
                    PageRequest.of(pageNumber, pageSize, sort),
                    searchHits.getTotalHits()
            );
        }
    }
    
    /**
     * Check if ordering needs to be applied after flattening
     * This is true if any orderBy field is a nested field
     */
    private boolean needsPostFlatteningOrdering(DynamicQuery dynamicQuery) {
        if (CollectionUtils.isEmpty(dynamicQuery.getOrderBy())) {
            return false;
        }
        
        for (Pair<String, com.beyt.jdq.core.model.enums.Order> orderPair : dynamicQuery.getOrderBy()) {
            if (orderPair.getFirst().contains(".")) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Apply ordering to results after flattening
     */
    private <ResultType> List<ResultType> applyOrdering(List<ResultType> results, DynamicQuery dynamicQuery) {
        if (CollectionUtils.isEmpty(dynamicQuery.getOrderBy())) {
            return results;
        }
        
        // Create a comparator based on orderBy fields
        Comparator<ResultType> comparator = null;
        
        for (Pair<String, com.beyt.jdq.core.model.enums.Order> orderPair : dynamicQuery.getOrderBy()) {
            String fieldPath = orderPair.getFirst();
            com.beyt.jdq.core.model.enums.Order order = orderPair.getSecond();
            
            // Map the entity field path to the result field name
            String resultFieldName = findResultFieldName(dynamicQuery, fieldPath);
            if (resultFieldName == null) {
                // If not found in select mappings, try to use the last part of the path
                // e.g., "roles.id" -> "id", but we need the alias if it exists
                // For now, try the full path first, then the last segment
                resultFieldName = fieldPath.contains(".") ? fieldPath.substring(fieldPath.lastIndexOf(".") + 1) : fieldPath;
            }
            
            // Make final for lambda
            final String finalResultFieldName = resultFieldName;
            final com.beyt.jdq.core.model.enums.Order finalOrder = order;
            
            Comparator<ResultType> fieldComparator = (r1, r2) -> {
                Object v1 = getFieldValue(r1, finalResultFieldName);
                Object v2 = getFieldValue(r2, finalResultFieldName);
                int cmp = compareValues(v1, v2);
                return finalOrder == com.beyt.jdq.core.model.enums.Order.DESC ? -cmp : cmp;
            };
            
            if (comparator == null) {
                comparator = fieldComparator;
            } else {
                comparator = comparator.thenComparing(fieldComparator);
            }
        }
        
        if (comparator != null) {
            results.sort(comparator);
        }
        
        return results;
    }
    
    /**
     * Find the result field name for a given source path
     */
    private String findResultFieldName(DynamicQuery dynamicQuery, String sourcePath) {
        if (CollectionUtils.isEmpty(dynamicQuery.getSelect())) {
            return null;
        }
        
        for (Pair<String, String> selectPair : dynamicQuery.getSelect()) {
            if (selectPair.getFirst().equals(sourcePath)) {
                return selectPair.getSecond();
            }
        }
        
        return null;
    }

    /**
     * Count entities matching the criteria list
     */
    public <Entity> long count(Class<Entity> entityClass, List<com.beyt.jdq.core.model.Criteria> searchCriteriaList) {
        DynamicQuery dynamicQuery = DynamicQuery.of(searchCriteriaList);
        NativeQuery query = prepareQuery(entityClass, dynamicQuery);
        SearchHits<Entity> searchHits = elasticsearchOperations.search(query, entityClass);
        return searchHits.getTotalHits();
    }

    /**
     * Prepare NativeQuery from DynamicQuery
     */
    private <Entity> NativeQuery prepareQuery(Class<Entity> entityClass, DynamicQuery dynamicQuery) {
        NativeQueryBuilder queryBuilder = new NativeQueryBuilder();
        
        // Build the main query from criteria
        Query mainQuery = buildQueryFromCriteria(dynamicQuery.getWhere());
        queryBuilder.withQuery(mainQuery);
        
        // Build sort object
        Sort sort = Sort.unsorted();
        if (dynamicQuery.getOrderBy() != null && !dynamicQuery.getOrderBy().isEmpty()) {
            List<Sort.Order> orders = new ArrayList<>();
            for (Pair<String, com.beyt.jdq.core.model.enums.Order> orderPair : dynamicQuery.getOrderBy()) {
                Sort.Direction direction = orderPair.getSecond() == com.beyt.jdq.core.model.enums.Order.ASC 
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
                orders.add(new Sort.Order(direction, orderPair.getFirst()));
            }
            sort = Sort.by(orders);
        }
        
        // Add pagination with sort
        int pageNumber = dynamicQuery.getPageNumber() != null ? dynamicQuery.getPageNumber() : 0;
        int pageSize = dynamicQuery.getPageSize() != null ? dynamicQuery.getPageSize() : 20;
        queryBuilder.withPageable(PageRequest.of(pageNumber, pageSize, sort));
        
        return queryBuilder.build();
    }

    /**
     * Build Elasticsearch Query from criteria list
     */
    private Query buildQueryFromCriteria(List<com.beyt.jdq.core.model.Criteria> criteriaList) {
        if (criteriaList == null || criteriaList.isEmpty()) {
            return Query.of(q -> q.matchAll(m -> m));
        }

        // Validate OR operator usage
        validateOrOperatorUsage(criteriaList);

        // Split criteria list by OR operators
        List<List<com.beyt.jdq.core.model.Criteria>> orGroups = new java.util.ArrayList<>();
        List<com.beyt.jdq.core.model.Criteria> currentGroup = new java.util.ArrayList<>();
        
        for (com.beyt.jdq.core.model.Criteria criteria : criteriaList) {
            if (criteria.getOperation() == CriteriaOperator.OR) {
                if (!currentGroup.isEmpty()) {
                    orGroups.add(currentGroup);
                    currentGroup = new java.util.ArrayList<>();
                }
            } else {
                currentGroup.add(criteria);
            }
        }
        
        // Add the last group
        if (!currentGroup.isEmpty()) {
            orGroups.add(currentGroup);
        }
        
        // If only one group, build it as AND query
        if (orGroups.size() == 1) {
            return buildAndQuery(orGroups.get(0));
        }
        
        // Multiple groups - combine with OR
        List<Query> shouldQueries = new ArrayList<>();
        for (List<com.beyt.jdq.core.model.Criteria> group : orGroups) {
            Query groupQuery = buildAndQuery(group);
            shouldQueries.add(groupQuery);
        }
        
        return Query.of(q -> q.bool(b -> b
            .should(shouldQueries)
            .minimumShouldMatch("1")));
    }
    
    /**
     * Validate OR operator usage
     * - OR cannot be the first or last element
     * - OR must have non-OR criteria before and after it
     */
    private void validateOrOperatorUsage(List<com.beyt.jdq.core.model.Criteria> criteriaList) {
        if (criteriaList == null || criteriaList.isEmpty()) {
            return;
        }
        
        // Check if first element is OR
        if (criteriaList.get(0).getOperation() == CriteriaOperator.OR) {
            throw new com.beyt.jdq.core.model.exception.DynamicQueryNoAvailableOrOperationUsageException(
                "OR operator cannot be the first element in criteria list");
        }
        
        // Check if last element is OR
        if (criteriaList.get(criteriaList.size() - 1).getOperation() == CriteriaOperator.OR) {
            throw new com.beyt.jdq.core.model.exception.DynamicQueryNoAvailableOrOperationUsageException(
                "OR operator cannot be the last element in criteria list");
        }
        
        // Check if only OR operators exist
        boolean hasNonOrCriteria = false;
        for (com.beyt.jdq.core.model.Criteria criteria : criteriaList) {
            if (criteria.getOperation() != CriteriaOperator.OR) {
                hasNonOrCriteria = true;
                break;
            }
        }
        
        if (!hasNonOrCriteria) {
            throw new com.beyt.jdq.core.model.exception.DynamicQueryNoAvailableOrOperationUsageException(
                "Cannot have only OR operators in criteria list");
        }
    }
    
    /**
     * Build AND query from criteria list (no OR operators)
     */
    private Query buildAndQuery(List<com.beyt.jdq.core.model.Criteria> criteriaList) {
        if (criteriaList.isEmpty()) {
            return Query.of(q -> q.matchAll(m -> m));
        }
        
        if (criteriaList.size() == 1) {
            return buildCriteriaQuery(criteriaList.get(0));
        }
        
        List<Query> mustQueries = new ArrayList<>();
        for (com.beyt.jdq.core.model.Criteria criteria : criteriaList) {
            Query criteriaQuery = buildCriteriaQuery(criteria);
            if (criteriaQuery != null) {
                mustQueries.add(criteriaQuery);
            }
        }
        
        return Query.of(q -> q.bool(b -> b.must(mustQueries)));
    }

    /**
     * Build individual criteria query
     * Handles both regular fields and nested fields
     */
    @SuppressWarnings("unchecked")
    private Query buildCriteriaQuery(com.beyt.jdq.core.model.Criteria criteria) {
        String fieldName = criteria.getKey();
        CriteriaOperator operator = criteria.getOperation();
        List<Object> values = criteria.getValues();
        
        // Handle PARENTHES operator (nested criteria)
        if (operator == CriteriaOperator.PARENTHES) {
            if (values != null && !values.isEmpty() && values.get(0) instanceof List) {
                List<com.beyt.jdq.core.model.Criteria> nestedCriteria = (List<com.beyt.jdq.core.model.Criteria>) values.get(0);
                return buildQueryFromCriteria(nestedCriteria);
            }
            return null;
        }
        
        if (StringUtils.isBlank(fieldName) || operator == null) {
            return null;
        }
        
        if (values == null || values.isEmpty()) {
            return null;
        }
        
        // Check if this is a nested field query
        // Handle left join syntax: "department<id" means check if department exists
        // For SPECIFIED operator, check the parent nested object, not the child field
        boolean hasLeftJoinSyntax = fieldName.contains("<");
        String normalizedFieldName = fieldName.replace("<", ".");
        
        // Special case: "department<id" with SPECIFIED=false should check if "department" doesn't exist
        // In Elasticsearch nested documents, if the nested object is null, none of its fields exist
        if (hasLeftJoinSyntax && operator == CriteriaOperator.SPECIFIED) {
            // Extract the parent path (before the last dot)
            int lastDotIndex = normalizedFieldName.lastIndexOf(".");
            if (lastDotIndex > 0) {
                String parentPath = normalizedFieldName.substring(0, lastDotIndex);
                // For nested documents, check if any field in the nested object exists
                // If the nested object is null, the field won't exist in Elasticsearch
                boolean shouldExist = Boolean.parseBoolean(values.get(0).toString());
                if (shouldExist) {
                    // Check if the nested field exists (the actual field, not the parent)
                    String fieldToCheck = normalizedFieldName;
                    return Query.of(q -> q.exists(e -> e.field(fieldToCheck)));
                } else {
                    // Check if the nested field doesn't exist (meaning the parent nested object is null)
                    String fieldToCheck = normalizedFieldName;
                    return Query.of(q -> q.bool(b -> b
                        .mustNot(Query.of(mq -> mq.exists(e -> e.field(fieldToCheck))))));
                }
            }
        }
        
        // Detect nested path (e.g., "department.name" or "roles.roleAuthorizations.authorization.menuIcon")
        NestedPathInfo nestedPathInfo = analyzeNestedPath(normalizedFieldName);
        
        if (nestedPathInfo != null && nestedPathInfo.hasNestedPath()) {
            // Build nested query
            return buildNestedQuery(nestedPathInfo, operator, values, false);
        }
        
        switch (operator) {
            case EQUAL:
                if (values.size() > 1) {
                    // Multiple values - build OR query with match_phrase for each value
                    List<Query> shouldQueries = new ArrayList<>();
                    for (Object value : values) {
                        Object finalValue = value;
                        shouldQueries.add(Query.of(q -> q.matchPhrase(mp -> mp.field(fieldName).query(finalValue.toString()))));
                    }
                    return Query.of(q -> q.bool(b -> b
                        .should(shouldQueries)
                        .minimumShouldMatch("1")));
                } else {
                    // Single value - use match_phrase for exact phrase matching
                    Object finalValue = values.get(0);
                    return Query.of(q -> q.matchPhrase(mp -> mp.field(fieldName).query(finalValue.toString())));
                }
                
            case NOT_EQUAL:
                if (values.size() > 1) {
                    // Exclude all specified values
                    List<Query> mustNotQueries = new ArrayList<>();
                    for (Object value : values) {
                        Object finalValue = value;
                        mustNotQueries.add(Query.of(q -> q.matchPhrase(mp -> mp.field(fieldName).query(finalValue.toString()))));
                    }
                    return Query.of(q -> q.bool(b -> b
                        .must(Query.of(mq -> mq.exists(e -> e.field(fieldName))))
                        .mustNot(mustNotQueries)));
                } else {
                    // Exclude single value
                    Object finalValue = values.get(0);
                    return Query.of(q -> q.bool(b -> b
                        .must(Query.of(mq -> mq.exists(e -> e.field(fieldName))))
                        .mustNot(Query.of(mnq -> mnq.matchPhrase(mp -> mp.field(fieldName).query(finalValue.toString()))))));
                }

                
            case CONTAIN:
                if (values.size() > 1) {
                    // Multiple values - OR logic (match any)
                    List<Query> shouldQueries = new ArrayList<>();
                    for (Object value : values) {
                        String wildcardPattern = "*" + escapeWildcard(value.toString()).toLowerCase() + "*";
                        shouldQueries.add(Query.of(q -> q.wildcard(w -> w
                            .field(fieldName)
                            .value(wildcardPattern)
                            .caseInsensitive(true))));
                    }
                    return Query.of(q -> q.bool(b -> b
                        .should(shouldQueries)
                        .minimumShouldMatch("1")));
                } else {
                    String wildcardPattern = "*" + escapeWildcard(values.get(0).toString()).toLowerCase() + "*";
                    return Query.of(q -> q.wildcard(w -> w
                        .field(fieldName)
                        .value(wildcardPattern)
                        .caseInsensitive(true)));
                }
                
            case DOES_NOT_CONTAIN:
                // Multiple values - AND logic (must not contain any)
                if (values.size() > 1) {
                    List<Query> mustNotQueries = new ArrayList<>();
                    for (Object value : values) {
                        String wildcardPattern = "*" + escapeWildcard(value.toString()).toLowerCase() + "*";
                        mustNotQueries.add(Query.of(q -> q.wildcard(w -> w
                            .field(fieldName)
                            .value(wildcardPattern)
                            .caseInsensitive(true))));
                    }
                    return Query.of(q -> q.bool(b -> b
                        .must(Query.of(mq -> mq.exists(e -> e.field(fieldName))))
                        .mustNot(mustNotQueries)));
                } else {
                    String wildcardPattern = "*" + escapeWildcard(values.get(0).toString()).toLowerCase() + "*";
                    return Query.of(q -> q.bool(b -> b
                        .must(Query.of(mq -> mq.exists(e -> e.field(fieldName))))
                        .mustNot(Query.of(mnq -> mnq.wildcard(w -> w
                            .field(fieldName)
                            .value(wildcardPattern)
                            .caseInsensitive(true))))));
                }
                
            case START_WITH:
                if (values.size() > 1) {
                    // Multiple values - OR logic (match any)
                    List<Query> shouldQueries = new ArrayList<>();
                    for (Object value : values) {
                        String prefixValue = value.toString().toLowerCase();
                        shouldQueries.add(Query.of(q -> q.prefix(p -> p
                            .field(fieldName)
                            .value(prefixValue)
                            .caseInsensitive(true))));
                    }
                    return Query.of(q -> q.bool(b -> b
                        .should(shouldQueries)
                        .minimumShouldMatch("1")));
                } else {
                    String prefixValue = values.get(0).toString().toLowerCase();
                    return Query.of(q -> q.prefix(p -> p
                        .field(fieldName)
                        .value(prefixValue)
                        .caseInsensitive(true)));
                }
                
            case END_WITH:
                if (values.size() > 1) {
                    // Multiple values - OR logic (match any)
                    List<Query> shouldQueries = new ArrayList<>();
                    for (Object value : values) {
                        String wildcardPattern = "*" + escapeWildcard(value.toString()).toLowerCase();
                        shouldQueries.add(Query.of(q -> q.wildcard(w -> w
                            .field(fieldName)
                            .value(wildcardPattern)
                            .caseInsensitive(true))));
                    }
                    return Query.of(q -> q.bool(b -> b
                        .should(shouldQueries)
                        .minimumShouldMatch("1")));
                } else {
                    String wildcardPattern = "*" + escapeWildcard(values.get(0).toString()).toLowerCase();
                    return Query.of(q -> q.wildcard(w -> w
                        .field(fieldName)
                        .value(wildcardPattern)
                        .caseInsensitive(true)));
                }
                
            case GREATER_THAN:
                return Query.of(q -> q.range(r -> r.field(fieldName).gt(asJsonData(values.get(0)))));
                
            case GREATER_THAN_OR_EQUAL:
                return Query.of(q -> q.range(r -> r.field(fieldName).gte(asJsonData(values.get(0)))));
                
            case LESS_THAN:
                return Query.of(q -> q.range(r -> r.field(fieldName).lt(asJsonData(values.get(0)))));
                
            case LESS_THAN_OR_EQUAL:
                return Query.of(q -> q.range(r -> r.field(fieldName).lte(asJsonData(values.get(0)))));
                
            case SPECIFIED:
                boolean exists = Boolean.parseBoolean(values.get(0).toString());
                if (exists) {
                    return Query.of(q -> q.exists(e -> e.field(fieldName)));
                } else {
                    return Query.of(q -> q.bool(b -> b
                        .mustNot(Query.of(mq -> mq.exists(e -> e.field(fieldName))))));
                }
                
            default:
                throw new DynamicQueryIllegalArgumentException("Unsupported operator: " + operator);
        }
    }
    
    /**
     * Convert value to JsonData for range queries
     */
    private co.elastic.clients.json.JsonData asJsonData(Object value) {
        if (value instanceof Number) {
            return co.elastic.clients.json.JsonData.of(((Number) value).doubleValue());
        } else if (value instanceof String) {
            return co.elastic.clients.json.JsonData.of((String) value);
        } else if (value instanceof Boolean) {
            return co.elastic.clients.json.JsonData.of((Boolean) value);
        } else {
            return co.elastic.clients.json.JsonData.of(value.toString());
        }
    }

    /**
     * Escape wildcard special characters
     */
    private String escapeWildcard(String value) {
        return value.replace("\\", "\\\\")
                   .replace("*", "\\*")
                   .replace("?", "\\?");
    }
    
    /**
     * Analyze a field path to detect nested paths and extract information
     * Returns null if the path doesn't contain nested fields
     * 
     * Note: This implementation treats dot-notation fields as potentially nested.
     * Fields marked with @Field(type = FieldType.Object) should use regular queries,
     * while @Field(type = FieldType.Nested) requires nested queries.
     * 
     * Since we can't easily introspect field types at runtime without reflection,
     * we use a heuristic: only create nested paths for known nested relationships.
     * For now, we return all paths and rely on Elasticsearch to handle them appropriately.
     */
    private NestedPathInfo analyzeNestedPath(String fieldPath) {
        if (!fieldPath.contains(".")) {
            return null; // Not a nested path
        }
        
        // Common embedded (Object type) fields that should NOT use nested queries
        Set<String> embeddedFields = Set.of("address");
        
        String firstSegment = fieldPath.substring(0, fieldPath.indexOf("."));
        if (embeddedFields.contains(firstSegment)) {
            // This is an embedded object, not a nested document
            // Don't use nested query
            return null;
        }
        
        // For Elasticsearch, we need to identify which part of the path is nested
        // Example: "department.name" - if department is nested
        // Example: "roles.roleAuthorizations.authorization.menuIcon" - multi-level nested
        
        List<String> nestedPaths = new ArrayList<>();
        String[] segments = fieldPath.split("\\.");
        StringBuilder currentPath = new StringBuilder();
        
        for (int i = 0; i < segments.length - 1; i++) {
            if (currentPath.length() > 0) {
                currentPath.append(".");
            }
            currentPath.append(segments[i]);
            
            // Add as potential nested path
            nestedPaths.add(currentPath.toString());
        }
        
        if (nestedPaths.isEmpty()) {
            return null;
        }
        
        return new NestedPathInfo(nestedPaths, fieldPath);
    }
    
    /**
     * Build a nested query for Elasticsearch
     * Handles multi-level nesting like roles.roleAuthorizations.authorization.menuIcon
     */
    private Query buildNestedQuery(NestedPathInfo pathInfo, CriteriaOperator operator, List<Object> values, boolean isLeftJoin) {
        // Build the inner query for the final field
        Query innerQuery = buildFieldQuery(pathInfo.fullPath, operator, values);
        
        if (innerQuery == null) {
            return null;
        }
        
        // Wrap in nested queries from innermost to outermost
        // For "roles.roleAuthorizations.authorization.menuIcon", we need:
        // nested(roles.roleAuthorizations.authorization, nested(roles.roleAuthorizations, nested(roles, query)))
        
        List<String> nestedPaths = pathInfo.nestedPaths;
        Query currentQuery = innerQuery;
        
        // Start from the deepest nested path and work outward
        for (int i = nestedPaths.size() - 1; i >= 0; i--) {
            String nestedPath = nestedPaths.get(i);
            
            // Use None mode for child score
            ChildScoreMode scoreMode = ChildScoreMode.None;
            
            if (isLeftJoin) {
                // For left joins, we want to include documents even if the nested path doesn't exist
                // Wrap in a bool query with should
                Query finalCurrentQuery = currentQuery;
                currentQuery = Query.of(q -> q.bool(b -> b
                    .should(Query.of(sq -> sq.nested(n -> n
                        .path(nestedPath)
                        .query(finalCurrentQuery)
                        .scoreMode(scoreMode))))
                    .should(Query.of(sq -> sq.bool(bq -> bq
                        .mustNot(Query.of(mq -> mq.exists(e -> e.field(nestedPath)))))))
                    .minimumShouldMatch("1")));
            } else {
                // Inner join - use nested query directly
                Query finalCurrentQuery = currentQuery;
                currentQuery = Query.of(q -> q.nested(n -> n
                    .path(nestedPath)
                    .query(finalCurrentQuery)
                    .scoreMode(scoreMode)));
            }
        }
        
        return currentQuery;
    }
    
    /**
     * Build a query for a single field (used within nested queries)
     * This is similar to buildCriteriaQuery but for a specific field without nesting logic
     */
    private Query buildFieldQuery(String fieldName, CriteriaOperator operator, List<Object> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        
        switch (operator) {
            case EQUAL:
                if (values.size() > 1) {
                    List<Query> shouldQueries = new ArrayList<>();
                    for (Object value : values) {
                        Object finalValue = value;
                        shouldQueries.add(Query.of(q -> q.matchPhrase(mp -> mp.field(fieldName).query(finalValue.toString()))));
                    }
                    return Query.of(q -> q.bool(b -> b
                        .should(shouldQueries)
                        .minimumShouldMatch("1")));
                } else {
                    Object finalValue = values.get(0);
                    return Query.of(q -> q.matchPhrase(mp -> mp.field(fieldName).query(finalValue.toString())));
                }
                
            case NOT_EQUAL:
                if (values.size() > 1) {
                    List<Query> mustNotQueries = new ArrayList<>();
                    for (Object value : values) {
                        Object finalValue = value;
                        mustNotQueries.add(Query.of(q -> q.matchPhrase(mp -> mp.field(fieldName).query(finalValue.toString()))));
                    }
                    return Query.of(q -> q.bool(b -> b
                        .must(Query.of(mq -> mq.exists(e -> e.field(fieldName))))
                        .mustNot(mustNotQueries)));
                } else {
                    Object finalValue = values.get(0);
                    return Query.of(q -> q.bool(b -> b
                        .must(Query.of(mq -> mq.exists(e -> e.field(fieldName))))
                        .mustNot(Query.of(mnq -> mnq.matchPhrase(mp -> mp.field(fieldName).query(finalValue.toString()))))));
                }
                
            case CONTAIN:
                if (values.size() > 1) {
                    List<Query> shouldQueries = new ArrayList<>();
                    for (Object value : values) {
                        String wildcardPattern = "*" + escapeWildcard(value.toString()).toLowerCase() + "*";
                        shouldQueries.add(Query.of(q -> q.wildcard(w -> w
                            .field(fieldName)
                            .value(wildcardPattern)
                            .caseInsensitive(true))));
                    }
                    return Query.of(q -> q.bool(b -> b
                        .should(shouldQueries)
                        .minimumShouldMatch("1")));
                } else {
                    String wildcardPattern = "*" + escapeWildcard(values.get(0).toString()).toLowerCase() + "*";
                    return Query.of(q -> q.wildcard(w -> w
                        .field(fieldName)
                        .value(wildcardPattern)
                        .caseInsensitive(true)));
                }
                
            case DOES_NOT_CONTAIN:
                if (values.size() > 1) {
                    List<Query> mustNotQueries = new ArrayList<>();
                    for (Object value : values) {
                        String wildcardPattern = "*" + escapeWildcard(value.toString()).toLowerCase() + "*";
                        mustNotQueries.add(Query.of(q -> q.wildcard(w -> w
                            .field(fieldName)
                            .value(wildcardPattern)
                            .caseInsensitive(true))));
                    }
                    return Query.of(q -> q.bool(b -> b
                        .must(Query.of(mq -> mq.exists(e -> e.field(fieldName))))
                        .mustNot(mustNotQueries)));
                } else {
                    String wildcardPattern = "*" + escapeWildcard(values.get(0).toString()).toLowerCase() + "*";
                    return Query.of(q -> q.bool(b -> b
                        .must(Query.of(mq -> mq.exists(e -> e.field(fieldName))))
                        .mustNot(Query.of(mnq -> mnq.wildcard(w -> w
                            .field(fieldName)
                            .value(wildcardPattern)
                            .caseInsensitive(true))))));
                }
                
            case START_WITH:
                if (values.size() > 1) {
                    List<Query> shouldQueries = new ArrayList<>();
                    for (Object value : values) {
                        String prefixValue = value.toString().toLowerCase();
                        shouldQueries.add(Query.of(q -> q.prefix(p -> p
                            .field(fieldName)
                            .value(prefixValue)
                            .caseInsensitive(true))));
                    }
                    return Query.of(q -> q.bool(b -> b
                        .should(shouldQueries)
                        .minimumShouldMatch("1")));
                } else {
                    String prefixValue = values.get(0).toString().toLowerCase();
                    return Query.of(q -> q.prefix(p -> p
                        .field(fieldName)
                        .value(prefixValue)
                        .caseInsensitive(true)));
                }
                
            case END_WITH:
                if (values.size() > 1) {
                    List<Query> shouldQueries = new ArrayList<>();
                    for (Object value : values) {
                        String wildcardPattern = "*" + escapeWildcard(value.toString()).toLowerCase();
                        shouldQueries.add(Query.of(q -> q.wildcard(w -> w
                            .field(fieldName)
                            .value(wildcardPattern)
                            .caseInsensitive(true))));
                    }
                    return Query.of(q -> q.bool(b -> b
                        .should(shouldQueries)
                        .minimumShouldMatch("1")));
                } else {
                    String wildcardPattern = "*" + escapeWildcard(values.get(0).toString()).toLowerCase();
                    return Query.of(q -> q.wildcard(w -> w
                        .field(fieldName)
                        .value(wildcardPattern)
                        .caseInsensitive(true)));
                }
                
            case GREATER_THAN:
                return Query.of(q -> q.range(r -> r.field(fieldName).gt(asJsonData(values.get(0)))));
                
            case GREATER_THAN_OR_EQUAL:
                return Query.of(q -> q.range(r -> r.field(fieldName).gte(asJsonData(values.get(0)))));
                
            case LESS_THAN:
                return Query.of(q -> q.range(r -> r.field(fieldName).lt(asJsonData(values.get(0)))));
                
            case LESS_THAN_OR_EQUAL:
                return Query.of(q -> q.range(r -> r.field(fieldName).lte(asJsonData(values.get(0)))));
                
            case SPECIFIED:
                boolean exists = Boolean.parseBoolean(values.get(0).toString());
                if (exists) {
                    return Query.of(q -> q.exists(e -> e.field(fieldName)));
                } else {
                    return Query.of(q -> q.bool(b -> b
                        .mustNot(Query.of(mq -> mq.exists(e -> e.field(fieldName))))));
                }
                
            default:
                throw new DynamicQueryIllegalArgumentException("Unsupported operator: " + operator);
        }
    }
    
    /**
     * Extract field mappings from @JdqModel annotated class
     */
    private <ResultType> void extractIfJdqModel(DynamicQuery dynamicQuery, Class<ResultType> resultTypeClass) {
        if (!resultTypeClass.isAnnotationPresent(JdqModel.class)) {
            return;
        }

        List<Pair<String, String>> select = new ArrayList<>();
        recursiveSubModelFiller(resultTypeClass, select, new ArrayList<>(), "");
        dynamicQuery.setSelect(select);
    }

    /**
     * Recursively process @JdqSubModel and @JdqField annotations
     */
    private <ResultType> void recursiveSubModelFiller(Class<ResultType> resultTypeClass, 
                                                      List<Pair<String, String>> select, 
                                                      List<String> dbPrefixList, 
                                                      String entityPrefix) {
        Field[] declaredFields;
        if (resultTypeClass.isRecord()) {
            RecordComponent[] recordComponents = resultTypeClass.getRecordComponents();
            declaredFields = new Field[recordComponents.length];
            for (int i = 0; i < recordComponents.length; i++) {
                try {
                    declaredFields[i] = resultTypeClass.getDeclaredField(recordComponents[i].getName());
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            declaredFields = resultTypeClass.getDeclaredFields();
        }

        for (Field declaredField : declaredFields) {
            if (declaredField.isAnnotationPresent(JdqSubModel.class)) {
                JdqSubModel annotation = declaredField.getAnnotation(JdqSubModel.class);
                String subModelValue = annotation.value();
                
                List<String> newPrefixList = new ArrayList<>(dbPrefixList);
                if (StringUtils.isNotBlank(subModelValue)) {
                    newPrefixList.add(subModelValue);
                }
                recursiveSubModelFiller(declaredField.getType(), select, newPrefixList, 
                    entityPrefix + declaredField.getName() + ".");
            } else if (FieldUtil.isSupportedType(declaredField.getType())) {
                if (declaredField.isAnnotationPresent(JdqIgnoreField.class)) {
                    if (resultTypeClass.isRecord()) {
                        throw new DynamicQueryIllegalArgumentException("Record class can not have @JdqIgnoreField annotation");
                    }
                    continue;
                }

                if (declaredField.isAnnotationPresent(JdqField.class)) {
                    select.add(Pair.of(
                        prefixCreator(dbPrefixList) + declaredField.getAnnotation(JdqField.class).value(), 
                        entityPrefix + declaredField.getName()
                    ));
                } else {
                    select.add(Pair.of(
                        prefixCreator(dbPrefixList) + declaredField.getName(),
                        entityPrefix + declaredField.getName()
                    ));
                }
            } else {
                if (resultTypeClass.isRecord()) {
                    throw new DynamicQueryIllegalArgumentException("Record didnt support nested model type: " + declaredField.getType().getName());
                }
            }
        }
    }

    private String prefixCreator(List<String> prefixList) {
        String collect = String.join(".", prefixList);
        if (StringUtils.isNotBlank(collect)) {
            collect += ".";
        }
        return collect;
    }

    /**
     * Convert entity to result type using field mappings from DynamicQuery select
     */
    private <Entity, ResultType> ResultType convertEntityToResultType(
            Entity entity, 
            Class<ResultType> resultClass, 
            DynamicQuery dynamicQuery) {
        try {
            if (CollectionUtils.isEmpty(dynamicQuery.getSelect())) {
                // No projection specified, try to cast
                if (resultClass.isAssignableFrom(entity.getClass())) {
                    return resultClass.cast(entity);
                }
                return null;
            }

            // Build a map of field names to values from the entity
            Map<String, Object> fieldValues = new HashMap<>();
            for (Pair<String, String> selectPair : dynamicQuery.getSelect()) {
                String sourceField = selectPair.getFirst();   // Entity field path (e.g., "name", "department.name")
                String targetField = selectPair.getSecond();  // Result class field name
                
                // Get value from entity using reflection
                Object value = getFieldValue(entity, sourceField);
                fieldValues.put(targetField, value);
            }

            return createInstance(resultClass, fieldValues, dynamicQuery);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get field value from entity using reflection, supports nested paths
     */
    private Object getFieldValue(Object entity, String fieldPath) {
        try {
            String[] parts = fieldPath.split("\\.");
            Object current = entity;
            
            for (String part : parts) {
                if (current == null) {
                    return null;
                }
                
                // Try to get field value
                Field field = findField(current.getClass(), part);
                if (field != null) {
                    field.setAccessible(true);
                    current = field.get(current);
                } else {
                    // Try getter method
                    String getterName = "get" + part.substring(0, 1).toUpperCase() + part.substring(1);
                    try {
                        Method getter = current.getClass().getMethod(getterName);
                        current = getter.invoke(current);
                    } catch (NoSuchMethodException e) {
                        return null;
                    }
                }
            }
            
            return current;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Find field in class hierarchy
     */
    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Create instance of result class using field values
     */
    private <ResultType> ResultType createInstance(
            Class<ResultType> resultClass, 
            Map<String, Object> fieldValues,
            DynamicQuery dynamicQuery) throws Exception {
        
        if (resultClass.isRecord()) {
            return createRecordInstance(resultClass, fieldValues, dynamicQuery);
        } else {
            return createClassInstance(resultClass, fieldValues, dynamicQuery);
        }
    }

    /**
     * Create record instance using constructor
     */
    private <ResultType> ResultType createRecordInstance(
            Class<ResultType> resultClass, 
            Map<String, Object> fieldValues,
            DynamicQuery dynamicQuery) throws Exception {
        
        RecordComponent[] components = resultClass.getRecordComponents();
        Class<?>[] paramTypes = new Class<?>[components.length];
        Object[] args = new Object[components.length];
        
        for (int i = 0; i < components.length; i++) {
            paramTypes[i] = components[i].getType();
            String fieldName = components[i].getName();
            
            // Get the field to check for @JdqSubModel annotation
            Field field = resultClass.getDeclaredField(fieldName);
            
            // Check if it's a sub-model
            if (field.isAnnotationPresent(JdqSubModel.class)) {
                // Recursively create sub-model
                Map<String, Object> subFieldValues = new HashMap<>();
                String prefix = fieldName + ".";
                for (Map.Entry<String, Object> entry : fieldValues.entrySet()) {
                    if (entry.getKey().startsWith(prefix)) {
                        subFieldValues.put(entry.getKey().substring(prefix.length()), entry.getValue());
                    }
                }
                args[i] = createInstance(components[i].getType(), subFieldValues, dynamicQuery);
            } else {
                Object value = fieldValues.get(fieldName);
                args[i] = convertValue(value, components[i].getType());
            }
        }
        
        Constructor<ResultType> constructor = resultClass.getDeclaredConstructor(paramTypes);
        return constructor.newInstance(args);
    }

    /**
     * Create class instance using no-arg constructor and setters
     */
    private <ResultType> ResultType createClassInstance(
            Class<ResultType> resultClass, 
            Map<String, Object> fieldValues,
            DynamicQuery dynamicQuery) throws Exception {
        
        ResultType instance = resultClass.getDeclaredConstructor().newInstance();
        
        for (Field field : resultClass.getDeclaredFields()) {
            String fieldName = field.getName();
            
            // Check if it's a sub-model
            if (field.isAnnotationPresent(JdqSubModel.class)) {
                Map<String, Object> subFieldValues = new HashMap<>();
                String prefix = fieldName + ".";
                for (Map.Entry<String, Object> entry : fieldValues.entrySet()) {
                    if (entry.getKey().startsWith(prefix)) {
                        subFieldValues.put(entry.getKey().substring(prefix.length()), entry.getValue());
                    }
                }
                Object subModel = createInstance(field.getType(), subFieldValues, dynamicQuery);
                field.setAccessible(true);
                field.set(instance, subModel);
            } else if (fieldValues.containsKey(fieldName)) {
                Object value = fieldValues.get(fieldName);
                Object convertedValue = convertValue(value, field.getType());
                
                // Find setter method
                String setterName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                try {
                    Method setter = resultClass.getMethod(setterName, field.getType());
                    setter.invoke(instance, convertedValue);
                } catch (NoSuchMethodException e) {
                    // Try direct field access
                    field.setAccessible(true);
                    field.set(instance, convertedValue);
                }
            }
        }
        
        return instance;
    }
    
    /**
     * Convert value to target type
     */
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
        // If types already match, return as is
        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }
        
        // Handle numeric conversions
        if (value instanceof Number) {
            Number numValue = (Number) value;
            if (targetType == Integer.class || targetType == int.class) {
                return numValue.intValue();
            } else if (targetType == Long.class || targetType == long.class) {
                return numValue.longValue();
            } else if (targetType == Double.class || targetType == double.class) {
                return numValue.doubleValue();
            } else if (targetType == Float.class || targetType == float.class) {
                return numValue.floatValue();
            } else if (targetType == Short.class || targetType == short.class) {
                return numValue.shortValue();
            } else if (targetType == Byte.class || targetType == byte.class) {
                return numValue.byteValue();
            }
        }
        
        // Handle Date/Instant conversions
        if (value instanceof java.util.Date && targetType == java.time.Instant.class) {
            return ((java.util.Date) value).toInstant();
        }
        if (value instanceof java.time.Instant && targetType == java.util.Date.class) {
            return java.util.Date.from((java.time.Instant) value);
        }
        
        // Handle String conversions
        if (targetType == String.class) {
            return value.toString();
        }
        
        // Handle enum conversions
        if (targetType.isEnum() && value instanceof String) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object enumValue = Enum.valueOf((Class<? extends Enum>) targetType, (String) value);
            return enumValue;
        }
        
        // Default: return as is and let reflection handle it
        return value;
    }

    /**
     * Check if any criteria in the where clause references a nested field
     */
    private boolean hasNestedFieldInWhere(DynamicQuery dynamicQuery) {
        if (CollectionUtils.isEmpty(dynamicQuery.getWhere())) {
            return false;
        }
        
        for (com.beyt.jdq.core.model.Criteria criteria : dynamicQuery.getWhere()) {
            if (criteria.getKey() != null && criteria.getKey().contains(".")) {
                return true;
            }
            // Check for PARENTHES operator with nested criteria
            if (criteria.getOperation() == CriteriaOperator.PARENTHES && 
                criteria.getValues() != null && !criteria.getValues().isEmpty() &&
                criteria.getValues().get(0) instanceof List) {
                @SuppressWarnings("unchecked")
                List<com.beyt.jdq.core.model.Criteria> nestedCriteria = 
                    (List<com.beyt.jdq.core.model.Criteria>) criteria.getValues().get(0);
                if (hasNestedFieldInWhere(DynamicQuery.of(nestedCriteria))) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Count how many nested documents match the criteria
     * This simulates SQL join behavior where each matching nested document creates a result row
     */
    private <Entity> int countMatchingNestedDocuments(Entity entity, DynamicQuery dynamicQuery) {
        if (CollectionUtils.isEmpty(dynamicQuery.getWhere())) {
            return 1;
        }
        
        int maxMatches = 1;
        
        for (com.beyt.jdq.core.model.Criteria criteria : dynamicQuery.getWhere()) {
            if (criteria.getKey() == null || !criteria.getKey().contains(".")) {
                continue;
            }
            
            // Find the first collection in the path
            String[] parts = criteria.getKey().split("\\.");
            StringBuilder pathBuilder = new StringBuilder();
            
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) {
                    pathBuilder.append(".");
                }
                pathBuilder.append(parts[i]);
                
                Object value = getFieldValue(entity, pathBuilder.toString());
                if (value instanceof List) {
                    List<?> list = (List<?>) value;
                    // Count how many items in this list match the criteria
                    int matches = 0;
                    String remainingPath = criteria.getKey().substring(pathBuilder.length() + (pathBuilder.length() > 0 ? 1 : 0));
                    
                    for (Object item : list) {
                        if (matchesCriteria(item, remainingPath, criteria)) {
                            matches++;
                        }
                    }
                    
                    if (matches > maxMatches) {
                        maxMatches = matches;
                    }
                    break;
                }
            }
        }
        
        return maxMatches;
    }
    
    /**
     * Check if an object matches a criteria
     */
    private boolean matchesCriteria(Object obj, String path, com.beyt.jdq.core.model.Criteria criteria) {
        Object value = path.isEmpty() ? obj : getFieldValue(obj, path);
        
        if (criteria.getValues() == null || criteria.getValues().isEmpty()) {
            return false;
        }
        
        Object expectedValue = criteria.getValues().get(0);
        
        switch (criteria.getOperation()) {
            case EQUAL:
                return compareValues(value, expectedValue) == 0;
            case NOT_EQUAL:
                return compareValues(value, expectedValue) != 0;
            case GREATER_THAN:
                return compareValues(value, expectedValue) > 0;
            case GREATER_THAN_OR_EQUAL:
                return compareValues(value, expectedValue) >= 0;
            case LESS_THAN:
                return compareValues(value, expectedValue) < 0;
            case LESS_THAN_OR_EQUAL:
                return compareValues(value, expectedValue) <= 0;
            case CONTAIN:
                return value != null && value.toString().contains(expectedValue.toString());
            case DOES_NOT_CONTAIN:
                return value == null || !value.toString().contains(expectedValue.toString());
            case START_WITH:
                return value != null && value.toString().startsWith(expectedValue.toString());
            case END_WITH:
                return value != null && value.toString().endsWith(expectedValue.toString());
            case SPECIFIED:
                boolean shouldExist = Boolean.parseBoolean(expectedValue.toString());
                return shouldExist ? value != null : value == null;
            default:
                return false;
        }
    }
    
    /**
     * Compare two values, handling numeric type conversions
     */
    private int compareValues(Object value1, Object value2) {
        if (value1 == null && value2 == null) {
            return 0;
        }
        if (value1 == null) {
            return -1;
        }
        if (value2 == null) {
            return 1;
        }
        
        // Handle numeric comparisons with type conversion
        if (value1 instanceof Number && value2 instanceof Number) {
            double d1 = ((Number) value1).doubleValue();
            double d2 = ((Number) value2).doubleValue();
            return Double.compare(d1, d2);
        }
        
        // For other comparables, try direct comparison
        if (value1 instanceof Comparable && value2 instanceof Comparable) {
            try {
                @SuppressWarnings("unchecked")
                Comparable<Object> c1 = (Comparable<Object>) value1;
                return c1.compareTo(value2);
            } catch (ClassCastException e) {
                // If types don't match, compare as strings
                return value1.toString().compareTo(value2.toString());
            }
        }
        
        // Fall back to equals
        return Objects.equals(value1, value2) ? 0 : value1.hashCode() - value2.hashCode();
    }

    /**
     * Convert entity to result type with flattening for nested collections
     * This handles cases where we project nested fields like "roles.id" which need to be flattened
     */
    private <Entity, ResultType> List<ResultType> convertEntityToResultTypeWithFlattening(
            Entity entity,
            Class<ResultType> resultClass,
            DynamicQuery dynamicQuery) {
        
        // Check if we need flattening (any select contains a nested collection)
        boolean needsFlattening = needsFlattening(entity, dynamicQuery);
        
        if (!needsFlattening) {
            // No flattening needed, convert directly
            ResultType result = convertEntityToResultType(entity, resultClass, dynamicQuery);
            return result != null ? List.of(result) : List.of();
        }
        
        // We need to flatten - find the first collection in the projection path
        List<ResultType> results = new ArrayList<>();
        flattenAndConvert(entity, resultClass, dynamicQuery, results, new HashMap<>(), "", 0);
        return results;
    }
    
    /**
     * Check if flattening is needed based on whether any select path contains a collection
     */
    private <Entity> boolean needsFlattening(Entity entity, DynamicQuery dynamicQuery) {
        if (CollectionUtils.isEmpty(dynamicQuery.getSelect())) {
            return false;
        }
        
        for (Pair<String, String> selectPair : dynamicQuery.getSelect()) {
            String sourcePath = selectPair.getFirst();
            if (pathContainsCollection(entity, sourcePath)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if a path contains a collection at any level
     */
    private boolean pathContainsCollection(Object obj, String path) {
        if (obj == null) {
            return false;
        }
        
        String[] parts = path.split("\\.");
        Object current = obj;
        
        for (int i = 0; i < parts.length - 1; i++) {
            if (current == null) {
                return false;
            }
            
            Field field = findField(current.getClass(), parts[i]);
            if (field != null) {
                field.setAccessible(true);
                try {
                    Object value = field.get(current);
                    if (value instanceof List) {
                        return true;
                    }
                    current = value;
                } catch (Exception e) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return false;
    }
    
    /**
     * Recursively flatten nested collections and convert to result type
     * 
     * @param currentEntity The current entity to extract values from
     * @param resultClass The target result class
     * @param dynamicQuery The original dynamic query with full paths
     * @param results The list to accumulate results
     * @param accumulatedValues Values accumulated from parent levels
     * @param pathPrefix The path prefix consumed so far (e.g., "roles" or "roles.roleAuthorizations")
     * @param depth Recursion depth to prevent infinite loops
     */
    @SuppressWarnings("unchecked")
    private <Entity, ResultType> void flattenAndConvert(
            Object currentEntity,
            Class<ResultType> resultClass,
            DynamicQuery dynamicQuery,
            List<ResultType> results,
            Map<String, Object> accumulatedValues,
            String pathPrefix,
            int depth) {
        
        if (depth > 10) {
            // Prevent infinite recursion
            return;
        }
        
        // Build the full prefix for this level
        String currentPrefix = pathPrefix.isEmpty() ? pathPrefix : pathPrefix + ".";
        
        // Find the first collection field relative to current entity
        String collectionFieldName = findFirstCollectionInEntity(currentEntity);
        
        if (collectionFieldName == null) {
            // No more collections, extract all remaining values and create result
            Map<String, Object> fieldValues = new HashMap<>(accumulatedValues);
            
            for (Pair<String, String> selectPair : dynamicQuery.getSelect()) {
                String sourcePath = selectPair.getFirst();
                String targetField = selectPair.getSecond();
                
                if (!fieldValues.containsKey(targetField)) {
                    // Check if this path is meant for the current level
                    if (pathPrefix.isEmpty() || sourcePath.startsWith(pathPrefix + ".")) {
                        String relativePath = pathPrefix.isEmpty() ? sourcePath : sourcePath.substring(pathPrefix.length() + 1);
                        Object value = getFieldValue(currentEntity, relativePath);
                        fieldValues.put(targetField, value);
                    }
                }
            }
            
            try {
                ResultType result = createInstance(resultClass, fieldValues, dynamicQuery);
                if (result != null) {
                    results.add(result);
                }
            } catch (Exception e) {
                // Ignore errors
            }
            return;
        }
        
        // Get the collection value
        Object collectionValue = getFieldValue(currentEntity, collectionFieldName);
        String newPathPrefix = currentPrefix + collectionFieldName;
        
        if (collectionValue instanceof List) {
            List<?> list = (List<?>) collectionValue;
            if (list.isEmpty()) {
                // Empty collection - create one result with null values for collection fields
                Map<String, Object> fieldValues = new HashMap<>(accumulatedValues);
                
                for (Pair<String, String> selectPair : dynamicQuery.getSelect()) {
                    String sourcePath = selectPair.getFirst();
                    String targetField = selectPair.getSecond();
                    
                    if (!fieldValues.containsKey(targetField)) {
                        if (sourcePath.startsWith(newPathPrefix + ".")) {
                            fieldValues.put(targetField, null);
                        } else if (pathPrefix.isEmpty() || sourcePath.startsWith(pathPrefix + ".")) {
                            String relativePath = pathPrefix.isEmpty() ? sourcePath : sourcePath.substring(pathPrefix.length() + 1);
                            Object value = getFieldValue(currentEntity, relativePath);
                            fieldValues.put(targetField, value);
                        }
                    }
                }
                
                try {
                    ResultType result = createInstance(resultClass, fieldValues, dynamicQuery);
                    if (result != null) {
                        results.add(result);
                    }
                } catch (Exception e) {
                    // Ignore errors
                }
            } else {
                // Iterate through collection items
                for (Object item : list) {
                    Map<String, Object> newAccumulated = new HashMap<>(accumulatedValues);
                    
                    // Extract non-nested values from this collection item
                    for (Pair<String, String> selectPair : dynamicQuery.getSelect()) {
                        String sourcePath = selectPair.getFirst();
                        String targetField = selectPair.getSecond();
                        
                        // Skip if already accumulated
                        if (newAccumulated.containsKey(targetField)) {
                            continue;
                        }
                        
                        // Check if this path belongs to current or parent level
                        if (sourcePath.startsWith(newPathPrefix + ".")) {
                            // This is from a deeper level, will be handled in recursion
                            continue;
                        } else if (pathPrefix.isEmpty() || sourcePath.startsWith(pathPrefix + ".")) {
                            String relativePath = pathPrefix.isEmpty() ? sourcePath : sourcePath.substring(pathPrefix.length() + 1);
                            
                            // Check if this is from the current collection or a parent
                            if (!relativePath.contains(".") || !relativePath.startsWith(collectionFieldName + ".")) {
                                // From parent level
                                Object value = getFieldValue(currentEntity, relativePath);
                                newAccumulated.put(targetField, value);
                            } else {
                                // From current collection level
                                String itemRelativePath = relativePath.substring(collectionFieldName.length() + 1);
                                
                                // Check if it goes deeper
                                if (pathContainsCollection(item, itemRelativePath)) {
                                    // Has nested collection, will be handled in recursion
                                    continue;
                                } else {
                                    // Direct field from collection item
                                    Object value = getFieldValue(item, itemRelativePath);
                                    newAccumulated.put(targetField, value);
                                }
                            }
                        }
                    }
                    
                    // Recurse with the collection item as the new current entity
                    flattenAndConvert(item, resultClass, dynamicQuery, results, newAccumulated, newPathPrefix, depth + 1);
                }
            }
        }
    }
    
    /**
     * Find the first collection field in the current entity
     */
    private String findFirstCollectionInEntity(Object entity) {
        if (entity == null) {
            return null;
        }
        
        Field[] fields = entity.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(entity);
                if (value instanceof List) {
                    return field.getName();
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        
        return null;
    }
    
    /**
     * Find the first collection path in the select that hasn't been processed yet
     * This method finds collection paths relative to the current entity
     */
    private String findFirstCollectionPath(Object entity, DynamicQuery dynamicQuery, java.util.Set<String> processedTargets) {
        if (CollectionUtils.isEmpty(dynamicQuery.getSelect())) {
            return null;
        }
        
        // Group paths by their prefix to find the shortest collection path
        java.util.Set<String> checkedPaths = new HashSet<>();
        String shortestCollectionPath = null;
        
        for (Pair<String, String> selectPair : dynamicQuery.getSelect()) {
            String targetField = selectPair.getSecond();
            
            // Skip if we've already extracted this target
            if (processedTargets.contains(targetField)) {
                continue;
            }
            
            // Try to find a collection in the entity starting from the root
            String collectionPath = findCollectionInPath(entity, targetField, checkedPaths);
            
            if (collectionPath != null) {
                // Use the shortest collection path (first level of nesting)
                if (shortestCollectionPath == null || collectionPath.length() < shortestCollectionPath.length()) {
                    shortestCollectionPath = collectionPath;
                }
            }
        }
        
        return shortestCollectionPath;
    }
    
    /**
     * Find the first collection in a path from the entity
     */
    private String findCollectionInPath(Object entity, String targetField, java.util.Set<String> checkedPaths) {
        if (entity == null) {
            return null;
        }
        
        // Get all field names from entity
        Field[] fields = entity.getClass().getDeclaredFields();
        
        for (Field field : fields) {
            String fieldName = field.getName();
            
            if (checkedPaths.contains(fieldName)) {
                continue;
            }
            
            field.setAccessible(true);
            try {
                Object value = field.get(entity);
                if (value instanceof List) {
                    checkedPaths.add(fieldName);
                    return fieldName;
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        
        return null;
    }

    /**
     * Helper class to store nested path information
     */
    private static class NestedPathInfo {
        private final List<String> nestedPaths;  // e.g., ["roles", "roles.roleAuthorizations", "roles.roleAuthorizations.authorization"]
        private final String fullPath;            // e.g., "roles.roleAuthorizations.authorization.menuIcon"
        
        public NestedPathInfo(List<String> nestedPaths, String fullPath) {
            this.nestedPaths = nestedPaths;
            this.fullPath = fullPath;
        }
        
        public boolean hasNestedPath() {
            return nestedPaths != null && !nestedPaths.isEmpty();
        }
    }
}

