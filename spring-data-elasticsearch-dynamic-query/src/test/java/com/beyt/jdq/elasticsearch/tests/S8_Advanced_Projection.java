package com.beyt.jdq.elasticsearch.tests;

import com.beyt.jdq.core.model.annotation.JdqField;
import com.beyt.jdq.core.model.annotation.JdqModel;
import com.beyt.jdq.core.model.annotation.JdqSubModel;
import com.beyt.jdq.core.model.Criteria;
import com.beyt.jdq.core.model.CriteriaList;
import com.beyt.jdq.core.model.DynamicQuery;
import com.beyt.jdq.core.model.enums.CriteriaOperator;
import com.beyt.jdq.elasticsearch.ElasticsearchTestApplication;
import com.beyt.jdq.elasticsearch.entity.AdminUser;
import com.beyt.jdq.elasticsearch.util.PresentationUtil;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Pair;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Elasticsearch tests for ADVANCED PROJECTION operations.
 * 
 * NOTE: These tests are currently disabled because MongoSearchQueryTemplate does not yet support
 * deep nested joins through multiple $lookup stages in aggregation pipeline.
 * 
 * Tests various projection techniques including:
 * - Manual field mapping
 * - @JdqModel and @JdqField annotations
 * - @JdqSubModel for nested projections
 * - Records vs Classes
 * 
 * Current implementation status:
 * ✓ Entity structure created with proper relationships
 * ✓ Test data setup complete
 * ✗ MongoSearchQueryTemplate needs multiple $lookup stages for deep nesting
 * ✗ Need to handle projection mapping after $lookup operations
 */
@SpringBootTest(classes = ElasticsearchTestApplication.class)
@ActiveProfiles("estest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class S8_Advanced_Projection extends BaseElasticsearchJoinTestInstance {

    public static class AuthorizationSummary {
        private Long adminId;
        private String adminUsername;
        private Long roleId;
        private String roleName;
        private Long authorizationId;
        private String authorizationName;
        private String menuIcon;

        public AuthorizationSummary() {
        }

        public AuthorizationSummary(Long adminId, String adminUsername, Long roleId, String roleName, Long authorizationId, String authorizationName, String menuIcon) {
            this.adminId = adminId;
            this.adminUsername = adminUsername;
            this.roleId = roleId;
            this.roleName = roleName;
            this.authorizationId = authorizationId;
            this.authorizationName = authorizationName;
            this.menuIcon = menuIcon;
        }

        public Long getAdminId() {
            return adminId;
        }

        public void setAdminId(Long adminId) {
            this.adminId = adminId;
        }

        public String getAdminUsername() {
            return adminUsername;
        }

        public void setAdminUsername(String adminUsername) {
            this.adminUsername = adminUsername;
        }

        public Long getRoleId() {
            return roleId;
        }

        public void setRoleId(Long roleId) {
            this.roleId = roleId;
        }

        public String getRoleName() {
            return roleName;
        }

        public void setRoleName(String roleName) {
            this.roleName = roleName;
        }

        public Long getAuthorizationId() {
            return authorizationId;
        }

        public void setAuthorizationId(Long authorizationId) {
            this.authorizationId = authorizationId;
        }

        public String getAuthorizationName() {
            return authorizationName;
        }

        public void setAuthorizationName(String authorizationName) {
            this.authorizationName = authorizationName;
        }

        public String getMenuIcon() {
            return menuIcon;
        }

        public void setMenuIcon(String menuIcon) {
            this.menuIcon = menuIcon;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AuthorizationSummary)) return false;
            AuthorizationSummary that = (AuthorizationSummary) o;
            return Objects.equals(adminId, that.adminId) &&
                    Objects.equals(adminUsername, that.adminUsername);
        }

        @Override
        public int hashCode() {
            return Objects.hash(adminId, adminUsername, roleId, roleName, authorizationId, authorizationName, menuIcon);
        }
    }

    /**
     * Manual projection with deep nested join
     * Requires multiple $lookup stages to traverse:
     * AdminUser -> roles -> roleAuthorizations -> authorization
     */
    @Test
    public void roleJoin() {
        DynamicQuery dynamicQuery = new DynamicQuery();
        dynamicQuery.getSelect().add(Pair.of("id", "adminId"));
        dynamicQuery.getSelect().add(Pair.of("username", "adminUsername"));
        dynamicQuery.getSelect().add(Pair.of("roles.id", "roleId"));
        dynamicQuery.getSelect().add(Pair.of("roles.name", "roleName"));
        dynamicQuery.getSelect().add(Pair.of("roles.roleAuthorizations.authorization.id", "authorizationId"));
        dynamicQuery.getSelect().add(Pair.of("roles.roleAuthorizations.authorization.name", "authorizationName"));
        dynamicQuery.getSelect().add(Pair.of("roles.roleAuthorizations.authorization.menuIcon", "menuIcon"));
        var criteriaList = CriteriaList.of(Criteria.of("roles.roleAuthorizations.authorization.menuIcon", CriteriaOperator.START_WITH, "icon"));
        dynamicQuery.getWhere().addAll(criteriaList);
        PresentationUtil.prettyPrint(dynamicQuery);
        List<AdminUser> result = adminUserRepository.findAll(criteriaList);
        PresentationUtil.prettyPrint(result);
        assertEquals(List.of(adminUser1, adminUser2, adminUser3, adminUser4, adminUser5), result);

        List<AuthorizationSummary> result2 = adminUserRepository.findAll(dynamicQuery, AuthorizationSummary.class);
        PresentationUtil.prettyPrint(result2);

        assertEquals(List.of(new AuthorizationSummary(1L, "admin1", 1L, "role1", 1L, "auth1", "icon1"),
                new AuthorizationSummary(2L, "admin2", 2L, "role2", 2L, "auth2", "icon2"),
                new AuthorizationSummary(3L, "admin3", 3L, "role3", 3L, "auth3", "icon3"),
                new AuthorizationSummary(4L, "admin4", 4L, "role4", 4L, "auth4", "icon4"),
                new AuthorizationSummary(5L, "admin5", 5L, "role5", 5L, "auth5", "icon5")), result2);
    }


    @JdqModel // DONT MISS THIS ANNOTATION
    public static class AnnotatedAuthorizationSummary {
        @JdqField("id")
        private Long adminId;
        @JdqField("username")
        private String adminUsername;
        @JdqField("roles.id")
        private Long roleId;
        @JdqField("roles.name")
        private String roleName;
        @JdqField("roles.roleAuthorizations.authorization.id")
        private Long authorizationId;
        @JdqField("roles.roleAuthorizations.authorization.name")
        private String authorizationName;
        @JdqField("roles.roleAuthorizations.authorization.menuIcon")
        private String menuIcon;

        public AnnotatedAuthorizationSummary() {
        }

        public AnnotatedAuthorizationSummary(Long adminId, String adminUsername, Long roleId, String roleName, Long authorizationId, String authorizationName, String menuIcon) {
            this.adminId = adminId;
            this.adminUsername = adminUsername;
            this.roleId = roleId;
            this.roleName = roleName;
            this.authorizationId = authorizationId;
            this.authorizationName = authorizationName;
            this.menuIcon = menuIcon;
        }

        public Long getAdminId() {
            return adminId;
        }

        public void setAdminId(Long adminId) {
            this.adminId = adminId;
        }

        public String getAdminUsername() {
            return adminUsername;
        }

        public void setAdminUsername(String adminUsername) {
            this.adminUsername = adminUsername;
        }

        public Long getRoleId() {
            return roleId;
        }

        public void setRoleId(Long roleId) {
            this.roleId = roleId;
        }

        public String getRoleName() {
            return roleName;
        }

        public void setRoleName(String roleName) {
            this.roleName = roleName;
        }

        public Long getAuthorizationId() {
            return authorizationId;
        }

        public void setAuthorizationId(Long authorizationId) {
            this.authorizationId = authorizationId;
        }

        public String getAuthorizationName() {
            return authorizationName;
        }

        public void setAuthorizationName(String authorizationName) {
            this.authorizationName = authorizationName;
        }

        public String getMenuIcon() {
            return menuIcon;
        }

        public void setMenuIcon(String menuIcon) {
            this.menuIcon = menuIcon;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AnnotatedAuthorizationSummary)) return false;
            AnnotatedAuthorizationSummary that = (AnnotatedAuthorizationSummary) o;
            return Objects.equals(adminId, that.adminId) &&
                    Objects.equals(adminUsername, that.adminUsername) &&
                    Objects.equals(roleId, that.roleId) &&
                    Objects.equals(roleName, that.roleName) &&
                    Objects.equals(authorizationId, that.authorizationId) &&
                    Objects.equals(authorizationName, that.authorizationName) &&
                    Objects.equals(menuIcon, that.menuIcon);
        }

        @Override
        public int hashCode() {
            return Objects.hash(adminId, adminUsername, roleId, roleName, authorizationId, authorizationName, menuIcon);
        }
    }

    /**
     * Annotated projection - uses @JdqField to map entity fields to projection fields
     */
    @Test
    public void roleJoinWithAnnotatedModel() {
        DynamicQuery dynamicQuery = new DynamicQuery();
        dynamicQuery.getWhere().add(Criteria.of("roles.roleAuthorizations.authorization.menuIcon", CriteriaOperator.START_WITH, "icon"));
        PresentationUtil.prettyPrint(dynamicQuery);
        List<AnnotatedAuthorizationSummary> result2 = adminUserRepository.findAll(dynamicQuery, AnnotatedAuthorizationSummary.class);
        PresentationUtil.prettyPrint(result2);

        assertEquals(List.of(new AnnotatedAuthorizationSummary(1L, "admin1", 1L, "role1", 1L, "auth1", "icon1"),
                new AnnotatedAuthorizationSummary(2L, "admin2", 2L, "role2", 2L, "auth2", "icon2"),
                new AnnotatedAuthorizationSummary(3L, "admin3", 3L, "role3", 3L, "auth3", "icon3"),
                new AnnotatedAuthorizationSummary(4L, "admin4", 4L, "role4", 4L, "auth4", "icon4"),
                new AnnotatedAuthorizationSummary(5L, "admin5", 5L, "role5", 5L, "auth5", "icon5")), result2);
    }


    @JdqModel
    public static class AnnotatedAuthorizationSummarySubModel {
        @JdqField("id")
        private Long adminId;
        @JdqField("username")
        private String adminUsername;

        @JdqSubModel("roles")
        private Role role;

        public AnnotatedAuthorizationSummarySubModel() {
        }

        public AnnotatedAuthorizationSummarySubModel(Long adminId, String adminUsername, Role role) {
            this.adminId = adminId;
            this.adminUsername = adminUsername;
            this.role = role;
        }

        public Long getAdminId() {
            return adminId;
        }

        public void setAdminId(Long adminId) {
            this.adminId = adminId;
        }

        public String getAdminUsername() {
            return adminUsername;
        }

        public void setAdminUsername(String adminUsername) {
            this.adminUsername = adminUsername;
        }

        public Role getRole() {
            return role;
        }

        public void setRole(Role role) {
            this.role = role;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AnnotatedAuthorizationSummarySubModel)) return false;
            AnnotatedAuthorizationSummarySubModel that = (AnnotatedAuthorizationSummarySubModel) o;
            return Objects.equals(adminId, that.adminId) &&
                    Objects.equals(adminUsername, that.adminUsername) &&
                    Objects.equals(role, that.role);
        }

        @Override
        public int hashCode() {
            return Objects.hash(adminId, adminUsername, role);
        }

        @JdqModel
        public record Role(
                @JdqField("id") Long roleId,
                @JdqField("name") String roleName,
                @JdqSubModel("roleAuthorizations") RoleAuthorization roleAuthorization) {


            @JdqModel
            public record RoleAuthorization(
                    @JdqField("authorization.id") Long authorizationId,
                    @JdqField("authorization.name") String authorizationName,
                    @JdqField("authorization.menuIcon") String menuIcon
            ) {

            }
        }
    }


    /**
     * Nested projection with @JdqSubModel
     * Tests hierarchical projection structure
     */
    @Test
    public void roleJoinAnnotatedAuthorizationSummarySubModel() {
        DynamicQuery dynamicQuery = new DynamicQuery();
        dynamicQuery.getWhere().add(Criteria.of("roles.roleAuthorizations.authorization.menuIcon", CriteriaOperator.START_WITH, "icon"));
        PresentationUtil.prettyPrint(dynamicQuery);
        List<AnnotatedAuthorizationSummarySubModel> result2 = adminUserRepository.findAll(dynamicQuery, AnnotatedAuthorizationSummarySubModel.class);
        PresentationUtil.prettyPrint(result2);

        assertEquals(List.of(new AnnotatedAuthorizationSummarySubModel(1L, "admin1", new AnnotatedAuthorizationSummarySubModel.Role(1L, "role1", new AnnotatedAuthorizationSummarySubModel.Role.RoleAuthorization(1L, "auth1", "icon1"))),
                new AnnotatedAuthorizationSummarySubModel(2L, "admin2", new AnnotatedAuthorizationSummarySubModel.Role(2L, "role2", new AnnotatedAuthorizationSummarySubModel.Role.RoleAuthorization(2L, "auth2", "icon2"))),
                new AnnotatedAuthorizationSummarySubModel(3L, "admin3", new AnnotatedAuthorizationSummarySubModel.Role(3L, "role3", new AnnotatedAuthorizationSummarySubModel.Role.RoleAuthorization(3L, "auth3", "icon3"))),
                new AnnotatedAuthorizationSummarySubModel(4L, "admin4", new AnnotatedAuthorizationSummarySubModel.Role(4L, "role4", new AnnotatedAuthorizationSummarySubModel.Role.RoleAuthorization(4L, "auth4", "icon4"))),
                new AnnotatedAuthorizationSummarySubModel(5L, "admin5", new AnnotatedAuthorizationSummarySubModel.Role(5L, "role5", new AnnotatedAuthorizationSummarySubModel.Role.RoleAuthorization(5L, "auth5", "icon5")))), result2);
    }


    @JdqModel
    public record AnnotatedAuthorizationSummarySubModel2(
            @JdqField("id") Long adminId,
            @JdqField("username") String adminUsername,
            @JdqSubModel("roles") Role2 role) {

        @JdqModel
        public static class Role2 {
            @JdqField("id")
            private Long roleId;
            @JdqField("name")
            private String roleName;
            @JdqSubModel("roleAuthorizations")
            private RoleAuthorization2 roleAuthorization;

            public Role2() {
            }

            public Role2(Long roleId, String roleName, RoleAuthorization2 roleAuthorization) {
                this.roleId = roleId;
                this.roleName = roleName;
                this.roleAuthorization = roleAuthorization;
            }

            public Long getRoleId() {
                return roleId;
            }

            public void setRoleId(Long roleId) {
                this.roleId = roleId;
            }

            public String getRoleName() {
                return roleName;
            }

            public void setRoleName(String roleName) {
                this.roleName = roleName;
            }

            public RoleAuthorization2 getRoleAuthorization() {
                return roleAuthorization;
            }

            public void setRoleAuthorization(RoleAuthorization2 roleAuthorization) {
                this.roleAuthorization = roleAuthorization;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Role2)) return false;
                Role2 role2 = (Role2) o;
                return Objects.equals(roleId, role2.roleId) &&
                        Objects.equals(roleName, role2.roleName) &&
                        Objects.equals(roleAuthorization, role2.roleAuthorization);
            }

            @Override
            public int hashCode() {
                return Objects.hash(roleId, roleName, roleAuthorization);
            }


            @JdqModel
            public record RoleAuthorization2(
                    @JdqField("authorization.id") Long authorizationId,
                    @JdqField("authorization.name") String authorizationName,
                    @JdqField("authorization.menuIcon") String menuIcon
            ) {
            }
        }
    }

    /**
     * Mixed record and class projection with @JdqSubModel
     */
    @Test
    public void roleJoinAnnotatedAuthorizationSummarySubModel2() {
        DynamicQuery dynamicQuery = new DynamicQuery();
        dynamicQuery.getWhere().add(Criteria.of("roles.roleAuthorizations.authorization.menuIcon", CriteriaOperator.START_WITH, "icon"));
        PresentationUtil.prettyPrint(dynamicQuery);
        List<AnnotatedAuthorizationSummarySubModel2> result2 = adminUserRepository.findAll(dynamicQuery, AnnotatedAuthorizationSummarySubModel2.class);
        PresentationUtil.prettyPrint(result2);

        assertEquals(List.of(new AnnotatedAuthorizationSummarySubModel2(1L, "admin1", new AnnotatedAuthorizationSummarySubModel2.Role2(1L, "role1", new AnnotatedAuthorizationSummarySubModel2.Role2.RoleAuthorization2(1L, "auth1", "icon1"))),
                new AnnotatedAuthorizationSummarySubModel2(2L, "admin2", new AnnotatedAuthorizationSummarySubModel2.Role2(2L, "role2", new AnnotatedAuthorizationSummarySubModel2.Role2.RoleAuthorization2(2L, "auth2", "icon2"))),
                new AnnotatedAuthorizationSummarySubModel2(3L, "admin3", new AnnotatedAuthorizationSummarySubModel2.Role2(3L, "role3", new AnnotatedAuthorizationSummarySubModel2.Role2.RoleAuthorization2(3L, "auth3", "icon3"))),
                new AnnotatedAuthorizationSummarySubModel2(4L, "admin4", new AnnotatedAuthorizationSummarySubModel2.Role2(4L, "role4", new AnnotatedAuthorizationSummarySubModel2.Role2.RoleAuthorization2(4L, "auth4", "icon4"))),
                new AnnotatedAuthorizationSummarySubModel2(5L, "admin5", new AnnotatedAuthorizationSummarySubModel2.Role2(5L, "role5", new AnnotatedAuthorizationSummarySubModel2.Role2.RoleAuthorization2(5L, "auth5", "icon5")))), result2);
    }


    @JdqModel
    public record AnnotatedAuthorizationSummarySubModel3(
            @JdqField("id") Long adminId,
            @JdqField("username") String adminUsername,
            @JdqSubModel() Role3 role) { // empty sub model annotation

        @JdqModel
        public static class Role3 {
            @JdqField("roles.id")
            private Long roleId;
            @JdqField("roles.name")
            private String roleName;
            @JdqSubModel("roles.roleAuthorizations")
            private RoleAuthorization3 roleAuthorization;

            public Role3() {
            }

            public Role3(Long roleId, String roleName, RoleAuthorization3 roleAuthorization) {
                this.roleId = roleId;
                this.roleName = roleName;
                this.roleAuthorization = roleAuthorization;
            }

            public Long getRoleId() {
                return roleId;
            }

            public void setRoleId(Long roleId) {
                this.roleId = roleId;
            }

            public String getRoleName() {
                return roleName;
            }

            public void setRoleName(String roleName) {
                this.roleName = roleName;
            }

            public RoleAuthorization3 getRoleAuthorization() {
                return roleAuthorization;
            }

            public void setRoleAuthorization(RoleAuthorization3 roleAuthorization) {
                this.roleAuthorization = roleAuthorization;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Role3)) return false;
                Role3 role3 = (Role3) o;
                return Objects.equals(roleId, role3.roleId) &&
                        Objects.equals(roleName, role3.roleName) &&
                        Objects.equals(roleAuthorization, role3.roleAuthorization);
            }

            @Override
            public int hashCode() {
                return Objects.hash(roleId, roleName, roleAuthorization);
            }


            @JdqModel
            public record RoleAuthorization3(
                    @JdqField("authorization.id") Long authorizationId,
                    @JdqField("authorization.name") String authorizationName,
                    @JdqField("authorization.menuIcon") String menuIcon
            ) {
            }
        }
    }

    /**
     * Empty @JdqSubModel annotation - fields use full path
     */
    @Test
    public void roleJoinAnnotatedAuthorizationSummarySubModel3EmptySubModel() {
        DynamicQuery dynamicQuery = new DynamicQuery();
        dynamicQuery.getWhere().add(Criteria.of("roles.roleAuthorizations.authorization.menuIcon", CriteriaOperator.START_WITH, "icon"));
        PresentationUtil.prettyPrint(dynamicQuery);
        List<AnnotatedAuthorizationSummarySubModel3> result2 = adminUserRepository.findAll(dynamicQuery, AnnotatedAuthorizationSummarySubModel3.class);
        PresentationUtil.prettyPrint(result2);

        assertEquals(List.of(new AnnotatedAuthorizationSummarySubModel3(1L, "admin1", new AnnotatedAuthorizationSummarySubModel3.Role3(1L, "role1", new AnnotatedAuthorizationSummarySubModel3.Role3.RoleAuthorization3(1L, "auth1", "icon1"))),
                new AnnotatedAuthorizationSummarySubModel3(2L, "admin2", new AnnotatedAuthorizationSummarySubModel3.Role3(2L, "role2", new AnnotatedAuthorizationSummarySubModel3.Role3.RoleAuthorization3(2L, "auth2", "icon2"))),
                new AnnotatedAuthorizationSummarySubModel3(3L, "admin3", new AnnotatedAuthorizationSummarySubModel3.Role3(3L, "role3", new AnnotatedAuthorizationSummarySubModel3.Role3.RoleAuthorization3(3L, "auth3", "icon3"))),
                new AnnotatedAuthorizationSummarySubModel3(4L, "admin4", new AnnotatedAuthorizationSummarySubModel3.Role3(4L, "role4", new AnnotatedAuthorizationSummarySubModel3.Role3.RoleAuthorization3(4L, "auth4", "icon4"))),
                new AnnotatedAuthorizationSummarySubModel3(5L, "admin5", new AnnotatedAuthorizationSummarySubModel3.Role3(5L, "role5", new AnnotatedAuthorizationSummarySubModel3.Role3.RoleAuthorization3(5L, "auth5", "icon5")))), result2);
    }
}

