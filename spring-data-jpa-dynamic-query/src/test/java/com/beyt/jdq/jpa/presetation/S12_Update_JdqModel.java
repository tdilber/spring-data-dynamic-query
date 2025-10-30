package com.beyt.jdq.jpa.presetation;

import com.beyt.jdq.jpa.BaseTestInstance;
import com.beyt.jdq.jpa.TestApplication;
import com.beyt.jdq.core.model.annotation.JdqField;
import com.beyt.jdq.core.model.annotation.JdqModel;
import com.beyt.jdq.core.model.annotation.JdqSubModel;
import com.beyt.jdq.jpa.testenv.entity.authorization.AdminUser;
import com.beyt.jdq.jpa.testenv.entity.authorization.Authorization;
import com.beyt.jdq.jpa.testenv.entity.authorization.Role;
import com.beyt.jdq.jpa.testenv.repository.AdminUserRepository;
import com.beyt.jdq.jpa.testenv.repository.AuthorizationRepository;
import com.beyt.jdq.jpa.testenv.repository.RoleRepository;
import com.beyt.jdq.jpa.util.PresentationUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestApplication.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class S12_Update_JdqModel extends BaseTestInstance {
    
    @Autowired
    private AdminUserRepository adminUserRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private AuthorizationRepository authorizationRepository;
    
    
    /**
     * Simple update model - updates only main entity fields
     */
    @JdqModel
    public static class AdminUserUpdateModel {
        @JdqField("id")
        private Long id;
        
        @JdqField("username")
        private String username;
        
        @JdqField("password")
        private String password;
        
        public AdminUserUpdateModel() {
        }
        
        public AdminUserUpdateModel(Long id, String username, String password) {
            this.id = id;
            this.username = username;
            this.password = password;
        }
        
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
    }
    
    @Test
    @Transactional
    public void testSimpleUpdate() {
        // Get original data
        AdminUser originalAdmin = adminUserRepository.findById(1L).orElseThrow();
        assertEquals("admin1", originalAdmin.getUsername());
        assertEquals("password1", originalAdmin.getPassword());
        
        // Update using JdqModel
        AdminUserUpdateModel updateModel = new AdminUserUpdateModel(1L, "updatedAdmin1", "newPassword1");
        adminUserRepository.update(updateModel);
        
        // Flush and clear to ensure we're reading fresh data from DB
        entityManager.flush();
        entityManager.clear();
        
        // Verify update
        AdminUser updatedAdmin = adminUserRepository.findById(1L).orElseThrow();
        assertEquals("updatedAdmin1", updatedAdmin.getUsername());
        assertEquals("newPassword1", updatedAdmin.getPassword());
        
        PresentationUtil.prettyPrint(updatedAdmin);
        
        // Restore original data
        AdminUserUpdateModel restoreModel = new AdminUserUpdateModel(1L, "admin1", "password1");
        adminUserRepository.update(restoreModel);
        entityManager.flush();
        entityManager.clear();
    }
    
    /**
     * Partial update model - updates only username field
     */
    @JdqModel
    public static class AdminUserPartialUpdateModel {
        @JdqField("id")
        private Long id;
        
        @JdqField("username")
        private String username;
        
        public AdminUserPartialUpdateModel() {
        }
        
        public AdminUserPartialUpdateModel(Long id, String username) {
            this.id = id;
            this.username = username;
        }
        
        public Long getId() {
            return id;
        }
        
        public void setId(Long id) {
            this.id = id;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
    }
    
    @Test
    @Transactional
    public void testPartialUpdate() {
        // Get original data
        AdminUser originalAdmin = adminUserRepository.findById(2L).orElseThrow();
        String originalPassword = originalAdmin.getPassword();
        assertEquals("admin2", originalAdmin.getUsername());
        
        // Update only username
        AdminUserPartialUpdateModel updateModel = new AdminUserPartialUpdateModel(2L, "partiallyUpdatedAdmin2");
        adminUserRepository.update(updateModel);
        
        entityManager.flush();
        entityManager.clear();
        
        // Verify only username changed, password should remain the same
        AdminUser updatedAdmin = adminUserRepository.findById(2L).orElseThrow();
        assertEquals("partiallyUpdatedAdmin2", updatedAdmin.getUsername());
        assertEquals(originalPassword, updatedAdmin.getPassword());
        
        PresentationUtil.prettyPrint(updatedAdmin);
        
        // Restore original data
        AdminUserPartialUpdateModel restoreModel = new AdminUserPartialUpdateModel(2L, "admin2");
        adminUserRepository.update(restoreModel);
        entityManager.flush();
        entityManager.clear();
    }
    
    /**
     * Record-based update model
     */
    @JdqModel
    public record RoleUpdateModel(
            @JdqField("id") Long id,
            @JdqField("name") String name,
            @JdqField("description") String description
    ) {
    }
    
    @Test
    @Transactional
    public void testRecordBasedUpdate() {
        // Get original data
        Role originalRole = roleRepository.findById(1L).orElseThrow();
        assertEquals("role1", originalRole.getName());
        assertEquals("description1", originalRole.getDescription());
        
        // Update using record
        RoleUpdateModel updateModel = new RoleUpdateModel(1L, "updatedRole1", "updatedDescription1");
        roleRepository.update(updateModel);
        
        entityManager.flush();
        entityManager.clear();
        
        // Verify update
        Role updatedRole = roleRepository.findById(1L).orElseThrow();
        assertEquals("updatedRole1", updatedRole.getName());
        assertEquals("updatedDescription1", updatedRole.getDescription());
        
        PresentationUtil.prettyPrint(updatedRole);
        
        // Restore original data
        RoleUpdateModel restoreModel = new RoleUpdateModel(1L, "role1", "description1");
        roleRepository.update(restoreModel);
        entityManager.flush();
        entityManager.clear();
    }
    
    /**
     * Nested update with joined entity
     */
    @JdqModel
    public static class AuthorizationNestedUpdateModel {
        @JdqField("id")
        private Long adminId;
        
        @JdqField("username")
        private String username;
        
        @JdqField("roles.id")
        private Long roleId;
        
        @JdqField("roles.name")
        private String roleName;
        
        public AuthorizationNestedUpdateModel() {
        }
        
        public AuthorizationNestedUpdateModel(Long adminId, String username, Long roleId, String roleName) {
            this.adminId = adminId;
            this.username = username;
            this.roleId = roleId;
            this.roleName = roleName;
        }
        
        public Long getAdminId() {
            return adminId;
        }
        
        public void setAdminId(Long adminId) {
            this.adminId = adminId;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
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
    }
    
    @Test
    @Transactional
    public void testNestedEntityUpdate() {
        // Get original data
        AdminUser originalAdmin = adminUserRepository.findById(3L).orElseThrow();
        assertEquals("admin3", originalAdmin.getUsername());
        
        Role originalRole = roleRepository.findById(3L).orElseThrow();
        assertEquals("role3", originalRole.getName());
        
        // Update both admin and role
        AuthorizationNestedUpdateModel updateModel = new AuthorizationNestedUpdateModel(
                3L, "nestedUpdatedAdmin3", 3L, "nestedUpdatedRole3"
        );
        adminUserRepository.update(updateModel);
        
        entityManager.flush();
        entityManager.clear();
        
        // Verify both updates
        AdminUser updatedAdmin = adminUserRepository.findById(3L).orElseThrow();
        assertEquals("nestedUpdatedAdmin3", updatedAdmin.getUsername());
        
        Role updatedRole = roleRepository.findById(3L).orElseThrow();
        assertEquals("nestedUpdatedRole3", updatedRole.getName());
        
        PresentationUtil.prettyPrint(updatedAdmin);
        PresentationUtil.prettyPrint(updatedRole);
        
        // Restore original data
        AuthorizationNestedUpdateModel restoreModel = new AuthorizationNestedUpdateModel(
                3L, "admin3", 3L, "role3"
        );
        adminUserRepository.update(restoreModel);
        entityManager.flush();
        entityManager.clear();
    }
    
    /**
     * SubModel-based update
     */
    @JdqModel
    public static class AdminWithRoleSubModel {
        @JdqField("id")
        private Long adminId;
        
        @JdqField("username")
        private String username;
        
        @JdqSubModel("roles")
        private RoleSubModel role;
        
        public AdminWithRoleSubModel() {
        }
        
        public AdminWithRoleSubModel(Long adminId, String username, RoleSubModel role) {
            this.adminId = adminId;
            this.username = username;
            this.role = role;
        }
        
        public Long getAdminId() {
            return adminId;
        }
        
        public void setAdminId(Long adminId) {
            this.adminId = adminId;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public RoleSubModel getRole() {
            return role;
        }
        
        public void setRole(RoleSubModel role) {
            this.role = role;
        }
        
        @JdqModel
        public static class RoleSubModel {
            @JdqField("id")
            private Long roleId;
            
            @JdqField("name")
            private String roleName;
            
            @JdqField("description")
            private String roleDescription;
            
            public RoleSubModel() {
            }
            
            public RoleSubModel(Long roleId, String roleName, String roleDescription) {
                this.roleId = roleId;
                this.roleName = roleName;
                this.roleDescription = roleDescription;
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
            
            public String getRoleDescription() {
                return roleDescription;
            }
            
            public void setRoleDescription(String roleDescription) {
                this.roleDescription = roleDescription;
            }
        }
    }
    
    @Test
    @Transactional
    public void testSubModelUpdate() {
        // Get original data
        AdminUser originalAdmin = adminUserRepository.findById(4L).orElseThrow();
        assertEquals("admin4", originalAdmin.getUsername());
        
        Role originalRole = roleRepository.findById(4L).orElseThrow();
        assertEquals("role4", originalRole.getName());
        assertEquals("description4", originalRole.getDescription());
        
        // Update using SubModel
        AdminWithRoleSubModel.RoleSubModel roleUpdate = new AdminWithRoleSubModel.RoleSubModel(
                4L, "subModelUpdatedRole4", "subModelUpdatedDescription4"
        );
        AdminWithRoleSubModel updateModel = new AdminWithRoleSubModel(
                4L, "subModelUpdatedAdmin4", roleUpdate
        );
        adminUserRepository.update(updateModel);
        
        entityManager.flush();
        entityManager.clear();
        
        // Verify both updates
        AdminUser updatedAdmin = adminUserRepository.findById(4L).orElseThrow();
        assertEquals("subModelUpdatedAdmin4", updatedAdmin.getUsername());
        
        Role updatedRole = roleRepository.findById(4L).orElseThrow();
        assertEquals("subModelUpdatedRole4", updatedRole.getName());
        assertEquals("subModelUpdatedDescription4", updatedRole.getDescription());
        
        PresentationUtil.prettyPrint(updatedAdmin);
        PresentationUtil.prettyPrint(updatedRole);
        
        // Restore original data
        AdminWithRoleSubModel.RoleSubModel roleRestore = new AdminWithRoleSubModel.RoleSubModel(
                4L, "role4", "description4"
        );
        AdminWithRoleSubModel restoreModel = new AdminWithRoleSubModel(
                4L, "admin4", roleRestore
        );
        adminUserRepository.update(restoreModel);
        entityManager.flush();
        entityManager.clear();
    }
    
    /**
     * Record-based SubModel update
     */
    @JdqModel
    public record AdminWithRoleSubModelRecord(
            @JdqField("id") Long adminId,
            @JdqField("username") String username,
            @JdqSubModel("roles") RoleSubModelRecord role
    ) {
        @JdqModel
        public record RoleSubModelRecord(
                @JdqField("id") Long roleId,
                @JdqField("name") String roleName
        ) {
        }
    }
    
    @Test
    @Transactional
    public void testRecordSubModelUpdate() {
        // Get original data
        AdminUser originalAdmin = adminUserRepository.findById(5L).orElseThrow();
        assertEquals("admin5", originalAdmin.getUsername());
        
        Role originalRole = roleRepository.findById(5L).orElseThrow();
        assertEquals("role5", originalRole.getName());
        
        // Update using record SubModel
        AdminWithRoleSubModelRecord.RoleSubModelRecord roleUpdate = 
                new AdminWithRoleSubModelRecord.RoleSubModelRecord(5L, "recordUpdatedRole5");
        AdminWithRoleSubModelRecord updateModel = 
                new AdminWithRoleSubModelRecord(5L, "recordUpdatedAdmin5", roleUpdate);
        adminUserRepository.update(updateModel);
        
        entityManager.flush();
        entityManager.clear();
        
        // Verify both updates
        AdminUser updatedAdmin = adminUserRepository.findById(5L).orElseThrow();
        assertEquals("recordUpdatedAdmin5", updatedAdmin.getUsername());
        
        Role updatedRole = roleRepository.findById(5L).orElseThrow();
        assertEquals("recordUpdatedRole5", updatedRole.getName());
        
        PresentationUtil.prettyPrint(updatedAdmin);
        PresentationUtil.prettyPrint(updatedRole);
        
        // Restore original data
        AdminWithRoleSubModelRecord.RoleSubModelRecord roleRestore = 
                new AdminWithRoleSubModelRecord.RoleSubModelRecord(5L, "role5");
        AdminWithRoleSubModelRecord restoreModel = 
                new AdminWithRoleSubModelRecord(5L, "admin5", roleRestore);
        adminUserRepository.update(restoreModel);
        entityManager.flush();
        entityManager.clear();
    }
    
    /**
     * Update without main ID - should throw exception
     */
    @JdqModel
    public static class AdminUserWithoutIdModel {
        @JdqField("username")
        private String username;
        
        public AdminUserWithoutIdModel() {
        }
        
        public AdminUserWithoutIdModel(String username) {
            this.username = username;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
    }
    
    @Test
    public void testUpdateWithoutMainIdThrowsException() {
        AdminUserWithoutIdModel updateModel = new AdminUserWithoutIdModel("shouldFail");
        
        Exception exception = assertThrows(InvalidDataAccessApiUsageException.class, () -> {
            adminUserRepository.update(updateModel);
        });
        
        assertTrue(exception.getCause().getMessage().contains("Main entity ID is required"));
        PresentationUtil.prettyPrint("Expected exception: " + exception.getCause().getMessage());
    }
    
    /**
     * Update with non-existent ID - should throw exception
     */
    @Test
    public void testUpdateWithNonExistentIdThrowsException() {
        AdminUserUpdateModel updateModel = new AdminUserUpdateModel(999L, "nonExistent", "password");
        
        Exception exception = assertThrows(InvalidDataAccessApiUsageException.class, () -> {
            adminUserRepository.update(updateModel);
        });
        
        assertTrue(exception.getCause().getMessage().contains("No entity found with ID"));
        PresentationUtil.prettyPrint("Expected exception: " + exception.getCause().getMessage());
    }
    
    /**
     * Update nested entity without joined entity ID - should be ignored
     */
    @JdqModel
    public static class AdminWithRoleNoIdModel {
        @JdqField("id")
        private Long adminId;
        
        @JdqField("username")
        private String username;
        
        @JdqField("roles.name")
        private String roleName;
        
        public AdminWithRoleNoIdModel() {
        }
        
        public AdminWithRoleNoIdModel(Long adminId, String username, String roleName) {
            this.adminId = adminId;
            this.username = username;
            this.roleName = roleName;
        }
        
        public Long getAdminId() {
            return adminId;
        }
        
        public void setAdminId(Long adminId) {
            this.adminId = adminId;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getRoleName() {
            return roleName;
        }
        
        public void setRoleName(String roleName) {
            this.roleName = roleName;
        }
    }
    
    @Test
    @Transactional
    public void testUpdateNestedEntityWithoutIdIsIgnored() {
        // Get original data
        AdminUser originalAdmin = adminUserRepository.findById(1L).orElseThrow();
        String originalUsername = originalAdmin.getUsername();
        
        Role originalRole = roleRepository.findById(1L).orElseThrow();
        String originalRoleName = originalRole.getName();
        
        // Try to update without role ID - role update should be ignored
        AdminWithRoleNoIdModel updateModel = new AdminWithRoleNoIdModel(
                1L, originalUsername, "shouldBeIgnored"
        );
        adminUserRepository.update(updateModel);
        
        entityManager.flush();
        entityManager.clear();
        
        // Verify admin username didn't change and role name didn't change
        Role roleAfterUpdate = roleRepository.findById(1L).orElseThrow();
        assertEquals(originalRoleName, roleAfterUpdate.getName());
        
        PresentationUtil.prettyPrint("Role update ignored as expected (no role ID provided)");
    }
    
    /**
     * Test with null model - should throw exception
     */
    @Test
    public void testUpdateWithNullModelThrowsException() {
        Exception exception = assertThrows(InvalidDataAccessApiUsageException.class, () -> {
            adminUserRepository.update(null);
        });
        
        assertTrue(exception.getCause().getMessage().contains("cannot be null"));
        PresentationUtil.prettyPrint("Expected exception: " + exception.getCause().getMessage());
    }
    
    /**
     * Test with non-JdqModel annotated class - should throw exception
     */
    public static class NonAnnotatedModel {
        private Long id;
        private String username;
        
        public NonAnnotatedModel(Long id, String username) {
            this.id = id;
            this.username = username;
        }
    }
    
    @Test
    public void testUpdateWithNonAnnotatedModelThrowsException() {
        NonAnnotatedModel updateModel = new NonAnnotatedModel(1L, "test");
        
        Exception exception = assertThrows(InvalidDataAccessApiUsageException.class, () -> {
            adminUserRepository.update(updateModel);
        });
        
        assertTrue(exception.getCause().getMessage().contains("must be annotated with @JdqModel"));
        PresentationUtil.prettyPrint("Expected exception: " + exception.getCause().getMessage());
    }
    
    /**
     * Update with empty SubModel path
     */
    @JdqModel
    public static class AdminWithEmptySubModelPath {
        @JdqField("id")
        private Long adminId;
        
        @JdqField("username")
        private String username;
        
        @JdqSubModel() // Empty path
        private RoleWithFullPath role;
        
        public AdminWithEmptySubModelPath() {
        }
        
        public AdminWithEmptySubModelPath(Long adminId, String username, RoleWithFullPath role) {
            this.adminId = adminId;
            this.username = username;
            this.role = role;
        }
        
        public Long getAdminId() {
            return adminId;
        }
        
        public void setAdminId(Long adminId) {
            this.adminId = adminId;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public RoleWithFullPath getRole() {
            return role;
        }
        
        public void setRole(RoleWithFullPath role) {
            this.role = role;
        }
        
        @JdqModel
        public static class RoleWithFullPath {
            @JdqField("roles.id")
            private Long roleId;
            
            @JdqField("roles.name")
            private String roleName;
            
            public RoleWithFullPath() {
            }
            
            public RoleWithFullPath(Long roleId, String roleName) {
                this.roleId = roleId;
                this.roleName = roleName;
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
        }
    }
    
    @Test
    @Transactional
    public void testEmptySubModelPathUpdate() {
        // Get original data
        Role originalRole = roleRepository.findById(2L).orElseThrow();
        assertEquals("role2", originalRole.getName());
        
        // Update using empty SubModel path
        AdminWithEmptySubModelPath.RoleWithFullPath roleUpdate = 
                new AdminWithEmptySubModelPath.RoleWithFullPath(2L, "emptyPathUpdatedRole2");
        AdminWithEmptySubModelPath updateModel = 
                new AdminWithEmptySubModelPath(2L, "admin2", roleUpdate);
        adminUserRepository.update(updateModel);
        
        entityManager.flush();
        entityManager.clear();
        
        // Verify role update
        Role updatedRole = roleRepository.findById(2L).orElseThrow();
        assertEquals("emptyPathUpdatedRole2", updatedRole.getName());
        
        PresentationUtil.prettyPrint(updatedRole);
        
        // Restore original data
        AdminWithEmptySubModelPath.RoleWithFullPath roleRestore = 
                new AdminWithEmptySubModelPath.RoleWithFullPath(2L, "role2");
        AdminWithEmptySubModelPath restoreModel = 
                new AdminWithEmptySubModelPath(2L, "admin2", roleRestore);
        adminUserRepository.update(restoreModel);
        entityManager.flush();
        entityManager.clear();
    }
    
    /**
     * 4-Level Deep Join Update - Flat Model (using dot notation)
     * Level 1: AdminUser
     * Level 2: Role (via roles)
     * Level 3: RoleAuthorization (via roles.roleAuthorizations)
     * Level 4: Authorization (via roles.roleAuthorizations.authorization)
     */
    @JdqModel
    public static class FourLevelDeepUpdateModel {
        // Level 1: AdminUser
        @JdqField("id")
        private Long adminId;
        
        @JdqField("username")
        private String username;
        
        // Level 2: Role
        @JdqField("roles.id")
        private Long roleId;
        
        @JdqField("roles.name")
        private String roleName;
        
        @JdqField("roles.description")
        private String roleDescription;
        
        // Level 3: RoleAuthorization (Note: RoleAuthorization doesn't have many updatable fields besides ID)
        @JdqField("roles.roleAuthorizations.id")
        private Long roleAuthorizationId;
        
        // Level 4: Authorization
        @JdqField("roles.roleAuthorizations.authorization.id")
        private Long authorizationId;
        
        @JdqField("roles.roleAuthorizations.authorization.name")
        private String authorizationName;
        
        @JdqField("roles.roleAuthorizations.authorization.menuUrl")
        private String authorizationMenuUrl;
        
        @JdqField("roles.roleAuthorizations.authorization.menuIcon")
        private String authorizationMenuIcon;
        
        public FourLevelDeepUpdateModel() {
        }
        
        public FourLevelDeepUpdateModel(Long adminId, String username, Long roleId, String roleName, 
                                       String roleDescription, Long roleAuthorizationId, Long authorizationId, 
                                       String authorizationName, String authorizationMenuUrl, String authorizationMenuIcon) {
            this.adminId = adminId;
            this.username = username;
            this.roleId = roleId;
            this.roleName = roleName;
            this.roleDescription = roleDescription;
            this.roleAuthorizationId = roleAuthorizationId;
            this.authorizationId = authorizationId;
            this.authorizationName = authorizationName;
            this.authorizationMenuUrl = authorizationMenuUrl;
            this.authorizationMenuIcon = authorizationMenuIcon;
        }
        
        // Getters and Setters
        public Long getAdminId() {
            return adminId;
        }
        
        public void setAdminId(Long adminId) {
            this.adminId = adminId;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
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
        
        public String getRoleDescription() {
            return roleDescription;
        }
        
        public void setRoleDescription(String roleDescription) {
            this.roleDescription = roleDescription;
        }
        
        public Long getRoleAuthorizationId() {
            return roleAuthorizationId;
        }
        
        public void setRoleAuthorizationId(Long roleAuthorizationId) {
            this.roleAuthorizationId = roleAuthorizationId;
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
        
        public String getAuthorizationMenuUrl() {
            return authorizationMenuUrl;
        }
        
        public void setAuthorizationMenuUrl(String authorizationMenuUrl) {
            this.authorizationMenuUrl = authorizationMenuUrl;
        }
        
        public String getAuthorizationMenuIcon() {
            return authorizationMenuIcon;
        }
        
        public void setAuthorizationMenuIcon(String authorizationMenuIcon) {
            this.authorizationMenuIcon = authorizationMenuIcon;
        }
    }
    
    @Test
    @Transactional
    public void testFourLevelDeepUpdateFlat() {
        // Get original data from all 4 levels
        AdminUser originalAdmin = adminUserRepository.findById(1L).orElseThrow();
        assertEquals("admin1", originalAdmin.getUsername());
        
        Role originalRole = roleRepository.findById(1L).orElseThrow();
        assertEquals("role1", originalRole.getName());
        assertEquals("description1", originalRole.getDescription());
        
        Authorization originalAuth = authorizationRepository.findById(1L).orElseThrow();
        assertEquals("auth1", originalAuth.getName());
        assertEquals("/url1", originalAuth.getMenuUrl());
        assertEquals("icon1", originalAuth.getMenuIcon());
        
        // Update all 4 levels using flat model
        FourLevelDeepUpdateModel updateModel = new FourLevelDeepUpdateModel(
                1L, "4LevelUpdatedAdmin1",              // Level 1: AdminUser
                1L, "4LevelUpdatedRole1", "4LevelUpdatedDesc1",  // Level 2: Role
                1L,                                      // Level 3: RoleAuthorization
                1L, "4LevelUpdatedAuth1", "/4level-url1", "4level-icon1"  // Level 4: Authorization
        );
        adminUserRepository.update(updateModel);
        
        entityManager.flush();
        entityManager.clear();
        
        // Verify all 4 levels were updated
        AdminUser updatedAdmin = adminUserRepository.findById(1L).orElseThrow();
        assertEquals("4LevelUpdatedAdmin1", updatedAdmin.getUsername());
        PresentationUtil.prettyPrint("Level 1 - AdminUser: " + updatedAdmin.getUsername());
        
        Role updatedRole = roleRepository.findById(1L).orElseThrow();
        assertEquals("4LevelUpdatedRole1", updatedRole.getName());
        assertEquals("4LevelUpdatedDesc1", updatedRole.getDescription());
        PresentationUtil.prettyPrint("Level 2 - Role: " + updatedRole.getName() + ", " + updatedRole.getDescription());
        
        Authorization updatedAuth = authorizationRepository.findById(1L).orElseThrow();
        assertEquals("4LevelUpdatedAuth1", updatedAuth.getName());
        assertEquals("/4level-url1", updatedAuth.getMenuUrl());
        assertEquals("4level-icon1", updatedAuth.getMenuIcon());
        PresentationUtil.prettyPrint("Level 4 - Authorization: " + updatedAuth.getName() + ", " + updatedAuth.getMenuUrl() + ", " + updatedAuth.getMenuIcon());
        
        // Restore original data
        FourLevelDeepUpdateModel restoreModel = new FourLevelDeepUpdateModel(
                1L, "admin1",
                1L, "role1", "description1",
                1L,
                1L, "auth1", "/url1", "icon1"
        );
        adminUserRepository.update(restoreModel);
        entityManager.flush();
        entityManager.clear();
    }
    
    /**
     * 4-Level Deep Join Update - Nested SubModel (structured approach)
     */
    @JdqModel
    public static class FourLevelDeepNestedUpdateModel {
        @JdqField("id")
        private Long adminId;
        
        @JdqField("username")
        private String username;
        
        @JdqSubModel("roles")
        private RoleLevel2 role;
        
        public FourLevelDeepNestedUpdateModel() {
        }
        
        public FourLevelDeepNestedUpdateModel(Long adminId, String username, RoleLevel2 role) {
            this.adminId = adminId;
            this.username = username;
            this.role = role;
        }
        
        public Long getAdminId() {
            return adminId;
        }
        
        public void setAdminId(Long adminId) {
            this.adminId = adminId;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public RoleLevel2 getRole() {
            return role;
        }
        
        public void setRole(RoleLevel2 role) {
            this.role = role;
        }
        
        @JdqModel
        public static class RoleLevel2 {
            @JdqField("id")
            private Long roleId;
            
            @JdqField("name")
            private String roleName;
            
            @JdqField("description")
            private String roleDescription;
            
            @JdqSubModel("roleAuthorizations")
            private RoleAuthorizationLevel3 roleAuthorization;
            
            public RoleLevel2() {
            }
            
            public RoleLevel2(Long roleId, String roleName, String roleDescription, RoleAuthorizationLevel3 roleAuthorization) {
                this.roleId = roleId;
                this.roleName = roleName;
                this.roleDescription = roleDescription;
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
            
            public String getRoleDescription() {
                return roleDescription;
            }
            
            public void setRoleDescription(String roleDescription) {
                this.roleDescription = roleDescription;
            }
            
            public RoleAuthorizationLevel3 getRoleAuthorization() {
                return roleAuthorization;
            }
            
            public void setRoleAuthorization(RoleAuthorizationLevel3 roleAuthorization) {
                this.roleAuthorization = roleAuthorization;
            }
            
            @JdqModel
            public static class RoleAuthorizationLevel3 {
                @JdqField("id")
                private Long roleAuthorizationId;
                
                @JdqSubModel("authorization")
                private AuthorizationLevel4 authorization;
                
                public RoleAuthorizationLevel3() {
                }
                
                public RoleAuthorizationLevel3(Long roleAuthorizationId, AuthorizationLevel4 authorization) {
                    this.roleAuthorizationId = roleAuthorizationId;
                    this.authorization = authorization;
                }
                
                public Long getRoleAuthorizationId() {
                    return roleAuthorizationId;
                }
                
                public void setRoleAuthorizationId(Long roleAuthorizationId) {
                    this.roleAuthorizationId = roleAuthorizationId;
                }
                
                public AuthorizationLevel4 getAuthorization() {
                    return authorization;
                }
                
                public void setAuthorization(AuthorizationLevel4 authorization) {
                    this.authorization = authorization;
                }
                
                @JdqModel
                public static class AuthorizationLevel4 {
                    @JdqField("id")
                    private Long authorizationId;
                    
                    @JdqField("name")
                    private String authorizationName;
                    
                    @JdqField("menuUrl")
                    private String menuUrl;
                    
                    @JdqField("menuIcon")
                    private String menuIcon;
                    
                    public AuthorizationLevel4() {
                    }
                    
                    public AuthorizationLevel4(Long authorizationId, String authorizationName, String menuUrl, String menuIcon) {
                        this.authorizationId = authorizationId;
                        this.authorizationName = authorizationName;
                        this.menuUrl = menuUrl;
                        this.menuIcon = menuIcon;
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
                }
            }
        }
    }
    
    @Test
    @Transactional
    public void testFourLevelDeepUpdateNested() {
        // Get original data from all 4 levels
        AdminUser originalAdmin = adminUserRepository.findById(2L).orElseThrow();
        assertEquals("admin2", originalAdmin.getUsername());
        
        Role originalRole = roleRepository.findById(2L).orElseThrow();
        assertEquals("role2", originalRole.getName());
        
        Authorization originalAuth = authorizationRepository.findById(2L).orElseThrow();
        assertEquals("auth2", originalAuth.getName());
        
        // Build nested update model
        FourLevelDeepNestedUpdateModel.RoleLevel2.RoleAuthorizationLevel3.AuthorizationLevel4 authLevel4 = 
                new FourLevelDeepNestedUpdateModel.RoleLevel2.RoleAuthorizationLevel3.AuthorizationLevel4(
                        2L, "NestedAuth2", "/nested-url2", "nested-icon2"
                );
        
        FourLevelDeepNestedUpdateModel.RoleLevel2.RoleAuthorizationLevel3 roleAuthLevel3 = 
                new FourLevelDeepNestedUpdateModel.RoleLevel2.RoleAuthorizationLevel3(2L, authLevel4);
        
        FourLevelDeepNestedUpdateModel.RoleLevel2 roleLevel2 = 
                new FourLevelDeepNestedUpdateModel.RoleLevel2(
                        2L, "NestedRole2", "NestedDesc2", roleAuthLevel3
                );
        
        FourLevelDeepNestedUpdateModel updateModel = 
                new FourLevelDeepNestedUpdateModel(2L, "NestedAdmin2", roleLevel2);
        
        // Perform update
        adminUserRepository.update(updateModel);
        
        entityManager.flush();
        entityManager.clear();
        
        // Verify all 4 levels were updated
        AdminUser updatedAdmin = adminUserRepository.findById(2L).orElseThrow();
        assertEquals("NestedAdmin2", updatedAdmin.getUsername());
        PresentationUtil.prettyPrint("Level 1 (Nested) - AdminUser: " + updatedAdmin.getUsername());
        
        Role updatedRole = roleRepository.findById(2L).orElseThrow();
        assertEquals("NestedRole2", updatedRole.getName());
        assertEquals("NestedDesc2", updatedRole.getDescription());
        PresentationUtil.prettyPrint("Level 2 (Nested) - Role: " + updatedRole.getName() + ", " + updatedRole.getDescription());
        
        Authorization updatedAuth = authorizationRepository.findById(2L).orElseThrow();
        assertEquals("NestedAuth2", updatedAuth.getName());
        assertEquals("/nested-url2", updatedAuth.getMenuUrl());
        assertEquals("nested-icon2", updatedAuth.getMenuIcon());
        PresentationUtil.prettyPrint("Level 4 (Nested) - Authorization: " + updatedAuth.getName() + ", " + updatedAuth.getMenuUrl());
        
        // Restore original data
        FourLevelDeepNestedUpdateModel.RoleLevel2.RoleAuthorizationLevel3.AuthorizationLevel4 restoreAuth = 
                new FourLevelDeepNestedUpdateModel.RoleLevel2.RoleAuthorizationLevel3.AuthorizationLevel4(
                        2L, "auth2", "/url2", "icon2"
                );
        
        FourLevelDeepNestedUpdateModel.RoleLevel2.RoleAuthorizationLevel3 restoreRoleAuth = 
                new FourLevelDeepNestedUpdateModel.RoleLevel2.RoleAuthorizationLevel3(2L, restoreAuth);
        
        FourLevelDeepNestedUpdateModel.RoleLevel2 restoreRole = 
                new FourLevelDeepNestedUpdateModel.RoleLevel2(2L, "role2", "description2", restoreRoleAuth);
        
        FourLevelDeepNestedUpdateModel restoreModel = 
                new FourLevelDeepNestedUpdateModel(2L, "admin2", restoreRole);
        
        adminUserRepository.update(restoreModel);
        entityManager.flush();
        entityManager.clear();
    }
    
    /**
     * 4-Level Deep Join Update - Record-based (Java 14+)
     */
    @JdqModel
    public record FourLevelDeepRecordUpdateModel(
            @JdqField("id") Long adminId,
            @JdqField("username") String username,
            @JdqSubModel("roles") RoleLevel2Record role
    ) {
        @JdqModel
        public record RoleLevel2Record(
                @JdqField("id") Long roleId,
                @JdqField("name") String roleName,
                @JdqField("description") String roleDescription,
                @JdqSubModel("roleAuthorizations") RoleAuthorizationLevel3Record roleAuthorization
        ) {
            @JdqModel
            public record RoleAuthorizationLevel3Record(
                    @JdqField("id") Long roleAuthorizationId,
                    @JdqSubModel("authorization") AuthorizationLevel4Record authorization
            ) {
                @JdqModel
                public record AuthorizationLevel4Record(
                        @JdqField("id") Long authorizationId,
                        @JdqField("name") String authorizationName,
                        @JdqField("menuUrl") String menuUrl,
                        @JdqField("menuIcon") String menuIcon
                ) {
                }
            }
        }
    }
    
    @Test
    @Transactional
    public void testFourLevelDeepUpdateRecord() {
        // Get original data
        AdminUser originalAdmin = adminUserRepository.findById(3L).orElseThrow();
        assertEquals("admin3", originalAdmin.getUsername());
        
        Role originalRole = roleRepository.findById(3L).orElseThrow();
        assertEquals("role3", originalRole.getName());
        
        Authorization originalAuth = authorizationRepository.findById(3L).orElseThrow();
        assertEquals("auth3", originalAuth.getName());
        
        // Create record-based update model
        FourLevelDeepRecordUpdateModel updateModel = new FourLevelDeepRecordUpdateModel(
                3L, "RecordAdmin3",
                new FourLevelDeepRecordUpdateModel.RoleLevel2Record(
                        3L, "RecordRole3", "RecordDesc3",
                        new FourLevelDeepRecordUpdateModel.RoleLevel2Record.RoleAuthorizationLevel3Record(
                                3L,
                                new FourLevelDeepRecordUpdateModel.RoleLevel2Record.RoleAuthorizationLevel3Record.AuthorizationLevel4Record(
                                        3L, "RecordAuth3", "/record-url3", "record-icon3"
                                )
                        )
                )
        );
        
        // Perform update
        adminUserRepository.update(updateModel);
        
        entityManager.flush();
        entityManager.clear();
        
        // Verify all 4 levels
        AdminUser updatedAdmin = adminUserRepository.findById(3L).orElseThrow();
        assertEquals("RecordAdmin3", updatedAdmin.getUsername());
        
        Role updatedRole = roleRepository.findById(3L).orElseThrow();
        assertEquals("RecordRole3", updatedRole.getName());
        assertEquals("RecordDesc3", updatedRole.getDescription());
        
        Authorization updatedAuth = authorizationRepository.findById(3L).orElseThrow();
        assertEquals("RecordAuth3", updatedAuth.getName());
        assertEquals("/record-url3", updatedAuth.getMenuUrl());
        assertEquals("record-icon3", updatedAuth.getMenuIcon());
        
        PresentationUtil.prettyPrint("4-Level Record Update Success!");
        PresentationUtil.prettyPrint("  L1: " + updatedAdmin.getUsername());
        PresentationUtil.prettyPrint("  L2: " + updatedRole.getName());
        PresentationUtil.prettyPrint("  L4: " + updatedAuth.getName());
        
        // Restore
        FourLevelDeepRecordUpdateModel restoreModel = new FourLevelDeepRecordUpdateModel(
                3L, "admin3",
                new FourLevelDeepRecordUpdateModel.RoleLevel2Record(
                        3L, "role3", "description3",
                        new FourLevelDeepRecordUpdateModel.RoleLevel2Record.RoleAuthorizationLevel3Record(
                                3L,
                                new FourLevelDeepRecordUpdateModel.RoleLevel2Record.RoleAuthorizationLevel3Record.AuthorizationLevel4Record(
                                        3L, "auth3", "/url3", "icon3"
                                )
                        )
                )
        );
        adminUserRepository.update(restoreModel);
        entityManager.flush();
        entityManager.clear();
    }
}

