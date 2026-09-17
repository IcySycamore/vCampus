package edu.seu.vcampus.server.library;

import edu.seu.vcampus.server.db.DbHelper;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 【MySQL 版】图书馆连接来源：直接向 {@link DbHelper} 取真实连接。
 *
 * <p>
 * 生产装配只走这一份：图书馆 DAO 全都接受调用方传入的连接，所以上层从这里取的那条连接就是它们真正执行 SQL 的那条，事务边界不再是装饰。
 */
public final class LibraryConnectionSourceJdbc implements LibraryConnectionSource {

    /** @return 真实数据库连接 */
    @Override
    public Connection getConnection() throws SQLException {
        return DbHelper.getConnection();
    }
}
