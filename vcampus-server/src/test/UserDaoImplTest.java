package edu.seu.vcampus.dao;

import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * UserDaoImpl 实现类测试类.
 */
public class UserDaoImplTest {

    @Test
    public void testFindByUsername() {
        UserDaoImpl userDao = new UserDaoImpl();
        assertNull(userDao.findByUsername("test_user_not_exist"));
    }
}