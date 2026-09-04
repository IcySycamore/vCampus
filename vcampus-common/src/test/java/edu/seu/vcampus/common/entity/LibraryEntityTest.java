package edu.seu.vcampus.common.entity;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 图书馆公共实体测试。
 */
class LibraryEntityTest {

    @Test
    void bookExposesCatalogAndStockData() {
        Book book = new Book("978-7", "Java", "Author", "计算机", 3, 2);

        assertEquals("978-7", book.getIsbn());
        assertEquals("Java", book.getTitle());
        assertEquals("Author", book.getAuthor());
        assertEquals("计算机", book.getCategory());
        assertEquals(3, book.getTotalCopies());
        assertEquals(2, book.getAvailableCopies());
    }

    @Test
    void borrowRecordProtectsMutableDates() {
        Date borrowedAt = new Date(1000L);
        BorrowRecord record = new BorrowRecord("001", "978-7", "Java",
                borrowedAt, new Date(2000L));
        borrowedAt.setTime(3000L);

        assertFalse(record.isReturned());
        assertNotSame(borrowedAt, record.getBorrowedAt());
        record.setReturnedAt(new Date());
        assertTrue(record.isReturned());
    }
}
