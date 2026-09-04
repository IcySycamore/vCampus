package edu.seu.vcampus.client.view.shell;

import java.awt.Window;
import javax.swing.JDialog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 用户注册窗口结构测试。
 */
class RegisterDialogTest {

    @Test
    void exposesModalWindowConstructor() throws Exception {
        assertTrue(JDialog.class.isAssignableFrom(RegisterDialog.class));
        assertNotNull(RegisterDialog.class.getConstructor(Window.class));
    }
}
