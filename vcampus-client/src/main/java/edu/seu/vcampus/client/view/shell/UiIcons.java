package edu.seu.vcampus.client.view.shell;

import java.awt.Image;
import java.net.URL;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

/**
 * 加载并缩放 IconPark 图标资源。
 */
public final class UiIcons {

    private UiIcons() {
    }

    /**
     * 加载指定图标。
     *
     * @param name 不带扩展名的资源名称
     * @param size 图标边长
     * @return 缩放后的图标；资源缺失时为 null
     */
    public static Icon load(String name, int size) {
        URL resource = UiIcons.class.getResource("/icons/" + name + ".png");
        if (resource == null) {
            return null;
        }
        Image image = new ImageIcon(resource).getImage();
        return new ImageIcon(image.getScaledInstance(size, size, Image.SCALE_SMOOTH));
    }

    /** 创建会随窗口字号倍率同步缩放的图标标签。 */
    public static JLabel responsiveLabel(String name, int size, int alignment) {
        JLabel label = new JLabel(load(name, size), alignment);
        label.putClientProperty("responsiveIcon", Boolean.TRUE);
        return label;
    }
}
