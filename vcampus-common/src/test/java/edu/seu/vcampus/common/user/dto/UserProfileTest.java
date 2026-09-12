package edu.seu.vcampus.common.user.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 个人档案测试：字段读写契约。
 *
 * <p>
 * 姓名由服务端保证非空（未采集时用登录名顶上），所以这里不再测「回退」——那是服务端的责任，
 * 在客户端重复测一遍只会让人误以为界面也需要处理空值。
 */
class UserProfileTest {

    /**
     * 四个字段随构造赋值。
     */
    @Test
    void constructorFillsAllFields() {
        UserProfile profile = new UserProfile("uuid-1", "001", "张三", "学生");

        assertEquals("uuid-1", profile.getUuid());
        assertEquals("001", profile.getUserName());
        assertEquals("张三", profile.getRealName());
        assertEquals("学生", profile.getRole());
    }

    /**
     * 改登录名不影响姓名（姓名是独立字段，不是从登录名派生的）。
     */
    @Test
    void realNameIsIndependentFromUserName() {
        UserProfile profile = new UserProfile("uuid-1", "001", "张三", "学生");

        profile.setUserName("002");

        assertEquals("002", profile.getUserName());
        assertEquals("张三", profile.getRealName());
    }

    /**
     * setter 与 getter 一致（反序列化后的对象由 setter 填充）。
     */
    @Test
    void settersRoundTrip() {
        UserProfile profile = new UserProfile();
        profile.setUuid("uuid-9");
        profile.setUserName("009");
        profile.setRealName("李四");
        profile.setRole("教师");

        assertEquals("uuid-9", profile.getUuid());
        assertEquals("009", profile.getUserName());
        assertEquals("李四", profile.getRealName());
        assertEquals("教师", profile.getRole());
    }
}
