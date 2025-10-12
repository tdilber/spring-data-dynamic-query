package com.beyt.jdq.testenv.repository;

import com.beyt.jdq.repository.JpaDynamicQueryRepository;
import com.beyt.jdq.testenv.entity.User;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaDynamicQueryRepository<User, Long> {
}

