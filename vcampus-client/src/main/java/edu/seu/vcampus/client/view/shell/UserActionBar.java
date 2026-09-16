package edu.seu.vcampus.client.view.shell;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * 用户管理动作条：左侧动作按钮 + 右侧分页（{@link UserPagerPanel}）。
 *
 * <p>
 * 从 {@code UserManagePanel} 抽出（原文件破 200 行上限）。按钮只上报「要做什么」， 实现分别落在 {@link UserManageActions}（单条动作）与
 * {@link UserBatchImport}（批量动作）。
 */
class UserActionBar extends JPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /**
     * 构造动作条。
     *
     * @param actions 单条动作
     * @param batch   批量动作
     * @param table   表格（提供选中项）
     * @param pager   页脚（由调用方持有，便于刷新页码）
     */
    UserActionBar(final UserManageActions actions, final UserBatchImport batch,
            final UserManageTable table, UserPagerPanel pager) {
        setLayout(new BorderLayout());
        setOpaque(false);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        buttons.setOpaque(false);
        buttons.add(createButton("启用 / 禁用", new Runnable() {
            @Override
            public void run() {
                actions.toggleEnabled(table.selectedUser());
            }
        }));
        buttons.add(createButton("编辑姓名", new Runnable() {
            @Override
            public void run() {
                actions.rename(table.selectedUser());
            }
        }));
        buttons.add(createButton("新建用户", new Runnable() {
            @Override
            public void run() {
                actions.create();
            }
        }));
        buttons.add(createButton("注销", new Runnable() {
            @Override
            public void run() {
                actions.unregister(table.selectedUser());
            }
        }));
        buttons.add(createButton("批量注册(文件)", new Runnable() {
            @Override
            public void run() {
                batch.registerFromFile();
            }
        }));
        buttons.add(createButton("批量注销(选中)", new Runnable() {
            @Override
            public void run() {
                batch.unregister(table.selectedUserNames());
            }
        }));
        add(buttons, BorderLayout.WEST);
        add(pager, BorderLayout.EAST);
    }

    /** 把「点击 → 动作」包成按钮。 */
    private JButton createButton(String text, final Runnable action) {
        JButton button = new JButton(text);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                action.run();
            }
        });
        return button;
    }
}
