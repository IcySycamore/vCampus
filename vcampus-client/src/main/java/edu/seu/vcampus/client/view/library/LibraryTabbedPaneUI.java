package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.view.theme.UiTheme;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

/** 图书馆页面专用的圆角标签页外观。 */
final class LibraryTabbedPaneUI extends BasicTabbedPaneUI {

    private static final Color INACTIVE_BACKGROUND = new Color(232, 241, 246);
    private static final int CORNER_RADIUS = 14;

    @Override
    protected void installDefaults() {
        super.installDefaults();
        tabAreaInsets = new Insets(0, 8, 7, 0);
        selectedTabPadInsets = new Insets(0, 0, 0, 0);
        contentBorderInsets = new Insets(0, 0, 0, 0);
        tabInsets = new Insets(8, 14, 8, 14);
        textIconGap = 7;
    }

    @Override
    protected void paintTabBackground(Graphics graphics, int placement, int index,
            int x, int y, int width, int height, boolean selected) {
        Graphics2D graphics2d = (Graphics2D) graphics.create();
        graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2d.setColor(selected ? UiTheme.ACCENT : INACTIVE_BACKGROUND);
        graphics2d.fillRoundRect(x + 1, y + 1, width - 2, height - 3,
                CORNER_RADIUS, CORNER_RADIUS);
        graphics2d.dispose();
    }

    @Override
    protected void paintTabBorder(Graphics graphics, int placement, int index,
            int x, int y, int width, int height, boolean selected) {
        Graphics2D graphics2d = (Graphics2D) graphics.create();
        graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2d.setColor(selected ? UiTheme.ACCENT_DARK : UiTheme.NAVY_LIGHT);
        graphics2d.drawRoundRect(x + 1, y + 1, width - 3, height - 4,
                CORNER_RADIUS, CORNER_RADIUS);
        graphics2d.dispose();
    }

    @Override
    protected void paintContentBorder(Graphics graphics, int placement, int selectedIndex) {
    }

    @Override
    protected void paintFocusIndicator(Graphics graphics, int placement,
            java.awt.Rectangle[] rects, int index, java.awt.Rectangle iconRect,
            java.awt.Rectangle textRect, boolean selected) {
    }

    @Override
    public void installUI(JComponent component) {
        super.installUI(component);
        component.setOpaque(false);
    }
}
