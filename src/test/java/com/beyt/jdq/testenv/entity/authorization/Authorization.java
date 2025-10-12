package com.beyt.jdq.testenv.entity.authorization;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "my_authorization")
public class Authorization {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "menu_url")
    private String menuUrl;

    @Column(name = "menu_icon")
    private String menuIcon;

    @JsonIgnore
    @OneToMany(mappedBy = "authorization", fetch = FetchType.LAZY)
    Set<RoleAuthorization> roleAuthorizations;

    // Constructors
    public Authorization() {
    }

    public Authorization(Long id, String name, String menuUrl, String menuIcon) {
        this.id = id;
        this.name = name;
        this.menuUrl = menuUrl;
        this.menuIcon = menuIcon;
    }

    public Authorization(Long id, String name, String menuUrl, String menuIcon, Set<RoleAuthorization> roleAuthorizations) {
        this.id = id;
        this.name = name;
        this.menuUrl = menuUrl;
        this.menuIcon = menuIcon;
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

    public String getMenuUrl() {
        return menuUrl;
    }

    public void setMenuUrl(String menuUrl) {
        this.menuUrl = menuUrl;
    }

    public String getMenuIcon() {
        return menuIcon;
    }

    public void setMenuIcon(String menuIcon) {
        this.menuIcon = menuIcon;
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
        public static final String menuUrl = "menuUrl";
        public static final String menuIcon = "menuIcon";
        public static final String roleAuthorizations = "roleAuthorizations";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Authorization)) return false;
        Authorization that = (Authorization) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name) && Objects.equals(menuUrl, that.menuUrl) && Objects.equals(menuIcon, that.menuIcon);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, menuUrl, menuIcon);
    }

    @Override
    public String toString() {
        return "Authorization{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", menuUrl='" + menuUrl + '\'' +
                ", menuIcon='" + menuIcon + '\'' +
                '}';
    }
}
