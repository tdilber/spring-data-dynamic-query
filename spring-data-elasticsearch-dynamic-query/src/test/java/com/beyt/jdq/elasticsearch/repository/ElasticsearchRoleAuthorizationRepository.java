package com.beyt.jdq.elasticsearch.repository;

import com.beyt.jdq.elasticsearch.entity.RoleAuthorization;
import org.springframework.stereotype.Repository;

/**
 * Elasticsearch repository for RoleAuthorization entity.
 */
@Repository
public interface ElasticsearchRoleAuthorizationRepository extends ElasticsearchDynamicQueryRepository<RoleAuthorization, Long> {
}

