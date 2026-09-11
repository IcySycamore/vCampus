package edu.seu.vcampus.client.view.component;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

/** 按指定权重纵向或横向分配空间的布局。 */
public class ProportionalLayout implements LayoutManager {

    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;
    private final int orientation;
    private final int gap;
    private final float[] ratios;

    /**
     * 创建比例布局。
     *
     * @param orientation 排列方向
     * @param gap 组件间距
     * @param ratios 各组件所占比例
     */
    public ProportionalLayout(int orientation, int gap, float... ratios) {
        this.orientation = orientation;
        this.gap = gap;
        this.ratios = ratios.clone();
    }

    @Override
    public void addLayoutComponent(String name, Component component) {
    }

    @Override
    public void removeLayoutComponent(Component component) {
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return parent.getSize();
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return new Dimension(0, 0);
    }

    @Override
    public void layoutContainer(Container parent) {
        Component[] components = parent.getComponents();
        int count = Math.min(components.length, ratios.length);
        Insets insets = parent.getInsets();
        int totalGap = Math.max(0, count - 1) * gap;
        int insetSize = orientation == VERTICAL
                ? insets.top + insets.bottom : insets.left + insets.right;
        int available = (orientation == VERTICAL ? parent.getHeight()
                : parent.getWidth()) - insetSize - totalGap;
        float ratioTotal = ratioTotal(count);
        int position = 0;
        for (int index = 0; index < count; index++) {
            int size = index == count - 1 ? available - position
                    : Math.round(available * ratios[index] / ratioTotal);
            setBounds(parent, components[index], position + index * gap, size, insets);
            position += size;
        }
    }

    private float ratioTotal(int count) {
        float total = 0F;
        for (int index = 0; index < count; index++) {
            total += ratios[index];
        }
        return total;
    }

    private void setBounds(Container parent, Component component, int position, int size,
            Insets insets) {
        if (orientation == VERTICAL) {
            component.setBounds(insets.left, insets.top + position,
                    parent.getWidth() - insets.left - insets.right, Math.max(0, size));
        } else {
            component.setBounds(insets.left + position, insets.top, Math.max(0, size),
                    parent.getHeight() - insets.top - insets.bottom);
        }
    }
}
