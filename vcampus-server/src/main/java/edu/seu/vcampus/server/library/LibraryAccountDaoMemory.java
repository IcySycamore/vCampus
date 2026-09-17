package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.library.entity.LibraryAccount;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** 图书馆账户 DAO 的内存占位实现。 */
public final class LibraryAccountDaoMemory implements LibraryAccountDao {
    private final Map<String, LibraryAccount> m_accounts =
            new HashMap<String, LibraryAccount>();
    private final AtomicLong m_next_id = new AtomicLong(1L);

    @Override
    public synchronized LibraryAccount findByUserUuid(String userUuid) {
        return LibraryMemoryCopies.account(m_accounts.get(userUuid));
    }

    @Override
    public synchronized boolean insert(LibraryAccount account) {
        if (account == null || account.getUserUuid() == null
                || m_accounts.containsKey(account.getUserUuid())) {
            return false;
        }
        if (account.getId() == null) {
            account.setId(m_next_id.getAndIncrement());
        }
        m_accounts.put(account.getUserUuid(), LibraryMemoryCopies.account(account));
        return true;
    }

    @Override
    public synchronized boolean update(LibraryAccount account) {
        if (account == null || account.getUserUuid() == null
                || !m_accounts.containsKey(account.getUserUuid())) {
            return false;
        }
        m_accounts.put(account.getUserUuid(), LibraryMemoryCopies.account(account));
        return true;
    }

    @Override
    public synchronized boolean softDelete(String userUuid, Date updatedAt) {
        LibraryAccount account = m_accounts.get(userUuid);
        if (account == null) {
            return false;
        }
        if (!account.isDeleted()) {
            account.markDeleted(updatedAt);
        }
        return true;
    }
}
