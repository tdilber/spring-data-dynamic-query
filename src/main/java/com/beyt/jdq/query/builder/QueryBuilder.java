package com.beyt.jdq.query.builder;

import com.beyt.jdq.dto.Criteria;
import com.beyt.jdq.dto.DynamicQuery;
import com.beyt.jdq.query.builder.interfaces.*;
import com.beyt.jdq.repository.JpaDynamicQueryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.util.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class QueryBuilder<T, ID> implements DistinctWhereOrderByPage<T, ID>, WhereOrderByPage<T, ID>, OrderByPage<T, ID>, PageableResult<T, ID>, Result<T, ID> {
    protected final JpaDynamicQueryRepository<T, ID> jpaDynamicQueryRepository;
    protected final DynamicQuery dynamicQuery;

    public QueryBuilder(JpaDynamicQueryRepository<T, ID> jpaDynamicQueryRepository) {
        this.jpaDynamicQueryRepository = jpaDynamicQueryRepository;
        dynamicQuery = new DynamicQuery();
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
        return jpaDynamicQueryRepository.findAll(dynamicQuery);
    }

    public <ResultValue> List<ResultValue> getResult(Class<ResultValue> resultValueClass) {
        return jpaDynamicQueryRepository.findAll(dynamicQuery, resultValueClass);
    }

    public Page<T> getResultAsPage() {
        return jpaDynamicQueryRepository.findAllAsPage(dynamicQuery);
    }

    public <ResultValue> Page<ResultValue> getResultAsPage(Class<ResultValue> resultValueClass) {
        return jpaDynamicQueryRepository.findAllAsPage(dynamicQuery, resultValueClass);
    }
}
