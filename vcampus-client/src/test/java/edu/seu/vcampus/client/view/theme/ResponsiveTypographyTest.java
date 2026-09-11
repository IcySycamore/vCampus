package edu.seu.vcampus.client.view.theme;

import java.awt.Font;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 响应式字号配置测试。
 */
class ResponsiveTypographyTest {

    @AfterEach
    void restoreScale() {
        ResponsiveTypography.setUserScale(1F);
    }

    @Test
    void clampsUserScaleToSupportedRange() {
        ResponsiveTypography.setUserScale(2F);
        assertEquals(1.3F, ResponsiveTypography.getUserScale(), 0.001F);

        ResponsiveTypography.setUserScale(0.5F);
        assertEquals(1F, ResponsiveTypography.getUserScale(), 0.001F);
    }

    @Test
    void createsThemeFontAtRequestedSize() {
        Font font = UiTheme.font(Font.BOLD, 18F);

        assertEquals(Font.BOLD, font.getStyle());
        assertEquals(18, font.getSize());
    }
}
