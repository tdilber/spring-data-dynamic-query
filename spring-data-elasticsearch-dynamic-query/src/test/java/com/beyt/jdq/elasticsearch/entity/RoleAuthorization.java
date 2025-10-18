package com.beyt.jdq.elasticsearch.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Objects;

/**
 * Elasticsearch RoleAuthorization entity for testing.
 * Junction entity connecting Roles and Authorizations.
 */
@Document(indexName = "role_authorizations")
public class RoleAuthorization {
    
    @Id
    private Long id;
    
    @Field(type = FieldType.Long)
    private Long roleId;
    
    @Field(type = FieldType.Long)
    private Long authorizationId;
    
    @Field(type = FieldType.Nested)
    private Authorization authorization;

    public RoleAuthorization() {
    }

    public RoleAuthorization(Long id, Long roleId, Authorization authorization) {
        this.id = id;
        this.roleId = roleId;
        this.authorization = authorization;
        if (authorization != null) {
            this.authorizationId = authorization.getId();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Authorization getAuthorization() {
        return authorization;
    }

    public void setAuthorization(Authorization authorization) {
        this.authorization = authorization;
        if (authorization != null) {
            this.authorizationId = authorization.getId();
        }
    }

    public Long getAuthorizationId() {
        return authorizationId;
    }

    public void setAuthorizationId(Long authorizationId) {
        this.authorizationId = authorizationId;
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
                ", roleId=" + roleId +
                ", authorization=" + authorization +
                '}';
    }
}


