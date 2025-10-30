package com.beyt.jdq.jpa.repository;

import com.beyt.jdq.core.model.Criteria;
import com.beyt.jdq.core.model.DynamicQuery;
import com.beyt.jdq.core.repository.BaseDynamicQueryRepository;
import com.beyt.jdq.jpa.query.builder.QueryBuilder;
import com.beyt.jdq.core.util.ListConsumer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import javax.persistence.Tuple;
import java.util.List;

@NoRepositoryBean
public interface JpaDynamicQueryRepository<T, ID> extends BaseDynamicQueryRepository<T, ID>, JpaRepository<T, ID> {

    List<T> findAll(List<Criteria> criteriaList);

    List<T> findAll(DynamicQuery dynamicQuery);

    Page<T> findAllAsPage(DynamicQuery dynamicQuery);

    List<Tuple> findAllAsTuple(DynamicQuery dynamicQuery);

    Page<Tuple> findAllAsTuplePage(DynamicQuery dynamicQuery);

    <ResultType> List<ResultType> findAll(DynamicQuery dynamicQuery, Class<ResultType> resultTypeClass);

    <ResultType> Page<ResultType> findAllAsPage(DynamicQuery dynamicQuery, Class<ResultType> resultTypeClass);

    QueryBuilder<T, ID> queryBuilder();

    Page<T> findAll(List<Criteria> criteriaList, Pageable pageable);

    void update(Object jdqModel);

    long count(List<Criteria> criteriaList);

    void consumePartially(ListConsumer<T> processor, int pageSize);

    void consumePartially(Specification<T> specification, ListConsumer<T> processor, int pageSize);

    void consumePartially(List<Criteria> criteriaList, ListConsumer<T> processor, int pageSize);
}
