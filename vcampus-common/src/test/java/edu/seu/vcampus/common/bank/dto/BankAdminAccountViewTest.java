package edu.seu.vcampus.common.bank.dto;

import edu.seu.vcampus.common.bank.entity.BankAccountStatus;
import edu.seu.vcampus.common.user.entity.Role;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** BankAdminAccountView的联合视图映射与序列化测试。 */
class BankAdminAccountViewTest {

    /** 未开户时账户字段全为 null，opened 为 false。 */
    @Test
    void unopenedViewHasNullAccountFields() {
        BankAdminAccountView view =
                new BankAdminAccountView("zhao", "赵一", Role.STUDENT, true, null);
        assertFalse(view.isOpened());
        assertNull(view.getAccountId());
        assertNull(view.getBalance());
        assertNull(view.getStatus());
        assertNull(view.getCreatedAt());
        assertNull(view.getUpdatedAt());
    }

    /** 已开户时拷贝账户快照字段。 */
    @Test
    void openedViewCopiesAccountSnapshot() {
        BankAccountResponse account = new BankAccountResponse("A-1",
                new BigDecimal("12.34"), BankAccountStatus.FROZEN, new Date(1000L),
                new Date(2000L));
        BankAdminAccountView view =
                new BankAdminAccountView("zhao", "赵一", Role.STUDENT, false, account);
        assertTrue(view.isOpened());
        assertEquals("A-1", view.getAccountId());
        assertEquals(new BigDecimal("12.34"), view.getBalance());
        assertEquals(BankAccountStatus.FROZEN, view.getStatus());
        assertEquals(1000L, view.getCreatedAt().getTime());
        assertFalse(view.isUserEnabled());
    }

    /** 日期字段返回副本，外部修改不影响视图。 */
    @Test
    void returnsDefensiveDateCopies() {
        Date stamp = new Date(1000L);
        BankAdminAccountView view = new BankAdminAccountView("zhao", "赵一", Role.STUDENT,
                true, new BankAccountResponse("A-1", BigDecimal.ZERO,
                        BankAccountStatus.NORMAL, stamp, stamp));
        view.getCreatedAt().setTime(9999L);
        assertEquals(1000L, view.getCreatedAt().getTime());
    }

    /** 序列化往返保留视图字段。 */
    @Test
    void roundTripsView() throws IOException, ClassNotFoundException {
        BankAdminAccountView copy = BankDtoTestSupport.roundTrip(
                new BankAdminAccountView("zhao", "赵一", Role.TEACHER, true, null));
        assertEquals("zhao", copy.getUsername());
        assertEquals("赵一", copy.getDisplayName());
        assertEquals(Role.TEACHER, copy.getRole());
        assertTrue(copy.isUserEnabled());
        assertFalse(copy.isOpened());
    }
}
