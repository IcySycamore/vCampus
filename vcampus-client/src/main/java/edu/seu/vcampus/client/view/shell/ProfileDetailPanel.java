package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.VCampusClientApp;
import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.student.StudentService;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.student.entity.PersonCategory;
import edu.seu.vcampus.common.student.entity.StudentProfile;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * 在校档案明细：调 201 取本人档案并渲染（教师、学生通用）。
 *
 * <p>
 * 与 {@link ProfilePanel}（身份卡与页面骨架）分开，一是单文件不超过 200 行，二是明细这块将来
 * 加字段（院系、联系方式等）时只改这一个文件。
 *
 * <p>
 * <b>专业与研究方向是同一个字段的两种叫法</b>：学生看「专业」，教师看「研究方向」，取值都来自
 * {@code StudentProfile.field}。界面按人员类别换标签，两类用户各自看到熟悉的名词，服务端只需
 * 维护一个字段、一个索引。
 */
final class ProfileDetailPanel extends JPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 明细行容器（查询回来后就地替换内容）。 */
    private final JPanel m_rows = new JPanel(new GridLayout(0, 1, 0, 10));

    /** 创建明细面板并立即发起查询。 */
    ProfileDetailPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
        m_rows.setOpaque(false);
        add(m_rows, BorderLayout.NORTH);
        load();
    }

    /** 后台线程查本人档案；未连接时直接给提示。 */
    private void load() {
        final StudentService service = VCampusClientApp.getStudentService();
        if (service == null) {
            showHint("尚未连接服务器");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final StudentProfile profile = service.queryMyProfile();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            render(profile);
                        }
                    });
                } catch (final ApiException exception) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            showHint("暂未登记档案：" + exception.getMessage());
                        }
                    });
                }
            }
        }, "vcampus-profile").start();
    }

    /**
     * 渲染档案明细。
     *
     * @param profile 档案；null 表示没有记录
     */
    private void render(StudentProfile profile) {
        if (profile == null) {
            showHint("暂未登记档案");
            return;
        }
        boolean teacher = profile.getPersonCategory() == PersonCategory.TEACHER;
        clearRows();
        m_rows.add(row("人员类别", profile.getPersonCategory().getDisplayName()));
        m_rows.add(row("姓名", orDash(profile.getRealName())));
        m_rows.add(row(teacher ? "研究方向" : "专业", orDash(profile.getField())));
        m_rows.add(row(teacher ? "入职年份" : "入学年份",
                String.valueOf(profile.getJoinYear())));
        m_rows.add(row("在校状态",
                profile.getStatus() == null ? "-" : profile.getStatus().getDisplayName()));
        refresh();
    }

    /**
     * 显示一行提示（未连接、无档案等）。
     *
     * @param text 提示文本
     */
    private void showHint(String text) {
        clearRows();
        JLabel hint = new JLabel(text);
        hint.setForeground(UiTheme.MUTED);
        hint.setFont(UiTheme.font(Font.PLAIN, 14F));
        m_rows.add(hint);
        refresh();
    }

    /** 清空明细行。 */
    private void clearRows() {
        m_rows.removeAll();
    }

    /** 让界面重画（在 EDT 上调用）。 */
    private void refresh() {
        m_rows.revalidate();
        m_rows.repaint();
    }

    /**
     * 构造一行「标签 · 值」。
     *
     * @param label 标签
     * @param value 值
     * @return 行面板
     */
    private static JPanel row(String label, String value) {
        JPanel line = new JPanel(new BorderLayout(12, 0));
        line.setOpaque(false);
        JLabel name = new JLabel(label);
        name.setForeground(UiTheme.MUTED);
        name.setFont(UiTheme.font(Font.PLAIN, 14F));
        JLabel content = new JLabel(value);
        content.setForeground(UiTheme.TEXT);
        content.setFont(UiTheme.font(Font.BOLD, 15F));
        line.add(name, BorderLayout.WEST);
        line.add(content, BorderLayout.EAST);
        return line;
    }

    /**
     * 空值显示成短横线，避免界面上出现「null」。
     *
     * @param value 值
     * @return 非空文本
     */
    private static String orDash(String value) {
        return value == null || value.trim().length() == 0 ? "-" : value.trim();
    }
}
