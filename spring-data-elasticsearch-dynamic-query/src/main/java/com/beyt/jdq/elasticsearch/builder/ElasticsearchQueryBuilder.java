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

    public ElasticsearchQueryBuilder(ElasticsearchDynamicQueryRepository<T, ID> repository) {
        this.repository = repository;
    }

    @Override
    public List<T> getResult() {
        return repository.findAll(dynamicQuery);
    }

    @Override
    public <ResultValue> List<ResultValue> getResult(Class<ResultValue> resultValueClass) {
        return repository.findAll(dynamicQuery, resultValueClass);
    }

    @Override
    public Page<T> getResultAsPage() {
        return repository.findAllAsPage(dynamicQuery);
    }

    @Override
    public <ResultValue> Page<ResultValue> getResultAsPage(Class<ResultValue> resultValueClass) {
        return repository.findAllAsPage(dynamicQuery, resultValueClass);
    }

    /**
     * Get the underlying DynamicQuery object
     */
    public DynamicQuery getDynamicQuery() {
        return dynamicQuery;
    }
}




