package edu.seu.vcampus.common.bank.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 账户只允许绑定一次有效的稳定用户主键。 */
class BankAccountOwnershipTest {
    @Test
    void ownerCannotBeReassigned() {
        final BankAccount account = new BankAccount();
        account.setUserId(123L);
        account.setUserId(123L);
        assertThrows(IllegalStateException.class, new Executable() {
            @Override
            public void execute() {
                account.setUserId(456L);
            }
        });
        assertEquals(Long.valueOf(123L), account.getUserId());
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void rejectsInvalidOwnerId(final long userId) {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                new BankAccount().setUserId(userId);
            }
        });
    }
}
