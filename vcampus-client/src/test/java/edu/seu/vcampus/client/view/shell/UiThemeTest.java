package edu.seu.vcampus.client.view.shell;

import java.awt.Font;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 客户端主题配置测试。
 */
class UiThemeTest {

    @Test
    void exposesPaletteAndCreatesFonts() {
        Font font = UiTheme.font(Font.ITALIC, 15F);

        assertNotNull(UiTheme.NAVY);
        assertNotNull(UiTheme.ACCENT);
        assertEquals(Font.ITALIC, font.getStyle());
        assertEquals(15, font.getSize());
    }
}
