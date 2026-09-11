package edu.seu.vcampus.server.bankmodule;

import edu.seu.vcampus.common.bank.BankService;
import edu.seu.vcampus.common.bank.entity.BankAccount;
import java.math.BigDecimal;

/**
 * 银行账户服务实现。
 *
 * <p>提供账户余额查询、扣款、充值等操作，供shop等模块通过用户UUID调用。</p>
 */
public class BankServiceImpl implements BankService {

    /** 数据访问对象。 */
    private final BankDao bankDao;

    /**
     * 使用默认的数据访问实现构造服务。
     */
    public BankServiceImpl() {
        this(new BankDaoImpl());
    }

    /**
     * 使用指定的数据访问对象构造服务（便于测试时注入替身）。
     *
     * @param bankDao 数据访问对象
     */
    public BankServiceImpl(BankDao bankDao) {
        this.bankDao = bankDao;
    }

    @Override
    public BankAccount getAccountByUserUuid(String userUuid) {
        if (isBlank(userUuid)) {
            return null;
        }
        return bankDao.findByUserUuid(userUuid);
    }

    @Override
    public BigDecimal getBalance(String userUuid) {
        BankAccount account = getAccountByUserUuid(userUuid);
        return account != null ? account.getBalance() : null;
    }

    @Override
    public boolean deduct(String userUuid, BigDecimal amount) {
        if (isBlank(userUuid) || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        BankAccount account = bankDao.findByUserUuid(userUuid);
        if (account == null) {
            return false;
        }

        // 检查账户状态和余额
        if (!account.isOperational()) {
            return false;
        }

        if (!account.hasSufficientBalance(amount)) {
            return false;
        }

        // 扣款
        try {
            account.withdraw(amount);
            return bankDao.update(account);
        } catch (IllegalArgumentException | IllegalStateException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deposit(String userUuid, BigDecimal amount) {
        if (isBlank(userUuid) || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        BankAccount account = bankDao.findByUserUuid(userUuid);
        if (account == null) {
            return false;
        }

        // 检查账户状态
        if (!account.isOperational()) {
            return false;
        }

        // 充值
        try {
            account.deposit(amount);
            return bankDao.update(account);
        } catch (IllegalArgumentException | IllegalStateException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean hasSufficientBalance(String userUuid, BigDecimal amount) {
        if (isBlank(userUuid) || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        BankAccount account = getAccountByUserUuid(userUuid);
        return account != null && account.hasSufficientBalance(amount);
    }

    @Override
    public boolean isAccountOperational(String userUuid) {
        BankAccount account = getAccountByUserUuid(userUuid);
        return account != null && account.isOperational();
    }

    /**
     * 判断字符串是否为空或仅含空白字符。
     *
     * @param value 待判断的字符串
     * @return 为 null、空串或全空白时返回 true
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
