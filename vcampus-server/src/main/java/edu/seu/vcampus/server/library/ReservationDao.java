package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.library.entity.BookReservation;
import edu.seu.vcampus.common.library.entity.ReservationStatus;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/** 图书预约数据访问接口，由数据库负责人提供事务安全的实现。 */
public interface ReservationDao {
    /**
     * 查询用户全部预约，按申请时间降序。
     * @param userId 用户 UUID
     * @return 预约记录，不返回 null
     * @throws SQLException 数据访问失败
     */
    List<BookReservation> findByUser(String userId) throws SQLException;

    /**
     * 查询用户是否已有同一本书的等待或到馆预约。
     * @param connection 业务事务连接
     * @param userId 用户 UUID
     * @param isbn ISBN
     * @return 是否存在有效预约
     * @throws SQLException 数据访问失败
     */
    boolean hasActive(Connection connection, String userId, String isbn)
            throws SQLException;

    /**
     * 查找用户已到馆的有效预约并锁定记录。
     * @param connection 业务事务连接
     * @param userId 用户 UUID
     * @param isbn ISBN
     * @return 到馆预约；不存在时返回 null
     * @throws SQLException 数据访问失败
     */
    BookReservation findReady(Connection connection, String userId, String isbn)
            throws SQLException;

    /**
     * 按预约号查询有效预约并锁定记录。
     * @param connection 业务事务连接
     * @param id 预约号
     * @return 等待或到馆预约；不存在时返回 null
     * @throws SQLException 数据访问失败
     */
    BookReservation findActiveById(Connection connection, long id) throws SQLException;

    /**
     * 查询指定图书所有已过保留期的到馆预约并锁定，按到馆时间升序。
     * @param connection 业务事务连接
     * @param isbn ISBN
     * @param now 当前时间
     * @return 已过期预约，不返回 null
     * @throws SQLException 数据访问失败
     */
    List<BookReservation> findExpiredReady(Connection connection, String isbn,
            Timestamp now) throws SQLException;

    /**
     * 查询最早的等待预约并锁定记录。
     * @param connection 业务事务连接
     * @param isbn ISBN
     * @return 最早等待记录；没有时返回 null
     * @throws SQLException 数据访问失败
     */
    BookReservation findFirstWaiting(Connection connection, String isbn)
            throws SQLException;

    /**
     * 指定图书是否存在等待或到馆预约。
     * @param connection 业务事务连接
     * @param isbn ISBN
     * @return 是否有人预约
     * @throws SQLException 数据访问失败
     */
    boolean hasActiveForBook(Connection connection, String isbn) throws SQLException;

    /**
     * 新增等待预约；数据库须保证每个用户每个 ISBN 仅有一条有效预约。
     * @param connection 业务事务连接
     * @param reservation 新预约
     * @return 生成的正数预约号
     * @throws SQLException 数据访问失败
     */
    long insert(Connection connection, BookReservation reservation) throws SQLException;

    /**
     * 更新预约状态和到馆保留时间。
     * @param connection 业务事务连接
     * @param id 预约号
     * @param status 新状态
     * @param readyAt 到馆时间；仅 READY 非空
     * @param expiresAt 保留截止时间；仅 READY 非空
     * @return 更新成功为 true
     * @throws SQLException 数据访问失败
     */
    boolean updateStatus(Connection connection, long id, ReservationStatus status,
            Timestamp readyAt, Timestamp expiresAt) throws SQLException;
}
