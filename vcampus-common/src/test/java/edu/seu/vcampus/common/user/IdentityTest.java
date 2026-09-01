package edu.seu.vcampus.common.user;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 身份类（Student/Teacher/Admin）与枚举的序列化冒烟测试。
 */
class IdentityTest {

    /**
     * 各身份可经对象流往返且保持字段。
     *
     * @throws IOException            序列化失败
     * @throws ClassNotFoundException 反序列化失败
     */
    @Test
    void roundTrip() throws IOException, ClassNotFoundException {
        Student student = new Student();
        student.setUserName("001");
        student.setHumanInfo(buildInfo());

        Teacher teacher = new Teacher();
        teacher.setUserName("002");
        teacher.setWorkId("T001");
        teacher.setTitle(Title.PROFESSOR);
        teacher.setHumanInfo(buildInfo());

        Admin admin = new Admin();
        admin.setUserName("003");
        admin.setSuperAdmin(true);
        admin.setHumanInfo(buildInfo());

        assertEquals("001", roundTrip(student).getUserName());
        assertEquals("T001", roundTrip(teacher).getWorkId());
        assertEquals(Title.PROFESSOR, roundTrip(teacher).getTitle());
        assertTrue(roundTrip(admin).isSuperAdmin());
        assertNotNull(roundTrip(student).toExtraInfo());
        assertNotNull(roundTrip(teacher).toExtraInfo());
    }

    private HumanInfo buildInfo() {
        HumanInfo info = new HumanInfo();
        info.setName("测试");
        info.setDepartment(Department.COMPUTER_SCIENCE);
        info.setMajor(Major.SOFTWARE_ENGINEERING);
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