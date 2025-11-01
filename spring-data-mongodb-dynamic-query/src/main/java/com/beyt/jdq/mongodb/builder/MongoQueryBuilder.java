package com.beyt.jdq.mongodb.builder;

import com.beyt.jdq.core.model.Criteria;
import com.beyt.jdq.core.model.DynamicQuery;
import com.beyt.jdq.core.model.builder.BaseQueryBuilder;
import com.beyt.jdq.core.model.builder.QuerySimplifier;
import com.beyt.jdq.core.model.builder.interfaces.*;
import com.beyt.jdq.mongodb.repository.MongoDynamicQueryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.util.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MongoDB version of QueryBuilder.
 * Provides fluent API for building dynamic queries for MongoDB repositories.
 * 
 * @param <T> the domain type the builder operates on
 * @param <ID> the type of the id of the entity
 */
public class MongoQueryBuilder<T, ID> extends BaseQueryBuilder<T, ID> implements DistinctWhereOrderByPage<T, ID>, WhereOrderByPage<T, ID>, OrderByPage<T, ID>, PageableResult<T, ID>, Result<T, ID> {
    /** The MongoDB repository instance */
    protected final MongoDynamicQueryRepository<T, ID> mongoRepository;
    /** The dynamic query being built */
    protected final DynamicQuery dynamicQuery;

    /**
     * Creates a new query builder.
     * 
     * @param mongoRepository the MongoDB repository
     */
    public MongoQueryBuilder(MongoDynamicQueryRepository<T, ID> mongoRepository) {
        this.mongoRepository = mongoRepository;
        this.dynamicQuery = new DynamicQuery();
    }

    public DistinctWhereOrderByPage<T, ID> select(QuerySimplifier.SelectRule... selectRules) {
        dynamicQuery.getSelect().addAll(Arrays.stream(selectRules).map(o -> Pair.of(o.getFieldName(), o.getAlias())).collect(Collectors.toList()));
        return this;
    }

    public WhereOrderByPage<T, ID> distinct(boolean distinct) {
        dynamicQuery.setDistinct(distinct);
        return this;
    }

    public OrderByPage<T, ID> where(Criteria... criteria) {
        dynamicQuery.getWhere().addAll(Arrays.asList(criteria));
        return this;
    }

    public PageableResult<T, ID> orderBy(QuerySimplifier.OrderByRule... pairs) {
        dynamicQuery.getOrderBy().addAll(Arrays.stream(pairs).map(o -> Pair.of(o.getFieldName(), o.getOrderType())).collect(Collectors.toList()));
        return this;
    }

    public Result<T, ID> page(int pageNumber, int pageSize) {
        dynamicQuery.setPageSize(pageSize);
        dynamicQuery.setPageNumber(pageNumber);
        return this;
    }

    public List<T> getResult() {
        return mongoRepository.findAll(dynamicQuery);
    }

    public <ResultValue> List<ResultValue> getResult(Class<ResultValue> resultValueClass) {
        return mongoRepository.findAll(dynamicQuery, resultValueClass);
    }

    public Page<T> getResultAsPage() {
        return mongoRepository.findAllAsPage(dynamicQuery);
    }

    public <ResultValue> Page<ResultValue> getResultAsPage(Class<ResultValue> resultValueClass) {
        return mongoRepository.findAllAsPage(dynamicQuery, resultValueClass);
    }
}

