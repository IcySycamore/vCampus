package edu.seu.vcampus.client.view.bank;
import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.bank.BankService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.bank.dto.BankAccountResponse;
import edu.seu.vcampus.common.bank.dto.BankRechargeResponse;
import edu.seu.vcampus.common.bank.dto.BankTransactionListResponse;
import edu.seu.vcampus.common.bank.dto.BankTransactionQueryRequest;
import edu.seu.vcampus.common.constant.Command;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.Rectangle;
import javax.swing.Scrollable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import javax.swing.Box;
import javax.swing.BoxLayout;
/** 银行页面：账户概览、开户、充值与分页流水；业务请求统一由 UiTasks 执行。 */
public class BankPanel extends JPanel implements Scrollable {
    private static final long serialVersionUID = 1L;
    private final BankService api;
    private final BankAccountCard account = new BankAccountCard();
    private final BankTransactionsPanel transactions = new BankTransactionsPanel();
    private final JButton refresh = UiFactory.secondaryButton("刷新", "bank");
    private final JLabel feedback = new JLabel("正在读取账户…");
    private int page = 1;
    private boolean busy;
    private boolean refreshPending;
    /** 创建仅供布局预览的页面。 */
    public BankPanel() { this(null); }
    /**
     * 创建页面并查询本人账户。
     * @param api 银行服务；null 时仅展示离线布局
     */
    public BankPanel(BankService api) {
        this.api = api;
        setLayout(new BorderLayout(0, 18));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 20, 28));
        JPanel top = new JPanel(new BorderLayout(0, 18));
        top.setOpaque(false);
        top.add(createHeading(), BorderLayout.NORTH);
        top.add(account, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        add(transactions, BorderLayout.CENTER);
        feedback.setForeground(UiTheme.MUTED);
        feedback.setFont(UiTheme.font(Font.PLAIN, 12F));
        add(feedback, BorderLayout.SOUTH);
        bindActions();
        if (api == null) {
            account.showUnavailable();
            feedback.setText("请登录后使用校园银行");
            refresh.setEnabled(false);
        } else {
            refreshData();
        }
    }
    private JPanel createHeading() {
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JPanel text = new JPanel(new BorderLayout(0, 5));
        text.setOpaque(false);
        JLabel title = new JLabel("校园银行");
        title.setFont(UiTheme.font(Font.BOLD, 27F));
        title.setForeground(UiTheme.TEXT);
        text.add(title, BorderLayout.NORTH);
        JLabel subtitle = new JLabel("每一笔收支，都清晰可见");
        subtitle.setForeground(UiTheme.MUTED);
        text.add(subtitle, BorderLayout.SOUTH);
        heading.add(text, BorderLayout.CENTER);
        heading.add(refresh, BorderLayout.EAST);
        return heading;
    }
    private void bindActions() {
        refresh.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) { refreshData(); }
        });
        account.action.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                if (account.isUnopened()) {
                    new OpenAccountDialog(SwingUtilities.getWindowAncestor(BankPanel.this),
                            api, new Runnable() {
                                @Override public void run() { loadAccount(); }
                            }).setVisible(true);
                } else {
                    new RechargeDialog(SwingUtilities.getWindowAncestor(BankPanel.this), api,
                            new UiTasks.Success<BankRechargeResponse>() {
                                @Override public void accept(BankRechargeResponse result) {
                                    account.showAccount(result.getAccount());
                                    page = 1;
                                    loadTransactions("充值成功，余额已更新");
                                }
                            }).setVisible(true);
                }
            }
        });
        account.freeze.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                toggleFreeze(true);
            }
        });
        account.unfreeze.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                toggleFreeze(false);
            }
        });
        account.changePassword.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                changePassword();
            }
        });
        transactions.type.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                page = 1;
                loadTransactions("流水已更新");
            }
        });
        transactions.previous.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                page--;
                loadTransactions("流水已更新");
            }
        });
        transactions.next.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                page++;
                loadTransactions("流水已更新");
            }
        });
    }
    private void toggleFreeze(final boolean freeze) {
        if (api == null) {
            return;
        }
        final BankPasswordField field = new BankPasswordField();
        JPanel prompt = new JPanel();
        prompt.setLayout(new BoxLayout(prompt, BoxLayout.Y_AXIS));
        prompt.add(new JLabel(freeze ? "请输入银行账户密码来挂失" : "请输入银行账户密码来解冻"));
        prompt.add(Box.createVerticalStrut(8));
        prompt.add(field);
        boolean confirmed = BankDialogs.confirm(this,
                freeze ? "主动挂失" : "解除挂失", prompt, JOptionPane.PLAIN_MESSAGE);
        if (!confirmed) {
            return;
        }
        final char[] password = field.getPassword();
        setBusy(true, freeze ? "正在提交挂失…" : "正在提交解冻…");
        UiTasks.run(new UiTasks.Task<BankAccountResponse>() {
            @Override public BankAccountResponse run() {
                try {
                    return freeze ? api.freezeAccount(password)
                            : api.unfreezeAccount(password);
                } finally {
                    java.util.Arrays.fill(password, '\0');
                }
            }
        }, new UiTasks.Success<BankAccountResponse>() {
            @Override public void accept(BankAccountResponse result) {
                account.showAccount(result);
                setBusy(false, freeze ? "账户已挂失" : "账户已解冻");
            }
        }, new UiTasks.Failure() {
            @Override public void accept(ApiException error) {
                BankDialogs.message(BankPanel.this,
                        freeze ? "挂失失败" : "解冻失败",
                        "密码输入错误，请重新输入密码",
                        JOptionPane.ERROR_MESSAGE);
                fail(error);
            }
        });
    }
    private void changePassword() {
        // Keep every password input consistent: users can verify what they typed
        // before submitting instead of having four unrelated masked fields.
        final BankPasswordField campusField = new BankPasswordField();
        final BankPasswordField oldField = new BankPasswordField();
        final BankPasswordField newField = new BankPasswordField();
        final BankPasswordField confirm = new BankPasswordField();
        JPanel prompt = new JPanel();
        prompt.setLayout(new BoxLayout(prompt, BoxLayout.Y_AXIS));
        prompt.add(new JLabel("校园系统密码"));
        prompt.add(campusField);
        prompt.add(new JLabel("当前银行账户密码"));
        prompt.add(oldField);
        prompt.add(new JLabel("新密码（8至64个字符）"));
        prompt.add(newField);
        prompt.add(new JLabel("确认新密码"));
        prompt.add(confirm);
        boolean confirmed = BankDialogs.confirm(this, "修改银行密码", prompt,
                JOptionPane.PLAIN_MESSAGE);
        if (!confirmed) {
            campusField.clear();
            oldField.clear();
            newField.clear();
            confirm.clear();
            return;
        }
        final char[] campus = campusField.getPassword();
        final char[] old = oldField.getPassword();
        final char[] next = newField.getPassword();
        final char[] check = confirm.getPassword();
        if (next.length < 8 || next.length > 64 || !java.util.Arrays.equals(next, check)) {
            java.util.Arrays.fill(campus, '\0');
            java.util.Arrays.fill(old, '\0');
            java.util.Arrays.fill(next, '\0');
            java.util.Arrays.fill(check, '\0');
            campusField.clear();
            oldField.clear();
            newField.clear();
            confirm.clear();
            BankDialogs.message(this, "修改失败", "新密码长度需为8至64个字符，且两次输入一致",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        setBusy(true, "正在验证校园密码…");
        UiTasks.run(new UiTasks.Task<BankAccountResponse>() {
            @Override public BankAccountResponse run() {
                try {
                    return api.changePassword(campus, old, next);
                } finally {
                    java.util.Arrays.fill(campus, '\0');
                    java.util.Arrays.fill(old, '\0');
                    java.util.Arrays.fill(next, '\0');
                    java.util.Arrays.fill(check, '\0');
                }
            }
        }, new UiTasks.Success<BankAccountResponse>() {
            @Override public void accept(BankAccountResponse result) {
                account.showAccount(result);
                setBusy(false, "银行密码修改成功");
            }
        }, new UiTasks.Failure() {
            @Override public void accept(ApiException error) {
                BankDialogs.message(BankPanel.this, "修改失败", error.getMessage(),
                        JOptionPane.ERROR_MESSAGE);
                fail(error);
            }
        });
    }

    /** 支付等外部资金变动后重新加载余额和第一页流水。 */
    public void refreshData() {
        if (api == null) { return; }
        if (busy) {
            refreshPending = true;
            return;
        }
        loadAccount();
    }
    private void loadAccount() {
        setBusy(true, "正在读取账户…");
        UiTasks.run(new UiTasks.Task<BankAccountResponse>() {
            @Override public BankAccountResponse run() {
                return api.queryMyAccount();
            }
        }, new UiTasks.Success<BankAccountResponse>() {
            @Override public void accept(BankAccountResponse result) {
                account.showAccount(result);
                busy = false;
                page = 1;
                loadTransactions("账户与流水已更新");
            }
        }, new UiTasks.Failure() {
            @Override public void accept(ApiException error) {
                if (Command.BANK_ACCOUNT_NOT_OPENED.equals(error.getStatusCode())) {
                    account.showUnopened();
                    transactions.showMessage("开通账户后，充值和消费记录将在这里展示");
                    setBusy(false, "你的账户仅供本人使用，请验证校园身份并设置银行密码");
                } else {
                    account.showUnavailable();
                    transactions.showMessage("账户读取失败，请点击右上角刷新");
                    fail(error);
                }
            }
        });
    }
    private void loadTransactions(final String success) {
        if (busy || api == null) { return; }
        final BankTransactionQueryRequest query = new BankTransactionQueryRequest(page, 20,
                BankTableModels.typeAt(transactions.type.getSelectedIndex()));
        setBusy(true, "正在读取流水…");
        UiTasks.run(new UiTasks.Task<BankTransactionListResponse>() {
            @Override public BankTransactionListResponse run() {
                return api.listMyTransactions(query);
            }
        }, new UiTasks.Success<BankTransactionListResponse>() {
            @Override public void accept(BankTransactionListResponse result) {
                page = result.getPageNumber();
                transactions.showPage(result);
                setBusy(false, success);
            }
        }, new UiTasks.Failure() {
            @Override public void accept(ApiException error) {
                transactions.showMessage("流水加载失败，请刷新重试");
                fail(error);
            }
        });
    }
    private void fail(ApiException error) {
        setBusy(false, error.getMessage());
        feedback.setForeground(UiTheme.ACCENT);
    }
    private void setBusy(boolean value, String message) {
        busy = value;
        refresh.setEnabled(!value);
        account.setBusy(value);
        transactions.setBusy(value);
        feedback.setForeground(UiTheme.MUTED);
        feedback.setText(message);
        if (!value && refreshPending) {
            refreshPending = false;
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() { refreshData(); }
            });
        }
    }
    @Override
    public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override
    public int getScrollableUnitIncrement(Rectangle rect, int orientation, int direction) {
        return 24;
    }
    @Override
    public int getScrollableBlockIncrement(Rectangle rect, int orientation, int direction) {
        return Math.max(24, rect.height - 24);
    }
    @Override
    public boolean getScrollableTracksViewportWidth() { return true; }
    @Override
    public boolean getScrollableTracksViewportHeight() {
        return getParent() != null && getParent().getHeight() >= getPreferredSize().height;
    }
}
