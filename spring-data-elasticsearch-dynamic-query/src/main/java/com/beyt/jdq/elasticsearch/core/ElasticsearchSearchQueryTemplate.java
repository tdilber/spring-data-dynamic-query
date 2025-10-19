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
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.util.CollectionUtils;
import org.springframework.data.util.Pair;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.lucene.search.join.ScoreMode;
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
        NativeSearchQuery query = prepareQuery(entityClass, dynamicQuery);
        SearchHits<Entity> searchHits = elasticsearchOperations.search(query, entityClass);
        return searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    /**
     * Find all entities with projection matching the dynamic query
     */
    public <Entity, ResultType> List<ResultType> findAll(Class<Entity> entityClass, DynamicQuery dynamicQuery, Class<ResultType> resultClass) {
        // Extract @JdqModel annotations if present
        extractIfJdqModel(dynamicQuery, resultClass);
        
        // Check if projection is needed
        if (CollectionUtils.isEmpty(dynamicQuery.getSelect()) && entityClass.equals(resultClass)) {
            // No projection, just cast
            List<Entity> results = findAll(entityClass, dynamicQuery);
            return results.stream()
                    .map(entity -> (ResultType) entity)
                    .collect(Collectors.toList());
        }
        
        // Execute query and convert to result type
        NativeSearchQuery query = prepareQuery(entityClass, dynamicQuery);
        SearchHits<Entity> searchHits = elasticsearchOperations.search(query, entityClass);
        
        List<ResultType> results = searchHits.stream()
                .map(SearchHit::getContent)
                .map(entity -> convertEntityToResultType(entity, resultClass, dynamicQuery))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        // Handle distinct if needed
        if (dynamicQuery.isDistinct()) {
            results = results.stream().distinct().collect(Collectors.toList());
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
        NativeSearchQuery query = prepareQuery(entityClass, dynamicQuery);
        SearchHits<Entity> searchHits = elasticsearchOperations.search(query, entityClass);
        
        return new org.springframework.data.domain.PageImpl<>(
                searchHits.stream().map(SearchHit::getContent).collect(Collectors.toList()),
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
        if (CollectionUtils.isEmpty(dynamicQuery.getSelect()) && entityClass.equals(resultClass)) {
            // No projection, just cast
            Page<Entity> page = findAllAsPage(entityClass, dynamicQuery);
            return page.map(entity -> (ResultType) entity);
        }
        
        // Execute query and convert to result type
        NativeSearchQuery query = prepareQuery(entityClass, dynamicQuery);
        SearchHits<Entity> searchHits = elasticsearchOperations.search(query, entityClass);
        
        List<ResultType> results = searchHits.stream()
                .map(SearchHit::getContent)
                .map(entity -> convertEntityToResultType(entity, resultClass, dynamicQuery))
                .filter(Objects::nonNull)
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
     * Count entities matching the criteria list
     */
    public <Entity> long count(Class<Entity> entityClass, List<com.beyt.jdq.core.model.Criteria> searchCriteriaList) {
        DynamicQuery dynamicQuery = DynamicQuery.of(searchCriteriaList);
        NativeSearchQuery query = prepareQuery(entityClass, dynamicQuery);
        SearchHits<Entity> searchHits = elasticsearchOperations.search(query, entityClass);
        return searchHits.getTotalHits();
    }

    /**
     * Prepare NativeSearchQuery from DynamicQuery
     */
    private <Entity> NativeSearchQuery prepareQuery(Class<Entity> entityClass, DynamicQuery dynamicQuery) {
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        
        // Build the main query from criteria
        QueryBuilder mainQuery = buildQueryFromCriteria(dynamicQuery.getWhere());
        queryBuilder.withQuery(mainQuery);
        
        // Add sorting
        if (dynamicQuery.getOrderBy() != null && !dynamicQuery.getOrderBy().isEmpty()) {
            for (Pair<String, com.beyt.jdq.core.model.enums.Order> orderPair : dynamicQuery.getOrderBy()) {
                Sort.Direction direction = orderPair.getSecond() == com.beyt.jdq.core.model.enums.Order.ASC 
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
                queryBuilder.withSort(Sort.by(direction, orderPair.getFirst()));
            }
        }
        
        // Add pagination
        int pageNumber = dynamicQuery.getPageNumber() != null ? dynamicQuery.getPageNumber() : 0;
        int pageSize = dynamicQuery.getPageSize() != null ? dynamicQuery.getPageSize() : 20;
        queryBuilder.withPageable(PageRequest.of(pageNumber, pageSize));
        
        return queryBuilder.build();
    }

    /**
     * Build Elasticsearch QueryBuilder from criteria list
     */
    private QueryBuilder buildQueryFromCriteria(List<com.beyt.jdq.core.model.Criteria> criteriaList) {
        if (criteriaList == null || criteriaList.isEmpty()) {
            return QueryBuilders.matchAllQuery();
        }

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
        BoolQueryBuilder orQuery = QueryBuilders.boolQuery();
        for (List<com.beyt.jdq.core.model.Criteria> group : orGroups) {
            QueryBuilder groupQuery = buildAndQuery(group);
            orQuery.should(groupQuery);
        }
        orQuery.minimumShouldMatch(1);
        
        return orQuery;
    }
    
    /**
     * Build AND query from criteria list (no OR operators)
     */
    private QueryBuilder buildAndQuery(List<com.beyt.jdq.core.model.Criteria> criteriaList) {
        if (criteriaList.isEmpty()) {
            return QueryBuilders.matchAllQuery();
        }
        
        if (criteriaList.size() == 1) {
            return buildCriteriaQuery(criteriaList.get(0));
        }
        
        BoolQueryBuilder andQuery = QueryBuilders.boolQuery();
        for (com.beyt.jdq.core.model.Criteria criteria : criteriaList) {
            QueryBuilder criteriaQuery = buildCriteriaQuery(criteria);
            if (criteriaQuery != null) {
                andQuery.must(criteriaQuery);
            }
        }
        
        return andQuery;
    }

    /**
     * Build individual criteria query
     * Handles both regular fields and nested fields
     */
    @SuppressWarnings("unchecked")
    private QueryBuilder buildCriteriaQuery(com.beyt.jdq.core.model.Criteria criteria) {
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
                    return QueryBuilders.existsQuery(normalizedFieldName);
                } else {
                    // Check if the nested field doesn't exist (meaning the parent nested object is null)
                    return QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(normalizedFieldName));
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
                    BoolQueryBuilder orQuery = QueryBuilders.boolQuery();
                    for (Object value : values) {
                        // Use match_phrase for exact matching with analysis
                        orQuery.should(QueryBuilders.matchPhraseQuery(fieldName, value));
                    }
                    orQuery.minimumShouldMatch(1);
                    return orQuery;
                } else {
                    // Single value - use match_phrase for exact phrase matching
                    return QueryBuilders.matchPhraseQuery(fieldName, values.get(0));
                }
                
            case NOT_EQUAL:
                BoolQueryBuilder notEqualQuery = QueryBuilders.boolQuery();
                notEqualQuery.must(QueryBuilders.existsQuery(fieldName)); // Field must exist
                if (values.size() > 1) {
                    // Exclude all specified values
                    for (Object value : values) {
                        notEqualQuery.mustNot(QueryBuilders.matchPhraseQuery(fieldName, value));
                    }
                } else {
                    // Exclude single value
                    notEqualQuery.mustNot(QueryBuilders.matchPhraseQuery(fieldName, values.get(0)));
                }
                return notEqualQuery;
                
            case CONTAIN:
                return QueryBuilders.wildcardQuery(fieldName, "*" + escapeWildcard(values.get(0).toString()).toLowerCase() + "*")
                    .caseInsensitive(true);
                
            case DOES_NOT_CONTAIN:
                return QueryBuilders.boolQuery()
                    .must(QueryBuilders.existsQuery(fieldName)) // Field must exist
                    .mustNot(
                        QueryBuilders.wildcardQuery(fieldName, "*" + escapeWildcard(values.get(0).toString()).toLowerCase() + "*")
                            .caseInsensitive(true)
                    );
                
            case START_WITH:
                return QueryBuilders.prefixQuery(fieldName, values.get(0).toString().toLowerCase())
                    .caseInsensitive(true);
                
            case END_WITH:
                return QueryBuilders.wildcardQuery(fieldName, "*" + escapeWildcard(values.get(0).toString()).toLowerCase())
                    .caseInsensitive(true);
                
            case GREATER_THAN:
                return QueryBuilders.rangeQuery(fieldName).gt(values.get(0));
                
            case GREATER_THAN_OR_EQUAL:
                return QueryBuilders.rangeQuery(fieldName).gte(values.get(0));
                
            case LESS_THAN:
                return QueryBuilders.rangeQuery(fieldName).lt(values.get(0));
                
            case LESS_THAN_OR_EQUAL:
                return QueryBuilders.rangeQuery(fieldName).lte(values.get(0));
                
            case SPECIFIED:
                boolean exists = Boolean.parseBoolean(values.get(0).toString());
                if (exists) {
                    return QueryBuilders.existsQuery(fieldName);
                } else {
                    return QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(fieldName));
                }
                
            default:
                throw new DynamicQueryIllegalArgumentException("Unsupported operator: " + operator);
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
    private QueryBuilder buildNestedQuery(NestedPathInfo pathInfo, CriteriaOperator operator, List<Object> values, boolean isLeftJoin) {
        // Build the inner query for the final field
        QueryBuilder innerQuery = buildFieldQuery(pathInfo.fullPath, operator, values);
        
        if (innerQuery == null) {
            return null;
        }
        
        // Wrap in nested queries from innermost to outermost
        // For "roles.roleAuthorizations.authorization.menuIcon", we need:
        // nested(roles.roleAuthorizations.authorization, nested(roles.roleAuthorizations, nested(roles, query)))
        
        List<String> nestedPaths = pathInfo.nestedPaths;
        QueryBuilder currentQuery = innerQuery;
        
        // Start from the deepest nested path and work outward
        for (int i = nestedPaths.size() - 1; i >= 0; i--) {
            String nestedPath = nestedPaths.get(i);
            
            // Use SCORE mode for better relevance, or AVG for numeric aggregations
            ScoreMode scoreMode = ScoreMode.None;
            
            if (isLeftJoin) {
                // For left joins, we want to include documents even if the nested path doesn't exist
                // Wrap in a bool query with should
                BoolQueryBuilder leftJoinQuery = QueryBuilders.boolQuery();
                leftJoinQuery.should(QueryBuilders.nestedQuery(nestedPath, currentQuery, scoreMode));
                leftJoinQuery.should(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(nestedPath)));
                leftJoinQuery.minimumShouldMatch(1);
                currentQuery = leftJoinQuery;
            } else {
                // Inner join - use nested query directly
                currentQuery = QueryBuilders.nestedQuery(nestedPath, currentQuery, scoreMode);
            }
        }
        
        return currentQuery;
    }
    
    /**
     * Build a query for a single field (used within nested queries)
     * This is similar to buildCriteriaQuery but for a specific field without nesting logic
     */
    private QueryBuilder buildFieldQuery(String fieldName, CriteriaOperator operator, List<Object> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        
        switch (operator) {
            case EQUAL:
                if (values.size() > 1) {
                    BoolQueryBuilder orQuery = QueryBuilders.boolQuery();
                    for (Object value : values) {
                        orQuery.should(QueryBuilders.matchPhraseQuery(fieldName, value));
                    }
                    orQuery.minimumShouldMatch(1);
                    return orQuery;
                } else {
                    return QueryBuilders.matchPhraseQuery(fieldName, values.get(0));
                }
                
            case NOT_EQUAL:
                BoolQueryBuilder notEqualQuery = QueryBuilders.boolQuery();
                notEqualQuery.must(QueryBuilders.existsQuery(fieldName));
                if (values.size() > 1) {
                    for (Object value : values) {
                        notEqualQuery.mustNot(QueryBuilders.matchPhraseQuery(fieldName, value));
                    }
                } else {
                    notEqualQuery.mustNot(QueryBuilders.matchPhraseQuery(fieldName, values.get(0)));
                }
                return notEqualQuery;
                
            case CONTAIN:
                return QueryBuilders.wildcardQuery(fieldName, "*" + escapeWildcard(values.get(0).toString()).toLowerCase() + "*")
                    .caseInsensitive(true);
                
            case DOES_NOT_CONTAIN:
                return QueryBuilders.boolQuery()
                    .must(QueryBuilders.existsQuery(fieldName))
                    .mustNot(
                        QueryBuilders.wildcardQuery(fieldName, "*" + escapeWildcard(values.get(0).toString()).toLowerCase() + "*")
                            .caseInsensitive(true)
                    );
                
            case START_WITH:
                return QueryBuilders.prefixQuery(fieldName, values.get(0).toString().toLowerCase())
                    .caseInsensitive(true);
                
            case END_WITH:
                return QueryBuilders.wildcardQuery(fieldName, "*" + escapeWildcard(values.get(0).toString()).toLowerCase())
                    .caseInsensitive(true);
                
            case GREATER_THAN:
                return QueryBuilders.rangeQuery(fieldName).gt(values.get(0));
                
            case GREATER_THAN_OR_EQUAL:
                return QueryBuilders.rangeQuery(fieldName).gte(values.get(0));
                
            case LESS_THAN:
                return QueryBuilders.rangeQuery(fieldName).lt(values.get(0));
                
            case LESS_THAN_OR_EQUAL:
                return QueryBuilders.rangeQuery(fieldName).lte(values.get(0));
                
            case SPECIFIED:
                boolean exists = Boolean.parseBoolean(values.get(0).toString());
                if (exists) {
                    return QueryBuilders.existsQuery(fieldName);
                } else {
                    return QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(fieldName));
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

