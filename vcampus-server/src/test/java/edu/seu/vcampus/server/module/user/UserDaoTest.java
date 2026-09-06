package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.common.entity.User;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UserDao 接口契约测试：用 Mockito 隔离数据库，验证调用方可依赖的行为约定
 * （见 ADR-0005）。真实 SQL 行为由 {@link UserDaoImplTest} 覆盖。
 */
class UserDaoTest {

    /**
     * 按用户名查询命中时，应返回对应的用户对象。
     */
    @Test
    void findByUsernameReturnsMatchingUser() {
        UserDao dao = mock(UserDao.class);
        User expected = new User("001", "演示学生", 20, "男", "1", "学生");
        when(dao.findByUsername("001")).thenReturn(expected);

        User actual = dao.findByUsername("001");

        assertEquals("001", actual.getuId());
        assertEquals("演示学生", actual.getuName());
        verify(dao).findByUsername("001");
    }

    /**
     * 按用户名查询未命中时，约定返回 null（调用方须判空）。
     */
    @Test
    void findByUsernameReturnsNullWhenAbsent() {
        UserDao dao = mock(UserDao.class);
        when(dao.findByUsername("999")).thenReturn(null);

        assertNull(dao.findByUsername("999"));
    }

    /**
     * 新增用户成功时应返回 true。
     */
    @Test
    void addUserReturnsTrueOnSuccess() {
        UserDao dao = mock(UserDao.class);
        when(dao.addUser(any(User.class))).thenReturn(true);

        User user = new User("004", "新同学", 19, "女", "pwd", "学生");

        assertTrue(dao.addUser(user));
        verify(dao).addUser(eq(user));
    }

    /**
     * 查询全部用户时应返回列表；无数据时为空列表而非 null。
     */
    @Test
    void findAllReturnsList() {
        UserDao dao = mock(UserDao.class);
        List<User> users = new ArrayList<User>();
        users.add(new User("001", "演示学生", 20, "男", "1", "学生"));
        when(dao.findAll()).thenReturn(users);

        assertEquals(1, dao.findAll().size());
    }
}
