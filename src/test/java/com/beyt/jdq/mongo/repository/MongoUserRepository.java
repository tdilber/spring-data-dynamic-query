package com.beyt.jdq.mongo.repository;

import com.beyt.jdq.mongo.JpaDynamicQueryMongoRepository;
import com.beyt.jdq.mongo.entity.User;
import org.springframework.stereotype.Repository;

/**
 * MongoDB repository for User entity with dynamic query support.
 */
@Repository
public interface MongoUserRepository extends JpaDynamicQueryMongoRepository<User, Long> {
}

