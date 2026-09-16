package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 用户表格「行 ↔ 记录」与选中映射测试。
 */
class UserManageTableTest {

    @Test
    void showsRowsWithRoleAndState() {
        UserManageTable table = new UserManageTable();

        table.show(pageOf(user("001", "张三", Role.STUDENT, true),
                user("002", "李四", Role.TEACHER, false)));

        assertEquals(2, table.rowCount());
        assertEquals("001", table.table().getValueAt(0, 0));
        assertEquals("张三", table.table().getValueAt(0, 1));
        assertEquals("学生", table.table().getValueAt(0, 2));
        assertEquals("启用", table.table().getValueAt(0, 3));
        assertEquals("禁用", table.table().getValueAt(1, 3));
    }

    @Test
    void replacesRowsOnNextPage() {
        UserManageTable table = new UserManageTable();
        table.show(pageOf(user("001", "张三", Role.STUDENT, true)));

        table.show(pageOf(user("002", "李四", Role.TEACHER, true)));

        assertEquals(1, table.rowCount());
        assertEquals("002", table.table().getValueAt(0, 0));
    }

    @Test
    void reportsSelectedUserNamesInRowOrder() {
        UserManageTable table = new UserManageTable();
        table.show(pageOf(user("001", "张三", Role.STUDENT, true),
                user("002", "李四", Role.TEACHER, true),
                user("003", "王五", Role.ADMIN, true)));

        table.table().setRowSelectionInterval(2, 2);
        table.table().addRowSelectionInterval(0, 0);

        // JTable 的 getSelectedRows() 按行号升序返回
        assertEquals(Arrays.asList("001", "003"), table.selectedUserNames());
    }

    @Test
    void selectedUserRequiresExactlyOneRow() {
        UserManageTable table = new UserManageTable();
        table.show(pageOf(user("001", "张三", Role.STUDENT, true),
                user("002", "李四", Role.TEACHER, true)));

        assertNull(table.selectedUser());
        table.table().setRowSelectionInterval(1, 1);
        assertEquals("002", table.selectedUser().getUserName());
        table.table().setRowSelectionInterval(0, 1);
        assertNull(table.selectedUser());
    }

    @Test
    void toleratesNullPageAndEmptySelection() {
        UserManageTable table = new UserManageTable();

        table.show(null);

        assertEquals(0, table.rowCount());
        assertTrue(table.selectedUserNames().isEmpty());
    }

    private User user(String userName, String displayName, Role role, boolean enabled) {
        User user = new User();
        user.setUserName(userName);
        user.setDisplayName(displayName);
        user.setRole(role);
        user.setEnabled(enabled);
        return user;
    }

    private PageResponse<User> pageOf(User... users) {
        List<User> items = new ArrayList<User>(Arrays.asList(users));
        return new PageResponse<User>(items, items.size(), 1, 20);
    }
}
