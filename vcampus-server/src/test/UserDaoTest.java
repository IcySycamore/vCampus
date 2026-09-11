package edu.seu.vcampus.server.shopmodule.user;

import edu.seu.vcampus.common.user.HumanInfo;
import edu.seu.vcampus.common.user.Student;
import edu.seu.vcampus.common.user.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserDaoTest {

    @Test
    void testFindByUserId() {
        UserDao userDao = new UserDaoImpl();
        User user = userDao.findByUserId("001");

        assertNotNull(user, "应该能找到账号为 001 的用户");
        assertEquals("001", user.getHumanInfo().getId());
    }

    @Test
    void testUserModelUsesNewCommonUser() {
        Student student = new Student(
                new HumanInfo("001", "演示学生", null, null, null, 20, HumanInfo.Gender.MALE),
                "001",
                "123456");

        assertEquals("001", student.getHumanInfo().getId());
        assertEquals("演示学生", student.getHumanInfo().getName());
        assertEquals("001", student.getUserName());
    }
}