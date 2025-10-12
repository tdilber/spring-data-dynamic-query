package com.beyt.jdq.testenv.entity.authorization;

import jakarta.persistence.*;

import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "role")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    @OneToMany(mappedBy = "role", fetch = FetchType.EAGER)
    Set<RoleAuthorization> roleAuthorizations;

    // Constructors
    public Role() {
    }

    public Role(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public Role(Long id, String name, String description, Set<RoleAuthorization> roleAuthorizations) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.roleAuthorizations = roleAuthorizations;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<RoleAuthorization> getRoleAuthorizations() {
        return roleAuthorizations;
    }

    public void setRoleAuthorizations(Set<RoleAuthorization> roleAuthorizations) {
        this.roleAuthorizations = roleAuthorizations;
    }

    // FieldNameConstants
    public static final class Fields {
        public static final String id = "id";
        public static final String name = "name";
        public static final String description = "description";
        public static final String roleAuthorizations = "roleAuthorizations";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role)) return false;
        Role role = (Role) o;
        return Objects.equals(id, role.id) && Objects.equals(name, role.name) && Objects.equals(description, role.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description);
    }

    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", roleAuthorizations=" + roleAuthorizations +
                '}';
    }
}
