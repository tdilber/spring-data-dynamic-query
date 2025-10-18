package com.beyt.jdq.elasticsearch.core;

import com.beyt.jdq.core.deserializer.IDeserializer;
import com.beyt.jdq.core.model.DynamicQuery;
import com.beyt.jdq.core.model.enums.CriteriaOperator;
import com.beyt.jdq.core.model.exception.DynamicQueryIllegalArgumentException;
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
import org.springframework.data.util.Pair;

import java.util.List;
import java.util.stream.Collectors;

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
        // For now, return as the same type - projection will be implemented later
        List<Entity> results = findAll(entityClass, dynamicQuery);
        return results.stream()
                .map(entity -> (ResultType) entity)
                .collect(Collectors.toList());
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
        Page<Entity> page = findAllAsPage(entityClass, dynamicQuery);
        return page.map(entity -> (ResultType) entity);
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
        
        switch (operator) {
            case EQUAL:
                if (values.size() > 1) {
                    // Multiple values - use terms query
                    return QueryBuilders.termsQuery(fieldName, values);
                } else {
                    // Single value - use term query
                    return QueryBuilders.termQuery(fieldName, values.get(0));
                }
                
            case NOT_EQUAL:
                if (values.size() > 1) {
                    return QueryBuilders.boolQuery().mustNot(QueryBuilders.termsQuery(fieldName, values));
                } else {
                    return QueryBuilders.boolQuery().mustNot(QueryBuilders.termQuery(fieldName, values.get(0)));
                }
                
            case CONTAIN:
                return QueryBuilders.wildcardQuery(fieldName, "*" + escapeWildcard(values.get(0).toString()).toLowerCase() + "*")
                    .caseInsensitive(true);
                
            case DOES_NOT_CONTAIN:
                return QueryBuilders.boolQuery().mustNot(
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
}

