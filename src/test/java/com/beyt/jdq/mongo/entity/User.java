package com.beyt.jdq.mongo.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * MongoDB User entity for testing.
 */
@Document(collection = "users")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    private String name;
    private String surname;
    private Integer age;
    private Instant birthdate;
    private Status status;
    private Type type;

    public enum Status {
        ACTIVE, PASSIVE
    }

    public enum Type {
        ADMIN, USER
    }

    // Constructors
    public User() {
    }

    public User(Long id, String name, String surname, Integer age, Instant birthdate, Status status, Type type) {
        this.id = id;
        this.name = name;
        this.surname = surname;
        this.age = age;
        this.birthdate = birthdate;
        this.status = status;
        this.type = type;
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

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String name;
        private String surname;
        private Integer age;
        private Instant birthdate;
        private Status status;
        private Type type;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder surname(String surname) {
            this.surname = surname;
            return this;
        }

        public Builder age(Integer age) {
            this.age = age;
            return this;
        }

        public Builder birthdate(Instant birthdate) {
            this.birthdate = birthdate;
            return this;
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        public User build() {
            return new User(id, name, surname, age, birthdate, status, type);
        }
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) && 
               Objects.equals(name, user.name) && 
               Objects.equals(surname, user.surname) && 
               Objects.equals(age, user.age) && 
               Objects.equals(birthdate, user.birthdate) && 
               status == user.status && 
               type == user.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, surname, age, birthdate, status, type);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", surname='" + surname + '\'' +
                ", age=" + age +
                ", birthdate=" + birthdate +
                ", status=" + status +
                ", type=" + type +
                '}';
    }
}

