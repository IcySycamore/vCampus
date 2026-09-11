package edu.seu.vcampus.client.view.component;

import java.awt.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 工作台统计卡片测试。
 */
class StatCardPanelTest {

    @Test
    void buildsTextAndBadgeAreas() {
        StatCardPanel panel = new StatCardPanel("校园服务", "6", "student", Color.BLUE);

        assertEquals(2, panel.getComponentCount());
    }
}
