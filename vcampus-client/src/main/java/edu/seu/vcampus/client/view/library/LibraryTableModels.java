package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.library.entity.BorrowRecord;
import edu.seu.vcampus.common.library.entity.BookReservation;
import edu.seu.vcampus.common.library.entity.ReservationStatus;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Date;
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

            @Override
            public Class<?> getColumnClass(int column) {
                for (int row = 0; row < getRowCount(); row++) {
                    Object value = getValueAt(row, column);
                    if (value != null) {
                        return value.getClass();
                    }
                }
                return Object.class;
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

    static void showCatalog(DefaultTableModel model, List<Book> books) {
        model.setRowCount(0);
        for (Book book : books) {
            model.addRow(new Object[] {book.getIsbn(), book.getTitle(), book.getAuthor(),
                    book.getCategory(), book.getTotalCopies(), book.getAvailableCopies(),
                    book.isWithdrawn() ? "已下架" : "在馆"});
        }
    }

    static void showBorrows(DefaultTableModel model, Object data) {
        List<?> values = (List<?>) data;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        model.setRowCount(0);
        for (Object value : values) {
            BorrowRecord record = (BorrowRecord) value;
            if (record.isReturned() && !record.hasUnpaidFine()) {
                continue;
            }
            model.addRow(new Object[] {record.getId(), record.getBookTitle(),
                    format.format(record.getBorrowedAt()), format.format(record.getDueAt()),
                    record.getRenewalCount(), record.getFineAmount(), borrowStatus(record)});
        }
    }

    static void showReservations(DefaultTableModel model, List<BookReservation> records) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        model.setRowCount(0);
        for (BookReservation record : records) {
            model.addRow(new Object[] {record.getId(), record.getBookTitle(), record.getIsbn(),
                    date(format, record.getRequestedAt()), date(format, record.getExpiresAt()),
                    reservationStatus(record.getStatus())});
        }
    }

    private static String borrowStatus(BorrowRecord record) {
        if (record.hasUnpaidFine()) {
            return "待缴费";
        }
        if (record.isReturned()) {
            return "已归还";
        }
        return record.getDueAt() != null && new Date().after(record.getDueAt())
                ? "已逾期" : "借阅中";
    }

    private static String reservationStatus(ReservationStatus status) {
        if (status == ReservationStatus.WAITING) {
            return "排队中";
        }
        if (status == ReservationStatus.READY) {
            return "已到馆，请在保留期内借阅";
        }
        if (status == ReservationStatus.FULFILLED) {
            return "已借阅";
        }
        if (status == ReservationStatus.CANCELLED) {
            return "已取消";
        }
        return "已过期";
    }

    private static String date(SimpleDateFormat format, Date date) {
        return date == null ? "—" : format.format(date);
    }
}
