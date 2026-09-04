package edu.seu.vcampus.client.view.shell;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Map;
import java.util.WeakHashMap;
import javax.swing.JTable;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/**
 * 根据窗口宽度响应式放大字体和表格行高。
 */
public final class ResponsiveTypography {

    private static final Map<Component, Font> BASE_FONTS = new WeakHashMap<Component, Font>();
    private static final Map<JTable, Integer> BASE_ROWS = new WeakHashMap<JTable, Integer>();
    private static final Map<JLabel, ImageIcon> BASE_ICONS =
            new WeakHashMap<JLabel, ImageIcon>();
    private static final Map<Window, Integer> DESIGN_WIDTHS = new WeakHashMap<Window, Integer>();
    private static final Map<Window, Float> MAX_SCALES = new WeakHashMap<Window, Float>();
    private static float userScale = 1F;

    private ResponsiveTypography() {
    }

    /**
     * 为窗口安装响应式字号。全屏时最大放大至 1.32 倍。
     *
     * @param window 目标窗口
     * @param designWidth 设计基准宽度
     */
    public static void install(final Window window, final int designWidth) {
        install(window, designWidth, 1.32F);
    }

    /**
     * 为窗口安装可指定最大倍率的响应式字号。
     *
     * @param window 目标窗口
     * @param designWidth 设计基准宽度
     * @param maxScale 最大缩放倍率
     */
    public static void install(final Window window, final int designWidth,
            final float maxScale) {
        DESIGN_WIDTHS.put(window, designWidth);
        MAX_SCALES.put(window, maxScale);
        window.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        refresh(window, designWidth, maxScale);
                    }
                });
            }
        });
    }

    /**
     * 让组件在全屏时按窗口宽度等比例放大。
     *
     * @param window 所属窗口
     * @param component 需要缩放的组件
     * @param baseWidth 组件基准宽度
     * @param baseHeight 组件基准高度
     * @param designWidth 窗口设计基准宽度
     * @param maxScale 最大缩放倍率
     */
    public static void installScaledSize(final Window window, final Component component,
            final int baseWidth, final int baseHeight, final int designWidth,
            final float maxScale) {
        updateScaledSize(window, component, baseWidth, baseHeight, designWidth, maxScale);
        window.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                updateScaledSize(window, component, baseWidth, baseHeight,
                        designWidth, maxScale);
            }
        });
    }

    /**
     * 让指定区域始终占窗口固定宽度比例。
     *
     * @param window 所属窗口
     * @param component 需要调整宽度的区域
     * @param ratio 窗口宽度占比
     */
    public static void installProportionalWidth(final Window window,
            final Component component, final float ratio) {
        updateWidth(window, component, ratio);
        window.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                updateWidth(window, component, ratio);
            }
        });
    }

    /**
     * 设置用户字号倍率并立即刷新全部窗口。
     *
     * @param scale 1.0 至 1.3 的倍率
     */
    public static void setUserScale(float scale) {
        userScale = Math.max(1F, Math.min(1.3F, scale));
        for (Map.Entry<Window, Integer> entry : DESIGN_WIDTHS.entrySet()) {
            Float maximum = MAX_SCALES.get(entry.getKey());
            refresh(entry.getKey(), entry.getValue(), maximum == null ? 1.32F : maximum);
        }
    }

    /** @return 当前用户字号倍率 */
    public static float getUserScale() {
        return userScale;
    }

    private static void refresh(Window window, int designWidth, float maxScale) {
        float windowScale = (float) window.getWidth() / designWidth;
        float ratio = Math.max(1F, Math.min(maxScale, windowScale)) * userScale;
        apply(window, Math.min(1.55F, ratio));
        window.validate();
        window.repaint();
    }

    private static void updateWidth(Window window, Component component, float ratio) {
        component.setPreferredSize(new Dimension(Math.round(window.getWidth() * ratio), 0));
        window.validate();
    }

    private static void updateScaledSize(Window window, Component component,
            int baseWidth, int baseHeight, int designWidth, float maxScale) {
        float scale = Math.max(1F, Math.min(maxScale,
                (float) window.getWidth() / designWidth));
        component.setPreferredSize(new Dimension(Math.round(baseWidth * scale),
                Math.round(baseHeight * scale)));
        window.validate();
    }

    private static void apply(Component component, float ratio) {
        Font current = component.getFont();
        if (current != null && !BASE_FONTS.containsKey(component)) {
            BASE_FONTS.put(component, current);
        }
        Font base = BASE_FONTS.get(component);
        if (base != null) {
            component.setFont(base.deriveFont(base.getSize2D() * ratio));
        }
        if (component instanceof JTable) {
            JTable table = (JTable) component;
            if (!BASE_ROWS.containsKey(table)) {
                BASE_ROWS.put(table, table.getRowHeight());
            }
            table.setRowHeight(Math.round(BASE_ROWS.get(table) * ratio));
        }
        if (component instanceof JLabel) {
            applyResponsiveIcon((JLabel) component, ratio);
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                apply(child, ratio);
            }
        }
    }

    private static void applyResponsiveIcon(JLabel label, float ratio) {
        if (!Boolean.TRUE.equals(label.getClientProperty("responsiveIcon"))) {
            return;
        }
        Icon current = label.getIcon();
        if (!BASE_ICONS.containsKey(label) && current instanceof ImageIcon) {
            BASE_ICONS.put(label, (ImageIcon) current);
        }
        ImageIcon base = BASE_ICONS.get(label);
        if (base != null) {
            int size = Math.round(base.getIconWidth() * ratio);
            Image image = base.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
            label.setIcon(new ImageIcon(image));
        }
    }
}
