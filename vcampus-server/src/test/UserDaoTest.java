package edu.seu.vcampus.dao;

import edu.seu.vcampus.model.User;
import org.junit.Test;
// 关键部分：必须包含下面这行静态导入
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

public class UserDaoTest {

    @Test
    public void testFindByUsername() {
        UserDao userDao = new UserDaoImpl();
        User user = userDao.findByUsername("001");
        assertNotNull("应该能找到账号为 001 的用户", user);
        assertEquals("001", user.getuId());
    }
}