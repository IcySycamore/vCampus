package edu.seu.vcampus.client.view.bank;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.bank.BankService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.bank.dto.BankAdminAccountView;
import edu.seu.vcampus.common.bank.dto.BankAdminQuery;
import edu.seu.vcampus.common.message.PageResponse;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;

/**
 * 管理员的银行账户管理页：检索、分页浏览并处理他人的账户。
 *
 * <p>与学生/教师的个人页共用侧栏条目「校园银行」，由 {@code MainContentPanel} 按角色决定挂哪一个。
 * 客户端只负责显示，服务端对管理轨命令按 {@code Role.ADMIN} 判定 403。</p>
 */
public class BankAdminPanel extends JPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 每页账户数。 */
    private static final int PAGE_SIZE = 20;

    /** 银行 API；未装配时为 null。 */
    private final BankService api;

    private final JTextField keyword = new JTextField(16);
    private final JButton query = UiFactory.primaryButton("查询", "search");
    private final JButton refresh = UiFactory.secondaryButton("刷新", "bank");
    private final JButton previous = UiFactory.secondaryButton("上一页", "bank");
    private final JButton next = UiFactory.secondaryButton("下一页", "bank");
    private final JButton detail = UiFactory.secondaryButton("查看流水", "bank");
    private final JButton freeze = UiFactory.secondaryButton("冻结", "bank");
    private final JButton unfreeze = UiFactory.secondaryButton("解冻", "bank");
    private final JButton reset = UiFactory.secondaryButton("重置密码", "bank");
    private final JLabel pageLabel = new JLabel("第1 /1页");
    private final JLabel countLabel = new JLabel("共0个账户");
    private final JLabel feedback = new JLabel(" ");
    private final BankAdminTable table = new BankAdminTable();
    private BankAdminActions actions;
    private int pageNumber = 1;
    private boolean busy;

    /** 创建未接入 API的管理页（布局预览）。 */
    public BankAdminPanel() {
        this(null);
    }

    /**
     * 创建管理页。
     *
     * @param api 银行 API；null 表示未登录
     */
    public BankAdminPanel(BankService api) {
        this.api = api;
        setLayout(new BorderLayout(0, 16));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 20, 28));
        add(createHeader(), BorderLayout.NORTH);
        add(table, BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);
        bindActions();
        if (api == null) {
            feedback.setText("请以管理员身份登录后管理银行账户");
            setControlsEnabled(false);
        } else {
            actions = new BankAdminActions(api, this, new Runnable() {
                @Override
                public void run() {
                    load("操作已完成，列表已刷新");
                }
            });
            load("账户列表已加载");
        }
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 12));
        header.setOpaque(false);
        JPanel titles = new JPanel(new BorderLayout());
        titles.setOpaque(false);
        JLabel title = new JLabel("银行账户管理");
        title.setFont(UiTheme.font(Font.BOLD, 22F));
        title.setForeground(UiTheme.TEXT);
        JLabel subtitle = new JLabel("检索任意用户，查看流水、冻结账户或重置银行密码");
        subtitle.setFont(UiTheme.font(Font.PLAIN, 12F));
        subtitle.setForeground(UiTheme.MUTED);
        titles.add(title, BorderLayout.NORTH);
        titles.add(subtitle, BorderLayout.SOUTH);
        header.add(titles, BorderLayout.NORTH);

        JPanel search = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        search.setOpaque(false);
        JLabel hint = new JLabel("学号 /工号或姓名");
        hint.setFont(UiTheme.font(Font.PLAIN, 12F));
        hint.setForeground(UiTheme.MUTED);
        search.add(hint);
        search.add(keyword);
        search.add(query);
        header.add(search, BorderLayout.CENTER);
        header.add(refresh, BorderLayout.EAST);
        return header;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.setOpaque(false);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        buttons.setOpaque(false);
        buttons.add(detail);
        buttons.add(freeze);
        buttons.add(unfreeze);
        buttons.add(reset);
        footer.add(buttons, BorderLayout.WEST);

        JPanel pager = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        pager.setOpaque(false);
        pager.add(countLabel);
        pager.add(previous);
        pager.add(pageLabel);
        pager.add(next);
        footer.add(pager, BorderLayout.EAST);

        feedback.setForeground(UiTheme.MUTED);
        feedback.setFont(UiTheme.font(Font.PLAIN, 12F));
        footer.add(feedback, BorderLayout.SOUTH);
        return footer;
    }

    private void bindActions() {
        query.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                pageNumber = 1;
                load("检索完成");
            }
        });
        refresh.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                load("账户列表已刷新");
            }
        });
        previous.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (pageNumber > 1) {
                    pageNumber--;
                    load("已翻到上一页");
                }
            }
        });
        next.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                pageNumber++;
                load("已翻到下一页");
            }
        });
        detail.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                actions.showTransactions(table.selectedAccount());
            }
        });
        freeze.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                actions.freeze(table.selectedAccount());
            }
        });
        unfreeze.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                actions.unfreeze(table.selectedAccount());
            }
        });
        reset.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                actions.resetPassword(table.selectedAccount());
            }
        });
    }

    private void load(final String success) {
        if (busy || api == null) {
            return;
        }
        setBusy(true, "正在读取账户…");
        final BankAdminQuery request =
                new BankAdminQuery(keyword.getText(), pageNumber, PAGE_SIZE);
        UiTasks.run(new UiTasks.Task<PageResponse<BankAdminAccountView>>() {
            @Override
            public PageResponse<BankAdminAccountView> run() {
                return api.listAccounts(request);
            }
        }, new UiTasks.Success<PageResponse<BankAdminAccountView>>() {
            @Override
            public void accept(PageResponse<BankAdminAccountView> page) {
                pageNumber = page.getPageNumber();
                table.show(page);
                countLabel.setText("共 " + page.getTotal() + "个账户");
                pageLabel.setText("第 " + pageNumber + " / " + totalPagesOf(page) + "页");
                setBusy(false, success);
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                table.show(null);
                setBusy(false, " ");
                feedback.setForeground(UiTheme.ACCENT);
                feedback.setText(error.getMessage());
            }
        });
    }

    private static long totalPagesOf(PageResponse<?> page) {
        long total = page.getTotalPages();
        return total < 1L ? 1L : total;
    }

    private void setControlsEnabled(boolean enabled) {
        keyword.setEnabled(enabled);
        query.setEnabled(enabled);
        refresh.setEnabled(enabled);
        detail.setEnabled(enabled);
        freeze.setEnabled(enabled);
        unfreeze.setEnabled(enabled);
        reset.setEnabled(enabled);
    }

    private void setBusy(boolean value, String text) {
        busy = value;
        setControlsEnabled(!value);
        previous.setEnabled(!value && pageNumber > 1);
        next.setEnabled(!value);
        if (text != null) {
            feedback.setForeground(UiTheme.MUTED);
            feedback.setText(text);
        }
    }

    /** @return 账户表格（供测试驱动） */
    JTable table() {
        return table.table();
    }

    /** @return 关键字输入框（供测试驱动） */
    JTextField keywordField() {
        return keyword;
    }
}
