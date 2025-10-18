package com.beyt.jdq.elasticsearch.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Elasticsearch Customer entity for testing.
 * References User entity via nested object or ID.
 */
@Document(indexName = "customers")
public class Customer implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    @Field(type = FieldType.Keyword)
    private String name;
    
    @Field(type = FieldType.Integer)
    private Integer age;
    
    @Field(type = FieldType.Date)
    private Instant birthdate;

    @Field(type = FieldType.Long)
    private Long userId;
    
    @Field(type = FieldType.Nested)
    private User user;

    // Constructors
    public Customer() {
    }

    public Customer(Long id, String name, Integer age, Instant birthdate, User user) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.birthdate = birthdate;
        this.user = user;
        if (user != null) {
            this.userId = user.getId();
        }
    }

    // Getters and Setters
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

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Instant getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(Instant birthdate) {
        this.birthdate = birthdate;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            this.userId = user.getId();
        }
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer)) return false;
        Customer customer = (Customer) o;
        return Objects.equals(id, customer.id) && 
               Objects.equals(name, customer.name) && 
               Objects.equals(age, customer.age) && 
               Objects.equals(birthdate, customer.birthdate) && 
               Objects.equals(user, customer.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, age, birthdate, user);
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", age=" + age +
                ", birthdate=" + birthdate +
                ", user=" + user +
                '}';
    }
}

