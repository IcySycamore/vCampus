package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.entity.BorrowRecord;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/**
 * 图书借阅记录数据访问接口，由数据库负责人提供实现。
 * 带 Connection 的方法必须复用传入连接，不得自行提交、回滚或关闭连接。
 * 实现须支持多线程调用，不得在实例字段中保存当前事务连接。
 */
public interface BorrowDao {

    /**
     * 查询用户的全部借阅记录，含已归还记录，按借出时间降序返回。
     * 实现负责获取和释放本次查询使用的连接及资源。
     *
     * @param userId 用户 ID，与 BorrowRecord.userId 的字符串标识一致
     * @return 借阅记录，无记录时返回空列表，不返回 null
     * @throws SQLException 数据访问失败
     */
    List<BorrowRecord> findByUser(String userId) throws SQLException;

    /**
     * 查询用户是否存在该书的未归还记录；该检查不能替代插入时的并发唯一性保障。
     *
     * @param connection 业务层管理的事务连接
     * @param userId 用户 ID
     * @param isbn ISBN
     * @return 存在未归还记录为 true
     * @throws SQLException 数据访问失败
     */
    boolean hasActive(Connection connection, String userId, String isbn) throws SQLException;

    /**
     * 新增未归还的借阅记录，保存书名快照及业务层传入的借出、应还时间。
     * 同一用户同一 ISBN 至多存在一条未归还记录，并发插入也须保证此约束。
     *
     * @param connection 业务层管理的事务连接
     * @param record 新记录，id 和 returnedAt 尚未设置
     * @return 数据存储生成的正数记录号，由业务层设置到 record.id
     * @throws SQLException 数据访问失败、未取得记录号或并发重复借阅
     */
    long insert(Connection connection, BorrowRecord record) throws SQLException;

    /**
     * 按记录号查询指定用户的未归还记录，必须同时验证归属与未归还状态。
     *
     * @param connection 业务层管理的事务连接
     * @param userId 用户 ID
     * @param id 借阅记录号
     * @return 未归还记录；不存在、属于其他用户或已归还时返回 null
     * @throws SQLException 数据访问失败
     */
    BorrowRecord findActiveById(Connection connection, String userId, long id) throws SQLException;

    /**
     * 原子地将未归还记录标记为已归还，并发重复归还至多一次返回 true。
     *
     * @param connection 业务层管理的事务连接
     * @param id 已由 findActiveById 校验用户归属的记录号
     * @param returnedAt 实际归还时间
     * @return 更新成功为 true；不存在或已归还为 false，且不修改原记录
     * @throws SQLException 数据访问失败
     */
    boolean markReturned(Connection connection, long id, Timestamp returnedAt) throws SQLException;
}
