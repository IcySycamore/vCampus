package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.student.StudentService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.student.entity.StudentModifyRequest;
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
 * 申请单详情对话框：把一条修改申请完整摊开给教务看。
 *
 * <p>
 * 「修改审核」表格列宽有限，变更内容与理由都会被截断成「field=计算机科…」，而审批恰恰要看全文——
 * 这是一条会写进学籍的记录，不该靠猜。所以详情里逐项解码展示（见
 * {@link StudentModifyRequests#describeChanges(String)}）。
 *
 * <p>
 * 打开时顺带查一次目标学籍，把<b>学号与姓名</b>带出来：教务本就有全量查看权（{@code STUDENT_VIEW_ALL}），
 * 一次 201 就够，否则列表里只看得见一个账户 uuid，不知道该批给谁。查不到（比如档案已注销）
 * 也要能看申请全文，所以查证失败不弹错、只少那两行。
 *
 * <p>
 * 文本拼装是纯函数 {@link #linesOf(StudentModifyRequest, StudentProfile)}，可直接单测（ADR-0005 不写 GUI 自动化）。
 */
final class ModifyRequestDetailDialog extends JDialog {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /**
     * 打开详情：先查目标学籍，再弹窗。
     *
     * @param parent 宿主组件（用于定位与取所属窗口）
     * @param api 学籍 API；未装配时为 null（只展示申请单本身）
     * @param request 申请单
     */
    static void open(final Component parent, final StudentService api,
            final StudentModifyRequest request) {
        if (request == null) {
            return;
        }
        final Window owner = SwingUtilities.getWindowAncestor(parent);
        if (api == null || request.getProfileId() == null) {
            show(owner, request, null);
            return;
        }
        final long profileId = request.getProfileId().longValue();
        UiTasks.run(new UiTasks.Task<StudentProfile>() {
            @Override
            public StudentProfile run() {
                return api.queryProfile(profileId);
            }
        }, new UiTasks.Success<StudentProfile>() {
            @Override
            public void accept(StudentProfile profile) {
                show(owner, request, profile);
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                show(owner, request, null);
            }
        });
    }

    /**
     * 弹出对话框。
     *
     * @param owner 宿主窗口
     * @param request 申请单
     * @param profile 目标学籍；null 表示没查到
     */
    private static void show(Window owner, StudentModifyRequest request, StudentProfile profile) {
        new ModifyRequestDetailDialog(owner, request, profile).setVisible(true);
    }

    /**
     * 构造详情对话框。
     *
     * @param owner 宿主窗口
     * @param request 申请单
     * @param profile 目标学籍；null 表示没查到
     */
    private ModifyRequestDetailDialog(Window owner, StudentModifyRequest request,
            StudentProfile profile) {
        super(owner, "申请单详情 · #" + dash(request.getRequestId()), ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(0, 10));
        getContentPane().setBackground(UiTheme.BACKGROUND);
        add(createTextArea(request, profile), BorderLayout.CENTER);
        add(createActions(), BorderLayout.SOUTH);
        setPreferredSize(new Dimension(560, 430));
        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * 详情全文（纯函数，可单测）。
     *
     * @param request 申请单
     * @param profile 目标学籍；null 表示没查到
     * @return 逐行文本
     */
    static List<String> linesOf(StudentModifyRequest request, StudentProfile profile) {
        List<String> lines = new ArrayList<String>();
        lines.add("申请单号：" + dash(request.getRequestId()));
        lines.add("目标学籍：" + targetText(request, profile));
        lines.add("申请人（账户）：" + dash(request.getApplicantUuid()));
        lines.add("状态：" + (request.getStatus() == null
                ? "-"
                : request.getStatus().getDisplayName()));
        lines.add("申请时间：" + ModifyRequestTableModels.timeText(request.getAppliedAt()));
        lines.add("");
        lines.add("变更内容：");
        List<String> changes = StudentModifyRequests.describeChanges(request.getChangesJson());
        if (changes.isEmpty()) {
            lines.add("  （空：这条申请没有带任何字段变更）");
        } else {
            lines.addAll(changes);
        }
        lines.add("");
        lines.add("申请理由：" + dash(request.getReason()));
        lines.add("审核意见：" + dash(request.getComment()));
        lines.add("审核时间：" + ModifyRequestTableModels.timeText(request.getAuditedAt()));
        return lines;
    }

    /**
     * 目标学籍一行：带上能认人的学号与姓名。
     *
     * @param request 申请单
     * @param profile 目标学籍；null 表示没查到
     * @return 描述文本
     */
    private static String targetText(StudentModifyRequest request, StudentProfile profile) {
        String base = "主键 " + dash(request.getProfileId());
        if (profile == null) {
            return base + "（未查到档案：可能已注销）";
        }
        StringBuilder text = new StringBuilder(base);
        text.append("　学号 ").append(dash(profile.getStudentNo()));
        text.append("　姓名 ").append(dash(profile.getRealName()));
        return text.toString();
    }

    private JScrollPane createTextArea(StudentModifyRequest request, StudentProfile profile) {
        StringBuilder body = new StringBuilder();
        List<String> lines = linesOf(request, profile);
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
