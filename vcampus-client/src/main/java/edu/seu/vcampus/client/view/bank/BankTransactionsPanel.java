package edu.seu.vcampus.client.view.bank;

import edu.seu.vcampus.client.view.component.RoundedPanel;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.bank.dto.BankTransactionListResponse;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/** 流水区域：类型筛选、空状态、只读表格和分页。 */
final class BankTransactionsPanel extends RoundedPanel {
    private static final long serialVersionUID = 1L;
    final JComboBox<String> type = new JComboBox<String>(
            new String[] {"全部交易", "充值", "消费", "返现"});
    final JButton previous = new JButton("上一页");
    final JButton next = new JButton("下一页");
    private final JLabel pageLabel = new JLabel("第 1 页");
    private final JLabel count = new JLabel("每页 20 条");
    private final JLabel empty = new JLabel("开户后，你的资金流水将显示在这里", JLabel.CENTER);
    private final CardLayout cards = new CardLayout();
    private final JPanel body = new JPanel(cards);
    private final DefaultTableModel model = BankTableModels.create();
    private int page = 1;
    private long totalPages;
    private boolean available;

    BankTransactionsPanel() {
        super(new BorderLayout(0, 14), 20, UiTheme.SURFACE);
        setBorder(BorderFactory.createEmptyBorder(20, 22, 18, 22));
        setPreferredSize(new Dimension(650, 300));
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JLabel title = new JLabel("资金流水");
        title.setFont(UiTheme.font(Font.BOLD, 18F));
        title.setForeground(UiTheme.TEXT);
        heading.add(title, BorderLayout.WEST);
        JPanel filter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filter.setOpaque(false);
        filter.add(new JLabel("交易类型"));
        type.setPreferredSize(new Dimension(116, 32));
        filter.add(type);
        heading.add(filter, BorderLayout.EAST);
        add(heading, BorderLayout.NORTH);
        JTable table = new JTable(model);
        UiFactory.styleTable(table);
        table.setFont(UiTheme.font(Font.PLAIN, 13F));
        table.setDefaultRenderer(Object.class, new RowRenderer());
        table.getColumnModel().getColumn(0).setPreferredWidth(170);
        table.getColumnModel().getColumn(1).setPreferredWidth(60);
        table.getColumnModel().getColumn(4).setPreferredWidth(180);
        table.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        body.setBackground(UiTheme.SURFACE);
        body.add(scroll, "table");
        empty.setForeground(UiTheme.MUTED);
        body.add(empty, "empty");
        cards.show(body, "empty");
        add(body, BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        count.setForeground(UiTheme.MUTED);
        footer.add(count, BorderLayout.WEST);
        JPanel paging = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        paging.setOpaque(false);
        paging.add(previous);
        paging.add(pageLabel);
        paging.add(next);
        footer.add(paging, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
        setBusy(false);
    }

    void showPage(BankTransactionListResponse result) {
        available = true;
        page = result.getPageNumber();
        totalPages = result.getTotalPages();
        BankTableModels.fill(model, result.getTransactions());
        empty.setText(type.getSelectedIndex() == 0 ? "暂无流水，充值后可在这里查看记录"
                : "暂无这类交易，试试其他筛选条件");
        cards.show(body, result.getTransactions().isEmpty() ? "empty" : "table");
        count.setText("共 " + result.getTotalCount() + " 笔交易 · 每页 " + result.getPageSize() + " 条");
        pageLabel.setText("第 " + page + " / " + Math.max(1, totalPages) + " 页");
    }

    void showMessage(String message) {
        available = false;
        model.setRowCount(0);
        empty.setText(message);
        cards.show(body, "empty");
        count.setText("每页 20 条");
        pageLabel.setText("—");
    }

    void setBusy(boolean busy) {
        type.setEnabled(!busy && available);
        previous.setEnabled(!busy && available && page > 1);
        next.setEnabled(!busy && available && page < totalPages);
    }

    /** 交替行底色和收支着色；选中行保留选中色。 */
    private static final class RowRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focus, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focus, row, column);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            setHorizontalAlignment(column == 2 || column == 3 ? RIGHT : LEFT);
            if (!selected) {
                setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 250));
                setForeground(column == 2 ? (String.valueOf(value).startsWith("−")
                        ? UiTheme.ACCENT : UiTheme.SUCCESS) : UiTheme.TEXT);
            }
            return this;
        }
    }
}
