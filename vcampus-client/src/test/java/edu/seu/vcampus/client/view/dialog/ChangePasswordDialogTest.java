package edu.seu.vcampus.client.view.dialog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 修改密码输入校验测试（纯函数）。
 */
class ChangePasswordDialogTest {

    @Test
    void rejectsBlankInput() {
        assertEquals("请完整填写密码", ChangePasswordDialog.validate("", "new", "new"));
        assertEquals("请完整填写密码", ChangePasswordDialog.validate("old", "", ""));
    }

    @Test
    void rejectsMismatchedConfirmation() {
        assertEquals("两次输入的新密码不一致",
                ChangePasswordDialog.validate("old", "new1", "new2"));
    }

    @Test
    void rejectsUnchangedPassword() {
        assertEquals("新密码不能与原密码相同",
                ChangePasswordDialog.validate("same", "same", "same"));
    }

    @Test
    void acceptsValidChange() {
        assertNull(ChangePasswordDialog.validate("old", "new", "new"));
    }
}
