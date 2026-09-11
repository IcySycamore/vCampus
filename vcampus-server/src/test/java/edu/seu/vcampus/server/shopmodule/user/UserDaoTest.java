package edu.seu.vcampus.server.shopmodule.user;

import edu.seu.vcampus.common.user.HumanInfo;
import edu.seu.vcampus.common.user.Student;
import edu.seu.vcampus.common.user.User;
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
    void findByUserIdReturnsMatchingUser() {
        UserDao dao = mock(UserDao.class);
        User expected = new Student(new HumanInfo("001", "演示学生", null, null, null,
                20, HumanInfo.Gender.MALE), "001", "1");
        when(dao.findByUserId("001")).thenReturn(expected);

        User actual = dao.findByUserId("001");

        assertEquals("001", actual.getHumanInfo().getId());
        assertEquals("演示学生", actual.getHumanInfo().getName());
        verify(dao).findByUserId("001");
    }

    /**
     * 按用户名查询未命中时，约定返回 null（调用方须判空）。
     */
    @Test
    void findByUserIdReturnsNullWhenAbsent() {
        UserDao dao = mock(UserDao.class);
        when(dao.findByUserId("999")).thenReturn(null);

        assertNull(dao.findByUserId("999"));
    }

    /**
     * 新增用户成功时应返回 true。
     */
    @Test
    void addUserReturnsTrueOnSuccess() {
        UserDao dao = mock(UserDao.class);
        when(dao.addUser(any(User.class))).thenReturn(true);

        User user = new Student(new HumanInfo("004", "新同学", null, null, null,
            19, HumanInfo.Gender.FEMALE), "004", "pwd");

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
        users.add(new Student(new HumanInfo("001", "演示学生", null, null, null,
            20, HumanInfo.Gender.MALE), "001", "1"));
        when(dao.findAll()).thenReturn(users);

        assertEquals(1, dao.findAll().size());
    }
}
