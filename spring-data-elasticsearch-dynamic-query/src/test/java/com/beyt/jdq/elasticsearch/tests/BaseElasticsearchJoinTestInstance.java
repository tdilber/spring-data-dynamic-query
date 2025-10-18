package com.beyt.jdq.elasticsearch.tests;

import com.beyt.jdq.elasticsearch.entity.*;
import com.beyt.jdq.elasticsearch.repository.*;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Base test instance for Elasticsearch Join tests.
 * Provides test data setup for Student, Department, Course, and Authorization entities.
 */
public abstract class BaseElasticsearchJoinTestInstance {

    @Autowired
    protected ElasticsearchCourseRepository courseRepository;

    @Autowired
    protected ElasticsearchStudentRepository studentRepository;

    @Autowired
    protected ElasticsearchDepartmentRepository departmentRepository;

    @Autowired
    protected ElasticsearchAdminUserRepository adminUserRepository;

    @Autowired
    protected ElasticsearchRoleRepository roleRepository;

    @Autowired
    protected ElasticsearchAuthorizationRepository authorizationRepository;

    @Autowired
    protected ElasticsearchRoleAuthorizationRepository roleAuthorizationRepository;

    public static final Calendar INSTANCE = Calendar.getInstance();

    // Test departments
    protected Department department1;
    protected Department department2;
    protected Department department3;
    protected Department department4;
    protected Department department5;
    protected Department department6;
    protected Department department7;
    protected Department department8;
    protected Department department9;
    protected Department department10;

    // Test addresses (embedded in students)
    protected Address address1;
    protected Address address2;
    protected Address address3;
    protected Address address4;
    protected Address address5;
    protected Address address6;
    protected Address address7;
    protected Address address8;
    protected Address address9;
    protected Address address10;

    // Test courses
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

    // Test students
    protected Student student1;
    protected Student student2;
    protected Student student3;
    protected Student student4;
    protected Student student5;
    protected Student student6;
    protected Student student7;
    protected Student student8;
    protected Student student9;
    protected Student student10;
    protected Student student11;

    // Test authorizations
    protected Authorization authorization1;
    protected Authorization authorization2;
    protected Authorization authorization3;
    protected Authorization authorization4;
    protected Authorization authorization5;

    // Test roles
    protected Role role1;
    protected Role role2;
    protected Role role3;
    protected Role role4;
    protected Role role5;

    // Test role authorizations
    protected RoleAuthorization roleAuthorization1;
    protected RoleAuthorization roleAuthorization2;
    protected RoleAuthorization roleAuthorization3;
    protected RoleAuthorization roleAuthorization4;
    protected RoleAuthorization roleAuthorization5;

    // Test admin users
    protected AdminUser adminUser1;
    protected AdminUser adminUser2;
    protected AdminUser adminUser3;
    protected AdminUser adminUser4;
    protected AdminUser adminUser5;

    public BaseElasticsearchJoinTestInstance() {
        // Initialize data - will be saved in @BeforeAll
        initializeTestData();
    }

    private void initializeTestData() {
        // Initialize departments
        department1 = new Department(1L, "Computer Science");
        department2 = new Department(2L, "Mathematics");
        department3 = new Department(3L, "Physics");
        department4 = new Department(4L, "Chemistry");
        department5 = new Department(5L, "Biology");
        department6 = new Department(6L, "English Literature");
        department7 = new Department(7L, "History");
        department8 = new Department(8L, "Geography");
        department9 = new Department(9L, "Political Science");
        department10 = new Department(10L, "Economics");

        // Initialize addresses (will be embedded in students)
        address1 = new Address(1L, "123 Main St", "New York", "NY", "10001");
        address2 = new Address(2L, "456 Park Ave", "Chicago", "IL", "60605");
        address3 = new Address(3L, "789 Broadway", "Los Angeles", "CA", "90001");
        address4 = new Address(4L, "321 Market St", "San Francisco", "CA", "94105");
        address5 = new Address(5L, "654 Elm St", "Dallas", "TX", "75001");
        address6 = new Address(6L, "987 Oak St", "Houston", "TX", "77002");
        address7 = new Address(7L, "345 Pine St", "Philadelphia", "PA", "19019");
        address8 = new Address(8L, "678 Maple St", "Phoenix", "AZ", "85001");
        address9 = new Address(9L, "102 Beach St", "Miami", "FL", "33101");
        address10 = new Address(10L, "567 Hill St", "Atlanta", "GA", "30301");

        // Initialize courses
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

        // Initialize authorizations
        authorization1 = new Authorization(1L, "auth1", "/url1", "icon1");
        authorization2 = new Authorization(2L, "auth2", "/url2", "icon2");
        authorization3 = new Authorization(3L, "auth3", "/url3", "icon3");
        authorization4 = new Authorization(4L, "auth4", "/url4", "icon4");
        authorization5 = new Authorization(5L, "auth5", "/url5", "icon5");

        // Note: Students, roles, role authorizations, and admin users will be initialized after
        // their dependencies are saved in @BeforeAll
    }

    @BeforeAll
    public void init() {
        // Clear existing data
        courseRepository.deleteAll();
        studentRepository.deleteAll();
        departmentRepository.deleteAll();
        adminUserRepository.deleteAll();
        roleRepository.deleteAll();
        authorizationRepository.deleteAll();
        roleAuthorizationRepository.deleteAll();

        // Save departments first
        departmentRepository.save(department1);
        departmentRepository.save(department2);
        departmentRepository.save(department3);
        departmentRepository.save(department4);
        departmentRepository.save(department5);
        departmentRepository.save(department6);
        departmentRepository.save(department7);
        departmentRepository.save(department8);
        departmentRepository.save(department9);
        departmentRepository.save(department10);

        // Save courses
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

        // Initialize and save students with embedded addresses and references to departments and courses
        student1 = new Student(1L, "John Doe", address1, department1, List.of(course1, course2));
        student2 = new Student(2L, "Jane Smith", address2, department2, List.of(course2, course4));
        student3 = new Student(3L, "Robert Johnson", address3, department3, List.of(course3));
        student4 = new Student(4L, "Emily Davis", address4, department4, List.of(course4));
        student5 = new Student(5L, "Michael Miller", address5, department5, List.of(course5));
        student6 = new Student(6L, "Sarah Wilson", address6, department6, List.of(course6));
        student7 = new Student(7L, "David Moore", address7, department7, List.of(course7));
        student8 = new Student(8L, "Jessica Taylor", address8, department8, List.of(course8));
        student9 = new Student(9L, "Daniel Anderson", address9, department9, List.of(course9));
        student10 = new Student(10L, "Jennifer Thomas", address10, department10, List.of(course10));
        student11 = new Student(11L, "Talha Dilber", null, null, List.of());

        studentRepository.save(student1);
        studentRepository.save(student2);
        studentRepository.save(student3);
        studentRepository.save(student4);
        studentRepository.save(student5);
        studentRepository.save(student6);
        studentRepository.save(student7);
        studentRepository.save(student8);
        studentRepository.save(student9);
        studentRepository.save(student10);
        studentRepository.save(student11);
        
        // Update departments with their students (bidirectional relationship for Elasticsearch)
        department1.setStudents(List.of(student1));
        department2.setStudents(List.of(student2));
        department3.setStudents(List.of(student3));
        department4.setStudents(List.of(student4));
        department5.setStudents(List.of(student5));
        department6.setStudents(List.of(student6));
        department7.setStudents(List.of(student7));
        department8.setStudents(List.of(student8));
        department9.setStudents(List.of(student9));
        department10.setStudents(List.of(student10));
        
        departmentRepository.save(department1);
        departmentRepository.save(department2);
        departmentRepository.save(department3);
        departmentRepository.save(department4);
        departmentRepository.save(department5);
        departmentRepository.save(department6);
        departmentRepository.save(department7);
        departmentRepository.save(department8);
        departmentRepository.save(department9);
        departmentRepository.save(department10);

        // Save authorizations
        authorizationRepository.save(authorization1);
        authorizationRepository.save(authorization2);
        authorizationRepository.save(authorization3);
        authorizationRepository.save(authorization4);
        authorizationRepository.save(authorization5);

        // Initialize and save role authorizations
        roleAuthorization1 = new RoleAuthorization(1L, 1L, authorization1);
        roleAuthorization2 = new RoleAuthorization(2L, 2L, authorization2);
        roleAuthorization3 = new RoleAuthorization(3L, 3L, authorization3);
        roleAuthorization4 = new RoleAuthorization(4L, 4L, authorization4);
        roleAuthorization5 = new RoleAuthorization(5L, 5L, authorization5);

        roleAuthorizationRepository.save(roleAuthorization1);
        roleAuthorizationRepository.save(roleAuthorization2);
        roleAuthorizationRepository.save(roleAuthorization3);
        roleAuthorizationRepository.save(roleAuthorization4);
        roleAuthorizationRepository.save(roleAuthorization5);

        // Initialize and save roles with role authorizations
        role1 = new Role(1L, "role1", "description1", List.of(roleAuthorization1));
        role2 = new Role(2L, "role2", "description2", List.of(roleAuthorization2));
        role3 = new Role(3L, "role3", "description3", List.of(roleAuthorization3));
        role4 = new Role(4L, "role4", "description4", List.of(roleAuthorization4));
        role5 = new Role(5L, "role5", "description5", List.of(roleAuthorization5));

        roleRepository.save(role1);
        roleRepository.save(role2);
        roleRepository.save(role3);
        roleRepository.save(role4);
        roleRepository.save(role5);

        // Initialize and save admin users with roles
        adminUser1 = new AdminUser(1L, "admin1", "password1", List.of(role1));
        adminUser2 = new AdminUser(2L, "admin2", "password2", List.of(role2));
        adminUser3 = new AdminUser(3L, "admin3", "password3", List.of(role3));
        adminUser4 = new AdminUser(4L, "admin4", "password4", List.of(role4));
        adminUser5 = new AdminUser(5L, "admin5", "password5", List.of(role5));

        adminUserRepository.save(adminUser1);
        adminUserRepository.save(adminUser2);
        adminUserRepository.save(adminUser3);
        adminUserRepository.save(adminUser4);
        adminUserRepository.save(adminUser5);
        
        // Update roles with their role authorizations (bidirectional relationship for Elasticsearch)
        role1.setRoleAuthorizations(List.of(roleAuthorization1));
        role2.setRoleAuthorizations(List.of(roleAuthorization2));
        role3.setRoleAuthorizations(List.of(roleAuthorization3));
        role4.setRoleAuthorizations(List.of(roleAuthorization4));
        role5.setRoleAuthorizations(List.of(roleAuthorization5));
        
        roleRepository.save(role1);
        roleRepository.save(role2);
        roleRepository.save(role3);
        roleRepository.save(role4);
        roleRepository.save(role5);
        
        // Update admin users with their roles (bidirectional relationship for Elasticsearch)
        adminUser1.setRoles(List.of(role1));
        adminUser2.setRoles(List.of(role2));
        adminUser3.setRoles(List.of(role3));
        adminUser4.setRoles(List.of(role4));
        adminUser5.setRoles(List.of(role5));
        
        adminUserRepository.save(adminUser1);
        adminUserRepository.save(adminUser2);
        adminUserRepository.save(adminUser3);
        adminUserRepository.save(adminUser4);
        adminUserRepository.save(adminUser5);
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


