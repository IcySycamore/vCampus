package edu.seu.vcampus.client.view.dialog;

import edu.seu.vcampus.client.view.shell.StringHandler;

import java.awt.Window;
import javax.swing.JDialog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 全局搜索窗口结构测试。
 */
class GlobalSearchDialogTest {

    @Test
    void acceptsJavaSevenNavigationCallback() throws Exception {
        assertTrue(JDialog.class.isAssignableFrom(GlobalSearchDialog.class));
        assertNotNull(GlobalSearchDialog.class.getConstructor(
                Window.class, String.class, StringHandler.class));
    }
}
