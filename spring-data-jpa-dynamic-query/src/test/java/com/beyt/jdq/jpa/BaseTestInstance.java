package com.beyt.jdq.jpa;

import com.beyt.jdq.jpa.testenv.entity.Customer;
import com.beyt.jdq.jpa.testenv.entity.User;
import com.beyt.jdq.jpa.testenv.entity.authorization.*;
import com.beyt.jdq.jpa.testenv.entity.school.Address;
import com.beyt.jdq.jpa.testenv.entity.school.Course;
import com.beyt.jdq.jpa.testenv.entity.school.Department;
import com.beyt.jdq.jpa.testenv.entity.school.Student;
import com.beyt.jdq.jpa.testenv.repository.*;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManager;
import java.util.Calendar;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

public abstract class BaseTestInstance {

    @Autowired
    protected CustomerRepository customerRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected AddressRepository addressRepository;

    @Autowired
    protected RoleRepository roleRepository;

    @Autowired
    protected DepartmentRepository departmentRepository;

    @Autowired
    protected CourseRepository courseRepository;

    @Autowired
    protected StudentRepository studentRepository;

    @Autowired
    protected AuthorizationRepository authorizationRepository;

    @Autowired
    protected RoleAuthorizationRepository roleAuthorizationRepository;

    @Autowired
    protected AdminUserRepository adminUserRepository;


    @Autowired
    protected EntityManager entityManager;

    public final User user1;
    public final User user2;
    public final User user3;
    public final User user4;
    public final User user5;
    public final User user6;
    public final User user7;
    public final User user8;

    public final Customer customer1;
    public final Customer customer2;
    public final Customer customer3;
    public final Customer customer4;
    public final Customer customer5;
    public final Customer customer6;
    public final Customer customer7;
    public final Customer customer8;
    public static final Calendar INSTANCE = Calendar.getInstance();

    protected Address address1 = new Address(null, "123 Main St", "New York", "NY", "10001");
    protected Address address2 = new Address(null, "456 Park Ave", "Chicago", "IL", "60605");
    protected Address address3 = new Address(null, "789 Broadway", "Los Angeles", "CA", "90001");
    protected Address address4 = new Address(null, "321 Market St", "San Francisco", "CA", "94105");
    protected Address address5 = new Address(null, "654 Elm St", "Dallas", "TX", "75001");
    protected Address address6 = new Address(null, "987 Oak St", "Houston", "TX", "77002");
    protected Address address7 = new Address(null, "345 Pine St", "Philadelphia", "PA", "19019");
    protected Address address8 = new Address(null, "678 Maple St", "Phoenix", "AZ", "85001");
    protected Address address9 = new Address(null, "102 Beach St", "Miami", "FL", "33101");
    protected Address address10 = new Address(null, "567 Hill St", "Atlanta", "GA", "30301");


    protected Department department1 = new Department(null, "Computer Science");
    protected Department department2 = new Department(null, "Mathematics");
    protected Department department3 = new Department(null, "Physics");
    protected Department department4 = new Department(null, "Chemistry");
    protected Department department5 = new Department(null, "Biology");
    protected Department department6 = new Department(null, "English Literature");
    protected Department department7 = new Department(null, "History");
    protected Department department8 = new Department(null, "Geography");
    protected Department department9 = new Department(null, "Political Science");
    protected Department department10 = new Department(null, "Economics");

    protected Course course1 = new Course(null, "Introduction to Computer Science", Timestamp.valueOf("2016-06-18 00:00:00"), 50, true, "Introduction to fundamental concepts of computer science.");
    protected Course course2 = new Course(null, "Calculus I", Timestamp.valueOf("2017-06-18 00:00:00"), 60, true, "Introduction to fundamental concepts of calculus.");
    protected Course course3 = new Course(null, "Calculus II", Timestamp.valueOf("2018-06-18 00:00:00"), 250, null, "Advanced topics in calculus including integrals and series.");
    protected Course course4 = new Course(null, "Physics I", Timestamp.valueOf("2019-06-18 00:00:00"), 250, null, "Introduction to classical mechanics and Newtonian physics.");
    protected Course course5 = new Course(null, "Physics II", Timestamp.valueOf("2020-06-18 00:00:00"), 250, null, "Advanced topics in physics including electromagnetism and thermodynamics.");
    protected Course course6 = new Course(null, "Chemistry I", Timestamp.valueOf("2021-06-18 00:00:00"), 40, null, "Basic principles of chemistry including atomic structure and chemical bonding.");
    protected Course course7 = new Course(null, "Chemistry II", Timestamp.valueOf("2022-06-18 00:00:00"), 30, null, "Continuation of chemistry studies covering topics like kinetics and equilibrium.");
    protected Course course8 = new Course(null, "Biology I", Timestamp.valueOf("2015-06-18 00:00:00"), 20, true, "Introduction to cellular biology and genetics.");
    protected Course course9 = new Course(null, "Biology II", Timestamp.valueOf("2013-06-18 00:00:00"), 54, true, "Advanced topics in biology including evolution and ecology.");
    protected Course course10 = new Course(null, "English Literature I", Timestamp.valueOf("2025-06-18 00:00:00"), 10, false, "Exploration of classic works of English literature and literary analysis.");

    protected Student student1 = new Student(null, "John Doe", address1, department1, List.of(course1, course2));
    protected Student student2 = new Student(null, "Jane Smith", address2, department2, List.of(course2, course4));
    protected Student student3 = new Student(null, "Robert Johnson", address3, department3, List.of(course3));
    protected Student student4 = new Student(null, "Emily Davis", address4, department4, List.of(course4));
    protected Student student5 = new Student(null, "Michael Miller", address5, department5, List.of(course5));
    protected Student student6 = new Student(null, "Sarah Wilson", address6, department6, List.of(course6));
    protected Student student7 = new Student(null, "David Moore", address7, department7, List.of(course7));
    protected Student student8 = new Student(null, "Jessica Taylor", address8, department8, List.of(course8));
    protected Student student9 = new Student(null, "Daniel Anderson", address9, department9, List.of(course9));
    protected Student student10 = new Student(null, "Jennifer Thomas", address10, department10, List.of(course10));
    protected Student student11 = new Student(null, "Talha Dilber", null, null, List.of());

    protected Authorization authorization1 = new Authorization(null, "auth1", "/url1", "icon1");
    protected Authorization authorization2 = new Authorization(null, "auth2", "/url2", "icon2");
    protected Authorization authorization3 = new Authorization(null, "auth3", "/url3", "icon3");
    protected Authorization authorization4 = new Authorization(null, "auth4", "/url4", "icon4");
    protected Authorization authorization5 = new Authorization(null, "auth5", "/url5", "icon5");

    protected Role role1 = new Role(null, "role1", "description1");
    protected Role role2 = new Role(null, "role2", "description2");
    protected Role role3 = new Role(null, "role3", "description3");
    protected Role role4 = new Role(null, "role4", "description4");
    protected Role role5 = new Role(null, "role5", "description5");

    protected RoleAuthorization roleAuthorization1 = new RoleAuthorization(null, role1, authorization1);
    protected RoleAuthorization roleAuthorization2 = new RoleAuthorization(null, role2, authorization2);
    protected RoleAuthorization roleAuthorization3 = new RoleAuthorization(null, role3, authorization3);
    protected RoleAuthorization roleAuthorization4 = new RoleAuthorization(null, role4, authorization4);
    protected RoleAuthorization roleAuthorization5 = new RoleAuthorization(null, role5, authorization5);


    protected AdminUser adminUser1 = new AdminUser(null, "admin1", "password1", List.of(role1));
    protected AdminUser adminUser2 = new AdminUser(null, "admin2", "password2", List.of(role2));
    protected AdminUser adminUser3 = new AdminUser(null, "admin3", "password3", List.of(role3));
    protected AdminUser adminUser4 = new AdminUser(null, "admin4", "password4", List.of(role4));
    protected AdminUser adminUser5 = new AdminUser(null, "admin5", "password5", List.of(role5));

    public BaseTestInstance() {
        user1 = new User(null, "Name 1", "Surname 1", 35, INSTANCE.toInstant(), User.Status.PASSIVE, User.Type.USER);
        customer1 = new Customer(null, "Customer 1", 20, INSTANCE.toInstant(), user1);
        INSTANCE.add(Calendar.MONTH, -1);
        user2 = new User(null, "Name 2", "Surname 1", 36, INSTANCE.toInstant(), User.Status.ACTIVE, User.Type.ADMIN);
        customer2 = new Customer(null, "Customer 2", 21, INSTANCE.toInstant(), user2);
        INSTANCE.add(Calendar.MONTH, -1);
        user3 = new User(null, "Name 3", "Surname 1", 37, INSTANCE.toInstant(), User.Status.PASSIVE, User.Type.USER);
        customer3 = new Customer(null, "Customer 3", 22, INSTANCE.toInstant(), user3);
        INSTANCE.add(Calendar.MONTH, -1);
        user4 = new User(null, "Name 4", "Surname 1", 38, INSTANCE.toInstant(), User.Status.ACTIVE, User.Type.USER);
        customer4 = new Customer(null, "Customer 4", 23, INSTANCE.toInstant(), user4);
        INSTANCE.add(Calendar.MONTH, -1);
        user5 = new User(null, "Name 5", "Surname 1", 39, INSTANCE.toInstant(), User.Status.PASSIVE, User.Type.ADMIN);
        customer5 = new Customer(null, "Customer 5", 24, INSTANCE.toInstant(), user5);
        INSTANCE.add(Calendar.MONTH, -1);
        user6 = new User(null, "Name 6", "Surname 1", 40, INSTANCE.toInstant(), User.Status.ACTIVE, User.Type.ADMIN);
        customer6 = new Customer(null, "Customer 6", 25, INSTANCE.toInstant(), user6);
        INSTANCE.add(Calendar.MONTH, -1);
        user7 = new User(null, "Name 7", "Surname 1", 41, INSTANCE.toInstant(), User.Status.ACTIVE, User.Type.USER);
        customer7 = new Customer(null, "Customer 7", 26, INSTANCE.toInstant(), user7);
        INSTANCE.add(Calendar.MONTH, -1);
        user8 = new User(null, "Name 8", "Surname 1", 42, INSTANCE.toInstant(), User.Status.PASSIVE, User.Type.ADMIN);
        customer8 = new Customer(null, null, 27, INSTANCE.toInstant(), user8);

    }


    @BeforeAll
    public void init() {
        if (userRepository.count() != 0) {
            return;
        }
        userRepository.save(user1);
        userRepository.save(user2);
        userRepository.save(user3);
        userRepository.save(user4);
        userRepository.save(user5);
        userRepository.save(user6);
        userRepository.save(user7);
        userRepository.save(user8);

        if (customerRepository.count() != 0) {
            return;
        }
        customerRepository.save(customer1);
        customerRepository.save(customer2);
        customerRepository.save(customer3);
        customerRepository.save(customer4);
        customerRepository.save(customer5);
        customerRepository.save(customer6);
        customerRepository.save(customer7);
        customerRepository.save(customer8);

        if (addressRepository.count() != 0) {
            return;
        }
        
        // Save departments and courses first (no cascade dependencies)
        departmentRepository.saveAllAndFlush(List.of(department1, department2, department3, department4, department5, department6, department7, department8, department9, department10));
        courseRepository.saveAllAndFlush(List.of(course1, course2, course3, course4, course5, course6, course7, course8, course9, course10));

        // Save students with cascade - this will automatically save addresses due to CascadeType.ALL
        studentRepository.saveAllAndFlush(List.of(student1, student2, student3, student4, student5, student6, student7, student8, student9, student10, student11));

        if (authorizationRepository.count() != 0) {
            return;
        }
        authorizationRepository.saveAllAndFlush(List.of(authorization1, authorization2, authorization3, authorization4, authorization5));

        roleRepository.saveAllAndFlush(List.of(role1, role2, role3, role4, role5));

        roleAuthorizationRepository.saveAllAndFlush(List.of(roleAuthorization1, roleAuthorization2, roleAuthorization3, roleAuthorization4, roleAuthorization5));

        adminUserRepository.saveAllAndFlush(List.of(adminUser1, adminUser2, adminUser3, adminUser4, adminUser5));
    }

}
