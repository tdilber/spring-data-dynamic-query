package com.beyt.jdq.elasticsearch.tests;

import com.beyt.jdq.elasticsearch.entity.Course;
import com.beyt.jdq.elasticsearch.entity.Customer;
import com.beyt.jdq.elasticsearch.entity.User;
import com.beyt.jdq.elasticsearch.repository.ElasticsearchCourseRepository;
import com.beyt.jdq.elasticsearch.repository.ElasticsearchCustomerRepository;
import com.beyt.jdq.elasticsearch.repository.ElasticsearchUserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Calendar;
import java.util.Date;

/**
 * Base test instance for Elasticsearch tests.
 * Provides test data setup and common test entities.
 */
public abstract class BaseElasticsearchTestInstance {

    @Autowired
    protected ElasticsearchCourseRepository courseRepository;

    @Autowired
    protected ElasticsearchUserRepository userRepository;

    @Autowired
    protected ElasticsearchCustomerRepository customerRepository;

    public static final Calendar INSTANCE = Calendar.getInstance();

    // Test course data - similar to JPA tests
    protected Course course1;
    protected Course course2;
    protected Course course3;
    protected Course course4;
    protected Course course5;
    protected Course course6;
    protected Course course7;
    protected Course course8;
    protected Course course9;
    protected Course course10;

    // Test user and customer data
    protected User user1;
    protected User user2;
    protected User user3;
    protected User user4;
    protected User user5;
    protected User user6;
    protected User user7;
    protected User user8;

    protected Customer customer1;
    protected Customer customer2;
    protected Customer customer3;
    protected Customer customer4;
    protected Customer customer5;
    protected Customer customer6;
    protected Customer customer7;
    protected Customer customer8;

    public BaseElasticsearchTestInstance() {
        // Initialize test data
        course1 = new Course(1L, "Introduction to Computer Science", createDate(2016, 6, 18), 50, true, "Introduction to fundamental concepts of computer science.");
        course2 = new Course(2L, "Calculus I", createDate(2017, 6, 18), 60, true, "Introduction to fundamental concepts of calculus.");
        course3 = new Course(3L, "Calculus II", createDate(2018, 6, 18), 250, null, "Advanced topics in calculus including integrals and series.");
        course4 = new Course(4L, "Physics I", createDate(2019, 6, 18), 250, null, "Introduction to classical mechanics and Newtonian physics.");
        course5 = new Course(5L, "Physics II", createDate(2020, 6, 18), 250, null, "Advanced topics in physics including electromagnetism and thermodynamics.");
        course6 = new Course(6L, "Chemistry I", createDate(2021, 6, 18), 40, null, "Basic principles of chemistry including atomic structure and chemical bonding.");
        course7 = new Course(7L, "Chemistry II", createDate(2022, 6, 18), 30, null, "Continuation of chemistry studies covering topics like kinetics and equilibrium.");
        course8 = new Course(8L, "Biology I", createDate(2015, 6, 18), 20, true, "Introduction to cellular biology and genetics.");
        course9 = new Course(9L, "Biology II", createDate(2013, 6, 18), 54, true, "Advanced topics in biology including evolution and ecology.");
        course10 = new Course(10L, "English Literature I", createDate(2025, 6, 18), 10, false, "Exploration of classic works of English literature and literary analysis.");
        
        // Initialize user and customer test data
        // Use shared INSTANCE calendar (not a local variable) so tests can manipulate it
        user1 = new User(1L, "Name 1", "Surname 1", 35, INSTANCE.toInstant(), User.Status.PASSIVE, User.Type.USER);
        customer1 = new Customer(1L, "Customer 1", 20, INSTANCE.toInstant(), user1);
        INSTANCE.add(Calendar.MONTH, -1);
        user2 = new User(2L, "Name 2", "Surname 1", 36, INSTANCE.toInstant(), User.Status.ACTIVE, User.Type.ADMIN);
        customer2 = new Customer(2L, "Customer 2", 21, INSTANCE.toInstant(), user2);
        INSTANCE.add(Calendar.MONTH, -1);
        user3 = new User(3L, "Name 3", "Surname 1", 37, INSTANCE.toInstant(), User.Status.PASSIVE, User.Type.USER);
        customer3 = new Customer(3L, "Customer 3", 22, INSTANCE.toInstant(), user3);
        INSTANCE.add(Calendar.MONTH, -1);
        user4 = new User(4L, "Name 4", "Surname 1", 38, INSTANCE.toInstant(), User.Status.ACTIVE, User.Type.USER);
        customer4 = new Customer(4L, "Customer 4", 23, INSTANCE.toInstant(), user4);
        INSTANCE.add(Calendar.MONTH, -1);
        user5 = new User(5L, "Name 5", "Surname 1", 39, INSTANCE.toInstant(), User.Status.PASSIVE, User.Type.ADMIN);
        customer5 = new Customer(5L, "Customer 5", 24, INSTANCE.toInstant(), user5);
        INSTANCE.add(Calendar.MONTH, -1);
        user6 = new User(6L, "Name 6", "Surname 1", 40, INSTANCE.toInstant(), User.Status.ACTIVE, User.Type.ADMIN);
        customer6 = new Customer(6L, "Customer 6", 25, INSTANCE.toInstant(), user6);
        INSTANCE.add(Calendar.MONTH, -1);
        user7 = new User(7L, "Name 7", "Surname 1", 41, INSTANCE.toInstant(), User.Status.ACTIVE, User.Type.USER);
        customer7 = new Customer(7L, "Customer 7", 26, INSTANCE.toInstant(), user7);
        INSTANCE.add(Calendar.MONTH, -1);
        user8 = new User(8L, "Name 8", "Surname 1", 42, INSTANCE.toInstant(), User.Status.PASSIVE, User.Type.ADMIN);
        customer8 = new Customer(8L, null, 27, INSTANCE.toInstant(), user8);
    }

    @BeforeAll
    public void init() throws InterruptedException {
        // Clear existing data
        courseRepository.deleteAll();
        userRepository.deleteAll();
        customerRepository.deleteAll();
        
        // Insert course test data
        courseRepository.save(course1);
        courseRepository.save(course2);
        courseRepository.save(course3);
        courseRepository.save(course4);
        courseRepository.save(course5);
        courseRepository.save(course6);
        courseRepository.save(course7);
        courseRepository.save(course8);
        courseRepository.save(course9);
        courseRepository.save(course10);
        
        // Insert user test data
        userRepository.save(user1);
        userRepository.save(user2);
        userRepository.save(user3);
        userRepository.save(user4);
        userRepository.save(user5);
        userRepository.save(user6);
        userRepository.save(user7);
        userRepository.save(user8);
        
        // Insert customer test data
        customerRepository.save(customer1);
        customerRepository.save(customer2);
        customerRepository.save(customer3);
        customerRepository.save(customer4);
        customerRepository.save(customer5);
        customerRepository.save(customer6);
        customerRepository.save(customer7);
        customerRepository.save(customer8);
        
        // Wait for Elasticsearch to index documents
        Thread.sleep(1500);
    }

    /**
     * Helper method to create Date objects
     */
    protected Date createDate(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, day, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}


