package edu.seu.vcampus.client.view.shell;

import javax.swing.JFrame;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 客户端主窗口结构测试。
 */
class MainFrameTest {

    @Test
    void providesUserAndRoleConstructors() throws Exception {
        assertTrue(JFrame.class.isAssignableFrom(MainFrame.class));
        assertNotNull(MainFrame.class.getConstructor(String.class));
        assertNotNull(MainFrame.class.getConstructor(String.class, String.class));
    }
}
