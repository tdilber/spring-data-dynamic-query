package com.beyt.jdq.mongo.repository;

import com.beyt.jdq.mongo.JpaDynamicQueryMongoRepository;
import com.beyt.jdq.mongo.entity.Customer;
import org.springframework.stereotype.Repository;

/**
 * MongoDB repository for Customer entity with dynamic query support.
 */
@Repository
public interface MongoCustomerRepository extends JpaDynamicQueryMongoRepository<Customer, Long> {
}

