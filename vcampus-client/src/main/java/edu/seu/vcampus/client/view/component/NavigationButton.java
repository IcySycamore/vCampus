package edu.seu.vcampus.client.view.component;

import edu.seu.vcampus.client.view.theme.UiIcons;
import edu.seu.vcampus.client.view.theme.UiTheme;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.SwingConstants;

/**
 * 具有统一图标、悬停和选中样式的侧栏导航按钮。
 */
public class NavigationButton extends JButton {

    private static final long serialVersionUID = 1L;
    private static final Color NORMAL_TEXT = new Color(190, 204, 219);
    private final String page;
    private boolean selectedState;

    /**
     * 创建导航按钮。
     *
     * @param text 显示文字
     * @param page 页面标识
     */
    public NavigationButton(String text, String page) {
        super(text, UiIcons.load(page + "-light", 22));
        this.page = page;
        setHorizontalAlignment(SwingConstants.LEFT);
        setIconTextGap(13);
        setFont(UiTheme.font(Font.BOLD, 16F));
        setForeground(NORMAL_TEXT);
        setBorder(normalBorder());
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFocusPainted(false);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                if (!selectedState) {
                    setForeground(Color.WHITE);
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                if (!selectedState) {
                    setForeground(NORMAL_TEXT);
                }
            }
        });
    }

    /**
     * 更新按钮选中样式。
     *
     * @param selected 是否选中
     */
    public void setSelectedState(boolean selected) {
        selectedState = selected;
        setForeground(selected ? Color.WHITE : NORMAL_TEXT);
        setBorder(selected ? selectedBorder() : normalBorder());
    }

    /**
     * 返回按钮对应页面。
     *
     * @return 页面标识
     */
    public String getPage() {
        return page;
    }

    private javax.swing.border.Border normalBorder() {
        return BorderFactory.createEmptyBorder(16, 27, 16, 18);
    }

    private javax.swing.border.Border selectedBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, UiTheme.ACCENT),
                BorderFactory.createEmptyBorder(16, 23, 16, 18));
    }
}
