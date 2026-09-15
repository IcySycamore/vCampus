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
}
