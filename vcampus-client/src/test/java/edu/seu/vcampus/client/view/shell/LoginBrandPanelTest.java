package edu.seu.vcampus.client.view.shell;

import java.awt.GridBagLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * 登录品牌展示区测试。
 */
class LoginBrandPanelTest {

    @Test
    void buildsBrandContentWithGridLayout() {
        LoginBrandPanel panel = new LoginBrandPanel();

        assertInstanceOf(GridBagLayout.class, panel.getLayout());
        assertEquals(1, panel.getComponentCount());
    }
}
