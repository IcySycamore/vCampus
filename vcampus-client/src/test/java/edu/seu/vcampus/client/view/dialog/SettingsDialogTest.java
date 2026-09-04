package edu.seu.vcampus.client.view.dialog;

import java.awt.Window;
import javax.swing.JDialog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 界面设置窗口结构测试。
 */
class SettingsDialogTest {

    @Test
    void exposesModalWindowConstructor() throws Exception {
        assertTrue(JDialog.class.isAssignableFrom(SettingsDialog.class));
        assertNotNull(SettingsDialog.class.getConstructor(Window.class));
    }
}
