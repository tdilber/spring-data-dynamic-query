package com.beyt.jdq.elasticsearch.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;
import java.util.Objects;

/**
 * Elasticsearch Role entity for testing.
 * Represents user roles with associated authorizations.
 */
@Document(indexName = "roles")
public class Role {
    
    @Id
    private Long id;
    
    @Field(type = FieldType.Keyword)
    private String name;
    
    @Field(type = FieldType.Text)
    private String description;
    
    @Field(type = FieldType.Long)
    private List<Long> roleAuthorizationIds;
    
    @Field(type = FieldType.Nested)
    private List<RoleAuthorization> roleAuthorizations;

    public Role() {
    }

    public Role(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public Role(Long id, String name, String description, List<RoleAuthorization> roleAuthorizations) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.roleAuthorizations = roleAuthorizations;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<RoleAuthorization> getRoleAuthorizations() {
        return roleAuthorizations;
    }

    public void setRoleAuthorizations(List<RoleAuthorization> roleAuthorizations) {
        this.roleAuthorizations = roleAuthorizations;
    }

    public List<Long> getRoleAuthorizationIds() {
        return roleAuthorizationIds;
    }

    public void setRoleAuthorizationIds(List<Long> roleAuthorizationIds) {
        this.roleAuthorizationIds = roleAuthorizationIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role)) return false;
        Role role = (Role) o;
        return Objects.equals(id, role.id) && 
               Objects.equals(name, role.name) && 
               Objects.equals(description, role.description);
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


