package edu.seu.vcampus.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * User 实体类测试类.
 */
public class UserTest {

    @Test
    public void testUserGetterSetter() {
        User user = new User();
        user.setuId("123");
        user.setuName("Test");
        user.setSalt("salt123");

        assertEquals("123", user.getuId());
        assertEquals("Test", user.getuName());
        assertEquals("salt123", user.getSalt());
    }
}