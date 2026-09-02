package edu.seu.vcampus.common.user.dto;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * DTO 序列化冒烟测试：验证各 DTO 可经对象流往返。
 */
class UserDtoSerializationTest {

    /**
     * 序列化后再反序列化，字段保持一致。
     *
     * @throws IOException            序列化失败
     * @throws ClassNotFoundException 反序列化失败
     */
    @Test
    void roundTrip() throws IOException, ClassNotFoundException {
        LoginRequest loginReq = new LoginRequest();
        loginReq.m_user_name = "001";
        loginReq.m_role = "学生";

        LoginChallenge challenge = new LoginChallenge();
        challenge.m_salt = "abc123";
        challenge.m_nonce = "nonce1";

        LoginVerify verify = new LoginVerify();
        verify.m_user_name = "001";
        verify.m_proof = "e5e9fa1ba31ecd1ae84f75caaa474f3a";

        RegisterRequest regReq = new RegisterRequest();
        regReq.m_user_name = "002";
        regReq.m_role = "教师";
        regReq.m_password = "secret";

        LoginResponse result = new LoginResponse();
        result.m_role = "学生";

        assertEquals("001", roundTrip(loginReq).m_user_name);
        assertEquals("nonce1", roundTrip(challenge).m_nonce);
        assertEquals("e5e9fa1ba31ecd1ae84f75caaa474f3a",
                roundTrip(verify).m_proof);
        assertEquals("secret", roundTrip(regReq).m_password);
        assertEquals("学生", roundTrip(result).m_role);
    }

    private <T> T roundTrip(T value) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bytes);
        out.writeObject(value);
        out.close();
        ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()));
        Object read = in.readObject();
        in.close();
        return (T) read;
    }
}