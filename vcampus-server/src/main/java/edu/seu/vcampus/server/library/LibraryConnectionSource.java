package edu.seu.vcampus.server.library;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 图书馆的连接来源：借还、续借、预约这类跨表写必须落在<b>同一条</b>连接上。
 *
 * <p>
 * 这里原本用的是 {@code javax.sql.DataSource}，而测试替身对 {@code setAutoCommit} / {@code commit} /
 * {@code rollback} 全是空操作。于是装配到 JDBC 时上层以为自己在事务里，下层 DAO 拿到的却是一根假连接 —— {@code borrow} /
 * {@code returnBook} 声称的原子性是虚构的，中途失败会留下半截状态<b>而且不报错</b>。
 *
 * <p>
 * 收敛成这一个接口之后，实现只有两个，语义清楚：
 * <ul>
 * <li>内存版给一条可提交、可回滚的占位连接（内存 DAO 不看连接内容，仅作测试替身）；</li>
 * <li>jdbc 版给真实连接，事务才真的存在（生产走这条）。</li>
 * </ul>
 */
public interface LibraryConnectionSource {

    /**
     * 取一条连接；调用方负责关闭。
     *
     * @return 连接
     * @throws SQLException 取连接失败
     */
    Connection getConnection() throws SQLException;
}
