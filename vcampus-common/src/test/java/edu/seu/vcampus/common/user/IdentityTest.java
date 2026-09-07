package edu.seu.vcampus.common.user;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 用户账户与档案的序列化冒烟测试。
 */
class IdentityTest {

    /**
     * 用户与档案可经对象流往返且保持字段。
     *
     * @throws IOException            序列化失败
     * @throws ClassNotFoundException 反序列化失败
     */
    @Test
    void roundTrip() throws IOException, ClassNotFoundException {
        User user = new User("001", "secret", Role.STUDENT);
        HumanInfo info = buildInfo();

        assertEquals("001", roundTrip(user).getUserName());
        assertEquals(Role.STUDENT, roundTrip(user).getRole());
        assertEquals("测试", roundTrip(info).getName());
        assertNotNull(roundTrip(info).getUuid());
    }

    private HumanInfo buildInfo() {
        HumanInfo info = new HumanInfo();
        info.setName("测试");
        info.setDepartment(Department.COMPUTER_SCIENCE);
        info.setMajor(Major.SOFTWARE_ENGINEERING);
        info.setTitle(Title.PROFESSOR);
        return info;
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