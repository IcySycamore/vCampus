package edu.seu.vcampus.server.bank;

import edu.seu.vcampus.common.bank.dto.BankAccountResponse;
import edu.seu.vcampus.common.bank.dto.BankRechargeResponse;
import edu.seu.vcampus.common.bank.dto.BankTransactionListResponse;
import edu.seu.vcampus.common.bank.dto.BankTransactionQueryRequest;
import edu.seu.vcampus.common.bank.entity.BankAccount;
import edu.seu.vcampus.common.bank.entity.BankAccountStatus;
import edu.seu.vcampus.common.bank.entity.BankTransaction;
import edu.seu.vcampus.common.bank.entity.BankTransactionType;
import edu.seu.vcampus.common.bank.exception.BankAccountNotOpenedException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 银行核心业务服务的内存实现。
 *
 * <p>当前项目尚未提供银行 DAO 和数据表，因此本实现使用线程安全的内存存储，
 * 并将账户余额更新和流水写入放在同一把账户锁内。后续接入数据库时可保留本类
 * 的业务接口，把存储部分替换为事务 DAO。</p>
 */
public class BankService {
    private final Map<Long, AccountRecord> accounts =
            new ConcurrentHashMap<Long, AccountRecord>();

    private final AtomicLong transactionSequence = new AtomicLong(0L);

    /**
     * 只查询已有账户；未开户时抛出 BankAccountNotOpenedException。
     * @param userId 已认证的用户编号
     * @return 账户只读响应
     */
    public BankAccountResponse queryAccount(Long userId) {
        AccountRecord record = requireAccount(userId);
        synchronized (record) {
            return BankAccountResponse.fromAccount(record.account);
        }
    }
    /**
     * 为用户充值并记录充值流水。
     * @param userId 已认证的用户编号
     * @param amount 充值金额，必须大于零
     * @return 充值后的账户和本次流水
     */
    public BankRechargeResponse recharge(Long userId, BigDecimal amount) {
        AccountRecord record = requireAccount(userId);
        validateAmount(amount);
        synchronized (record) {
            BigDecimal before = record.account.getBalance();
            record.account.deposit(amount);
            BankTransaction transaction = createTransaction(record.account,
                    BankTransactionType.RECHARGE, amount, before, null, "账户充值");
            record.transactions.add(transaction);
            return new BankRechargeResponse(
                    BankAccountResponse.fromAccount(record.account), transaction);
        }
    }
    /**
     * 给已开户用户返现并记录流水，供商城等服务端模块调用。
     * @param userId 收款用户的稳定主键
     * @param amount 返现金额，必须大于零
     * @param relatedOrderId 关联订单编号，可为空
     * @param description 返现说明，可为空
     * @return 本次返现流水
     */
    public BankTransaction cashback(Long userId, BigDecimal amount,
            String relatedOrderId, String description) {
        return changeBalance(userId, amount, BankTransactionType.CASHBACK,
                relatedOrderId, description);
    }
    /**
     * 扣减用户余额并记录消费流水，供商店等服务端模块调用。
     * @param userId 已认证的用户编号
     * @param amount 消费金额，必须大于零且不超过余额
     * @param relatedOrderId 关联订单编号，可为空
     * @param description 消费说明，可为空
     * @return 已创建的消费流水
     */
    public BankTransaction consume(Long userId, BigDecimal amount,
            String relatedOrderId, String description) {
        return changeBalance(userId, amount, BankTransactionType.CONSUMPTION,
                relatedOrderId, description);
    }
    /**
     * 按类型分页查询用户流水。
     * @param userId 已认证的用户编号
     * @param request 分页和类型条件
     * @return 当前页流水响应
     */
    public BankTransactionListResponse listTransactions(Long userId,
            BankTransactionQueryRequest request) {
        AccountRecord record = requireAccount(userId);
        BankTransactionQueryRequest query = request == null
                ? new BankTransactionQueryRequest() : request;
        synchronized (record) {
            List<BankTransaction> filtered = filterTransactions(record.transactions,
                    query.getType());
            long total = filtered.size();
            long offset = ((long) query.getPageNumber() - 1L) * query.getPageSize();
            List<BankTransaction> page = new ArrayList<BankTransaction>();
            if (offset < total) {
                long end = Math.min(total, offset + query.getPageSize());
                for (long index = offset; index < end; index++) {
                    page.add(filtered.get((int) index));
                }
            }
            return new BankTransactionListResponse(page, query.getPageNumber(),
                    query.getPageSize(), total);
        }
    }
    /**
     * 显式开户；重复或并发请求返回已有账户，不重置余额、状态和开户时间。
     * @param userId 已认证的稳定用户主键；由调用方校验用户存在及开户资格
     * @return 零余额新账户或已有账户的只读快照
     */
    public BankAccountResponse openAccount(Long userId) {
        requireUserId(userId);
        AccountRecord record = accounts.get(userId);
        if (record == null) {
            Date now = new Date();
            AccountRecord created = new AccountRecord(new BankAccount(
                    "A-" + UUID.randomUUID(), userId, BigDecimal.ZERO,
                    BankAccountStatus.NORMAL, now, now));
            AccountRecord previous = accounts.putIfAbsent(userId, created);
            record = previous == null ? created : previous;
        }
        synchronized (record) {
            return BankAccountResponse.fromAccount(record.account);
        }
    }
    /** 在同一账户锁内完成消费或返现和流水记录。 */
    private BankTransaction changeBalance(Long userId, BigDecimal amount,
            BankTransactionType type, String relatedOrderId, String description) {
        AccountRecord record = requireAccount(userId);
        validateAmount(amount);
        synchronized (record) {
            BigDecimal before = record.account.getBalance();
            if (type == BankTransactionType.CONSUMPTION) {
                record.account.withdraw(amount);
            } else {
                record.account.deposit(amount);
            }
            BankTransaction transaction = createTransaction(record.account,
                    type, amount, before, relatedOrderId, description);
            record.transactions.add(transaction);
            return transaction;
        }
    }
    private AccountRecord requireAccount(Long userId) {
        requireUserId(userId);
        AccountRecord record = accounts.get(userId);
        if (record == null) {
            throw new BankAccountNotOpenedException();
        }
        return record;
    }
    private BankTransaction createTransaction(BankAccount account,
            BankTransactionType type, BigDecimal amount, BigDecimal before,
            String relatedOrderId, String description) {
        BigDecimal after = account.getBalance();
        return new BankTransaction("T-" + transactionSequence.incrementAndGet(),
                account.getAccountId(), type, amount, before, after,
                relatedOrderId, description, new Date());
    }
    private static List<BankTransaction> filterTransactions(
            List<BankTransaction> source, BankTransactionType type) {
        List<BankTransaction> result = new ArrayList<BankTransaction>();
        for (BankTransaction transaction : source) {
            if (type == null || type == transaction.getType()) {
                result.add(transaction);
            }
        }
        return result;
    }
    private static void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
    }
    private static void requireUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
    }
    private static final class AccountRecord {
        private final BankAccount account;
        private final List<BankTransaction> transactions =
                new ArrayList<BankTransaction>();
        private AccountRecord(BankAccount account) {
            this.account = account;
        }
    }
}
