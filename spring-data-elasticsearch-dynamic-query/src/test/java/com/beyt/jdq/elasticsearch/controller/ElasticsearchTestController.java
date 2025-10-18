package com.beyt.jdq.elasticsearch.controller;

import com.beyt.jdq.core.model.CriteriaList;
import com.beyt.jdq.core.model.DynamicQuery;
import com.beyt.jdq.elasticsearch.tests.S9_Query_Builder;
import com.beyt.jdq.elasticsearch.entity.Course;
import com.beyt.jdq.elasticsearch.entity.Customer;
import com.beyt.jdq.elasticsearch.entity.User;
import com.beyt.jdq.elasticsearch.repository.ElasticsearchAdminUserRepository;
import com.beyt.jdq.elasticsearch.repository.ElasticsearchCourseRepository;
import com.beyt.jdq.elasticsearch.repository.ElasticsearchCustomerRepository;
import com.beyt.jdq.elasticsearch.repository.ElasticsearchUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Elasticsearch test controller for testing argument resolvers.
 * Provides REST endpoints that use CriteriaList and DynamicQuery as method parameters.
 * The argument resolvers automatically convert query parameters to these types.
 */
@RestController
@RequestMapping("/elasticsearch-test-api")
public class ElasticsearchTestController {
    @Autowired
    private ElasticsearchCourseRepository courseRepository;
    
    @Autowired
    private ElasticsearchAdminUserRepository adminUserRepository;
    
    @Autowired
    private ElasticsearchUserRepository userRepository;
    
    @Autowired
    private ElasticsearchCustomerRepository customerRepository;

    /**
     * Test CriteriaList argument resolver
     * Example: /elasticsearch-test-api/course?key0=name&operation0=CONTAIN&values0=Calculus
     */
    @GetMapping("/course")
    public ResponseEntity<List<Course>> getCourseWithCriteria(CriteriaList criteriaList) {
        List<Course> courseList = courseRepository.findAll(criteriaList);
        return ResponseEntity.ok().body(courseList);
    }

    /**
     * Test DynamicQuery argument resolver for pagination
     * Example: /elasticsearch-test-api/course/as-page?key0=name&operation0=START_WITH&values0=Physics&page=0&pageSize=10
     */
    @GetMapping("/course/as-page")
    public ResponseEntity<Page<Course>> getCourseWithSearchFilterAsPage(DynamicQuery dynamicQuery) {
        Page<Course> coursePage = courseRepository.findAllAsPage(dynamicQuery);
        return ResponseEntity.ok().body(coursePage);
    }

    /**
     * Test DynamicQuery argument resolver with projection
     * Example: /elasticsearch-test-api/course/as-list?select0=id&select1=username&selectAs0=adminId&selectAs1=adminUsername
     */
    @GetMapping("/course/as-list")
    public ResponseEntity<List<S9_Query_Builder.AuthorizationSummary>> getCourseWithSearchFilterAsList(DynamicQuery dynamicQuery) {
        List<S9_Query_Builder.AuthorizationSummary> summaryList = adminUserRepository.findAll(dynamicQuery, S9_Query_Builder.AuthorizationSummary.class);
        return ResponseEntity.ok().body(summaryList);
    }

    /**
     * Test CriteriaList argument resolver for Customer
     * Example: /elasticsearch-test-api/customer?key0=age&operation0=EQUAL&values0=24
     */
    @GetMapping("/customer")
    public ResponseEntity<List<Customer>> getCustomerWithCriteria(CriteriaList criteriaList) {
        List<Customer> customerList = customerRepository.findAll(criteriaList);
        return ResponseEntity.ok().body(customerList);
    }

    /**
     * Test CriteriaList argument resolver for User
     * Example: /elasticsearch-test-api/user?key0=status&operation0=EQUAL&values0=ACTIVE
     */
    @GetMapping("/user")
    public ResponseEntity<List<User>> getUserWithCriteria(CriteriaList criteriaList) {
        List<User> userList = userRepository.findAll(criteriaList);
        return ResponseEntity.ok().body(userList);
    }

    /**
     * Test DynamicQuery argument resolver for User with pagination
     * Example: /elasticsearch-test-api/user/as-page?key0=status&operation0=EQUAL&values0=ACTIVE&page=0&pageSize=10
     */
    @GetMapping("/user/as-page")
    public ResponseEntity<Page<User>> getUserWithSearchFilterAsPage(DynamicQuery dynamicQuery) {
        Page<User> userPage = userRepository.findAllAsPage(dynamicQuery);
        return ResponseEntity.ok().body(userPage);
    }

    /**
     * Test DynamicQuery argument resolver for Customer as list
     * Example: /elasticsearch-test-api/customer/as-list?select0=name&select1=age&page=0&pageSize=10
     */
    @GetMapping("/customer/as-list")
    public ResponseEntity<List<Customer>> getCustomerWithSearchFilterAsList(DynamicQuery dynamicQuery) {
        List<Customer> customerList = customerRepository.findAll(dynamicQuery, Customer.class);
        return ResponseEntity.ok().body(customerList);
    }

    /**
     * Test DynamicQuery argument resolver for User as list
     * Example: /elasticsearch-test-api/user/as-list?select0=name&select1=surname&select2=birthdate&page=0&pageSize=2
     */
    @GetMapping("/user/as-list")
    public ResponseEntity<List<User>> getUserWithSearchFilterAsList(DynamicQuery dynamicQuery) {
        List<User> userList = userRepository.findAll(dynamicQuery, User.class);
        return ResponseEntity.ok().body(userList);
    }

    /**
     * Test DynamicQuery argument resolver for Customer with pagination
     * Example: /elasticsearch-test-api/customer/as-page?key0=age&operation0=GREATER_THAN&values0=23&page=0&pageSize=5
     */
    @GetMapping("/customer/as-page")
    public ResponseEntity<Page<Customer>> getCustomerWithSearchFilterAsPage(DynamicQuery dynamicQuery) {
        Page<Customer> customerPage = customerRepository.findAllAsPage(dynamicQuery);
        return ResponseEntity.ok().body(customerPage);
    }
}

