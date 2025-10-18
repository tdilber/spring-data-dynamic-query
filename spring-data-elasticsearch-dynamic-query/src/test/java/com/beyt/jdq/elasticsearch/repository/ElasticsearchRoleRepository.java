package com.beyt.jdq.elasticsearch.repository;

import com.beyt.jdq.elasticsearch.entity.Role;
import org.springframework.stereotype.Repository;

/**
 * Elasticsearch repository for Role entity.
 */
@Repository
public interface ElasticsearchRoleRepository extends ElasticsearchDynamicQueryRepository<Role, Long> {
}

