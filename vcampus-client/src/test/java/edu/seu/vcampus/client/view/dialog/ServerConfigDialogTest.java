package edu.seu.vcampus.client.view.dialog;

import edu.seu.vcampus.client.network.ClientServerConfig;

import java.awt.GraphicsEnvironment;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 服务器地址配置对话框测试：初始值回显、探测失败时的提示。
 *
 * <p>
 * 用例会真的创建 {@link javax.swing.JDialog}，所以必须跑在带显示的环境（本地桌面，或 CI 下的 Xvfb）。
 * 无显示时整体跳过（见 {@link #requireDisplay()}），不让 HeadlessException 把构建打红。
 */
class ServerConfigDialogTest {

    /** 无显示环境（如 CI 未起 Xvfb）时跳过：headless 下 Swing 顶层窗口无法创建。 */
    @BeforeEach
    void requireDisplay() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "无显示环境（headless），跳过需要真实 JDialog 的用例");
    }

    @Test
    void showsCurrentConfiguration() {
        ServerConfigDialog dialog = new ServerConfigDialog(null,
                ClientServerConfig.of("203.0.113.10", 18888));
        try {
            assertEquals("203.0.113.10", dialog.hostText());
            assertEquals("18888", dialog.portText());
            assertNotNull(dialog.testButton());
            assertTrue(dialog.messageText().trim().isEmpty(), dialog.messageText());
        } finally {
            dialog.dispose();
        }
    }

    @Test
    void reportsConnectionFailure() throws Exception {
        final ServerConfigDialog dialog = new ServerConfigDialog(null,
                ClientServerConfig.of("127.0.0.1", 1), 200);
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    dialog.testButton().doClick();
                }
            });
            for (int i = 0; i < 100 && !dialog.messageText().contains("连接失败"); i++) {
                Thread.sleep(50);
            }
            assertTrue(dialog.messageText().contains("连接失败"), dialog.messageText());
        } finally {
            dialog.dispose();
        }
    }
}
