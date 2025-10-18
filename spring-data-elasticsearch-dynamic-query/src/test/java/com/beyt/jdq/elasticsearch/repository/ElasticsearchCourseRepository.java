package com.beyt.jdq.elasticsearch.repository;

import com.beyt.jdq.elasticsearch.entity.Course;
import org.springframework.stereotype.Repository;

/**
 * Elasticsearch repository for Course entity with dynamic query support.
 */
@Repository
public interface ElasticsearchCourseRepository extends ElasticsearchDynamicQueryRepository<Course, Long> {
}

