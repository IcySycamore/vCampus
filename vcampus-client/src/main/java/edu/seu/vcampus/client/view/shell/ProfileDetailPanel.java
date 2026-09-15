package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.student.StudentService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.common.student.dto.StudentModifyRequest;
import edu.seu.vcampus.common.student.entity.StudentProfile;
import edu.seu.vcampus.common.user.entity.Role;

import java.awt.BorderLayout;
import java.util.Map;
import javax.swing.JPanel;

/**
 * 在校档案页：调 201 取本人档案交给 {@link ProfileRowsPanel} 渲染，底部 {@link ProfileActionBar}
 * 负责「申请修改 / 提交申请 / 取消」的切换。
 *
 * <p>
 * 修改是<b>就地</b>的：点「申请修改」后表格里可改的那几项直接变成控件，行的位置一个都不动
 * （见 {@link ProfileRowsPanel}），不弹窗口、也不另开一块表单。
 *
 * <p>
 * 提交前先过 {@link StudentModifyRequests#check} 这层纯规则，不通过就把原因写到操作条的就地提示里；
 * 通过后才发 202。成功也只是「落一条待审记录」，学籍要等教务在 203 通过才会变。
 */
final class ProfileDetailPanel extends JPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 学籍 API；未装配时为 null（给出提示而不抛异常）。 */
    private final StudentService m_student;

    /** 表格（查看态 / 修改态共用）。 */
    private final ProfileRowsPanel m_rows = new ProfileRowsPanel();

    /** 底部操作条。 */
    private final ProfileActionBar m_actions;

    /** 最近一次查回来的本人档案；null 表示没查到。 */
    private StudentProfile m_profile;

    /**
     * 创建档案页并立即发起查询。
     *
     * @param student 学籍 API；未装配时可为 null
     * @param role 当前登录角色；null 视为无权限
     */
    ProfileDetailPanel(StudentService student, Role role) {
        this.m_student = student;
        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        add(m_rows, BorderLayout.NORTH);
        add(m_actions = new ProfileActionBar(this, role), BorderLayout.SOUTH);
        load();
    }

    /** 重新查一次本人档案（「刷新」按钮的落点）。 */
    void reload() {
        load();
    }

    /** 进入修改态：可改的那几项就地变成控件。 */
    void startEdit() {
        if (m_student == null || m_profile == null) {
            m_actions.setStatus("暂未登记档案，无法申请修改");
            return;
        }
        m_rows.startEdit(m_profile);
        m_actions.setEditing(true);
        m_actions.setStatus("");
    }

    /** 放弃这次修改，切回查看态。 */
    void cancelEdit() {
        m_rows.show(m_profile);
        m_actions.setEditing(false);
        m_actions.setStatus("");
    }

    /** 提交申请：先过纯规则检查，再交给服务端。 */
    void submit() {
        if (m_student == null || m_profile == null) {
            m_actions.setStatus("暂未登记档案，无法提交申请");
            return;
        }
        final String reason = m_rows.reason();
        String problem = StudentModifyRequests.check(m_profile, m_rows.fieldText(),
                m_rows.yearText(), m_rows.statusName(), reason);
        if (problem != null) {
            m_actions.setStatus(problem);
            return;
        }
        final Map<String, String> changes = m_rows.changes();
        final StudentModifyRequest request =
                new StudentModifyRequest(m_profile.getId(), changes, reason);
        m_actions.setStatus("提交中…");
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                m_student.applyModification(request);
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void ignored) {
                m_rows.show(m_profile);
                m_actions.setEditing(false);
                m_actions.setStatus("已提交，等待教务审核");
            }
        });
    }

    /**
     * 查本人档案并回填。
     *
     * <p>
     * 线程切换与失败提示统一交给 {@link UiTasks}（ADR-0009 D1/D9）：页面里不出现裸线程、
     * {@code SwingUtilities.invokeLater} 与 {@code try/catch}。未装配 API 时直接给提示。
     */
    private void load() {
        final StudentService service = m_student;
        if (service == null) {
            showHint("尚未连接服务器");
            return;
        }
        UiTasks.run(new UiTasks.Task<StudentProfile>() {
            @Override
            public StudentProfile run() {
                return service.queryMyProfile();
            }
        }, new UiTasks.Success<StudentProfile>() {
            @Override
            public void accept(StudentProfile profile) {
                m_profile = profile;
                m_rows.show(profile);
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                showHint("暂未登记档案：" + error.getMessage());
            }
        });
    }

    /**
     * 显示提示并切回查看态。
     *
     * @param text 提示文本
     */
    private void showHint(String text) {
        m_profile = null;
        m_actions.setEditing(false);
        m_actions.setStatus(text);
        m_rows.show(null);
    }
}
