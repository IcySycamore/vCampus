package edu.seu.vcampus.common.bank.dto;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** BankAdminSetFrozenRequest的入参校验与序列化测试。 */
class BankAdminSetFrozenRequestTest {

    /** 空用户名被拒绝。 */
    @Test
    void rejectsBlankUsername() {
        assertInvalid(null);
        assertInvalid(" ");
    }

    private static void assertInvalid(final String username) {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                new BankAdminSetFrozenRequest(username, true);
            }
        });
    }

    /** 序列化往返保留用户名与冻结位。 */
    @Test
    void roundTripsFlag() throws IOException, ClassNotFoundException {
        BankAdminSetFrozenRequest copy =
                BankDtoTestSupport.roundTrip(new BankAdminSetFrozenRequest("zhao", true));
        assertEquals("zhao", copy.getUsername());
        assertTrue(copy.isFrozen());
    }
}
