package edu.seu.vcampus.client.view.component;

import edu.seu.vcampus.client.view.theme.UiTheme;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

/**
 * 简洁的无边框标签页外观。
 */
public class ModernTabbedPaneUI extends BasicTabbedPaneUI {

    @Override
    protected void installDefaults() {
        super.installDefaults();
        tabAreaInsets = new Insets(0, 8, 5, 0);
        selectedTabPadInsets = new Insets(0, 0, 0, 0);
        contentBorderInsets = new Insets(0, 0, 0, 0);
        tabInsets = new Insets(9, 15, 9, 15);
        textIconGap = 7;
    }

    @Override
    protected void paintTabBackground(Graphics graphics, int placement, int index,
            int x, int y, int width, int height, boolean selected) {
        graphics.setColor(selected ? new Color(218, 237, 246) : UiTheme.BACKGROUND);
        graphics.fillRoundRect(x, y, width, height - 2, 12, 12);
    }

    @Override
    protected void paintTabBorder(Graphics graphics, int placement, int index,
            int x, int y, int width, int height, boolean selected) {
    }

    @Override
    protected void paintContentBorder(Graphics graphics, int placement, int selectedIndex) {
    }

    @Override
    protected void paintFocusIndicator(Graphics graphics, int placement, java.awt.Rectangle[] rects,
            int index, java.awt.Rectangle iconRect, java.awt.Rectangle textRect,
            boolean selected) {
    }

    @Override
    public void installUI(JComponent component) {
        super.installUI(component);
        component.setOpaque(false);
    }
}
