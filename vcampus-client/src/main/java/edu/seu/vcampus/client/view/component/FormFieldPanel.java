package edu.seu.vcampus.client.view.component;

import edu.seu.vcampus.client.view.theme.UiTheme;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * 对话框中统一的标签和输入控件行。
 */
public class FormFieldPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /**
     * 创建表单行。
     *
     * @param text 标签文字
     * @param component 输入组件
     * @param width 输入组件宽度
     */
    public FormFieldPanel(String text, JComponent component, int width) {
        super(new BorderLayout(12, 0));
        setOpaque(false);
        JLabel label = new JLabel(text);
        label.setPreferredSize(new Dimension(78, 38));
        label.setForeground(UiTheme.NAVY);
        label.setFont(UiTheme.font(Font.BOLD, 13F));
        add(label, BorderLayout.WEST);
        component.setPreferredSize(new Dimension(width, 38));
        component.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
        if (component instanceof JTextField) {
            ((JTextField) component).setColumns(18);
        }
        add(component, BorderLayout.CENTER);
    }

    /**
     * 将表单行添加到使用 GridBagLayout 的父面板。
     *
     * @param parent 父面板
     * @param row 行号
     */
    public void addTo(JPanel parent, int row) {
        GridBagConstraints grid = new GridBagConstraints();
        grid.gridy = row;
        grid.weightx = 1;
        grid.fill = GridBagConstraints.HORIZONTAL;
        grid.insets = new Insets(7, 5, 7, 5);
        parent.add(this, grid);
    }
}
