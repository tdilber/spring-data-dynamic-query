package com.beyt.jdq.presetation;

import com.beyt.jdq.BaseTestInstance;
import com.beyt.jdq.TestApplication;
import com.beyt.jdq.dto.enums.Order;
import com.beyt.jdq.testenv.repository.AdminUserRepository;
import com.beyt.jdq.util.PresentationUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Objects;

import static com.beyt.jdq.query.builder.QuerySimplifier.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = TestApplication.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class S9_Query_Builder extends BaseTestInstance {
    private @Autowired AdminUserRepository adminUserRepository;


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
    }
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
                .where(Field("roles.roleAuthorizations.authorization.menuIcon").startWith("icon"), Parantesis(Field("id").eq(3), OR, Field("roles.id").eq(4), OR, Field("id").eq(5)), Parantesis(Field("id").eq(5), OR, Field("id").eq(4), OR, Field("roles.id").eq(3)))
                .orderBy(OrderBy("roles.id", Order.DESC))
                .page(1, 2)
                .getResultAsPage(AuthorizationSummary.class);

        PresentationUtil.prettyPrint(result);


        assertEquals(List.of(new AuthorizationSummary(3L, "admin3", 3L, "role3", 3L, "auth3", "icon3")), result.getContent());
    }
}
