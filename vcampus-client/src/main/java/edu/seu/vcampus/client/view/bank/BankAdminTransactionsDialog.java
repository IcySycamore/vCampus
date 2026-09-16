package edu.seu.vcampus.client.view.bank;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.bank.BankService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.bank.dto.BankAdminAccountView;
import edu.seu.vcampus.common.bank.dto.BankTransactionListResponse;
import edu.seu.vcampus.common.bank.dto.BankTransactionQueryRequest;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

/** 管理员查看指定用户资金流水的弹窗。 */
final class BankAdminTransactionsDialog extends JDialog {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 每页条数。 */
    private static final int PAGE_SIZE = 20;

    private final BankService api;
    private final String username;
    private final JComboBox<String> type =
            new JComboBox<String>(new String[] {"全部交易", "充值", "消费", "返现"});
    private final JButton previous = UiFactory.secondaryButton("上一页", "bank");
    private final JButton next = UiFactory.secondaryButton("下一页", "bank");
    private final JButton maximize = UiFactory.secondaryButton("最大化", "bank");
    private final JLabel pageLabel = new JLabel("第1 /1页");
    private final JLabel message = new JLabel(" ");
    private final DefaultTableModel model = BankTableModels.create();
    private int page = 1;
    private boolean busy;

    /**
     * 构造弹窗并加载第一页。
     *
     * @param owner 父窗口
     * @param api 银行 API
     * @param account 目标账户
     */
    BankAdminTransactionsDialog(Window owner, BankService api,
            BankAdminAccountView account) {
        super(owner, "资金流水", ModalityType.APPLICATION_MODAL);
        if (api == null || account == null) {
            throw new IllegalArgumentException("api and account are required");
        }
        this.api = api;
        this.username = account.getUsername();
        setContentPane(content());
        setMinimumSize(new Dimension(760, 480));
        pack();
        setResizable(false);
        BankDialogs.centerOnScreen(this);
        BankDialogs.installMaximize(this, maximize);
        load("流水已加载");
    }

    private JPanel content() {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(UiTheme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(22, 26, 20, 26));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        header.setOpaque(false);
        JLabel title = new JLabel("流水 · " + username);
        title.setFont(UiTheme.font(Font.BOLD, 16F));
        title.setForeground(UiTheme.TEXT);
        header.add(title);
        header.add(type);
        header.add(previous);
        header.add(next);
        header.add(pageLabel);
        header.add(maximize);
        root.add(header, BorderLayout.NORTH);

        JTable table = new JTable(model);
        table.setFont(UiTheme.font(Font.PLAIN, 13F));
        table.setRowHeight(28);
        UiFactory.styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        scroll.setPreferredSize(new Dimension(700, 340));
        root.add(scroll, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        south.setOpaque(false);
        message.setForeground(UiTheme.MUTED);
        message.setFont(UiTheme.font(Font.PLAIN, 12F));
        JButton close = UiFactory.secondaryButton("关闭", "bank");
        close.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });
        south.add(message);
        south.add(close);
        root.add(south, BorderLayout.SOUTH);

        type.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                page = 1;
                load("筛选结果已更新");
            }
        });
        previous.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (page > 1) {
                    page--;
                    load("流水已更新");
                }
            }
        });
        next.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                page++;
                load("流水已更新");
            }
        });
        return root;
    }

    private void load(final String success) {
        if (busy || api == null) {
            return;
        }
        setBusy(true, "正在读取流水…");
        final BankTransactionQueryRequest query = new BankTransactionQueryRequest(page,
                PAGE_SIZE, BankTableModels.typeAt(type.getSelectedIndex()));
        UiTasks.run(new UiTasks.Task<BankTransactionListResponse>() {
            @Override
            public BankTransactionListResponse run() {
                return api.listTransactionsOf(username, query);
            }
        }, new UiTasks.Success<BankTransactionListResponse>() {
            @Override
            public void accept(BankTransactionListResponse result) {
                page = result.getPageNumber();
                BankTableModels.fill(model, result.getTransactions());
                pageLabel.setText("第 " + page + " / " + totalPagesOf(result) + "页");
                setBusy(false, success);
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                setBusy(false, " ");
                message.setForeground(UiTheme.ACCENT);
                message.setText(error.getMessage());
            }
        });
    }

    private static long totalPagesOf(BankTransactionListResponse result) {
        long total = result.getTotalPages();
        return total < 1L ? 1L : total;
    }

    private void setBusy(boolean value, String text) {
        busy = value;
        type.setEnabled(!value);
        previous.setEnabled(!value && page > 1);
        next.setEnabled(!value);
        setDefaultCloseOperation(value
                ? WindowConstants.DO_NOTHING_ON_CLOSE : WindowConstants.DISPOSE_ON_CLOSE);
        if (text != null) {
            message.setForeground(UiTheme.MUTED);
            message.setText(text);
        }
    }
}
