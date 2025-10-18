package com.beyt.jdq.elasticsearch.builder;

import com.beyt.jdq.core.model.Criteria;
import com.beyt.jdq.core.model.DynamicQuery;
import com.beyt.jdq.core.model.builder.BaseQueryBuilder;
import com.beyt.jdq.core.model.builder.interfaces.*;
import com.beyt.jdq.core.model.enums.Order;
import com.beyt.jdq.elasticsearch.repository.ElasticsearchDynamicQueryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;

import java.io.Serializable;
import java.util.List;

/**
 * Fluent query builder for Elasticsearch dynamic queries.
 * Provides a chainable API for building complex queries.
 * 
 * @param <T> Entity type
 * @param <ID> ID type
 */
public class ElasticsearchQueryBuilder<T, ID extends Serializable> extends BaseQueryBuilder<T, ID> implements DistinctWhereOrderByPage<T, ID>, WhereOrderByPage<T, ID>, OrderByPage<T, ID>, PageableResult<T, ID>, Result<T, ID> {

    private final ElasticsearchDynamicQueryRepository<T, ID> repository;
    private final DynamicQuery dynamicQuery;

    public ElasticsearchQueryBuilder(ElasticsearchDynamicQueryRepository<T, ID> repository) {
        this.repository = repository;
        this.dynamicQuery = new DynamicQuery();
    }

    /**
     * Add a criteria to the query
     */
    public ElasticsearchQueryBuilder<T, ID> where(Criteria criteria) {
        dynamicQuery.getWhere().add(criteria);
        return this;
    }

    /**
     * Add multiple criteria to the query
     */
    public ElasticsearchQueryBuilder<T, ID> where(List<Criteria> criteriaList) {
        dynamicQuery.getWhere().addAll(criteriaList);
        return this;
    }

    /**
     * Add sorting to the query
     */
    public ElasticsearchQueryBuilder<T, ID> orderBy(String field, Order order) {
        dynamicQuery.getOrderBy().add(Pair.of(field, order));
        return this;
    }

    /**
     * Add pagination to the query
     */
    public ElasticsearchQueryBuilder<T, ID> page(int pageNumber, int pageSize) {
        dynamicQuery.setPageNumber(pageNumber);
        dynamicQuery.setPageSize(pageSize);
        return this;
    }

    @Override
    public List<T> getResult() {
        return List.of();
    }

    @Override
    public <ResultValue> List<ResultValue> getResult(Class<ResultValue> resultValueClass) {
        return List.of();
    }

    @Override
    public Page<T> getResultAsPage() {
        return null;
    }

    @Override
    public <ResultValue> Page<ResultValue> getResultAsPage(Class<ResultValue> resultValueClass) {
        return null;
    }
//
//    /**
//     * Execute the query and return results as List
//     */
//    public List<T> execute() {
//        return repository.findAll(dynamicQuery);
//    }
//
//    /**
//     * Execute the query and return results as Page
//     */
//    public Page<T> executeAsPage() {
//        return repository.findAllAsPage(dynamicQuery);
//    }
//
//    /**
//     * Execute the query with projection and return results as List
//     */
//    public <R> List<R> executeAs(Class<R> resultClass) {
//        return repository.findAll(dynamicQuery, resultClass);
//    }
//
//    /**
//     * Execute the query with projection and return results as Page
//     */
//    public <R> Page<R> executeAsPage(Class<R> resultClass) {
//        return repository.findAllAsPage(dynamicQuery, resultClass);
//    }

    /**
     * Get the underlying DynamicQuery object
     */
    public DynamicQuery getDynamicQuery() {
        return dynamicQuery;
    }
}




