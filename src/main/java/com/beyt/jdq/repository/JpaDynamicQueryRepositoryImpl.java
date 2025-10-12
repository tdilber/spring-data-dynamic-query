package com.beyt.jdq.repository;

import com.beyt.jdq.deserializer.IDeserializer;
import com.beyt.jdq.dto.Criteria;
import com.beyt.jdq.dto.DynamicQuery;
import com.beyt.jdq.query.DynamicQueryManager;
import com.beyt.jdq.query.RepositoryContext;
import com.beyt.jdq.query.builder.QueryBuilder;
import com.beyt.jdq.util.ListConsumer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import java.io.Serializable;
import java.util.List;


@Transactional(
        readOnly = true
)
public class JpaDynamicQueryRepositoryImpl<T, ID extends Serializable> extends SimpleJpaRepository<T, ID> implements JpaDynamicQueryRepository<T, ID>, JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

    protected final EntityManager entityManager;
    protected final IDeserializer deserializer;
    protected final RepositoryContext context;

    public JpaDynamicQueryRepositoryImpl(JpaEntityInformation<T, ?> entityInformation,
                                         EntityManager entityManager, IDeserializer deserializer) {

        super(entityInformation, entityManager);
        this.entityManager = entityManager;
        this.deserializer = deserializer;
        this.context = new RepositoryContext(entityManager, this.deserializer);
    }

    private RepositoryContext getQueryContext() {
        return context;
    }

    @Override
    public List<T> findAll(List<Criteria> criteriaList) {
        return DynamicQueryManager.findAll(this, criteriaList, getQueryContext());
    }

    @Override
    public List<T> findAll(DynamicQuery dynamicQuery) {
        return DynamicQueryManager.getEntityListBySelectableFilterAsList(this, dynamicQuery, getQueryContext());
    }

    @Override
    public Page<T> findAllAsPage(DynamicQuery dynamicQuery) {
        return DynamicQueryManager.getEntityListBySelectableFilterAsPage(this, dynamicQuery, getQueryContext());
    }

    @Override
    public List<Tuple> findAllAsTuple(DynamicQuery dynamicQuery) {
        return DynamicQueryManager.getEntityListBySelectableFilterWithTupleAsList(this, dynamicQuery, getQueryContext());
    }

    @Override
    public Page<Tuple> findAllAsTuplePage(DynamicQuery dynamicQuery) {
        return DynamicQueryManager.getEntityListBySelectableFilterWithTupleAsPage(this, dynamicQuery, getQueryContext());
    }

    @Override
    public <ResultType> List<ResultType> findAll(DynamicQuery dynamicQuery, Class<ResultType> resultTypeClass) {
        return DynamicQueryManager.getEntityListBySelectableFilterWithReturnTypeAsList(this, dynamicQuery, resultTypeClass, getQueryContext());
    }

    @Override
    public <ResultType> Page<ResultType> findAllAsPage(DynamicQuery dynamicQuery, Class<ResultType> resultTypeClass) {
        return DynamicQueryManager.getEntityListBySelectableFilterWithReturnTypeAsPage(this, dynamicQuery, resultTypeClass, getQueryContext());
    }

    @Override
    public QueryBuilder<T, ID> queryBuilder() {
        return new QueryBuilder<>(this);
    }

    @Override
    public Page<T> findAll(List<Criteria> criteriaList, Pageable pageable) {
        return DynamicQueryManager.findAll(this, criteriaList, pageable, getQueryContext());
    }

    static <T> Specification<T> getSpecificationWithCriteria(List<Criteria> criteriaList, RepositoryContext context) {
        return DynamicQueryManager.getSpecification(criteriaList, context);
    }

    public Class<T> getDomainClass() {
        return super.getDomainClass();
    }

    @Override
    public long count(List<Criteria> criteriaList) {
        return DynamicQueryManager.count(this, criteriaList, getQueryContext());
    }

    @Override
    public void consumePartially(ListConsumer<T> processor, int pageSize) {
        consumePartially((Specification<T>) null, processor, pageSize);
    }

    @Override
    public void consumePartially(Specification<T> specification, ListConsumer<T> processor, int pageSize) {
        Page<T> page = this.findAll((Specification<T>) null, PageRequest.of(0, pageSize));
        processor.accept(page.getContent());
        long totalElements = page.getTotalElements();
        for (int i = 1; (long) i * pageSize < totalElements; i++) {
            page = this.findAll(specification, PageRequest.of(i, pageSize));
            processor.accept(page.getContent());
        }
    }

    @Override
    public void consumePartially(List<Criteria> criteriaList, ListConsumer<T> processor, int pageSize) {
        long totalElements = DynamicQueryManager.count(this, criteriaList, getQueryContext());

        for (int i = 0; (long) i * pageSize < totalElements; i++) {
            processor.accept(DynamicQueryManager.findAll(this, criteriaList, PageRequest.of(i, pageSize), getQueryContext()).getContent());
        }
    }
}
