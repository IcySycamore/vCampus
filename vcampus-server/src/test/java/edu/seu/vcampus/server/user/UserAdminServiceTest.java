package edu.seu.vcampus.server.user;

import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.user.dto.BatchResult;
import edu.seu.vcampus.common.user.dto.UserQuery;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UserAdminService 测试：管理轨的查询、编辑、启停与注销（含档案撤销）。
 */
class UserAdminServiceTest {

    private InMemoryUserRepository repository;
    private AccountProvisioning provisioning;
    private UserAdminService service;

    /**
     * 每个用例使用独立仓库与开户登记表。
     */
    @BeforeEach
    void setUp() {
        repository = new InMemoryUserRepository();
        provisioning = new AccountProvisioning();
        service = new UserAdminService(repository, provisioning);
    }

    /** 默认查询：按登录名排序并给出总数与总页数。 */
    @Test
    void listsAllUsersWithPaging() {
        seed("003", "管理员", true);
        seed("001", "学生", true);
        seed("002", "教师", true);

        PageResponse<User> firstPage = service.listUsers(new UserQuery(null, null, null, 1, 2));

        assertEquals(3L, firstPage.getTotal());
        assertEquals(2, firstPage.getItems().size());
        assertEquals(2, firstPage.getTotalPages());
        assertEquals("001", firstPage.getItems().get(0).getUserName());
        assertTrue(firstPage.hasNext());

        PageResponse<User> secondPage = service.listUsers(new UserQuery(null, null, null, 2, 2));
        assertEquals(1, secondPage.getItems().size());
        assertFalse(secondPage.hasNext());
    }

    /** 关键词匹配登录名或姓名，角色与启用状态可过滤。 */
    @Test
    void filtersByKeywordRoleAndEnabled() {
        seed("001", "学生", true);
        seed("002", "教师", false);
        repository.update("002", "李四");

        assertEquals(1L, service.listUsers(new UserQuery("李", null, null, 1, 20)).getTotal());
        assertEquals(1L,
                service.listUsers(new UserQuery(null, Role.TEACHER, null, 1, 20)).getTotal());
        assertEquals(1L,
                service.listUsers(new UserQuery(null, null, Boolean.FALSE, 1, 20)).getTotal());
        assertEquals(0L, service.listUsers(new UserQuery("不存在", null, null, 1, 20)).getTotal());
    }

    /** null 条件等价于查询全部。 */
    @Test
    void toleratesNullQuery() {
        seed("001", "学生", true);
        assertEquals(1L, service.listUsers(null).getTotal());
    }

    /** 编辑姓名：目标不存在返回 false，空姓名不改动。 */
    @Test
    void updatesDisplayName() {
        seed("001", "学生", true);

        assertTrue(service.updateUser("001", "张三"));
        assertEquals("张三", repository.findByUsername("001").getDisplayName());
        assertTrue(service.updateUser("001", "  "));
        assertEquals("张三", repository.findByUsername("001").getDisplayName());
        assertFalse(service.updateUser("ghost", "无名"));
        assertFalse(service.updateUser(null, "无名"));
    }

    /** 启停：目标不存在返回 false。 */
    @Test
    void togglesEnabled() {
        seed("001", "学生", true);

        assertTrue(service.setEnabled("001", false));
        assertFalse(repository.findByUsername("001").isEnabled());
        assertFalse(service.setEnabled("ghost", true));
        assertFalse(service.setEnabled(null, true));
    }

    /** 注销：先撤销各模块档案，再删除账户。 */
    @Test
    void unregisterRevokesProfilesThenDeletes() {
        seed("001", "学生", true);
        final List<String> revoked = new ArrayList<String>();
        provisioning.add(new AccountProvisioner() {
            @Override
            public void provision(String userUuid, String userName, Role role) {
                // 本用例只验证撤销路径
            }

            @Override
            public void revoke(String userUuid) {
                revoked.add(userUuid);
            }
        });

        assertTrue(service.unregister("001"));

        assertEquals(1, revoked.size());
        assertEquals("uuid-001", revoked.get(0));
        assertNull(repository.findByUsername("001"));
        assertFalse(service.unregister("001"));
    }

    /** 仓库为 null 时构造失败。 */
    @Test
    void rejectsNullRepository() {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                new UserAdminService(null, null);
            }
        });
    }

    /** 批量注销：成功的计数、不存在的计入失败明细。 */
    @Test
    void unregisterAllReportsPerItemResult() {
        seed("001", "学生", true);
        seed("002", "教师", true);

        BatchResult result = service.unregisterAll(Arrays.asList("001", "002", "ghost", "  "));

        assertEquals(2, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals("ghost", result.getFailures().get(0).getUserName());
        assertNull(repository.findByUsername("001"));
        assertNull(repository.findByUsername("002"));
    }

    /** null 输入不报错。 */
    @Test
    void unregisterAllToleratesNull() {
        assertEquals(0, service.unregisterAll(null).getSuccessCount());
    }

    private void seed(String username, String role, boolean enabled) {
        repository.save(new UserRepository.Credential(username, "uuid-" + username, username,
                "salt", "hash", role, enabled));
    }
}
