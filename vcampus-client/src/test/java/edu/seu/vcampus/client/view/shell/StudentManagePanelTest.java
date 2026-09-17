package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.common.user.entity.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 学籍列表页的页签标题测试。
 *
 * <p>
 * 只测 {@link StudentManagePanel#tabTitle(Role)} 这个静态判定——面板本身的构造需要学籍 API，而这里
 * 要钉住的恰恰是「名字跟着权限走」这条规则。
 */
class StudentManagePanelTest {

    /**
     * 能改的角色才叫「学籍管理」，只读的角色只能叫「学籍查询」。
     *
     * <p>
     * 名字叫「管理」而点进去什么都改不了，会让人以为找错了地方、或者怀疑自己缺权限；反过来把
     * 管理员的页签叫「查询」，也会让他找不到改状态与注销的入口。
     */
    @Test
    void titleFollowsWritePermission() {
        assertEquals("学籍管理", StudentManagePanel.tabTitle(Role.ADMIN));
        assertEquals("学籍查询", StudentManagePanel.tabTitle(Role.TEACHER));
    }

    /**
     * 学生与未登录（null）都是只读——即便将来把本页挂给他们，标题也不会说成「管理」。
     */
    @Test
    void readOnlyRolesFallBackToQuery() {
        assertEquals("学籍查询", StudentManagePanel.tabTitle(Role.STUDENT));
        assertEquals("学籍查询", StudentManagePanel.tabTitle(null));
    }
}
