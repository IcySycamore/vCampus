package edu.seu.vcampus.server.bank;

import edu.seu.vcampus.common.bank.entity.BankAccount;
import edu.seu.vcampus.common.bank.entity.BankAccountStatus;
import edu.seu.vcampus.common.bank.entity.BankTransaction;
import edu.seu.vcampus.common.bank.entity.BankTransactionType;
import edu.seu.vcampus.server.db.DbHelper;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link BankStoreJdbc} 的真库集成测试。
 *
 * <p>
 * <b>环境门控</b>（见 ADR-0005）：连不上 MySQL 时整体跳过。前置是库中已有给定表
 * {@code tblBankAccount}、{@code tblBankTransaction} 与扩展补丁补出的四个密码/挂失列。
 *
 * <p>
 * 账户表有指向 {@code tblUser} 的外键，所以每个用例先插一行临时用户，跑完按外键顺序
 * （流水 → 账户 → 用户）物理删除。标识一律带时间戳，避免与演示数据或上一次运行互撞；列宽也要
 * 守着：{@code uUuid} 是 {@code CHAR(36)}、{@code baId} 是 {@code VARCHAR(20)}、
 * {@code uId} 是 {@code VARCHAR(8)}。
 */
class BankStoreJdbcTest {

    /** 测试 uuid 前缀。 */
    private static final String UUID_PREFIX = "jdbc-bank-it-";

    /** 本轮测试用户 uuid。 */
    private String m_ownerUuid;

    /** 本轮测试账户业务编号。 */
    private String m_accountId;

    /** 被测后端。 */
    private BankStoreJdbc m_store;

    /** 每个用例前确认数据库可用、建临时用户并生成唯一标识。 */
    @BeforeEach
    void setUp() {
        Assumptions.assumeTrue(databaseAvailable(),
                "MySQL 不可用，跳过 JDBC 集成测试（docker compose up -d mysql 后自动执行）");
        long stamp = System.nanoTime();
        m_ownerUuid = uuid(stamp);
        m_accountId = "A-" + Long.toHexString(stamp).substring(0, 12);
        m_store = new BankStoreJdbc();
        insertUser(m_ownerUuid, "B" + Long.toHexString(stamp).substring(0, 6));
    }

    /** 用例后按外键顺序物理删除测试数据。 */
    @AfterEach
    void tearDown() {
        executeUpdate("DELETE FROM tblBankTransaction WHERE btAccountId = ?", m_accountId);
        executeUpdate("DELETE FROM tblBankAccount WHERE baId = ?", m_accountId);
        executeUpdate("DELETE FROM tblUser WHERE uUuid = ?", m_ownerUuid);
    }

    @Test
    void insertedAccountIsLoadedBackWithCredential() {
        BankAccount account = account(m_accountId, m_ownerUuid, BigDecimal.ZERO,
                BankAccountStatus.NORMAL);
        BankCredentialRecord credential = new BankCredentialRecord(salt(), hash(), new Date(),
                null);
        assertTrue(m_store.insertAccount(account, credential), "开户落库应成功");

        BankAccount loaded = findAccount(m_store.loadAccounts());
        assertNotNull(loaded, "loadAccounts 应能读到刚开的账户");
        assertEquals(m_accountId, loaded.getAccountId());
        assertEquals(m_ownerUuid, loaded.getOwnerUuid());
        assertEquals(0, loaded.getBalance().compareTo(BigDecimal.ZERO));
        assertEquals(BankAccountStatus.NORMAL, loaded.getStatus());

        BankCredentialRecord stored = m_store.loadCredential(m_accountId);
        assertNotNull(stored, "应能读回凭据");
        assertTrue(stored.hasPassword(), "凭据应带盐与摘要");
        assertTrue(stored.samePassword(credential), "盐与摘要应原样落库");
    }

    @Test
    void updateAccountPersistsBalanceStateAndFreezeTime() {
        m_store.insertAccount(account(m_accountId, m_ownerUuid, BigDecimal.ZERO,
                BankAccountStatus.NORMAL), null);

        BankAccount loaded = findAccount(m_store.loadAccounts());
        loaded.setBalance(BigDecimal.valueOf(88).setScale(2));
        loaded.setStatus(BankAccountStatus.FROZEN);
        loaded.setUpdatedAt(new Date());
        assertTrue(m_store.updateAccount(loaded), "更新账户应命中");

        Date frozenAt = new Date();
        assertTrue(m_store.updateCredential(m_accountId,
                new BankCredentialRecord(null, null, null, frozenAt)), "更新挂失时间应命中");

        BankAccount reloaded = findAccount(m_store.loadAccounts());
        assertEquals(0, reloaded.getBalance().compareTo(BigDecimal.valueOf(88).setScale(2)));
        assertEquals(BankAccountStatus.FROZEN, reloaded.getStatus());
        BankCredentialRecord credential = m_store.loadCredential(m_accountId);
        assertNotNull(credential.getFrozenAt(), "挂失时间应落库");
        assertFalse(credential.hasPassword(), "未设密码时不应凭空补上");
    }

    @Test
    void appendedTransactionsAreLoadedAndSequenceResumes() {
        m_store.insertAccount(account(m_accountId, m_ownerUuid, BigDecimal.ZERO,
                BankAccountStatus.NORMAL), null);

        long high = System.currentTimeMillis() % 100000000L + 1000000L;
        BankTransaction first = transaction("T-" + high, BigDecimal.TEN, BigDecimal.ZERO);
        BankTransaction second = transaction("T-" + (high + 1), BigDecimal.TEN, BigDecimal.TEN);
        assertTrue(m_store.appendTransaction(first), "第一条流水应写入");
        assertTrue(m_store.appendTransaction(second), "第二条流水应写入");

        List<BankTransaction> loaded = m_store.loadTransactions(m_accountId);
        assertEquals(2, loaded.size(), "应读回两条流水");
        assertEquals("T-" + high, loaded.get(0).getTransactionId(), "应按时间升序");
        assertEquals(0, loaded.get(1).getBalanceBefore().compareTo(BigDecimal.TEN));

        assertTrue(m_store.loadMaxSequence() >= high + 1L,
                "最大流水序号应覆盖刚写入的号，重启后才能接着分配");
    }

    @Test
    void sameOwnerCannotOpenTwoAccounts() {
        assertTrue(m_store.insertAccount(account(m_accountId, m_ownerUuid, BigDecimal.ZERO,
                BankAccountStatus.NORMAL), null), "首次开户应成功");
        assertFalse(m_store.insertAccount(account("A-" + Long.toHexString(System.nanoTime()),
                m_ownerUuid, BigDecimal.ZERO, BankAccountStatus.NORMAL), null),
                "同一用户第二次开户应被唯一键拒绝");
    }

    /**
     * 造一个账户。
     *
     * @param accountId 账户业务编号
     * @param ownerUuid 所属用户 uuid
     * @param balance 余额
     * @param status 状态
     * @return 账户实体
     */
    private static BankAccount account(String accountId, String ownerUuid, BigDecimal balance,
            BankAccountStatus status) {
        Date now = new Date();
        return new BankAccount(accountId, ownerUuid, balance, status, now, now);
    }

    /**
     * 造一条充值流水（前后余额须与类型自洽）。
     *
     * <p>
     * 账户号必须用本用例真实开户的那一个：{@code tblBankTransaction.btAccountId} 上有指向
     * {@code tblBankAccount.baId} 的外键，写占位值会被数据库拒绝。
     *
     * @param transactionId 流水号
     * @param amount 金额
     * @param before 交易前余额
     * @return 流水实体
     */
    private BankTransaction transaction(String transactionId, BigDecimal amount,
            BigDecimal before) {
        return new BankTransaction(transactionId, m_accountId, BankTransactionType.RECHARGE,
                amount, before, before.add(amount), null, "集成测试", new Date());
    }

    /**
     * 生成一个 8 字节长度的固定盐。
     *
     * @return 盐
     */
    private static byte[] salt() {
        return "1234567890abcdef".getBytes();
    }

    /**
     * 生成一个 32 字节长度的固定摘要。
     *
     * @return 摘要
     */
    private static byte[] hash() {
        return "0123456789abcdef0123456789abcdef".getBytes();
    }

    /**
     * 从账户列表里挑出本用例的账户。
     *
     * @param all 全部账户
     * @return 匹配的账户；找不到返回 null
     */
    private BankAccount findAccount(List<BankAccount> all) {
        for (BankAccount item : all) {
            if (m_accountId.equals(item.getAccountId())) {
                return item;
            }
        }
        return null;
    }

    /**
     * 生成恰好 36 位的测试 uuid（{@code uUuid} 是 {@code CHAR(36)}）。
     *
     * @param stamp 时间戳
     * @return 补足 36 位的 uuid
     */
    private static String uuid(long stamp) {
        StringBuilder builder = new StringBuilder(UUID_PREFIX);
        builder.append(Long.toHexString(stamp));
        while (builder.length() < 36) {
            builder.append('0');
        }
        return builder.substring(0, 36);
    }

    /**
     * 插入一行临时用户，满足账户表的外键。
     *
     * @param ownerUuid 用户 uuid
     * @param loginId 登录 ID，最多 8 字符
     */
    private static void insertUser(String ownerUuid, String loginId) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement("INSERT INTO tblUser (uUuid, uId, uName,"
                    + " uPwd, uRole) VALUES (?, ?, ?, ?, ?)");
            statement.setString(1, ownerUuid);
            statement.setString(2, loginId);
            statement.setString(3, "集成测试");
            statement.setString(4, "x");
            statement.setString(5, "学生");
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("插入测试用户失败", e);
        } finally {
            close(null, statement, connection);
        }
    }

    /**
     * 执行一条写语句（用于清理测试数据）。
     *
     * @param sql 语句
     * @param param 唯一参数
     */
    private static void executeUpdate(String sql, String param) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(sql);
            statement.setString(1, param);
            statement.executeUpdate();
        } catch (SQLException e) {
            // 清理失败不影响用例结论
        } catch (RuntimeException e) {
            // 数据库不可用时 setUp 已跳过，这里同样忽略
        } finally {
            close(null, statement, connection);
        }
    }

    /**
     * 探测数据库是否可用。
     *
     * @return 能取到连接返回 true
     */
    private static boolean databaseAvailable() {
        Connection connection = null;
        try {
            connection = DbHelper.getConnection();
            return connection != null;
        } catch (SQLException e) {
            return false;
        } catch (RuntimeException e) {
            return false;
        } finally {
            close(null, null, connection);
        }
    }

    /**
     * 安静关闭资源。
     *
     * @param rows 结果集；可为 null
     * @param statement 语句；可为 null
     * @param connection 连接；可为 null
     */
    private static void close(java.sql.ResultSet rows, PreparedStatement statement,
            Connection connection) {
        if (rows != null) {
            try {
                rows.close();
            } catch (SQLException ignored) {
                // 关闭失败无影响
            }
        }
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException ignored) {
                // 关闭失败无影响
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // 关闭失败无影响
            }
        }
    }
}
