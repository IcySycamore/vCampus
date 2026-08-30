package edu.seu.vcampus.client.view.shell;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.Enumeration;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.FontUIResource;

/**
 * 客户端统一字体、颜色和 Swing 外观配置。
 */
public final class UiTheme {

    public static final Color NAVY = new Color(17, 45, 69);
    public static final Color NAVY_LIGHT = new Color(31, 73, 101);
    public static final Color ACCENT = new Color(194, 57, 62);
    public static final Color ACCENT_DARK = new Color(157, 42, 49);
    public static final Color BACKGROUND = new Color(245, 241, 230);
    public static final Color SURFACE = Color.WHITE;
    public static final Color TEXT = new Color(27, 43, 56);
    public static final Color MUTED = new Color(104, 123, 138);
    public static final Color BORDER = new Color(221, 230, 236);
    public static final Color SUCCESS = new Color(31, 142, 106);
    private static final String FONT_FAMILY = chooseFont();

    /** 禁止实例化工具类。 */
    private UiTheme() {
    }

    /**
     * 启用 Nimbus，并为全部 Swing 控件应用统一中文字体和基础色。
     */
    public static void applyNimbus() {
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) {
                setLookAndFeel(info.getClassName());
                break;
            }
        }
        FontUIResource font = new FontUIResource(FONT_FAMILY, Font.PLAIN, 16);
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (key.toString().endsWith(".font")) {
                UIManager.put(key, font);
            }
        }
        UIManager.put("control", SURFACE);
        UIManager.put("text", TEXT);
        UIManager.put("nimbusBase", NAVY);
        UIManager.put("nimbusFocus", ACCENT);
        UIManager.put("nimbusSelectionBackground", ACCENT);
    }

    /**
     * 创建统一字体。
     *
     * @param style Font 样式
     * @param size 字号
     * @return 字体
     */
    public static Font font(int style, float size) {
        return new Font(FONT_FAMILY, style, Math.round(size));
    }

    private static void setLookAndFeel(String className) {
        try {
            UIManager.setLookAndFeel(className);
        } catch (ClassNotFoundException exception) {
            return;
        } catch (InstantiationException exception) {
            return;
        } catch (IllegalAccessException exception) {
            return;
        } catch (UnsupportedLookAndFeelException exception) {
            return;
        }
    }

    private static String chooseFont() {
        String[] available = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        java.util.List<String> fonts = Arrays.asList(available);
        String[] preferred = {"Microsoft YaHei UI", "Segoe UI", "Microsoft YaHei", "Dialog"};
        for (String font : preferred) {
            if (fonts.contains(font)) {
                return font;
            }
        }
        return Font.SANS_SERIF;
    }
}
