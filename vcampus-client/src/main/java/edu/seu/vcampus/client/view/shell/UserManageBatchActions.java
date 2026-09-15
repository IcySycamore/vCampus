package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.user.UserImportFile;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.common.user.dto.BatchResult;
import edu.seu.vcampus.common.user.dto.RegisterRequest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/** Handles file-based registration and multi-row unregistration. */
final class UserManageBatchActions {
    private final UserManagePanel panel;

    UserManageBatchActions(UserManagePanel panel) {
        this.panel = panel;
    }

    void registerFromFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择批量注册文件（登录名,姓名,角色,口令）");
        if (chooser.showOpenDialog(panel) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        final List<RegisterRequest> requests = read(chooser.getSelectedFile());
        if (requests == null || requests.isEmpty()) {
            if (requests != null) {
                panel.warn("文件中没有可导入的账号");
            }
            return;
        }
        String prompt = "共解析到 " + requests.size()
                + " 个账号，确认导入？已存在的登录名会记入失败明细。";
        if (!confirm(prompt, "批量注册")) {
            return;
        }
        UiTasks.run(new UiTasks.Task<BatchResult>() {
            @Override
            public BatchResult run() {
                return panel.api().batchRegister(requests);
            }
        }, showResult("批量注册结果"));
    }

    void unregisterSelected() {
        int[] selected = panel.table().getSelectedRows();
        if (selected == null || selected.length == 0) {
            panel.warn("请先在表格中选中要注销的账号（可按 Ctrl/Shift 多选）");
            return;
        }
        final List<String> names = selectedNames(selected);
        if (!confirm("确认注销以下 " + names.size()
                + " 个账号？各模块档案会一并撤销。\n\n" + join(names), "批量注销")) {
            return;
        }
        UiTasks.run(new UiTasks.Task<BatchResult>() {
            @Override
            public BatchResult run() {
                return panel.api().batchUnregister(names);
            }
        }, showResult("批量注销结果"));
    }

    private List<RegisterRequest> read(File file) {
        try {
            return UserImportFile.parse(file);
        } catch (IOException e) {
            panel.warn("读取文件失败：" + e.getMessage());
        } catch (IllegalArgumentException e) {
            panel.warn(e.getMessage());
        }
        return null;
    }

    private List<String> selectedNames(int[] selected) {
        List<String> names = new ArrayList<String>();
        for (int viewRow : selected) {
            int row = panel.table().convertRowIndexToModel(viewRow);
            if (row >= 0 && row < panel.rows().size()) {
                names.add(panel.rows().get(row).getUserName());
            }
        }
        return names;
    }

    private String join(List<String> names) {
        StringBuilder text = new StringBuilder();
        for (String name : names) {
            text.append(name).append("  ");
        }
        return text.toString();
    }

    private boolean confirm(String message, String title) {
        return JOptionPane.showConfirmDialog(panel, message, title,
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
    }

    private UiTasks.Success<BatchResult> showResult(final String title) {
        return new UiTasks.Success<BatchResult>() {
            @Override
            public void accept(BatchResult result) {
                StringBuilder text = new StringBuilder(result.summary());
                for (BatchResult.Failure failure : result.getFailures()) {
                    text.append("\n").append(failure);
                }
                JOptionPane.showMessageDialog(panel, text.toString(), title,
                        result.isAllSucceeded() ? JOptionPane.INFORMATION_MESSAGE
                                : JOptionPane.WARNING_MESSAGE);
                panel.refresh();
            }
        };
    }
}
