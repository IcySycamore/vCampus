package edu.seu.vcampus.client.view.component;

import edu.seu.vcampus.client.view.theme.UiIcons;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * 登录表单中带图标的文本输入行。
 */
public class IconTextFieldPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /**
     * 创建带图标输入行。
     *
     * @param icon 图标名称
     * @param field 输入框
     */
    public IconTextFieldPanel(String icon, JTextField field) {
        super(new BorderLayout(9, 0));
        setBackground(new Color(250, 250, 252));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(222, 210, 214)),
                BorderFactory.createEmptyBorder(7, 11, 7, 11)));
        add(new JLabel(UiIcons.load(icon, 18)), BorderLayout.WEST);
        field.setBorder(BorderFactory.createEmptyBorder());
        field.setOpaque(false);
        field.setPreferredSize(new Dimension(280, 28));
        add(field, BorderLayout.CENTER);
    }
}
