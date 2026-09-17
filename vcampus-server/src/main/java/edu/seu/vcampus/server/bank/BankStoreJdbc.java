package edu.seu.vcampus.server.bank;

import edu.seu.vcampus.common.bank.entity.BankAccount;
import edu.seu.vcampus.common.bank.entity.BankAccountStatus;
import edu.seu.vcampus.common.bank.entity.BankTransaction;
import edu.seu.vcampus.common.bank.entity.BankTransactionType;
import edu.seu.vcampus.server.db.DatabaseAccessException;
import edu.seu.vcampus.server.db.DbHelper;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 【MySQL 版】银行账户与流水的持久化后端：落表 {@code tblBankAccount} 与 {@code tblBankTransaction}。
 *
 * <p>
 * 由 {@code -Dvcampus.store=jdbc} 装配，缺省仍是 {@link BankStoreMemory}。表结构见
 * {@code sql/vCampus.sql}（账户与流水两张表是课程给定的）加 {@code sql/vCampus-extend.sql} （补银行密码与挂失时间四列）。
 *
 * <p>
 * <b>标识</b>：账户表的主键是 {@code baUuid}，业务识别用唯一的 {@code baId}（形如 {@code A-<uuid>}，由 {@code BankService}
 * 生成），所属人用唯一的 {@code uUuid}。本类所有读写都 按 {@code baId} 定位，与全库「对外访问基于 uuid/业务号」的约定一致。
 *
 * <p>
 * <b>状态列</b>：{@code baState} 落库存枚举名（{@code NORMAL}/{@code FROZEN}/{@code CLOSED}），
 * 与图书馆、学籍各表一致；读时兼容给定表默认值那套中文显示名，便于人工查库时改过值也能读回来。
 *
 * <p>
 * <b>不加锁</b>：并发语义由 {@link BankService} 的 {@code synchronized (record)} 承担，本实现按
 * 「调用方已持有账户锁」编写，每条语句自成事务。
 */
public final class BankStoreJdbc implements BankStore {

    /** 账户查询列。 */
    private static final String ACCOUNT_COLUMNS = "baUuid, baId, uUuid, baBalance, baState, baCreatedAt, baUpdatedAt";

    /** 流水查询列。 */
    private static final String TRANSACTION_COLUMNS = "btId, btAccountId, btType, btAmount,"
            + " btBalanceBefore, btBalanceAfter, btRelatedOrderId, btDescription, btCreatedAt";

    /** @return 全部账户，按开户时间升序 */
    @Override
    public List<BankAccount> loadAccounts() {
        List<BankAccount> found = new ArrayList<BankAccount>();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement("SELECT " + ACCOUNT_COLUMNS
                    + " FROM tblBankAccount ORDER BY baCreatedAt ASC, baId ASC");
            rows = statement.executeQuery();
            while (rows.next()) {
                found.add(toAccount(rows));
            }
            return found;
        } catch (SQLException e) {
            throw new DatabaseAccessException("加载银行账户失败", e);
        } finally {
            closeQuietly(rows);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    /** @param accountId 账户业务编号 @return 凭据；无密码或账户不存在时返回 null */
    @Override
    public BankCredentialRecord loadCredential(String accountId) {
        if (accountId == null) {
            return null;
        }
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement("SELECT baPwdSalt, baPwdHash, baPwdSetAt,"
                    + " baFrozenAt FROM tblBankAccount WHERE baId = ?");
            statement.setString(1, accountId);
            rows = statement.executeQuery();
            if (!rows.next()) {
                return null;
            }
            return new BankCredentialRecord(rows.getBytes("baPwdSalt"), rows.getBytes("baPwdHash"),
                    rows.getTimestamp("baPwdSetAt"), rows.getTimestamp("baFrozenAt"));
        } catch (SQLException e) {
            throw new DatabaseAccessException("读取银行凭据失败: " + accountId, e);
        } finally {
            closeQuietly(rows);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    /** @param accountId 账户业务编号 @return 该账户流水，按时间升序 */
    @Override
    public List<BankTransaction> loadTransactions(String accountId) {
        List<BankTransaction> found = new ArrayList<BankTransaction>();
        if (accountId == null) {
            return found;
        }
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement("SELECT " + TRANSACTION_COLUMNS
                    + " FROM tblBankTransaction WHERE btAccountId = ?"
                    + " ORDER BY btCreatedAt ASC, btId ASC");
            statement.setString(1, accountId);
            rows = statement.executeQuery();
            while (rows.next()) {
                found.add(toTransaction(rows));
            }
            return found;
        } catch (SQLException e) {
            throw new DatabaseAccessException("读取银行流水失败: " + accountId, e);
        } finally {
            closeQuietly(rows);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    /** @return 库里 {@code T-<n>} 形式流水的最大 n；无流水返回 0 */
    @Override
    public long loadMaxSequence() {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement("SELECT MAX(CAST(SUBSTRING(btId, 3)"
                    + " AS UNSIGNED)) FROM tblBankTransaction WHERE btId LIKE 'T-%'");
            rows = statement.executeQuery();
            return rows.next() ? rows.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new DatabaseAccessException("恢复银行流水序号失败", e);
        } finally {
            closeQuietly(rows);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    /** @param account 新账户 @param credential 凭据，可为 null @return 写入成功为 true */
    @Override
    public boolean insertAccount(BankAccount account, BankCredentialRecord credential) {
        if (account == null || account.getAccountId() == null) {
            return false;
        }
        String sql = "INSERT INTO tblBankAccount (baUuid, baId, uUuid, baBalance, baState,"
                + " baCreatedAt, baUpdatedAt, baPwdSalt, baPwdHash, baPwdSetAt, baFrozenAt)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(sql);
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, account.getAccountId());
            statement.setString(3, account.getOwnerUuid());
            statement.setBigDecimal(4, account.getBalance());
            statement.setString(5, statusName(account.getStatus()));
            statement.setTimestamp(6, timestamp(account.getCreatedAt() == null
                    ? new Date()
                    : account.getCreatedAt()));
            statement.setTimestamp(7, timestamp(account.getUpdatedAt() == null
                    ? new Date()
                    : account.getUpdatedAt()));
            if (credential == null) {
                statement.setNull(8, java.sql.Types.VARCHAR);
                statement.setNull(9, java.sql.Types.VARCHAR);
                statement.setNull(10, java.sql.Types.TIMESTAMP);
                statement.setNull(11, java.sql.Types.TIMESTAMP);
            } else {
                statement.setBytes(8, credential.getSalt());
                statement.setBytes(9, credential.getHash());
                statement.setTimestamp(10, timestamp(credential.getSetAt()));
                statement.setTimestamp(11, timestamp(credential.getFrozenAt()));
            }
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                return false;// 该用户或该账户编号已开户
            }
            throw new DatabaseAccessException("开户落库失败: " + account.getAccountId(), e);
        } finally {
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    /** @param account 已变更的账户 @return 命中记录为 true */
    @Override
    public boolean updateAccount(BankAccount account) {
        if (account == null || account.getAccountId() == null) {
            return false;
        }
        String sql = "UPDATE tblBankAccount SET baBalance = ?, baState = ?, baUpdatedAt = ?"
                + " WHERE baId = ?";
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(sql);
            statement.setBigDecimal(1, account.getBalance());
            statement.setString(2, statusName(account.getStatus()));
            statement.setTimestamp(3, timestamp(account.getUpdatedAt() == null
                    ? new Date()
                    : account.getUpdatedAt()));
            statement.setString(4, account.getAccountId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseAccessException("保存银行账户失败: " + account.getAccountId(), e);
        } finally {
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    /** @param accountId 账户业务编号 @param credential 新凭据 @return 命中记录为 true */
    @Override
    public boolean updateCredential(String accountId, BankCredentialRecord credential) {
        if (accountId == null) {
            return false;
        }
        String sql = "UPDATE tblBankAccount SET baPwdSalt = ?, baPwdHash = ?, baPwdSetAt = ?,"
                + " baFrozenAt = ? WHERE baId = ?";
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(sql);
            if (credential == null) {
                statement.setNull(1, java.sql.Types.VARCHAR);
                statement.setNull(2, java.sql.Types.VARCHAR);
                statement.setNull(3, java.sql.Types.TIMESTAMP);
                statement.setNull(4, java.sql.Types.TIMESTAMP);
            } else {
                statement.setBytes(1, credential.getSalt());
                statement.setBytes(2, credential.getHash());
                statement.setTimestamp(3, timestamp(credential.getSetAt()));
                statement.setTimestamp(4, timestamp(credential.getFrozenAt()));
            }
            statement.setString(5, accountId);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseAccessException("保存银行凭据失败: " + accountId, e);
        } finally {
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    /** @param transaction 已生成的流水 @return 写入成功为 true */
    @Override
    public boolean appendTransaction(BankTransaction transaction) {
        if (transaction == null) {
            return false;
        }
        String sql = "INSERT INTO tblBankTransaction (btId, btAccountId, btType, btAmount,"
                + " btBalanceBefore, btBalanceAfter, btRelatedOrderId, btDescription, btCreatedAt)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(sql);
            statement.setString(1, transaction.getTransactionId());
            statement.setString(2, transaction.getAccountId());
            statement.setString(3, transaction.getType().name());
            statement.setBigDecimal(4, transaction.getAmount());
            statement.setBigDecimal(5, transaction.getBalanceBefore());
            statement.setBigDecimal(6, transaction.getBalanceAfter());
            statement.setString(7, transaction.getRelatedOrderId());
            statement.setString(8, transaction.getDescription());
            statement.setTimestamp(9, timestamp(transaction.getCreatedAt()));
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseAccessException("写银行流水失败: " + transaction.getTransactionId(), e);
        } finally {
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    /**
     * 结果行 → 账户。
     *
     * @param rows 已定位到某行的结果集
     * @return 账户
     * @throws SQLException 读取失败
     */
    private static BankAccount toAccount(ResultSet rows) throws SQLException {
        return new BankAccount(rows.getString("baId"), rows.getString("uUuid"),
                rows.getBigDecimal("baBalance"), status(rows.getString("baState")),
                rows.getTimestamp("baCreatedAt"), rows.getTimestamp("baUpdatedAt"));
    }

    /**
     * 结果行 → 流水。
     *
     * @param rows 已定位到某行的结果集
     * @return 流水
     * @throws SQLException 读取失败
     */
    private static BankTransaction toTransaction(ResultSet rows) throws SQLException {
        return new BankTransaction(rows.getString("btId"), rows.getString("btAccountId"),
                type(rows.getString("btType")), rows.getBigDecimal("btAmount"),
                rows.getBigDecimal("btBalanceBefore"), rows.getBigDecimal("btBalanceAfter"),
                rows.getString("btRelatedOrderId"), rows.getString("btDescription"),
                rows.getTimestamp("btCreatedAt"));
    }

    /**
     * 账户状态的落库取值。
     *
     * @param status 状态；null 视作正常
     * @return 枚举名
     */
    private static String statusName(BankAccountStatus status) {
        return status == null ? BankAccountStatus.NORMAL.name() : status.name();
    }

    /**
     * 库中文本 → 账户状态。
     *
     * @param text 枚举名或给定表默认的中文显示名；无法识别时视作正常
     * @return 账户状态
     */
    private static BankAccountStatus status(String text) {
        if (text == null) {
            return BankAccountStatus.NORMAL;
        }
        try {
            return BankAccountStatus.valueOf(text);
        } catch (IllegalArgumentException e) {
            BankAccountStatus byName = BankAccountStatus.fromDisplayName(text);
            return byName == null ? BankAccountStatus.NORMAL : byName;
        }
    }

    /**
     * 库中文本 → 流水类型。
     *
     * @param text 枚举名；无法识别时视作消费
     * @return 流水类型
     */
    private static BankTransactionType type(String text) {
        if (text == null) {
            return BankTransactionType.CONSUMPTION;
        }
        try {
            return BankTransactionType.valueOf(text);
        } catch (IllegalArgumentException e) {
            return BankTransactionType.CONSUMPTION;
        }
    }

    /**
     * 日期 → SQL 时间戳。
     *
     * @param date 日期；可为 null
     * @return 时间戳；null 输入返回 null
     */
    private static java.sql.Timestamp timestamp(Date date) {
        return date == null ? null : new java.sql.Timestamp(date.getTime());
    }

    /**
     * 安静关闭资源。
     *
     * @param closeable 可关闭对象；可为 null
     */
    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
            // 关闭失败不影响业务结果
        }
    }
}
