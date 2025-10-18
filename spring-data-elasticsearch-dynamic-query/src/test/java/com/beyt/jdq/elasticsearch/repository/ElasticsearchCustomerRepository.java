package com.beyt.jdq.elasticsearch.repository;

import com.beyt.jdq.elasticsearch.entity.Customer;
import org.springframework.stereotype.Repository;

/**
 * Elasticsearch repository for Customer entity with dynamic query support.
 */
@Repository
public interface ElasticsearchCustomerRepository extends ElasticsearchDynamicQueryRepository<Customer, Long> {
}

