package edu.seu.vcampus.client.view.shell;

import javax.swing.JFrame;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 登录主窗口结构测试。
 */
class LoginFrameTest {

    @Test
    void providesDefaultFrameConstructor() throws Exception {
        assertTrue(JFrame.class.isAssignableFrom(LoginFrame.class));
        assertNotNull(LoginFrame.class.getConstructor());
    }
}
