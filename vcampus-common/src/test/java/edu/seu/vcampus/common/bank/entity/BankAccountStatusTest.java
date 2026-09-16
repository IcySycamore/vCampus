package edu.seu.vcampus.common.bank.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** BankAccountStatus 的显示名与解析测试。 */
class BankAccountStatusTest {

    @Test
    void mapsEveryStatusToAndFromDisplayName() {
        for (BankAccountStatus status : BankAccountStatus.values()) {
            assertEquals(status,
                    BankAccountStatus.fromDisplayName(status.getDisplayName()));
        }
    }

    @Test
    void unknownDisplayNameReturnsNull() {
        assertNull(BankAccountStatus.fromDisplayName("未知"));
        assertNull(BankAccountStatus.fromDisplayName(null));
    }
}
