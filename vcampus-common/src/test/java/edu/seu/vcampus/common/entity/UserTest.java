package edu.seu.vcampus.common.entity;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * User 实体测试：字段读写与跨模块序列化一致性（见 ADR-0006）。
 */
class UserTest {

    /**
     * 全参构造后各字段应可正确读出。
     */
    @Test
    void constructorSetsAllFields() {
        User user = new User("001", "演示学生", 20, "男", "1", "学生");

        assertEquals("001", user.getuId());
        assertEquals("演示学生", user.getuName());
        assertEquals(Integer.valueOf(20), user.getuAge());
        assertEquals("男", user.getuSex());
        assertEquals("1", user.getuPwd());
        assertEquals("学生", user.getuRole());
    }

    /**
     * setter 写入的值应可由 getter 读回。
     */
    @Test
    void settersRoundTrip() {
        User user = new User();
        user.setuId("002");
        user.setuName("演示教师");
        user.setuAge(35);
        user.setuSex("女");
        user.setuPwd("pwd");
        user.setuRole("教师");

        assertEquals("002", user.getuId());
        assertEquals("演示教师", user.getuName());
        assertEquals(Integer.valueOf(35), user.getuAge());
        assertEquals("女", user.getuSex());
        assertEquals("pwd", user.getuPwd());
        assertEquals("教师", user.getuRole());
    }

    /**
     * 序列化再反序列化后，字段应与原对象一致（登录响应需经对象流传输）。
     *
     * @throws Exception 序列化/IO 异常
     */
    @Test
    void serializationRoundTrip() throws Exception {
        User original = new User("003", "管理员", 30, "男", "1", "管理员");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(original);
        oos.flush();

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        User copy = (User) ois.readObject();

        assertEquals(original.getuId(), copy.getuId());
        assertEquals(original.getuName(), copy.getuName());
        assertEquals(original.getuAge(), copy.getuAge());
        assertEquals(original.getuSex(), copy.getuSex());
        assertEquals(original.getuPwd(), copy.getuPwd());
        assertEquals(original.getuRole(), copy.getuRole());
    }
}
