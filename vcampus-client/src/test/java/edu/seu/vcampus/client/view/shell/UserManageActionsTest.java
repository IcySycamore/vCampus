package edu.seu.vcampus.client.view.shell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 新建用户表单校验测试（纯函数）。
 */
class UserManageActionsTest {

    @Test
    void rejectsBlankUserName() {
        assertEquals("请填写登录名", UserManageActions.validateNewUser("", "张三", "pw", "pw"));
    }

    @Test
    void rejectsBlankPassword() {
        assertEquals("请填写初始密码", UserManageActions.validateNewUser("2025001", "张三", "", ""));
    }

    @Test
    void rejectsMismatchedConfirmation() {
        assertEquals("两次输入的密码不一致",
                UserManageActions.validateNewUser("2025001", "张三", "pw1", "pw2"));
    }

    @Test
    void acceptsCompleteInput() {
        assertNull(UserManageActions.validateNewUser("2025001", "张三", "pw1", "pw1"));
    }

    @Test
    void acceptsMissingDisplayNameBecauseUserNameIsTheFallback() {
        assertNull(UserManageActions.validateNewUser("2025001", "", "pw1", "pw1"));
    }

    @Test
    void rejectsBlankResetPassword() {
        assertEquals("请填写新密码", UserManageActions.validateResetPassword("", ""));
    }

    @Test
    void rejectsMismatchedResetConfirmation() {
        assertEquals("两次输入的新密码不一致",
                UserManageActions.validateResetPassword("pw1", "pw2"));
    }

    @Test
    void acceptsMatchingResetPassword() {
        assertNull(UserManageActions.validateResetPassword("pw1", "pw1"));
    }
}
