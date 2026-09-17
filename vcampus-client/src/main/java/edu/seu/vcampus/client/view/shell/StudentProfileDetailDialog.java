package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.student.entity.PersonCategory;
import edu.seu.vcampus.common.student.entity.StudentProfile;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * 人员档案详情对话框：把一行学籍完整摊开，供教师查看、供管理员核对。
 *
 * <p>
 * 与 {@link ModifyRequestDetailDialog}（申请单详情）成对：列表的列宽有限，「专业 / 研究方向」被截断
 * 成「计算机科学与jinnn…」时看不出到底填了什么，而这正是教师查人时最常要看的一格。
 *
 * <p>
 * <b>纯只读</b>：不查库、不改数据、也不发请求——行里的数据已经从 208 拿全了（姓名也已被服务端联查
 * 补上）。因此教师在只读的「学籍查询」页里点开它没有任何越权风险，管理员那一侧用的是同一份实现。
 *
 * <p>
 * 文本拼装是纯函数 {@link #linesOf(StudentProfile)}，可直接单测（ADR-0005 不写 GUI 自动化）。
 */
final class StudentProfileDetailDialog extends JDialog {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /**
     * 打开详情。
     *
     * @param parent 宿主组件（用于定位与取所属窗口）
     * @param profile 学籍；为 null 时不弹窗
     */
    static void open(Component parent, StudentProfile profile) {
        if (profile == null) {
            return;
        }
        new StudentProfileDetailDialog(SwingUtilities.getWindowAncestor(parent), profile)
                .setVisible(true);
    }

    /**
     * 构造详情对话框。
     *
     * @param owner 宿主窗口
     * @param profile 学籍
     */
    private StudentProfileDetailDialog(Window owner, StudentProfile profile) {
        super(owner, titleOf(profile), ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(0, 10));
        getContentPane().setBackground(UiTheme.BACKGROUND);
        add(createTextArea(profile), BorderLayout.CENTER);
        add(createActions(), BorderLayout.SOUTH);
        setPreferredSize(new Dimension(480, 340));
        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * 窗口标题：带上主键与姓名，一眼知道看的是谁的档案。
     *
     * @param profile 学籍
     * @return 标题
     */
    static String titleOf(StudentProfile profile) {
        return "人员档案详情 · #" + dash(profile.getId()) + " " + nameOf(profile);
    }

    /**
     * 详情全文（纯函数，可单测）。
     *
     * @param profile 学籍
     * @return 逐行文本
     */
    static List<String> linesOf(StudentProfile profile) {
        List<String> lines = new ArrayList<String>();
        lines.add("主键：" + dash(profile.getId()));
        lines.add("学号：" + dash(profile.getStudentNo()));
        lines.add("姓名：" + nameOf(profile));
        lines.add("人员类别：" + categoryText(profile));
        lines.add("在校状态：" + statusText(profile));
        lines.add("入校年份：" + profile.getJoinYear());
        lines.add("");
        lines.add(fieldLabel(profile) + "：" + fieldText(profile));
        lines.add("账户标识：" + dash(profile.getUserUuid()));
        return lines;
    }

    /**
     * 学术方向一行的标签：学生读作「专业」，教师读作「研究方向」。
     *
     * <p>
     * 两边共用同一个 {@code field} 字段（见 {@link PersonCategory}），标签跟着类别走，档案读起来才
     * 顺——对教师写「专业」会让看的人以为填错了。
     *
     * @param profile 学籍
     * @return 标签
     */
    static String fieldLabel(StudentProfile profile) {
        return profile.getPersonCategory() == PersonCategory.TEACHER ? "研究方向" : "专业";
    }

    /**
     * 学术方向的取值。空白时给一句说明而不是短横线：新生自助填写的档案本来就是空的，
     * 「还没填」与「填了但看不见」是两回事。
     *
     * @param profile 学籍
     * @return 取值文本
     */
    private static String fieldText(StudentProfile profile) {
        String field = profile.getField();
        return field == null || field.trim().length() == 0 ? "（尚未填写）" : field;
    }

    /**
     * 姓名；账户还没采集姓名时服务端会用 uuid 顶上。
     *
     * @param profile 学籍
     * @return 姓名文本
     */
    private static String nameOf(StudentProfile profile) {
        return dash(profile.getRealName());
    }

    /**
     * 人员类别文本。
     *
     * @param profile 学籍
     * @return 类别显示名
     */
    private static String categoryText(StudentProfile profile) {
        return profile.getPersonCategory().getDisplayName();
    }

    /**
     * 在校状态文本。
     *
     * @param profile 学籍
     * @return 状态显示名
     */
    private static String statusText(StudentProfile profile) {
        return profile.getStatus() == null ? "-" : profile.getStatus().getDisplayName();
    }

    /**
     * 正文区域。
     *
     * @param profile 学籍
     * @return 带滚动条的文本区
     */
    private JScrollPane createTextArea(StudentProfile profile) {
        StringBuilder body = new StringBuilder();
        List<String> lines = linesOf(profile);
        int index = 0;
        while (index < lines.size()) {
            body.append(lines.get(index)).append('\n');
            index = index + 1;
        }
        JTextArea text = new JTextArea(body.toString());
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(UiTheme.font(Font.PLAIN, 13F));
        text.setForeground(UiTheme.TEXT);
        text.setBackground(UiTheme.SURFACE);
        text.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        JScrollPane scroll = new JScrollPane(text);
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        return scroll;
    }

    /**
     * 底部按钮区（只有关闭）。
     *
     * @return 按钮栏
     */
    private JPanel createActions() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(0, 16, 14, 16));
        JPanel buttons = new JPanel(new GridLayout(1, 1, 8, 0));
        buttons.setOpaque(false);
        JButton close = new JButton("关闭");
        close.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });
        buttons.add(close);
        bar.add(buttons, BorderLayout.EAST);
        return bar;
    }

    /**
     * 空值统一显示成短横线。
     *
     * @param value 原值
     * @return 原值；null 或空白时为 "-"
     */
    private static String dash(String value) {
        return value == null || value.trim().length() == 0 ? "-" : value;
    }

    /**
     * 主键 → 文本。
     *
     * @param id 主键；可为 null
     * @return 文本
     */
    private static String dash(Long id) {
        return id == null ? "-" : id.toString();
    }
}
