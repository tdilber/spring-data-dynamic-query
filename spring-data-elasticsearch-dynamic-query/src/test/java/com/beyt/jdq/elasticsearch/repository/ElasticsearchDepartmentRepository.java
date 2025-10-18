package com.beyt.jdq.elasticsearch.repository;

import com.beyt.jdq.elasticsearch.entity.Department;
import org.springframework.stereotype.Repository;

/**
 * Elasticsearch repository for Department entity.
 * Extends ElasticsearchDynamicQueryRepository for dynamic query support.
 * The findAll(CriteriaList) method is inherited from ElasticsearchDynamicQueryRepository.
 */
@Repository
public interface ElasticsearchDepartmentRepository extends ElasticsearchDynamicQueryRepository<Department, Long> {
    // No need to declare findAll(CriteriaList) - it's inherited from parent interface
}

