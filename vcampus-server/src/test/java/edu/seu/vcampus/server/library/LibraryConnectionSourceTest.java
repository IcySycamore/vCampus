package edu.seu.vcampus.server.library;

import edu.seu.vcampus.server.db.DatabaseAvailability;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 连接来源两种实现的契约。
 *
 * <p>
 * 这个接缝此前是 {@code javax.sql.DataSource}，而 jdbc 模式下注入的仍是内存实现 —— 于是上层 {@code setAutoCommit(false)}
 * 落在假连接上、下层 DAO 各写各的，借书这类跨表写中途失败会留半截状态 且不报错。测试锁住两件事：内存版给出可提交可回滚的占位连接，jdbc 版给出真实连接。
 */
class LibraryConnectionSourceTest {

    /**
     * 内存版应给出相互独立、支持事务调用的占位连接。
     *
     * @throws Exception 取连接或调用事务方法失败
     */
    @Test
    void memorySourceGivesUsablePlaceholderConnection() throws Exception {
        LibraryConnectionSource source = new LibraryConnectionSourceMemory();
        Connection first = source.getConnection();
        Connection second = source.getConnection();

        assertNotNull(first);
        assertNotSame(first, second, "每次取连接应相互独立");
        first.setAutoCommit(false);
        assertFalse(first.getAutoCommit(), "占位连接应记住 autoCommit 设置");
        first.commit();
        first.rollback();
        first.close();
        assertTrue(first.isClosed());
    }

    /**
     * jdbc 版应向 DbHelper 取真实连接（MySQL 不可用时按 ADR-0005 跳过）。
     *
     * @throws Exception 取连接失败
     */
    @Test
    void jdbcSourceGivesRealConnection() throws Exception {
        Assumptions.assumeTrue(DatabaseAvailability.isReady(), "MySQL 不可用，跳过");

        Connection connection = new LibraryConnectionSourceJdbc().getConnection();
        try {
            assertNotNull(connection);
            assertTrue(connection.isValid(2), "jdbc 来源应给出可用连接");
            assertFalse(connection.isClosed());
        } finally {
            connection.close();
        }
    }
}
