package com.beyt.jdq.elasticsearch.repository;

import com.beyt.jdq.elasticsearch.entity.AdminUser;
import org.springframework.stereotype.Repository;

/**
 * Elasticsearch repository for AdminUser entity.
 * Extends ElasticsearchDynamicQueryRepository for dynamic query support.
 * The findAll(CriteriaList) method is inherited from ElasticsearchDynamicQueryRepository.
 */
@Repository
public interface ElasticsearchAdminUserRepository extends ElasticsearchDynamicQueryRepository<AdminUser, Long> {
    // No need to declare findAll(CriteriaList) - it's inherited from parent interface
}

