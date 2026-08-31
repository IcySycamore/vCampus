package edu.seu.vcampus.common.user.dto;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

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
        SaltRequest saltReq = new SaltRequest();
        saltReq.m_user_name = "001";
        saltReq.m_action = "LOGIN";

        SaltResponse saltResp = new SaltResponse();
        saltResp.m_salt = "abc123";

        LoginRequest loginReq = new LoginRequest();
        loginReq.m_user_name = "001";
        loginReq.m_role = "学生";
        loginReq.m_hashed = "e5e9fa1ba31ecd1ae84f75caaa474f3a";

        RegisterRequest regReq = new RegisterRequest();
        regReq.m_user_name = "002";
        regReq.m_role = "学生";
        regReq.m_hashed = "abc";
        Map<String, Object> extra = new HashMap<String, Object>();
        extra.put("学号", "61524D04");
        regReq.m_extra_info = extra;

        LoginResult result = new LoginResult();
        result.m_role = "学生";
        result.m_extra_info = extra;

        assertEquals("001", roundTrip(saltReq).m_user_name);
        assertEquals("abc123", roundTrip(saltResp).m_salt);
        assertEquals("学生", roundTrip(loginReq).m_role);
        assertEquals("002", roundTrip(regReq).m_user_name);
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