package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.library.dto.BookQuery;
import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.message.PageResponse;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 馆藏 DAO 的内存占位实现。 */
public final class BookDaoMemory implements BookDao {
    private final Map<String, Book> m_books = new LinkedHashMap<String, Book>();

    /**
     * 创建带演示馆藏的内存书库，供正式演示入口使用。
     * @return 已录入示例图书的书库
     */
    public static BookDaoMemory withSampleBooks() {
        BookDaoMemory books = new BookDaoMemory();
        books.addSample("9787111213826", "Java编程思想", "Bruce Eckel", "计算机", 8);
        books.addSample("9787111547426", "Effective Java", "Joshua Bloch", "计算机", 6);
        books.addSample("9787302423287", "Java语言程序设计", "梁勇", "计算机", 10);
        books.addSample("9787111641247", "深入理解Java虚拟机", "周志明", "计算机", 7);
        books.addSample("9787111612728", "算法（第4版）", "Robert Sedgewick", "计算机", 5);
        books.addSample("9787111407010", "代码整洁之道", "Robert C. Martin", "软件工程", 4);
        books.addSample("9787111558422", "数据库系统概念", "Abraham Silberschatz", "数据库", 6);
        return books;
    }

    @Override
    public synchronized PageResponse<Book> search(BookQuery query) {
        return page(query, false);
    }

    @Override
    public synchronized Book findByIsbn(Connection connection, String isbn) {
        return LibraryMemoryCopies.book(m_books.get(isbn));
    }

    @Override
    public synchronized boolean adjustAvailable(Connection connection, String isbn,
            int change) {
        Book book = m_books.get(isbn);
        if (book == null || change < 0 && book.isWithdrawn()) {
            return false;
        }
        int available = book.getAvailableCopies() + change;
        if (available < 0 || available > book.getTotalCopies()) {
            return false;
        }
        book.setAvailableCopies(available);
        return true;
    }

    @Override
    public synchronized PageResponse<Book> searchCatalog(BookQuery query) {
        return page(query, true);
    }

    @Override
    public synchronized boolean insertBook(Connection connection, Book book) {
        if (!valid(book) || m_books.containsKey(book.getIsbn())) {
            return false;
        }
        m_books.put(book.getIsbn(), LibraryMemoryCopies.book(book));
        return true;
    }

    @Override
    public synchronized boolean updateBook(Connection connection, Book book) {
        if (!valid(book) || !m_books.containsKey(book.getIsbn())) {
            return false;
        }
        m_books.put(book.getIsbn(), LibraryMemoryCopies.book(book));
        return true;
    }

    @Override
    public synchronized boolean withdrawBook(Connection connection, String isbn) {
        Book book = m_books.get(isbn);
        if (book == null) {
            return false;
        }
        book.setWithdrawn(true);
        return true;
    }

    private PageResponse<Book> page(BookQuery query, boolean includeWithdrawn) {
        BookQuery actual = query == null ? new BookQuery() : query;
        List<Book> matched = new ArrayList<Book>();
        for (Book book : m_books.values()) {
            if ((includeWithdrawn || !book.isWithdrawn()) && matches(book, actual)) {
                matched.add(LibraryMemoryCopies.book(book));
            }
        }
        Collections.sort(matched, new Comparator<Book>() {
            @Override
            public int compare(Book left, Book right) {
                return text(left.getTitle()).compareTo(text(right.getTitle()));
            }
        });
        int offset = PageResponse.offsetOf(actual.getPageNumber(), actual.getPageSize());
        int end = Math.min(offset + actual.getPageSize(), matched.size());
        List<Book> items = offset >= matched.size()
                ? Collections.<Book>emptyList() : matched.subList(offset, end);
        return new PageResponse<Book>(items, matched.size(), actual.getPageNumber(),
                actual.getPageSize());
    }

    private boolean matches(Book book, BookQuery query) {
        String keyword = text(query.getKeyword()).trim();
        if (keyword.length() == 0) {
            return true;
        }
        String field = text(query.getField());
        if ("title".equals(field)) {
            return contains(book.getTitle(), keyword);
        }
        if ("author".equals(field)) {
            return contains(book.getAuthor(), keyword);
        }
        if ("isbn".equals(field)) {
            return contains(book.getIsbn(), keyword);
        }
        return contains(book.getTitle(), keyword) || contains(book.getAuthor(), keyword)
                || contains(book.getIsbn(), keyword);
    }

    private boolean contains(String value, String keyword) {
        return text(value).contains(text(keyword));
    }

    private String text(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private boolean valid(Book book) {
        return book != null && book.getIsbn() != null
                && book.getAvailableCopies() >= 0
                && book.getAvailableCopies() <= book.getTotalCopies();
    }

    private void addSample(String isbn, String title, String author,
            String category, int copies) {
        insertBook(null, new Book(isbn, title, author, category, copies, copies));
    }
}
