package edu.seu.vcampus.client.view.component;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 侧栏导航按钮测试。
 */
class NavigationButtonTest {

    @Test
    void exposesPageAndSelectedState() {
        NavigationButton button = new NavigationButton("图书馆", "library");

        button.setSelectedState(true);

        assertEquals("library", button.getPage());
        assertTrue(button.getBorder().getBorderInsets(button).left > 0);
    }
}
