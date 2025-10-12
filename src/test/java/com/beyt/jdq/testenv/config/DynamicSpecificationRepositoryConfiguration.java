package com.beyt.jdq.testenv.config;

import com.beyt.jdq.annotation.EnableJpaDynamicQuery;
import com.beyt.jdq.repository.JpaDynamicQueryRepositoryImpl;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaDynamicQuery
public class DynamicSpecificationRepositoryConfiguration {

}
