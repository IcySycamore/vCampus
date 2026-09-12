package edu.seu.vcampus.common.user.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 个人档案测试：界面显示名的回退规则。
 *
 * <p>
 * 界面以前只能显示用户名，根因是链路里没有姓名。这里锁住回退契约——无论姓名为 null、
 * 空串还是纯空格，{@code getDisplayName()} 都必须给出可直接显示的字符串。
 */
class UserProfileTest {

    /**
     * 有姓名时优先用姓名。
     */
    @Test
    void displayNamePrefersRealName() {
        UserProfile profile = new UserProfile("uuid-1", "001", "张三", "学生");

        assertEquals("张三", profile.getRealName());
        assertEquals("张三", profile.getDisplayName());
    }

    /**
     * 未采集姓名（管理员账号）时回退登录名。
     */
    @Test
    void displayNameFallsBackToUserName() {
        UserProfile profile = new UserProfile("uuid-1", "001", null, "管理员");

        assertEquals("001", profile.getDisplayName());
    }

    /**
     * 纯空格的姓名不算数，仍回退登录名。
     */
    @Test
    void blankRealNameFallsBackToUserName() {
        UserProfile profile = new UserProfile("uuid-1", "001", "   ", "学生");

        assertEquals("001", profile.getDisplayName());
    }

    /**
     * 姓名两侧空白被去掉，避免界面上出现奇怪的对齐。
     */
    @Test
    void realNameIsTrimmed() {
        UserProfile profile = new UserProfile("uuid-1", "001", "  张三  ", "学生");

        assertEquals("张三", profile.getDisplayName());
    }

    /**
     * 姓名与登录名都为空时返回空串，不返回 null（调用方不必判空）。
     */
    @Test
    void bothEmptyGivesEmptyString() {
        UserProfile profile = new UserProfile("uuid-1", null, null, "学生");

        assertEquals("", profile.getDisplayName());
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
        assertEquals("李四", profile.getDisplayName());
    }
}
