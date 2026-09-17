package edu.seu.vcampus.common.bank.dto;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** BankAdminResetPasswordRequest的摘要长度校验、防御性拷贝与序列化测试。 */
class BankAdminResetPasswordRequestTest {

    /** 盐或摘要长度不符时拒绝。 */
    @Test
    void rejectsInvalidSaltOrHashLength() {
        assertInvalid(new byte[15], new byte[32]);
        assertInvalid(new byte[16], new byte[31]);
        assertInvalid(null, new byte[32]);
    }

    private static void assertInvalid(final byte[] salt, final byte[] hash) {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                new BankAdminResetPasswordRequest("zhao", salt, hash);
            }
        });
    }

    /** 构造后修改入参数组不影响请求。 */
    @Test
    void copiesSaltAndHash() {
        byte[] salt = new byte[16];
        byte[] hash = new byte[32];
        BankAdminResetPasswordRequest request =
                new BankAdminResetPasswordRequest("zhao", salt, hash);
        salt[0] = 1;
        hash[0] = 1;
        assertEquals(0, request.getSalt()[0]);
        assertEquals(0, request.getHash()[0]);
    }

    /** 序列化往返保留用户名与摘要长度。 */
    @Test
    void roundTripsCredentials() throws IOException, ClassNotFoundException {
        BankAdminResetPasswordRequest copy = BankDtoTestSupport.roundTrip(
                new BankAdminResetPasswordRequest("zhao", new byte[16], new byte[32]));
        assertEquals("zhao", copy.getUsername());
        assertEquals(16, copy.getSalt().length);
        assertEquals(32, copy.getHash().length);
    }
}
