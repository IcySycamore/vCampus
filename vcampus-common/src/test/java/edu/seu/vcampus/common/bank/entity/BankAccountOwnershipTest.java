package edu.seu.vcampus.common.bank.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 账户只允许绑定一次有效的稳定用户主键。 */
class BankAccountOwnershipTest {
    private static final String OWNER_UUID = "7f4c2a10-94ad-4b42-8cae-51fd93e6a001";
    private static final String OTHER_UUID = "7f4c2a10-94ad-4b42-8cae-51fd93e6a002";
    @Test
    void ownerCannotBeReassigned() {
        final BankAccount account = new BankAccount();
        account.setOwnerUuid(OWNER_UUID);
        account.setOwnerUuid(new String(OWNER_UUID));
        assertThrows(IllegalStateException.class, new Executable() {
            @Override
            public void execute() {
                account.setOwnerUuid(OTHER_UUID);
            }
        });
        assertEquals(OWNER_UUID, account.getOwnerUuid());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t\n"})
    void rejectsBlankOwnerUuid(final String ownerUuid) {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                new BankAccount().setOwnerUuid(ownerUuid);
            }
        });
    }
}
