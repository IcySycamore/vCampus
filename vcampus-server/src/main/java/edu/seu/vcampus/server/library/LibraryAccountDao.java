package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.library.entity.LibraryAccount;
import java.sql.SQLException;
import java.util.Date;

/** 图书馆读者账户数据访问接口，由数据库负责人提供实现。 */
public interface LibraryAccountDao {
    /**
     * 按用户 UUID 查询账户，包含已经软删除的记录。
     * @param userUuid 用户 UUID
     * @return 图书馆账户；不存在时返回 null
     * @throws SQLException 数据访问失败
     */
    LibraryAccount findByUserUuid(String userUuid) throws SQLException;

    /**
     * 新增账户并回填主键。
     * @param account 待新增账户
     * @return 新增成功为 true
     * @throws SQLException 数据访问失败
     */
    boolean insert(LibraryAccount account) throws SQLException;

    /**
     * 保存账户状态和借阅上限。
     * @param account 待保存账户
     * @return 更新成功为 true
     * @throws SQLException 数据访问失败
     */
    boolean update(LibraryAccount account) throws SQLException;

    /**
     * 按用户 UUID 软删除账户。
     * @param userUuid 用户 UUID
     * @param updatedAt 删除时间
     * @return 更新成功或账户已删除为 true
     * @throws SQLException 数据访问失败
     */
    boolean softDelete(String userUuid, Date updatedAt) throws SQLException;
}
