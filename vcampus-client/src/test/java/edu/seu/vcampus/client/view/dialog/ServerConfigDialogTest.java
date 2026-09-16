package edu.seu.vcampus.client.view.dialog;

import edu.seu.vcampus.client.network.ClientServerConfig;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 服务器地址配置对话框测试：初始值回显、探测失败时的提示。
 */
class ServerConfigDialogTest {

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
