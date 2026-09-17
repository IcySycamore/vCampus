package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.library.entity.Book;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

/** 管理员图书编辑表单；ISBN 在修改时固定，可借数量由服务器计算。 */
final class LibraryBookEditor extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JTextField isbn = field("catalogIsbn");
    private final JTextField title = field("catalogTitle");
    private final JTextField author = field("catalogAuthor");
    private final JTextField category = field("catalogCategory");
    private final JSpinner total = new JSpinner(new SpinnerNumberModel(1, 0, Integer.MAX_VALUE, 1));
    private final JLabel mode = new JLabel("请先从左侧选择一本图书");
    private boolean editing;
    private boolean active;

    LibraryBookEditor() {
        setLayout(new BorderLayout(0, 12));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        mode.setName("catalogEditorMode");
        mode.setForeground(UiTheme.MUTED);
        add(mode, BorderLayout.NORTH);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 10));
        form.setOpaque(false);
        form.add(new JLabel("ISBN"));
        form.add(isbn);
        form.add(new JLabel("书名"));
        form.add(title);
        form.add(new JLabel("作者"));
        form.add(author);
        form.add(new JLabel("分类"));
        form.add(category);
        form.add(new JLabel("馆藏总数"));
        total.setName("catalogTotal");
        form.add(total);
        add(form, BorderLayout.CENTER);
        edit(null);
    }

    void edit(Book book) {
        editing = book != null;
        active = editing;
        isbn.setText(editing ? book.getIsbn() : "");
        title.setText(editing ? book.getTitle() : "");
        author.setText(editing ? book.getAuthor() : "");
        category.setText(editing ? book.getCategory() : "");
        total.setValue(editing ? book.getTotalCopies() : 1);
        isbn.setEditable(false);
        mode.setText(editing ? "修改所选图书 · ISBN 不可更改" : "请先从左侧选择一本图书");
    }

    void startCreate() {
        edit(null);
        active = true;
        isbn.setEditable(true);
        mode.setText("录入新书 · 请填写完整资料后保存");
    }

    boolean isEditing() {
        return editing;
    }

    boolean isActive() {
        return active;
    }

    UiTasks.Task<Book> snapshot() {
        final String count = ((JSpinner.DefaultEditor) total.getEditor()).getTextField().getText();
        final String code = isbn.getText().trim();
        final String name = title.getText().trim();
        final String writer = author.getText().trim();
        final String kind = category.getText().trim();
        return new UiTasks.Task<Book>() {
            @Override
            public Book run() {
                int copies;
                try {
                    if (!count.trim().matches("[0-9]+")) {
                        throw new NumberFormatException();
                    }
                    copies = Integer.parseInt(count.trim());
                } catch (NumberFormatException exception) {
                    throw new ApiException(StatusCode.BAD_REQUEST, "馆藏总数必须是非负整数");
                }
                if (code.length() == 0 || name.length() == 0
                        || writer.length() == 0 || kind.length() == 0) {
                    throw new ApiException(StatusCode.BAD_REQUEST, "请填写 ISBN、书名、作者和分类");
                }
                return new Book(code, name, writer, kind, copies, 0);
            }
        };
    }

    void enableInputs(boolean enabled) {
        boolean usable = enabled && active;
        isbn.setEnabled(usable);
        title.setEnabled(usable);
        author.setEnabled(usable);
        category.setEnabled(usable);
        total.setEnabled(usable);
    }

    private JTextField field(String name) {
        JTextField field = new JTextField(16);
        field.setName(name);
        return field;
    }
}
