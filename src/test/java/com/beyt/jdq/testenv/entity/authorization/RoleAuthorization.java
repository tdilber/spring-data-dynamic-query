// RoleAuthorization.java
package com.beyt.jdq.testenv.entity.authorization;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.Objects;

@Entity
@Table(name = "role_authorization")
public class RoleAuthorization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToOne
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "authorization_id")
    private Authorization authorization;

    // Constructors
    public RoleAuthorization() {
    }

    public RoleAuthorization(Long id, Role role, Authorization authorization) {
        this.id = id;
        this.role = role;
        this.authorization = authorization;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Authorization getAuthorization() {
        return authorization;
    }

    public void setAuthorization(Authorization authorization) {
        this.authorization = authorization;
    }

    // FieldNameConstants
    public static final class Fields {
        public static final String id = "id";
        public static final String role = "role";
        public static final String authorization = "authorization";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoleAuthorization)) return false;
        RoleAuthorization that = (RoleAuthorization) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "RoleAuthorization{" +
                "id=" + id +
                ", authorization=" + authorization +
                '}';
    }
}
