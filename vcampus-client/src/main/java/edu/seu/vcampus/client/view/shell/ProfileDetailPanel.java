package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.student.StudentService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.common.student.dto.StudentModifyRequest;
import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.StudentProfile;
import edu.seu.vcampus.common.user.entity.Role;

import java.awt.BorderLayout;
import java.util.Calendar;
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
 * 提交分两条路：学籍<b>还没填写过</b>时是自助建档，填完直接生效（204，在校状态与账号由服务端定）；
 * 填写过之后是修改申请，先过 {@link StudentModifyRequests#check} 这层纯规则，通过才发 202，
 * 成功也只是「落一条待审记录」，学籍要等教务在 203 通过才会变。
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

    /** 当前这次编辑是「自助填写」（204 立即生效）还是「修改申请」（202 待审）。 */
    private boolean m_enrolling;

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

    /**
     * 进入修改态：可改的那几项就地变成控件，提交后走 202 审核流程。
     *
     * <p>
     * 没有档案就提不了修改申请（申请得指向一条学籍），这时提示先自助填写，而不是悄悄替学生改成
     * 「填写」——两种意图混在一个按钮里，管理员那边就会出现「学生说提了、我这里一条都没有」。
     */
    void startEdit() {
        if (m_student == null) {
            m_actions.setStatus("尚未连接服务器");
            return;
        }
        if (m_profile == null) {
            m_actions.setStatus("暂未登记学籍，请先点「填写学籍信息」");
            return;
        }
        m_enrolling = false;
        m_rows.startEdit(m_profile);
        m_actions.setEditing(true, false);
        m_actions.setStatus("");
    }

    /**
     * 进入填写态（自助建档）：提交后走 204，立即生效、不经审核。
     *
     * <p>
     * 与「申请修改」分成两个入口，是因为两者改的是同一个表格、结果却完全不同：填写立即生效，
     * 修改要等教务通过。靠「学术方向是不是空的」隐式分流，会让「申请修改」在学籍为空白时永远
     * 走不到审核那条路——而学籍是内存存储，每次重启都退回空白，审核队列于是长期是空的。
     */
    void startEnroll() {
        if (m_student == null) {
            m_actions.setStatus("尚未连接服务器");
            return;
        }
        if (m_profile == null) {
            m_profile = blankProfile();
        }
        m_enrolling = true;
        m_rows.startEdit(m_profile);
        m_actions.setEditing(true, true);
        m_actions.setStatus("填写后直接生效（无需审核）；要改已填写的内容请用「申请修改」");
    }

    /**
     * 学籍是否还没填写过：学术方向为空即视为「第一次填写」。
     *
     * <p>
     * 这里只决定界面走哪条路，真正的判定在服务端 204 里再查一遍（客户端说什么都不作数）。
     *
     * @return true 表示该走自助建档
     */
    private boolean needsEnroll() {
        return m_profile == null || m_profile.getField() == null
                || m_profile.getField().trim().length() == 0;
    }

    /** 现建一条本人空白档案，仅用作自助填写表单的初值。 */
    private StudentProfile blankProfile() {
        return new StudentProfile(null, Calendar.getInstance().get(Calendar.YEAR),
                CampusStatus.ENROLLED);
    }

    /** 放弃这次修改，切回查看态。 */
    void cancelEdit() {
        m_rows.show(m_profile);
        m_actions.setEditing(false, false);
        m_actions.setStatus("");
    }

    /** 提交：由进入编辑的那个按钮决定走哪条路（填写 = 204 立即生效，修改 = 202 待审）。 */
    void submit() {
        if (m_student == null) {
            m_actions.setStatus("尚未连接服务器");
            return;
        }
        if (m_enrolling) {
            submitEnroll();
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
                m_actions.setEditing(false, false);
                m_actions.setStatus("已提交，等待教务审核");
            }
        });
    }

    /**
     * 自助建档（204）：只提交学术方向与入校年份，在校状态与账号 uuid 都由服务端定。
     *
     * <p>
     * 这是「新用户也能建自己的学籍」那条需求的客户端落点：填完直接生效，不必等教务审核。
     */
    private void submitEnroll() {
        final String field = m_rows.fieldText() == null ? "" : m_rows.fieldText().trim();
        if (field.length() == 0) {
            m_actions.setStatus("请填写专业 / 研究方向");
            return;
        }
        final String yearText = m_rows.yearText() == null ? "" : m_rows.yearText().trim();
        if (!StudentModifyRequests.isInteger(yearText)) {
            m_actions.setStatus("入校年份请填 4 位数字");
            return;
        }
        final StudentProfile payload = new StudentProfile(
                m_profile == null ? null : m_profile.getUserUuid(),
                Integer.parseInt(yearText), CampusStatus.ENROLLED);
        payload.setField(field);
        m_actions.setStatus("提交中…");
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                m_student.registerStudent(payload);
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void ignored) {
                m_actions.setEditing(false, false);
                m_actions.setStatus("学籍信息已填写");
                reload();
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
                m_actions.setEnrollAvailable(needsEnroll());
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                showHint("暂未登记档案：" + error.getMessage());
                // 查不到档案正是最该让学生自己填写的情形
                m_actions.setEnrollAvailable(true);
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
        m_actions.setEditing(false, false);
        m_actions.setStatus(text);
        m_rows.show(null);
    }
}
