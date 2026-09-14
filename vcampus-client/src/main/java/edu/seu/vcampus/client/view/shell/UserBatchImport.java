package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.user.BatchProgressListener;
import edu.seu.vcampus.client.user.UserAdminService;
import edu.seu.vcampus.client.user.UserImportFile;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.common.constant.ProtocolLimit;
import edu.seu.vcampus.common.user.dto.BatchResult;
import edu.seu.vcampus.common.user.dto.RegisterRequest;

import java.awt.Component;
import java.io.IOException;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 * 批量操作：从文件批量注册（命令 103）与批量注销（命令 105）。
 *
 * <p>
 * <b>分片对用户可见</b>（见 ADR-0010 D1）：导入前先告知「共 N 个账号、将分 M 批发送、每批最多
 * {@link ProtocolLimit#MAX_BATCH_SIZE} 条」，导入中通过 {@link BatchProgressListener} 上报「已处理 x / N」，
 * 而不是让用户对着一个没有反应的窗口猜。
 */
public final class UserBatchImport {

    /** 失败明细最多展示的行数（其余折叠成一句提示）。 */
    private static final int MAX_DETAIL_ROWS = 15;

    /** 管理轨 API。 */
    private final UserAdminService m_api;

    /** 对话框父组件。 */
    private final Component m_parent;

    /** 完成后的刷新回调；可为 null。 */
    private final Runnable m_onChanged;

    /** 进度上报出口；可为 null。 */
    private final BatchProgressListener m_progress;

    /**
     * 构造批量动作执行器。
     *
     * @param api      用户管理 API
     * @param parent   对话框父组件
     * @param onChanged 完成后的刷新回调；可为 null
     * @param progress 进度上报出口；可为 null
     */
    public UserBatchImport(UserAdminService api, Component parent, Runnable onChanged,
            BatchProgressListener progress) {
        this.m_api = api;
        this.m_parent = parent;
        this.m_onChanged = onChanged;
        this.m_progress = progress;
    }

    /**
     * 计算分片后的批次数（纯函数，便于单测与提示文案）。
     *
     * @param size 条目数
     * @return 批次数；size 为 0 或负数返回 0
     */
    public static int batchCount(int size) {
        if (size <= 0) {
            return 0;
        }
        return (size + ProtocolLimit.MAX_BATCH_SIZE - 1) / ProtocolLimit.MAX_BATCH_SIZE;
    }

    /** 从文件批量注册：选文件 → 解析 → 确认 → 分片发送 → 展示结果。 */
    public void registerFromFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择批量注册文件（登录名,姓名,角色,口令）");
        if (chooser.showOpenDialog(m_parent) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        final List<RegisterRequest> requests;
        try {
            requests = UserImportFile.parse(chooser.getSelectedFile());
        } catch (IOException e) {
            warn("读取文件失败：" + e.getMessage());
            return;
        } catch (IllegalArgumentException e) {
            warn(e.getMessage());
            return;
        }
        if (requests.isEmpty()) {
            warn("文件中没有可导入的账号");
            return;
        }
        if (JOptionPane.showConfirmDialog(m_parent,
                "共 " + requests.size() + " 个账号，将分 " + batchCount(requests.size()) + " 批发送（每批最多 "
                        + ProtocolLimit.MAX_BATCH_SIZE + " 条）。\n已存在的登录名会被跳过并记入失败明细。",
                "批量注册", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        UiTasks.run(new UiTasks.Task<BatchResult>() {
            @Override
            public BatchResult run() {
                return m_api.batchRegister(requests, m_progress);
            }
        }, new UiTasks.Success<BatchResult>() {
            @Override
            public void accept(BatchResult result) {
                showResult("批量注册结果", result);
                refresh();
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                refresh();
            }
        });
    }

    /**
     * 批量注销（命令 105）。
     *
     * @param userNames 选中的登录名
     */
    public void unregister(List<String> userNames) {
        if (userNames == null || userNames.isEmpty()) {
            warn("请先在表格中选中要注销的账号（可按 Ctrl/Shift 多选）");
            return;
        }
        final List<String> names = userNames;
        if (JOptionPane.showConfirmDialog(m_parent,
                "确认注销以下 " + names.size() + " 个账号？各模块档案会一并撤销。\n\n" + preview(names),
                "批量注销", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        UiTasks.run(new UiTasks.Task<BatchResult>() {
            @Override
            public BatchResult run() {
                return m_api.batchUnregister(names, m_progress);
            }
        }, new UiTasks.Success<BatchResult>() {
            @Override
            public void accept(BatchResult result) {
                showResult("批量注销结果", result);
                refresh();
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                refresh();
            }
        });
    }

    /** 确认框里预览登录名（过多则折叠）。 */
    private String preview(List<String> names) {
        StringBuilder builder = new StringBuilder();
        int limit = Math.min(names.size(), MAX_DETAIL_ROWS);
        for (int index = 0; index < limit; index++) {
            builder.append(names.get(index)).append("  ");
        }
        if (names.size() > limit) {
            builder.append("…… 等共 ").append(names.size()).append(" 个");
        }
        return builder.toString();
    }

    /** 展示批量结果：摘要 + 失败明细。 */
    private void showResult(String title, BatchResult result) {
        StringBuilder text = new StringBuilder(result.summary());
        List<BatchResult.Failure> failures = result.getFailures();
        int limit = Math.min(failures.size(), MAX_DETAIL_ROWS);
        for (int index = 0; index < limit; index++) {
            text.append("\n").append(failures.get(index).toString());
        }
        if (failures.size() > limit) {
            text.append("\n…… 其余 ").append(failures.size() - limit).append(" 条失败明细已省略");
        }
        JOptionPane.showMessageDialog(m_parent, text.toString(), title,
                JOptionPane.INFORMATION_MESSAGE);
    }

    /** 通知宿主刷新列表。 */
    private void refresh() {
        if (m_onChanged != null) {
            m_onChanged.run();
        }
    }

    private void warn(String message) {
        JOptionPane.showMessageDialog(m_parent, message, "无法继续", JOptionPane.WARNING_MESSAGE);
    }
}
