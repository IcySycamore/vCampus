package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.library.dto.BookQuery;
import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.message.PageResponse;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

/** 统一图书查询页的 Swing 组件与纯展示状态。 */
final class LibraryCatalogView extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final String[] QUERY_FIELDS = {"all", "title", "author", "isbn"};
    private final DefaultTableModel model = LibraryTableModels.create(new String[] {
            "ISBN", "书名", "作者", "分类", "馆藏总数", "可借数量", "状态"});
    private final JTable table = new JTable(model);
    private final List<Book> books = new ArrayList<Book>();
    private final boolean manager;
    private final JTextField keyword = new JTextField();
    private final JComboBox<String> field = new JComboBox<String>(
            new String[] {"全部字段", "书名", "作者", "ISBN"});
    private final List<JButton> actions = new ArrayList<JButton>();
    final LibraryBookEditor editor;
    final JLabel status = new JLabel("馆藏已加载，可输入关键词检索或点击表头排序");

    LibraryCatalogView(boolean manager, JButton borrowButton,
            JButton reserveButton,
            ActionListener borrow, ActionListener reserve, ActionListener catalogAction,
            ListSelectionListener selection) {
        this.manager = manager;
        setName(manager ? "libraryManagement" : "libraryCatalog");
        setLayout(new BorderLayout(12, 12));
        setBackground(UiTheme.BACKGROUND);
        add(searchHeader(catalogAction), BorderLayout.NORTH);
        configureTable();
        javax.swing.JScrollPane scroll = LibraryViewBuilder.scroll(table,
                manager ? "libraryManagementScroll" : "libraryCatalogScroll",
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        JPanel results = new JPanel(new BorderLayout(0, 8));
        results.setOpaque(false);
        results.add(commands(manager, borrowButton, reserveButton,
                borrow, reserve, catalogAction), BorderLayout.NORTH);
        editor = manager ? new LibraryBookEditor() : null;
        if (editor == null) {
            results.add(scroll, BorderLayout.CENTER);
        } else {
            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scroll, editor);
            split.setName("libraryManagementSplit");
            split.setResizeWeight(0.68D);
            split.setBorder(null);
            results.add(split, BorderLayout.CENTER);
        }
        add(results, BorderLayout.CENTER);
        if (selection != null) {
            table.getSelectionModel().addListSelectionListener(selection);
        }
    }
    private JPanel searchHeader(ActionListener action) {
        JPanel search = new JPanel(new BorderLayout(0, 8));
        search.setName(manager ? "libraryManagementSearchHeader" : "librarySearchHeader");
        search.setOpaque(false);
        JPanel keywordRow = new JPanel(new BorderLayout(8, 0));
        keywordRow.setOpaque(false);
        keywordRow.add(new JLabel("关键词"), BorderLayout.WEST);
        keywordRow.setName(manager ? "libraryManagementKeywordRow" : "libraryKeywordRow");
        keyword.setName("librarySearchKeyword");
        keyword.setActionCommand("0");
        keyword.addActionListener(action);
        keywordRow.add(keyword, BorderLayout.CENTER);
        search.add(keywordRow, BorderLayout.NORTH);
        JPanel controls = row();
        field.setName("librarySearchField");
        controls.add(field);
        controls.add(button("查询图书", 0, action));
        search.add(controls, BorderLayout.SOUTH);
        return search;
    }
    private JPanel commands(boolean manager, JButton borrowButton,
            JButton reserveButton, ActionListener borrow, ActionListener reserve,
            ActionListener action) {
        JPanel commands = row();
        if (manager) {
            commands.add(button("录入新书", 1, action));
            commands.add(button("保存资料", 2, action));
            commands.add(button("下架所选", 3, action));
        }
        if (borrowButton != null) {
            borrowButton.addActionListener(borrow);
            reserveButton.addActionListener(reserve);
            commands.add(borrowButton);
            commands.add(reserveButton);
        }
        return commands;
    }
    private JPanel row() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        row.setOpaque(false);
        return row;
    }

    private JButton button(String text, int action, ActionListener listener) {
        JButton button = action == 0 ? UiFactory.primaryButton(text, "search")
                : UiFactory.secondaryButton(text, "library");
        button.setName((manager ? "manageAction" : "catalogAction") + action);
        button.setActionCommand(String.valueOf(action));
        button.addActionListener(listener);
        actions.add(button);
        return button;
    }

    private void configureTable() {
        UiFactory.styleTable(table);
        table.setName(manager ? "managementTable" : "catalogTable");
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        int[] widths = {150, 190, 150, 100, 95, 95, 80};
        TableColumnModel columns = table.getColumnModel();
        for (int index = 0; index < widths.length; index++) {
            columns.getColumn(index).setPreferredWidth(widths[index]);
        }
        LibraryViewBuilder.installCatalogMarqueeColumns(table);
    }

    BookQuery query(int pageNumber, int pageSize) {
        return new BookQuery(keyword.getText().trim(), QUERY_FIELDS[field.getSelectedIndex()],
                pageNumber, pageSize);
    }

    void show(PageResponse<Book> result) {
        books.clear();
        books.addAll(result.getItems());
        table.clearSelection();
        LibraryTableModels.showCatalog(model, books);
        if (editor != null) {
            editor.edit(null);
        }
        status.setText("共 " + result.getTotal() + " 种图书"
                + (editor == null ? " · 选择后可借阅或预约" : " · 选择后可编辑资料"));
    }

    Book selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : books.get(table.convertRowIndexToModel(row));
    }

    void clearSelection() {
        table.clearSelection();
    }

    void updateControls(boolean ready) {
        for (JButton button : actions) {
            int action = Integer.parseInt(button.getActionCommand());
            button.setEnabled(ready && (action != 2 || editor.isActive())
                    && (action != 3 || selected() != null));
        }
        if (editor != null) {
            editor.enableInputs(ready);
        }
        table.setEnabled(ready);
    }
}
