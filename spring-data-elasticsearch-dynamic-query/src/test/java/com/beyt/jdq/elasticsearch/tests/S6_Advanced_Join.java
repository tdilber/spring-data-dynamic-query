package com.beyt.jdq.elasticsearch.tests;

import com.beyt.jdq.core.model.Criteria;
import com.beyt.jdq.core.model.CriteriaList;
import com.beyt.jdq.core.model.enums.CriteriaOperator;
import com.beyt.jdq.elasticsearch.ElasticsearchTestApplication;
import com.beyt.jdq.elasticsearch.entity.AdminUser;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Elasticsearch tests for ADVANCED JOIN operations.
 * 
 * NOTE: These tests are currently disabled because MongoSearchQueryTemplate does not yet support
 * deep nested joins through multiple $lookup stages in aggregation pipeline.
 * 
 * Current implementation status:
 * ✓ Entity structure created with proper relationships (AdminUser -> Role -> RoleAuthorization -> Authorization)
 * ✓ Test data setup complete
 * ✗ MongoSearchQueryTemplate needs multiple $lookup stages for deep nesting
 * ✗ Need to handle multiple $unwind operations for array unwinding
 * ✗ Need to build complex aggregation pipelines for nested paths
 * 
 * Elasticsearch challenges for deep joins:
 * - Multiple levels of nesting require multiple $lookup stages in aggregation pipeline
 * - Performance considerations: Deep $lookup operations can be slower than SQL joins
 * - Alternative approaches: Denormalization or embedding commonly accessed data
 * 
 * Elasticsearch aggregation pipeline example for roleJoin test:
 * [
 *   { $lookup: { from: "roles", localField: "roles", foreignField: "_id", as: "roles" } },
 *   { $unwind: "$roles" },
 *   { $lookup: { from: "role_authorizations", localField: "roles.roleAuthorizations", foreignField: "_id", as: "roles.roleAuthorizations" } },
 *   { $unwind: "$roles.roleAuthorizations" },
 *   { $lookup: { from: "authorizations", localField: "roles.roleAuthorizations.authorization", foreignField: "_id", as: "roles.roleAuthorizations.authorization" } },
 *   { $unwind: "$roles.roleAuthorizations.authorization" },
 *   { $match: { "roles.roleAuthorizations.authorization.menuIcon": { $regex: "^icon" } } }
 * ]
 * 
 * To enable these tests:
 * 1. Implement deep path analysis in MongoSearchQueryTemplate
 * 2. Build aggregation pipeline with multiple $lookup stages
 * 3. Handle array unwinding with $unwind at each level
 * 4. Support regex and other operators on deeply nested fields
 * 5. Handle left join syntax ('<') for outer joins at each level
 */
@SpringBootTest(classes = ElasticsearchTestApplication.class)
@ActiveProfiles("estest")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Disabled("TODO: Enable after previous tests pass")
public class S6_Advanced_Join extends BaseElasticsearchJoinTestInstance {

    /**
     * Multi-level inner join
     * Elasticsearch equivalent: Multiple $lookup with $unwind for nested array traversal
     * Example: Find admin users where roles.roleAuthorizations.authorization.menuIcon starts with "icon"
     * 
     * Path: AdminUser -> roles[] -> roleAuthorizations[] -> authorization -> menuIcon
     * 
     * This requires:
     * - 3 levels of $lookup operations (roles, roleAuthorizations, authorization)
     * - $unwind for each array field (roles, roleAuthorizations)
     * - Final $match with nested field criteria
     */
    @Test
    public void roleJoin() {
        var criteriaList = CriteriaList.of(
                Criteria.of("roles.roleAuthorizations.authorization.menuIcon", CriteriaOperator.START_WITH, "icon")
        );
        System.out.println("ADVANCED JOIN: roles.roleAuthorizations.authorization.menuIcon starts with 'icon'");
        List<AdminUser> adminUserList = adminUserRepository.findAll(criteriaList);
        System.out.println("Result: " + adminUserList);
        
        // All admin users (1-5) have roles with authorizations that have menuIcon starting with "icon"
        assertEquals(List.of(adminUser1, adminUser2, adminUser3, adminUser4, adminUser5), adminUserList);
    }

    /**
     * Multi-level left join
     * Elasticsearch equivalent: Multiple $lookup with preserveNullAndEmptyArrays option
     * Example: Find admin users where roles<roleAuthorizations<authorization<menuIcon starts with "icon"
     * 
     * Path: AdminUser <-left- roles[] <-left- roleAuthorizations[] <-left- authorization <-left- menuIcon
     * 
     * This requires:
     * - $lookup with preserveNullAndEmptyArrays: true at each level
     * - Handle null values gracefully
     * - Keep documents even if nested relationships don't exist
     * - Elasticsearch pipeline: $lookup -> $unwind(preserveNullAndEmptyArrays) -> repeat for each level
     */
    @Test
    public void roleLeftJoin() {
        var criteriaList = CriteriaList.of(
                Criteria.of("roles<roleAuthorizations<authorization<menuIcon", CriteriaOperator.START_WITH, "icon")
        );
        System.out.println("ADVANCED LEFT JOIN: roles<roleAuthorizations<authorization<menuIcon starts with 'icon'");
        List<AdminUser> adminUserList = adminUserRepository.findAll(criteriaList);
        System.out.println("Result: " + adminUserList);
        
        // All admin users should still be returned with left joins
        assertEquals(List.of(adminUser1, adminUser2, adminUser3, adminUser4, adminUser5), adminUserList);
    }

    /**
     * Mixed join types in single query
     * Elasticsearch equivalent: Combination of inner and left $lookup operations
     * Example: Complex query with both inner and outer joins at different levels
     */
    @Test
    public void mixedJoinTypes() {
        var criteriaList = CriteriaList.of(
                Criteria.of("roles.roleAuthorizations.authorization.name", CriteriaOperator.EQUAL, "auth1"),
                Criteria.of("username", CriteriaOperator.START_WITH, "admin")
        );
        System.out.println("MIXED JOINS: roles.roleAuthorizations.authorization.name = 'auth1' AND username starts with 'admin'");
        List<AdminUser> adminUserList = adminUserRepository.findAll(criteriaList);
        System.out.println("Result: " + adminUserList);
        
        // Only adminUser1 has role1 with roleAuthorization1 with authorization1
        assertEquals(List.of(adminUser1), adminUserList);
    }
    
    /**
     * Complex nested path with multiple conditions
     * Tests querying at different levels of the nested structure
     */
    @Test
    public void complexNestedQuery() {
        var criteriaList = CriteriaList.of(
                Criteria.of("roles.roleAuthorizations.authorization.menuIcon", CriteriaOperator.START_WITH, "icon"),
                Criteria.of("roles.name", CriteriaOperator.CONTAIN, "role"),
                Criteria.of("id", CriteriaOperator.LESS_THAN, 4)
        );
        System.out.println("COMPLEX NESTED: menuIcon starts with 'icon' AND role name contains 'role' AND id < 4");
        List<AdminUser> adminUserList = adminUserRepository.findAll(criteriaList);
        System.out.println("Result: " + adminUserList);
        
        // adminUser1, adminUser2, adminUser3 match all criteria
        assertEquals(List.of(adminUser1, adminUser2, adminUser3), adminUserList);
    }
}

