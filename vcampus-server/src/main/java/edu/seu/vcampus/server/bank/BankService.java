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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 银行核心业务服务。
 *
 * <p>
 * 并发语义建立在「每个账户一个 {@link BankRecord} 锁对象」上：余额变动与流水记录在同一把锁内 完成，因此不需要数据库事务。持久化通过 {@link BankStore}
 * 外挂：生产装配用 {@link BankStoreJdbc}，构造时把账户、凭据、流水与流水序号读回来。
 *
 * <p>
 * 本类处在内存与数据库的<b>接缝</b>上：内存里始终有一份完整状态，写入时同步落库；因此启动时 必须把库里的状态读回内存，否则就是「重启后账户不见了」。
 */
public class BankService {
    private final Map<String, BankRecord> accounts = new ConcurrentHashMap<String, BankRecord>();

    private final AtomicLong transactionSequence = new AtomicLong(0L);

    /** 持久化后端；由调用方显式传入，没有默认值。 */
    private final BankStore m_store;

    /**
     * 指定持久化后端构造，并立即恢复已落库的状态。
     *
     * <p>
     * 依赖全部显式传入
     *
     * @param store 持久化后端，不能为 null
     * @throws IllegalArgumentException store 为 null
     */
    public BankService(BankStore store) {
        if (store == null) {
            throw new IllegalArgumentException("store must not be null");
        }
        m_store = store;
        restore();
    }

    /**
     * 银行账户业务号前缀。
     */
    private static final String ACCOUNT_ID_PREFIX = "A-";

    /**
     * 业务号里随机部分的字符数。
     *
     * <p>
     * {@code tblBankAccount.baId} 是 {@code VARCHAR(20)}（课程给定表结构），所以整个业务号不能超过 20 字符：前缀 2 个 + 这里 16 个
     * = 18，留两个字符余量。直接拼 36 字符的 uuid 会得到 38 字符的 业务号，落库时 MySQL 报
     * {@code Data too long for column 'baId'}，开户直接失败。
     */
    private static final int ACCOUNT_ID_RANDOM_LENGTH = 16;

    /**
     * 生成银行账户业务号（形如 {@code A-1f0a...}）。
     *
     * @return 新的业务号
     */
    private static String newAccountId() {
        String random = UUID.randomUUID().toString().replace("-", "");
        return ACCOUNT_ID_PREFIX + random.substring(0, ACCOUNT_ID_RANDOM_LENGTH);
    }

    /**
     * 从后端读回账户、凭据、流水与流水序号。
     */
    private void restore() {
        List<BankAccount> stored = m_store.loadAccounts();
        for (BankAccount account : stored) {
            BankRecord record = new BankRecord(account,
                    toCredential(m_store.loadCredential(account.getAccountId())));
            record.transactions.addAll(m_store.loadTransactions(account.getAccountId()));
            accounts.put(account.getOwnerUuid(), record);
        }
        transactionSequence.set(m_store.loadMaxSequence());
    }

    /**
     * 落库快照 → 内存凭据。
     *
     * @param stored 快照；可为 null
     * @return 内存凭据；未设置密码时返回 null
     */
    private static BankCredential toCredential(BankCredentialRecord stored) {
        if (stored == null || !stored.hasPassword()) {
            return null;
        }
        return BankCredential.create(stored.getSalt(), stored.getHash());
    }

    /**
     * 内存凭据 → 落库快照。
     *
     * @param credential 内存凭据；可为 null
     * @param frozenAt   挂失时间；null 表示未挂失
     * @return 落库快照
     */
    private static BankCredentialRecord toRecord(BankCredential credential, Date frozenAt) {
        if (credential == null) {
            return new BankCredentialRecord(null, null, null, frozenAt);
        }
        return new BankCredentialRecord(credential.getSalt(), credential.getHash(),
                new Date(), frozenAt);
    }

    /**
     * 把「当前是否挂失」固化成落库快照。
     *
     * @param record 账户记录
     * @return 落库快照
     */
    private static BankCredentialRecord currentCredential(BankRecord record) {
        Date frozenAt = record.account.getStatus() == BankAccountStatus.FROZEN
                ? new Date()
                : null;
        return toRecord(record.credential, frozenAt);
    }

    /**
     * 只查询已有账户；未开户时抛出 BankAccountNotOpenedException。
     * 
     * @param ownerUuid 已认证的用户编号
     * @return 账户只读响应
     */
    public BankAccountResponse queryAccount(String ownerUuid) {
        BankRecord record = requireAccount(ownerUuid);
        synchronized (record) {
            return BankAccountResponse.fromAccount(record.account);
        }
    }

    public BankAccountResponse freezeAccount(String ownerUuid, char[] password) {
        return setFrozen(ownerUuid, password, true);
    }

    public BankAccountResponse unfreezeAccount(String ownerUuid, char[] password) {
        return setFrozen(ownerUuid, password, false);
    }

    public BankAccountResponse changePassword(String ownerUuid, char[] oldPassword, byte[] salt,
            byte[] hash) {
        BankRecord record = requireAccount(ownerUuid);
        synchronized (record) {
            record.changePassword(oldPassword, salt, hash);
            m_store.updateCredential(record.account.getAccountId(), currentCredential(record));
            return BankAccountResponse.fromAccount(record.account);
        }
    }

    private BankAccountResponse setFrozen(String ownerUuid, char[] password, boolean frozen) {
        BankRecord record = requireAccount(ownerUuid);
        synchronized (record) {
            record.verifyPassword(password);
            record.account.setStatus(frozen ? BankAccountStatus.FROZEN : BankAccountStatus.NORMAL);
            record.account.setUpdatedAt(new Date());
            m_store.updateAccount(record.account);
            m_store.updateCredential(record.account.getAccountId(), currentCredential(record));
            return BankAccountResponse.fromAccount(record.account);
        }
    }

    /**
     * 为用户充值并记录充值流水。
     * 
     * @param ownerUuid 已认证的用户编号
     * @param amount    充值金额，必须大于零
     * @return 充值后的账户和本次流水
     */
    public BankRechargeResponse recharge(String ownerUuid, BigDecimal amount) {
        BankRecord record = requireAccount(ownerUuid);
        validateAmount(amount);
        synchronized (record) {
            if (record.account.getStatus() == BankAccountStatus.FROZEN) {
                throw new IllegalStateException("账户已挂失");
            }
            BigDecimal before = record.account.getBalance();
            record.account.deposit(amount);
            BankTransaction transaction = createTransaction(record.account,
                    BankTransactionType.RECHARGE, amount, before, null, "账户充值");
            record.transactions.add(transaction);
            m_store.updateAccount(record.account);
            m_store.appendTransaction(transaction);
            return new BankRechargeResponse(
                    BankAccountResponse.fromAccount(record.account), transaction);
        }
    }

    /**
     * 给已开户用户返现并记录流水，供商城等服务端模块调用。
     * 
     * @param ownerUuid      收款用户的稳定主键
     * @param amount         返现金额，必须大于零
     * @param relatedOrderId 关联订单编号，可为空
     * @param description    返现说明，可为空
     * @return 本次返现流水
     */
    public BankTransaction cashback(String ownerUuid, BigDecimal amount,
            String relatedOrderId, String description) {
        return changeBalance(ownerUuid, amount, BankTransactionType.CASHBACK,
                relatedOrderId, description);
    }

    /**
     * 扣减用户余额并记录消费流水，供商店等服务端模块调用。
     * 
     * @param ownerUuid      已认证的用户编号
     * @param amount         消费金额，必须大于零且不超过余额
     * @param relatedOrderId 关联订单编号，可为空
     * @param description    消费说明，可为空
     * @return 已创建的消费流水
     */
    public BankTransaction consume(String ownerUuid, BigDecimal amount,
            String relatedOrderId, String description) {
        BankRecord record = requireAccount(ownerUuid);
        synchronized (record) {
            if (record.credential != null) {
                throw new IllegalStateException("此账户扣款必须验证银行密码");
            }
            return changeBalance(ownerUuid, amount, BankTransactionType.CONSUMPTION,
                    relatedOrderId, description);
        }
    }

    /**
     * 按类型分页查询用户流水。
     * 
     * @param ownerUuid 已认证的用户编号
     * @param request   分页和类型条件
     * @return 当前页流水响应
     */
    public BankTransactionListResponse listTransactions(String ownerUuid,
            BankTransactionQueryRequest request) {
        BankRecord record = requireAccount(ownerUuid);
        synchronized (record) {
            return record.page(request);
        }
    }

    /**
     * 显式开户；重复或并发请求返回已有账户，不重置余额、状态和开户时间。
     * 
     * @param ownerUuid 已认证的稳定用户主键；由调用方校验用户存在及开户资格
     * @return 零余额新账户或已有账户的只读快照
     */
    public BankAccountResponse openAccount(String ownerUuid) {
        return openAccount(ownerUuid, null);
    }

    /**
     * @param ownerUuid 用户编号
     * @param salt      盐
     * @param hash      摘要
     * @return 开户结果
     */
    public BankAccountResponse openAccount(String ownerUuid, byte[] salt, byte[] hash) {
        return openAccount(ownerUuid, new BankCredential(salt, hash));
    }

    private BankAccountResponse openAccount(String ownerUuid, BankCredential credential) {
        requireOwnerUuid(ownerUuid);
        BankRecord record = accounts.get(ownerUuid);
        if (record == null) {
            Date now = new Date();
            BankRecord created = new BankRecord(new BankAccount(
                    newAccountId(), ownerUuid, BigDecimal.ZERO,
                    BankAccountStatus.NORMAL, now, now), credential);
            BankRecord previous = accounts.putIfAbsent(ownerUuid, created);
            record = previous == null ? created : previous;
            if (previous == null) {
                m_store.insertAccount(created.account, toRecord(credential, null));
            }
        }
        synchronized (record) {
            if (record.credential == null && credential != null) {
                record.credential = credential;
                m_store.updateCredential(record.account.getAccountId(),
                        currentCredential(record));
            }
            return BankAccountResponse.fromAccount(record.account);
        }
    }

    /** 在同一账户锁内完成消费或返现和流水记录。 */
    private BankTransaction changeBalance(String ownerUuid, BigDecimal amount,
            BankTransactionType type, String relatedOrderId, String description) {
        BankRecord record = requireAccount(ownerUuid);
        validateAmount(amount);
        synchronized (record) {
            if (record.account.getStatus() == BankAccountStatus.FROZEN) {
                throw new IllegalStateException("账户已挂失");
            }
            BigDecimal before = record.account.getBalance();
            if (type == BankTransactionType.CONSUMPTION) {
                record.account.withdraw(amount);
            } else {
                record.account.deposit(amount);
            }
            BankTransaction transaction = createTransaction(record.account,
                    type, amount, before, relatedOrderId, description);
            record.transactions.add(transaction);
            m_store.updateAccount(record.account);
            m_store.appendTransaction(transaction);
            return transaction;
        }
    }

    /**
     * 带银行密码扣款；重复订单只返回原流水，金额不符拒绝。
     * 
     * @param ownerUuid      用户编号
     * @param password       银行密码
     * @param amount         金额
     * @param relatedOrderId 必填订单号
     * @param description    说明
     * @return 消费流水
     */
    public BankTransaction consumeWithPassword(String ownerUuid, char[] password,
            BigDecimal amount, String relatedOrderId, String description) {
        BankRecord record = requireAccount(ownerUuid);
        validateAmount(amount);
        if (relatedOrderId == null || relatedOrderId.trim().isEmpty()) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        synchronized (record) {
            BankTransaction existing = record.verifyPayment(password, amount, relatedOrderId);
            if (existing != null) {
                return existing;
            }
            return changeBalance(ownerUuid, amount, BankTransactionType.CONSUMPTION,
                    relatedOrderId, description);
        }
    }

    private BankRecord requireAccount(String ownerUuid) {
        requireOwnerUuid(ownerUuid);
        BankRecord record = accounts.get(ownerUuid);
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

    private static void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
    }

    private static void requireOwnerUuid(String ownerUuid) {
        if (ownerUuid == null || ownerUuid.trim().length() == 0) {
            throw new IllegalArgumentException("ownerUuid must not be blank");
        }
    }

    /**
     * 查询指定用户的账户；未开户返回 null，不抛未开户异常。
     *
     * @param ownerUuid 用户编号
     * @return 账户快照；未开户为 null
     */
    public BankAccountResponse findAccount(String ownerUuid) {
        requireOwnerUuid(ownerUuid);
        BankRecord record = accounts.get(ownerUuid);
        if (record == null) {
            return null;
        }
        synchronized (record) {
            return BankAccountResponse.fromAccount(record.account);
        }
    }

    /**
     * 管理端冻结或解冻指定账户，不校验目标用户的银行密码。
     *
     * @param ownerUuid 用户编号
     * @param frozen    true 冻结、false 解冻
     * @return 变更后的账户快照
     */
    public BankAccountResponse adminSetFrozen(String ownerUuid, boolean frozen) {
        BankRecord record = requireAccount(ownerUuid);
        synchronized (record) {
            record.account.setStatus(frozen
                    ? BankAccountStatus.FROZEN
                    : BankAccountStatus.NORMAL);
            record.account.setUpdatedAt(new Date());
            // 管理端与用户端改的是同一份状态，落库这一步不能漏：
            // 内存模式下漏了看不出来（界面读的就是内存），jdbc 模式下重启就丢了
            m_store.updateAccount(record.account);
            m_store.updateCredential(record.account.getAccountId(), currentCredential(record));
            return BankAccountResponse.fromAccount(record.account);
        }
    }

    /**
     * 管理端重置指定账户的银行密码，不校验旧密码；换盐换摘要后失败计数与锁定自然清零。
     *
     * @param ownerUuid 用户编号
     * @param salt      盐
     * @param hash      加盐摘要
     * @return 账户快照
     */
    public BankAccountResponse adminResetPassword(String ownerUuid, byte[] salt, byte[] hash) {
        BankRecord record = requireAccount(ownerUuid);
        synchronized (record) {
            record.credential = BankCredential.create(salt, hash);
            m_store.updateCredential(record.account.getAccountId(), currentCredential(record));
            return BankAccountResponse.fromAccount(record.account);
        }
    }
}
