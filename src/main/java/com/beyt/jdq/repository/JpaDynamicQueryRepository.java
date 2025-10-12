package com.beyt.jdq.repository;

import com.beyt.jdq.dto.Criteria;
import com.beyt.jdq.dto.DynamicQuery;
import com.beyt.jdq.query.builder.QueryBuilder;
import com.beyt.jdq.util.ListConsumer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import jakarta.persistence.Tuple;
import java.util.List;

@NoRepositoryBean
public interface JpaDynamicQueryRepository<T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

    List<T> findAll(List<Criteria> criteriaList);

    List<T> findAll(DynamicQuery dynamicQuery);

    Page<T> findAllAsPage(DynamicQuery dynamicQuery);

    List<Tuple> findAllAsTuple(DynamicQuery dynamicQuery);

    Page<Tuple> findAllAsTuplePage(DynamicQuery dynamicQuery);

    <ResultType> List<ResultType> findAll(DynamicQuery dynamicQuery, Class<ResultType> resultTypeClass);

    <ResultType> Page<ResultType> findAllAsPage(DynamicQuery dynamicQuery, Class<ResultType> resultTypeClass);

    QueryBuilder<T, ID> queryBuilder();

    Page<T> findAll(List<Criteria> criteriaList, Pageable pageable);

    long count(List<Criteria> criteriaList);

    void consumePartially(ListConsumer<T> processor, int pageSize);

    void consumePartially(Specification<T> specification, ListConsumer<T> processor, int pageSize);

    void consumePartially(List<Criteria> criteriaList, ListConsumer<T> processor, int pageSize);
}
