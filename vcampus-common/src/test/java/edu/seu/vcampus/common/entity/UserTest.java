package edu.seu.vcampus.common.entity;

import edu.seu.vcampus.common.user.HumanInfo;
import edu.seu.vcampus.common.user.Student;
import edu.seu.vcampus.common.user.User;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 兼容测试：验证新统一用户模型可按现有序列化契约工作。
 */
class UserTest {

    @Test
    void constructorSetsAllFields() {
        HumanInfo humanInfo = new HumanInfo("001", "演示学生", null, null, null, 20, HumanInfo.Gender.MALE);
        User user = new Student(humanInfo, "001", "1");

        assertEquals("001", user.getHumanInfo().getId());
        assertEquals("演示学生", user.getHumanInfo().getName());
        assertEquals("001", user.getUserName());
    }

    @Test
    void settersRoundTrip() {
        Student student = new Student();
        HumanInfo info = new HumanInfo("002", "演示教师", null, null, null, 35, HumanInfo.Gender.FEMALE);
        student.setHumanInfo(info);
        student.setUserName("teacher");
        student.setPassword("pwd");

        assertEquals("002", student.getHumanInfo().getId());
        assertEquals("演示教师", student.getHumanInfo().getName());
        assertEquals("teacher", student.getUserName());
        assertEquals("pwd", student.getPassword());
    }

    @Test
    void serializationRoundTrip() throws Exception {
        HumanInfo info = new HumanInfo("003", "管理员", null, null, null, 30, HumanInfo.Gender.MALE);
        User original = new Student(info, "003", "1");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(original);
        oos.flush();

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        User copy = (User) ois.readObject();

        assertEquals(original.getHumanInfo().getId(), copy.getHumanInfo().getId());
        assertEquals(original.getHumanInfo().getName(), copy.getHumanInfo().getName());
        assertEquals(original.getUserName(), copy.getUserName());
    }
}
