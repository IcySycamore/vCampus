package edu.seu.vcampus.client.view.shell;

import java.awt.Rectangle;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 比例布局尺寸分配测试。
 */
class ProportionalLayoutTest {

    @Test
    void dividesHorizontalSpaceByRatio() {
        JPanel panel = new JPanel(new ProportionalLayout(
                ProportionalLayout.HORIZONTAL, 10, 1F, 3F));
        JLabel first = new JLabel();
        JLabel second = new JLabel();
        panel.add(first);
        panel.add(second);
        panel.setSize(410, 100);

        panel.doLayout();

        assertEquals(new Rectangle(0, 0, 100, 100), first.getBounds());
        assertEquals(new Rectangle(110, 0, 300, 100), second.getBounds());
    }
}
