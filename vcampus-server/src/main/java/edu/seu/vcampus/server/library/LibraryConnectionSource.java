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
 * 收敛成这一个接口之后语义只有一个：给一条真实连接，调用方在它上面提交或回滚。 测试替身也不得返回「空操作连接」—— 那正是上面那个坑本身（实现只有
 * {@link LibraryConnectionSourceJdbc} 一份）。
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
