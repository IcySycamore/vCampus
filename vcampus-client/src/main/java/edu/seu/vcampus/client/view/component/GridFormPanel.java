package edu.seu.vcampus.client.view.component;

import edu.seu.vcampus.client.view.theme.UiTheme;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 「标签 · 值」网格容器：按（行, 列）往格子里放标签或控件，行列怎么排由调用方决定。
 *
 * <p>
 * <b>两个要点</b>：
 * <ul>
 * <li>每个格子的 {@code weighty} 恒为 0，各行按自己的首选高度走。改用 {@code GridLayout} 会被
 * 最高的子控件（比如下拉框）把整列撑高，容器高度翻倍、超出可视区的部分会被直接裁掉。</li>
 * <li>标签列 {@code weightx} 为 0、值列给 1：标签贴着左边，控件吃掉剩余宽度。行距列距统一收在
 * 这里，调用方不必自己算 Insets——各处的间距也才不会一人一个样。</li>
 * </ul>
 */
public class GridFormPanel extends JPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 行距。 */
    private static final int ROW_GAP = 8;

    /** 标签与值之间的留白。 */
    private static final int LABEL_GAP = 10;

    /** 值列与下一对标签之间的留白。 */
    private static final int VALUE_GAP = 18;

    /** 创建空网格。 */
    public GridFormPanel() {
        setLayout(new GridBagLayout());
        setOpaque(false);
    }

    /**
     * 放一个标签格。
     *
     * @param row 行号
     * @param column 列号
     * @param text 标签文本
     * @param required 是否加红色星号（表示提交必填）
     * @return 自身，便于连续摆放
     */
    public GridFormPanel addLabel(int row, int column, String text, boolean required) {
        JLabel label = new JLabel(required ? requiredMark(text) : text);
        label.setForeground(UiTheme.MUTED);
        label.setFont(UiTheme.font(Font.PLAIN, 14F));
        add(label, constraints(row, column, 1, 0.0, LABEL_GAP));
        return this;
    }

    /**
     * 放一个值格：查看态给文本标签，修改态给输入控件，位置完全一样。
     *
     * @param row 行号
     * @param column 列号
     * @param width 跨几列
     * @param component 控件
     * @return 自身
     */
    public GridFormPanel addValue(int row, int column, int width, JComponent component) {
        add(component, constraints(row, column, width, 1.0, VALUE_GAP));
        return this;
    }

    /**
     * 造一个值标签（查看态用）。
     *
     * @param text 文本
     * @return 标签
     */
    public static JLabel value(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UiTheme.TEXT);
        label.setFont(UiTheme.font(Font.BOLD, 15F));
        return label;
    }

    /**
     * 给必填标签加一个红色星号。
     *
     * @param text 标签文本
     * @return 带星号的 HTML 文本
     */
    public static String requiredMark(String text) {
        String hex = String.format("#%06x", Integer.valueOf(UiTheme.ACCENT.getRGB() & 0xFFFFFF));
        return "<html><font color='" + hex + "'>*</font> " + text + "</html>";
    }

    /**
     * 造格子约束。
     *
     * @param row 行号
     * @param column 列号
     * @param width 跨几列
     * @param weightx 横向权重：值列给 1，标签列给 0
     * @param rightGap 右侧留白
     * @return 约束
     */
    private static GridBagConstraints constraints(int row, int column, int width, double weightx,
            int rightGap) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.gridwidth = width;
        constraints.weightx = weightx;
        constraints.weighty = 0.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, ROW_GAP, rightGap);
        return constraints;
    }
}
