package com.beyt.jdq.elasticsearch.tests;

import com.beyt.jdq.core.model.enums.Order;
import com.beyt.jdq.elasticsearch.ElasticsearchTestApplication;
import com.beyt.jdq.elasticsearch.repository.ElasticsearchAdminUserRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Objects;

import static com.beyt.jdq.core.model.builder.QuerySimplifier.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Elasticsearch tests for QUERY BUILDER functionality.
 * 
 * NOTE: These tests are currently disabled because MongoSearchQueryTemplate does not yet support
 * the queryBuilder() method which is available in JPA repositories.
 * 
 * Current implementation status:
 * ✓ MongoAdminUserRepository interface created
 * ✓ Test data setup complete with AdminUser, Role, RoleAuthorization, Authorization entities
 * ✗ queryBuilder() method not implemented in JpaDynamicQueryMongoRepository interface
 * ✗ MongoQueryBuilder class needs to be created (similar to QueryBuilder for JPA)
 * 
 * Elasticsearch Query Builder challenges:
 * - Query Builder uses fluent API that builds DynamicQuery objects
 * - Current implementation uses JpaDynamicQueryRepository which is JPA-specific
 * - Need to create MongoQueryBuilder that works with MongoSearchQueryTemplate
 * - Builder pattern should support: select, distinct, where, orderBy, page
 * - Should integrate seamlessly with existing QuerySimplifier static methods
 * 
 * To enable these tests:
 * 1. Add queryBuilder() method to JpaDynamicQueryMongoRepository interface
 * 2. Create MongoQueryBuilder class with fluent API (similar to QueryBuilder)
 * 3. Implement builder in JpaDynamicQueryMongoRepositoryImpl
 * 4. Support all builder operations: select, distinct, where, orderBy, page
 * 5. Return results using MongoSearchQueryTemplate methods
 * 
 * Example usage (once implemented):
 * adminUserRepository.queryBuilder()
 *     .select(Select("id", "adminId"), Select("username", "adminUsername"))
 *     .where(Field("roles.name").contain("admin"))
 *     .orderBy(OrderBy("id", Order.DESC))
 *     .page(0, 10)
 *     .getResultAsPage(AuthorizationSummary.class);
 */
@SpringBootTest(classes = ElasticsearchTestApplication.class)
@ActiveProfiles("estest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class S9_Query_Builder extends BaseElasticsearchJoinTestInstance {
    private @Autowired ElasticsearchAdminUserRepository adminUserRepository;

    /**
     * DTO for authorization summary projection
     * Maps deeply nested fields from AdminUser -> Role -> RoleAuthorization -> Authorization
     */
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
            if (!(o instanceof AuthorizationSummary that)) return false;
            return Objects.equals(adminId, that.adminId) && Objects.equals(adminUsername, that.adminUsername) && Objects.equals(roleId, that.roleId) && Objects.equals(roleName, that.roleName) && Objects.equals(authorizationId, that.authorizationId) && Objects.equals(authorizationName, that.authorizationName) && Objects.equals(menuIcon, that.menuIcon);
        }

        @Override
        public int hashCode() {
            return Objects.hash(adminId, adminUsername, roleId, roleName, authorizationId, authorizationName, menuIcon);
        }

        @Override
        public String toString() {
            return "AuthorizationSummary{" +
                    "adminId=" + adminId +
                    ", adminUsername='" + adminUsername + '\'' +
                    ", roleId=" + roleId +
                    ", roleName='" + roleName + '\'' +
                    ", authorizationId=" + authorizationId +
                    ", authorizationName='" + authorizationName + '\'' +
                    ", menuIcon='" + menuIcon + '\'' +
                    '}';
        }
    }

    /**
     * Query Builder test with complex nested queries
     * 
     * This test demonstrates:
     * - Deep nested field selection with aliasing
     * - Complex WHERE conditions with multiple levels of nesting
     * - Parenthesis grouping with OR operations
     * - OrderBy on nested fields
     * - Pagination
     * 
     * Elasticsearch equivalent would require:
     * - Multiple $lookup stages for joins
     * - $unwind operations for array fields
     * - $project for field selection and aliasing
     * - $match for where conditions
     * - $sort for ordering
     * - $skip and $limit for pagination
     * 
     * Path structure:
     * AdminUser -> roles[] -> roleAuthorizations[] -> authorization -> {id, name, menuIcon}
     */
    @Test
    public void queryBuilder() {
        Page<AuthorizationSummary> result = adminUserRepository.queryBuilder()
                .select(Select("id", "adminId"),
                        Select("username", "adminUsername"),
                        Select("roles.id", "roleId"),
                        Select("roles.name", "roleName"),
                        Select("roles.roleAuthorizations.authorization.id", "authorizationId"),
                        Select("roles.roleAuthorizations.authorization.name", "authorizationName"),
                        Select("roles.roleAuthorizations.authorization.menuIcon", "menuIcon"))
                .distinct(false)
                .where(Field("roles.roleAuthorizations.authorization.menuIcon").startWith("icon"), 
                       Parantesis(Field("id").eq(3), OR, Field("roles.id").eq(4), OR, Field("id").eq(5)), 
                       Parantesis(Field("id").eq(5), OR, Field("id").eq(4), OR, Field("roles.id").eq(3)))
                .orderBy(OrderBy("roles.id", Order.DESC))
                .page(1, 2)
                .getResultAsPage(AuthorizationSummary.class);

        System.out.println("Query Builder Result: " + result);

        // Expected: adminUser3 with role3 data (matching all complex conditions)
        assertEquals(List.of(new AuthorizationSummary(3L, "admin3", 3L, "role3", 3L, "auth3", "icon3")), result.getContent());
    }
}

