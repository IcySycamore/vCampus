package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.view.component.GridFormPanel;
import edu.seu.vcampus.common.student.entity.PersonCategory;
import edu.seu.vcampus.common.student.entity.StudentProfile;

import java.util.Map;

/**
 * 档案表格：查看态是「标签 · 值」，进入修改态后<b>可申请修改的那几项原地变成输入控件</b>，
 * 标签前加红色星号；其余行原样不动。
 *
 * <p>
 * 两态共用同一套网格（行数、列数、行距都不变），只把「值」那一格换成控件。「前后看上去差不多」
 * 是有意为之：用户不用在脑子里重建一张表，改完一项立刻能对上原来那一行。摆放规则见
 * {@link GridFormPanel}，控件本身由 {@link StudentModifyForm} 持有，本类只决定「哪一格放什么」。
 *
 * <p>
 * 只给三项控件：{@code field} / {@code joinYear} / {@code status}。202 的服务端白名单只认这三项；
 * 人员类别由身份决定、姓名归用户模块维护，都不该在这里改。
 */
final class ProfileRowsPanel extends GridFormPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 修改态的四项控件。 */
    private final StudentModifyForm m_form = new StudentModifyForm();

    /** 是否处于修改态。 */
    private boolean m_editing;

    /** 当前档案；null 表示还没查到。 */
    private StudentProfile m_profile;

    /**
     * 渲染成查看态。
     *
     * @param profile 档案；null 表示没有记录
     */
    void show(StudentProfile profile) {
        m_profile = profile;
        m_editing = false;
        rebuild();
    }

    /**
     * 就地进入修改态：原来的值换成控件，行的位置不动。
     *
     * @param profile 档案；null 时什么都不做
     */
    void startEdit(StudentProfile profile) {
        if (profile == null) {
            return;
        }
        m_profile = profile;
        m_editing = true;
        m_form.prefill(profile);
        rebuild();
    }

    /** @return 界面上的学术方向文本 */
    String fieldText() {
        return m_form.fieldText();
    }

    /** @return 界面上的年份文本 */
    String yearText() {
        return m_form.yearText();
    }

    /** @return 界面选中的在校状态枚举名；未选为空串 */
    String statusName() {
        return m_form.statusName();
    }

    /** @return 申请理由，已去首尾空白 */
    String reason() {
        return m_form.reason();
    }

    /** @return 要提交的变更（只含真正改动过的项） */
    Map<String, String> changes() {
        return StudentModifyRequests.changesOf(m_profile, fieldText(), yearText(), statusName());
    }

    /** 按当前档案与状态重建整张表：行号、列号与两态一致，只是值格里换东西。 */
    private void rebuild() {
        removeAll();
        if (m_profile == null) {
            addLabel(0, 0, "暂未登录服务器", false);
            addValue(0, 1, 3, GridFormPanel.value("暂未登记档案"));
            finish();
            return;
        }
        boolean teacher = m_profile.getPersonCategory() == PersonCategory.TEACHER;
        addLabel(0, 0, "人员类别", false);
        addValue(0, 1, 1, GridFormPanel.value(m_profile.getPersonCategory().getDisplayName()));
        addLabel(0, 2, "姓名", false);
        addValue(0, 3, 1, GridFormPanel.value(orDash(m_profile.getRealName())));
        addLabel(1, 0, teacher ? "研究方向" : "专业", m_editing);
        addValue(1, 1, 1, m_editing ? m_form.field
                : GridFormPanel.value(orDash(m_profile.getField())));
        addLabel(1, 2, teacher ? "入职年份" : "入学年份", m_editing);
        addValue(1, 3, 1, m_editing ? m_form.year
                : GridFormPanel.value(String.valueOf(m_profile.getJoinYear())));
        addLabel(2, 0, "在校状态", m_editing);
        addValue(2, 1, 1, m_editing ? m_form.status : GridFormPanel.value(
                m_profile.getStatus() == null ? "-" : m_profile.getStatus().getDisplayName()));
        if (m_editing) {
            addLabel(3, 0, "申请理由", true);
            addValue(3, 1, 3, m_form.reason);
        }
        finish();
    }

    /** 重画。 */
    private void finish() {
        revalidate();
        repaint();
    }

    /**
     * 空值显示成短横线，避免界面上出现「null」。
     *
     * @param text 原值
     * @return 原值；空白时为 "-"
     */
    private static String orDash(String text) {
        return text == null || text.trim().length() == 0 ? "-" : text;
    }
}
