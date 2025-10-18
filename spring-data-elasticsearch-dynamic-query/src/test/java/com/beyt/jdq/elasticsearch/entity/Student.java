package com.beyt.jdq.elasticsearch.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;
import java.util.Objects;

/**
 * Elasticsearch Student entity for testing.
 * Uses embedded Address and nested references for Department and Courses.
 */
@Document(indexName = "students")
public class Student {
    
    @Id
    private Long id;
    
    @Field(type = FieldType.Text)
    private String name;
    
////     Embedded address - stored as part of student document
//    @Field(type = FieldType.Object)
//    private Address address;
//
    @Field(type = FieldType.Long)
    private Long departmentId;
    
//    // Reference to department
//    @Field(type = FieldType.Nested)
//    private Department department;
//
//    @Field(type = FieldType.Long)
//    private List<Long> courseIds;
//
//    // Reference to courses (many-to-many)
//    @Field(type = FieldType.Nested)
//    private List<Course> courses;

    public Student() {
    }

    public Student(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Student(Long id, String name, Address address, Department department, List<Course> courses) {
        this.id = id;
        this.name = name;
//        this.address = address;
//        this.department = department;
        if (department != null) {
            this.departmentId = department.getId();
        }
//        this.courses = courses;
    }

    public static Student of(String name) {
        Student student = new Student();
        student.setName(name);
        return student;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

//    public Address getAddress() {
//        return address;
//    }
//
//    public void setAddress(Address address) {
//        this.address = address;
//    }
//
//    public Department getDepartment() {
//        return department;
//    }
//
//    public void setDepartment(Department department) {
//        this.department = department;
//        if (department != null) {
//            this.departmentId = department.getId();
//        }
//    }

//    public List<Course> getCourses() {
//        return courses;
//    }
//
//    public void setCourses(List<Course> courses) {
//        this.courses = courses;
//    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

//    public List<Long> getCourseIds() {
//        return courseIds;
//    }
//
//    public void setCourseIds(List<Long> courseIds) {
//        this.courseIds = courseIds;
//    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Student)) return false;
        Student student = (Student) o;
        return Objects.equals(id, student.id) && Objects.equals(name, student.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "Student{" +
                "id=" + id +
                ", name='" + name + '\'' +
//                ", address=" + address +
//                ", department=" + department +
//                ", courses=" + courses +
                '}';
    }
}


