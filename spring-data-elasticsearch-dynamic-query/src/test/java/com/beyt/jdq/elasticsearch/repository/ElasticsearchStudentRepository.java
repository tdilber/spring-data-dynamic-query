package com.beyt.jdq.elasticsearch.repository;

import com.beyt.jdq.elasticsearch.entity.Student;
import org.springframework.stereotype.Repository;

/**
 * Elasticsearch repository for Student entity.
 * Extends ElasticsearchDynamicQueryRepository for dynamic query support.
 * The findAll(CriteriaList) method is inherited from ElasticsearchDynamicQueryRepository.
 */
@Repository
public interface ElasticsearchStudentRepository extends ElasticsearchDynamicQueryRepository<Student, Long> {
    // No need to declare findAll(CriteriaList) - it's inherited from parent interface
}

