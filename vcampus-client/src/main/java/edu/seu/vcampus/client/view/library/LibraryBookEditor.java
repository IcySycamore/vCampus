package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.library.entity.Book;
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
    private boolean editing;

    LibraryBookEditor() {
        setLayout(new GridLayout(0, 2, 8, 10));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        add(new JLabel("ISBN"));
        add(isbn);
        add(new JLabel("书名"));
        add(title);
        add(new JLabel("作者"));
        add(author);
        add(new JLabel("分类"));
        add(category);
        add(new JLabel("馆藏总数"));
        total.setName("catalogTotal");
        add(total);
    }

    void edit(Book book) {
        editing = book != null;
        isbn.setText(editing ? book.getIsbn() : "");
        title.setText(editing ? book.getTitle() : "");
        author.setText(editing ? book.getAuthor() : "");
        category.setText(editing ? book.getCategory() : "");
        total.setValue(editing ? book.getTotalCopies() : 1);
        isbn.setEditable(!editing);
    }

    boolean isEditing() {
        return editing;
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
        isbn.setEnabled(enabled);
        title.setEnabled(enabled);
        author.setEnabled(enabled);
        category.setEnabled(enabled);
        total.setEnabled(enabled);
    }

    private JTextField field(String name) {
        JTextField field = new JTextField(16);
        field.setName(name);
        return field;
    }
}
