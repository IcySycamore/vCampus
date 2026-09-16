package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.library.entity.BookReservation;
import edu.seu.vcampus.common.library.entity.BorrowRecord;
import edu.seu.vcampus.common.library.entity.LibraryAccount;

/** 内存 DAO 使用的实体副本工具。 */
final class LibraryMemoryCopies {
    private LibraryMemoryCopies() {
    }

    static Book book(Book value) {
        if (value == null) {
            return null;
        }
        Book copy = new Book(value.getIsbn(), value.getTitle(), value.getAuthor(),
                value.getCategory(), value.getTotalCopies(), value.getAvailableCopies());
        copy.setWithdrawn(value.isWithdrawn());
        return copy;
    }

    static BorrowRecord borrow(BorrowRecord value) {
        if (value == null) {
            return null;
        }
        BorrowRecord copy = new BorrowRecord(value.getUserId(), value.getIsbn(),
                value.getBookTitle(), value.getBorrowedAt(), value.getDueAt());
        copy.setId(value.getId());
        copy.setReturnedAt(value.getReturnedAt());
        copy.setRenewalCount(value.getRenewalCount());
        copy.setFineAmount(value.getFineAmount());
        copy.setFinePaid(value.isFinePaid());
        copy.setFineTransactionId(value.getFineTransactionId());
        return copy;
    }

    static BookReservation reservation(BookReservation value) {
        if (value == null) {
            return null;
        }
        BookReservation copy = new BookReservation(value.getUserId(), value.getIsbn(),
                value.getBookTitle(), value.getRequestedAt());
        copy.setId(value.getId());
        copy.setReadyAt(value.getReadyAt());
        copy.setExpiresAt(value.getExpiresAt());
        copy.setStatus(value.getStatus());
        return copy;
    }

    static LibraryAccount account(LibraryAccount value) {
        if (value == null) {
            return null;
        }
        LibraryAccount copy = new LibraryAccount(value.getUserUuid(),
                value.getBorrowLimit(), value.getCreatedAt());
        copy.setId(value.getId());
        copy.setStatus(value.getStatus());
        copy.setUpdatedAt(value.getUpdatedAt());
        if (value.isDeleted()) {
            copy.markDeleted(value.getUpdatedAt());
        }
        return copy;
    }
}
