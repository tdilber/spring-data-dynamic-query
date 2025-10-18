package com.beyt.jdq.elasticsearch.repository;

import com.beyt.jdq.elasticsearch.entity.Authorization;
import org.springframework.stereotype.Repository;

/**
 * Elasticsearch repository for Authorization entity.
 */
@Repository
public interface ElasticsearchAuthorizationRepository extends ElasticsearchDynamicQueryRepository<Authorization, Long> {
}

