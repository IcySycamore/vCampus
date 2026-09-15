package edu.seu.vcampus.common.library.entity;

import java.util.Date;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证图书馆账户归属、状态、额度和时间字段约束。 */
class LibraryAccountTest {
    @Test
    void createsOperationalAccountAndCopiesDates() {
        Date created = new Date(1000L);
        LibraryAccount account = new LibraryAccount("uuid-1", 30, created);
        created.setTime(2000L);

        assertEquals("uuid-1", account.getUserUuid());
        assertEquals(30, account.getBorrowLimit());
        assertEquals(1000L, account.getCreatedAt().getTime());
        assertEquals(LibraryAccountStatus.NORMAL, account.getStatus());
        assertTrue(account.isOperational());

        Date returned = account.getCreatedAt();
        returned.setTime(3000L);
        assertEquals(1000L, account.getCreatedAt().getTime());
    }

    @Test
    void suspendedOrDeletedAccountIsNotOperational() {
        LibraryAccount account = new LibraryAccount("uuid-1", 30, new Date());
        account.setStatus(LibraryAccountStatus.SUSPENDED);
        assertFalse(account.isOperational());
        account.setStatus(LibraryAccountStatus.NORMAL);
        account.markDeleted(new Date());
        assertFalse(account.isOperational());
    }

    @Test
    void rejectsInvalidLimitAndOwnerReplacement() {
        final LibraryAccount account = new LibraryAccount("uuid-1", 30, new Date());
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                account.setBorrowLimit(-1);
            }
        });
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                account.setBorrowLimit(31);
            }
        });
        assertThrows(IllegalStateException.class, new Executable() {
            @Override
            public void execute() {
                account.setUserUuid("uuid-2");
            }
        });
    }
}
