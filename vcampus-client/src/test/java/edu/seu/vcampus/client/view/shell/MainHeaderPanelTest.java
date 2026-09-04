package edu.seu.vcampus.client.view.shell;

import java.awt.Component;
import java.awt.Container;
import javax.swing.JButton;
import javax.swing.JTextField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 主窗口顶栏交互测试。
 */
class MainHeaderPanelTest {

    @Test
    void dispatchesSearchAndSettingsActions() {
        final String[] query = new String[1];
        final int[] settingsCount = new int[1];
        MainHeaderPanel panel = new MainHeaderPanel("10001", "学生",
                new StringHandler() {
                    @Override
                    public void handle(String value) {
                        query[0] = value;
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        settingsCount[0]++;
                    }
                });
        JTextField search = findField(panel);
        JButton settings = findButton(panel, "设置");

        assertNotNull(search);
        assertNotNull(settings);
        search.setText("图书馆");
        search.postActionEvent();
        settings.doClick();

        assertEquals("图书馆", query[0]);
        assertEquals(1, settingsCount[0]);
    }

    private JTextField findField(Container root) {
        for (Component component : root.getComponents()) {
            if (component instanceof JTextField) {
                return (JTextField) component;
            }
            if (component instanceof Container) {
                JTextField result = findField((Container) component);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private JButton findButton(Container root, String text) {
        for (Component component : root.getComponents()) {
            if (component instanceof JButton && text.equals(((JButton) component).getText())) {
                return (JButton) component;
            }
            if (component instanceof Container) {
                JButton result = findButton((Container) component, text);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}
