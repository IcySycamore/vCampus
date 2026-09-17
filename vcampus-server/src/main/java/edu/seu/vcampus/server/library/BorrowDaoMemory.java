package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.library.entity.BorrowRecord;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** 借阅记录 DAO 的内存占位实现。 */
public final class BorrowDaoMemory implements BorrowDao {
    private final Map<Long, BorrowRecord> m_records =
            new LinkedHashMap<Long, BorrowRecord>();
    private final AtomicLong m_next_id = new AtomicLong(1L);

    @Override
    public synchronized List<BorrowRecord> findByUser(String userId) {
        List<BorrowRecord> found = new ArrayList<BorrowRecord>();
        for (BorrowRecord record : m_records.values()) {
            if (same(userId, record.getUserId())) {
                found.add(LibraryMemoryCopies.borrow(record));
            }
        }
        Collections.sort(found, new Comparator<BorrowRecord>() {
            @Override
            public int compare(BorrowRecord left, BorrowRecord right) {
                long leftTime = left.getBorrowedAt() == null
                        ? 0L : left.getBorrowedAt().getTime();
                long rightTime = right.getBorrowedAt() == null
                        ? 0L : right.getBorrowedAt().getTime();
                return leftTime == rightTime ? 0 : leftTime < rightTime ? 1 : -1;
            }
        });
        return found;
    }

    @Override
    public synchronized boolean hasActive(Connection connection, String userId, String isbn) {
        for (BorrowRecord record : m_records.values()) {
            if (!record.isReturned() && same(userId, record.getUserId())
                    && same(isbn, record.getIsbn())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized long insert(Connection connection, BorrowRecord record)
            throws SQLException {
        if (record == null || hasActive(connection, record.getUserId(), record.getIsbn())) {
            throw new SQLException("duplicate or invalid active borrow");
        }
        long id = m_next_id.getAndIncrement();
        record.setId(id);
        m_records.put(id, LibraryMemoryCopies.borrow(record));
        return id;
    }

    @Override
    public synchronized BorrowRecord findActiveById(Connection connection, long id) {
        BorrowRecord record = m_records.get(id);
        return record == null || record.isReturned()
                ? null : LibraryMemoryCopies.borrow(record);
    }

    @Override
    public synchronized BorrowRecord findById(Connection connection, long id) {
        return LibraryMemoryCopies.borrow(m_records.get(id));
    }

    @Override
    public synchronized boolean markReturned(Connection connection, long id,
            Timestamp returnedAt, BigDecimal fineAmount, boolean finePaid) {
        BorrowRecord record = m_records.get(id);
        if (record == null || record.isReturned()) {
            return false;
        }
        record.setReturnedAt(returnedAt);
        record.setFineAmount(fineAmount);
        record.setFinePaid(finePaid);
        return true;
    }

    @Override
    public synchronized boolean renew(Connection connection, long id, Timestamp dueAt,
            int renewalCount) {
        BorrowRecord record = m_records.get(id);
        if (record == null || record.isReturned()) {
            return false;
        }
        record.setDueAt(dueAt);
        record.setRenewalCount(renewalCount);
        return true;
    }

    @Override
    public synchronized boolean markFinePaid(Connection connection, long id,
            String transactionId) {
        BorrowRecord record = m_records.get(id);
        if (record == null || record.isFinePaid()) {
            return false;
        }
        record.setFinePaid(true);
        record.setFineTransactionId(transactionId);
        return true;
    }

    private boolean same(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }
}
