package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.library.LibraryService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.library.entity.Book;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

/** 馆藏维护页面，通过模块 API 查询、保存和下架。 */
final class LibraryCatalogPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private final DefaultTableModel model = LibraryTableModels.create(new String[] {
            "ISBN", "书名", "作者", "分类", "馆藏总数", "可借数量", "状态"});
    private final JTable table = new JTable(model);
    private final List<Book> books = new ArrayList<Book>();
    private final LibraryBookEditor editor = new LibraryBookEditor();
    private final JTextField keyword = new JTextField(12);
    private final JLabel status = new JLabel("选择图书修改，或点击录入新书");
    private final List<JButton> buttons = new ArrayList<JButton>();
    private final LibraryService api;
    private final Runnable onChanged;
    private boolean busy;

    LibraryCatalogPanel(LibraryService api, Runnable onChanged) {
        this.api = api;
        this.onChanged = onChanged;
        setName("libraryCatalog");
        setLayout(new BorderLayout(12, 12));
        setBackground(UiTheme.BACKGROUND);
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setOpaque(false);
        toolbar.add(new JLabel("关键词"));
        toolbar.add(keyword);
        toolbar.add(button("查询馆藏", 0));
        toolbar.add(button("录入新书", 1));
        toolbar.add(button("保存资料", 2));
        toolbar.add(button("下架所选", 3));
        add(toolbar, BorderLayout.NORTH);
        UiFactory.styleTable(table);
        table.setName("catalogTable");
        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(UiTheme.BACKGROUND);
        add(scroll, BorderLayout.CENTER);
        JPanel side = new JPanel(new BorderLayout());
        side.setOpaque(false);
        side.add(editor, BorderLayout.NORTH);
        add(side, BorderLayout.EAST);
        add(status, BorderLayout.SOUTH);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent event) {
                int row = table.getSelectedRow();
                if (!event.getValueIsAdjusting() && !busy && row >= 0) {
                    editor.edit(books.get(table.convertRowIndexToModel(row)));
                }
            }
        });
        updateControls();
    }

    void refresh() {
        if (busy || !api.canManageCatalog()) {
            return;
        }
        final String text = keyword.getText().trim();
        busy = true;
        updateControls();
        UiTasks.run(new UiTasks.Task<List<Book>>() {
            @Override
            public List<Book> run() {
                return api.searchCatalog(text);
            }
        }, new UiTasks.Success<List<Book>>() {
            @Override
            public void accept(List<Book> result) {
                books.clear();
                table.clearSelection();
                model.setRowCount(0);
                books.addAll(result);
                for (Book book : books) {
                    model.addRow(new Object[] {book.getIsbn(), book.getTitle(), book.getAuthor(),
                            book.getCategory(), book.getTotalCopies(), book.getAvailableCopies(),
                            book.isWithdrawn() ? "已下架" : "在馆"});
                }
                editor.edit(null);
                busy = false;
                status.setText("共 " + books.size() + " 种图书，包含已下架馆藏");
                updateControls();
            }
        }, failure());
    }

    private JButton button(String text, final int action) {
        JButton button = UiFactory.secondaryButton(text, action == 0 ? "search" : "library");
        button.setName("catalogAction" + action);
        buttons.add(button);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                perform(action);
            }
        });
        return button;
    }

    private void perform(final int action) {
        if (busy || !api.canManageCatalog()) {
            return;
        }
        if (action == 0) {
            refresh();
        } else if (action == 1) {
            table.clearSelection();
            editor.edit(null);
        } else if (action == 2) {
            final boolean editing = editor.isEditing();
            final UiTasks.Task<Book> value = editor.snapshot();
            submit(new UiTasks.Task<Book>() {
                @Override
                public Book run() {
                    Book book = value.run();
                    return editing ? api.updateBook(book) : api.createBook(book);
                }
            });
        } else if (table.getSelectedRow() >= 0 && JOptionPane.showConfirmDialog(this,
                "下架后停止借出，已借图书仍可归还。", "确认下架",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            final String isbn = books.get(
                    table.convertRowIndexToModel(table.getSelectedRow())).getIsbn();
            submit(new UiTasks.Task<Book>() {
                @Override
                public Book run() {
                    return api.withdrawBook(isbn);
                }
            });
        }
    }

    private void submit(UiTasks.Task<Book> task) {
        busy = true;
        updateControls();
        UiTasks.run(task, new UiTasks.Success<Book>() {
            @Override
            public void accept(Book book) {
                busy = false;
                refresh();
                onChanged.run();
            }
        }, failure());
    }

    private UiTasks.Failure failure() {
        return new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                busy = false;
                status.setText(error.getMessage());
                updateControls();
            }
        };
    }

    private void updateControls() {
        boolean enabled = api.canManageCatalog() && !busy;
        for (JButton button : buttons) {
            button.setEnabled(enabled);
        }
        editor.enableInputs(enabled);
        table.setEnabled(enabled);
    }
}
