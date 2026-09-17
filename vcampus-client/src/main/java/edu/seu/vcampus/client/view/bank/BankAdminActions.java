package edu.seu.vcampus.client.view.bank;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.bank.BankService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.common.bank.dto.BankAdminAccountView;

import java.awt.Component;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** 管理端账户动作：冻结、解冻、查看流水与重置密码。 */
final class BankAdminActions {

    private final BankService api;
    private final Component parent;
    private final Runnable onChanged;

    /**
     * 创建动作编排器。
     *
     * @param api 银行 API
     * @param parent 对话框父组件
     * @param onChanged 成功后刷新列表的回调
     */
    BankAdminActions(BankService api, Component parent, Runnable onChanged) {
        this.api = api;
        this.parent = parent;
        this.onChanged = onChanged;
    }

    /** 冻结选中账户。
     * @param account 选中账户 */
    void freeze(BankAdminAccountView account) {
        setFrozen(account, true);
    }

    /** 解冻选中账户。
     * @param account 选中账户 */
    void unfreeze(BankAdminAccountView account) {
        setFrozen(account, false);
    }

    /** 查看选中账户的流水。
     * @param account 选中账户 */
    void showTransactions(BankAdminAccountView account) {
        if (account == null) {
            warn("请先选择一个账户");
            return;
        }
        new BankAdminTransactionsDialog(SwingUtilities.getWindowAncestor(parent), api, account)
                .setVisible(true);
    }

    /** 重置选中账户的银行密码。
     * @param account 选中账户 */
    void resetPassword(BankAdminAccountView account) {
        if (account == null) {
            warn("请先选择一个账户");
            return;
        }
        new ResetBankPasswordDialog(SwingUtilities.getWindowAncestor(parent), api, account,
                onChanged).setVisible(true);
    }

    private void setFrozen(final BankAdminAccountView account, final boolean frozen) {
        if (account == null) {
            warn("请先选择一个账户");
            return;
        }
        String verb = frozen ? "冻结" : "解冻";
        if (!BankDialogs.confirm(parent, "请确认",
                verb + "账户 " + account.getUsername() + "？",
                JOptionPane.WARNING_MESSAGE)) {
            return;
        }
        UiTasks.run(new UiTasks.Task<BankAdminAccountView>() {
            @Override
            public BankAdminAccountView run() {
                return api.setFrozen(account.getUsername(), frozen);
            }
        }, new UiTasks.Success<BankAdminAccountView>() {
            @Override
            public void accept(BankAdminAccountView result) {
                refresh();
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                warn(error.getMessage());
                refresh();
            }
        });
    }

    private void refresh() {
        if (onChanged != null) {
            onChanged.run();
        }
    }

    private void warn(String text) {
        BankDialogs.message(parent, "提示", text, JOptionPane.WARNING_MESSAGE);
    }
}
