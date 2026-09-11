package edu.seu.vcampus.client.view.dialog;

import java.awt.Window;
import javax.swing.JDialog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 修改密码窗口结构测试。
 */
class ChangePasswordDialogTest {

    @Test
    void exposesModalWindowConstructor() throws Exception {
        assertTrue(JDialog.class.isAssignableFrom(ChangePasswordDialog.class));
        assertNotNull(ChangePasswordDialog.class.getConstructor(Window.class));
    }
}
