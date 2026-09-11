package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.common.entity.Book;
import edu.seu.vcampus.common.entity.BorrowRecord;

import java.text.SimpleDateFormat;
import java.util.List;
import javax.swing.table.DefaultTableModel;

/**
 * 图书馆表格模型创建与响应数据渲染工具。
 */
final class LibraryTableModels {

    private LibraryTableModels() {
    }

    static DefaultTableModel create(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    static int showBooks(DefaultTableModel model, Object data) {
        List<?> values = (List<?>) data;
        model.setRowCount(0);
        for (Object value : values) {
            Book book = (Book) value;
            model.addRow(new Object[] {book.getIsbn(), book.getTitle(), book.getAuthor(),
                    book.getCategory(), book.getAvailableCopies()});
        }
        return values.size();
    }

    static void showBorrows(DefaultTableModel model, Object data) {
        List<?> values = (List<?>) data;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        model.setRowCount(0);
        for (Object value : values) {
            BorrowRecord record = (BorrowRecord) value;
            model.addRow(new Object[] {record.getId(), record.getBookTitle(),
                    format.format(record.getBorrowedAt()), format.format(record.getDueAt()),
                    record.isReturned() ? "已归还" : "借阅中"});
        }
    }
}
