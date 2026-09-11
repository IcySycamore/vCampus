package edu.seu.vcampus.dao;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * UserDao 接口测试类.
 */
public class UserDaoTest {

    @Test
    public void testUserDaoInterface() {
        UserDao userDao = new UserDaoImpl();
        assertNull(userDao.findByUsername("non_existing_user_id_12345"));
    }
}