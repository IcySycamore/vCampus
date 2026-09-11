package edu.seu.vcampus.common.bank.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** BankTransactionType 的显示名与解析测试。 */
class BankTransactionTypeTest {

    @Test
    void mapsEveryTypeToAndFromDisplayName() {
        for (BankTransactionType type : BankTransactionType.values()) {
            assertEquals(type,
                    BankTransactionType.fromDisplayName(type.getDisplayName()));
        }
    }

    @Test
    void unknownDisplayNameReturnsNull() {
        assertNull(BankTransactionType.fromDisplayName("未知"));
        assertNull(BankTransactionType.fromDisplayName(null));
    }
}
