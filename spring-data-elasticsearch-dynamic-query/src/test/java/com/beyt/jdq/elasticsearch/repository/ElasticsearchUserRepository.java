package com.beyt.jdq.elasticsearch.repository;

import com.beyt.jdq.elasticsearch.entity.User;
import org.springframework.stereotype.Repository;

/**
 * Elasticsearch repository for User entity with dynamic query support.
 */
@Repository
public interface ElasticsearchUserRepository extends ElasticsearchDynamicQueryRepository<User, Long> {
}

