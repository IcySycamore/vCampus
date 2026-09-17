package edu.seu.vcampus.common.bank.dto;

import edu.seu.vcampus.common.bank.entity.BankAccountStatus;
import edu.seu.vcampus.common.user.entity.Role;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 管理端看到的账户条目：用户信息与银行账户信息的联合视图。
 *
 * <p>银行账户按用户主键存放，只有一串编号，无法直接展示给管理员，因此把用户表的登录名、
 * 姓名、角色与账户快照拼在一起。用户尚未开户时 {@code opened} 为 false，账户相关字段全为 null。</p>
 */
public final class BankAdminAccountView implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    private final String username;
    private final String displayName;
    private final Role role;
    private final boolean userEnabled;
    private final boolean opened;
    private final String accountId;
    private final BigDecimal balance;
    private final BankAccountStatus status;
    private final Date createdAt;
    private final Date updatedAt;

    /**
     * 由用户信息与账户快照构造视图。
     *
     * @param username 登录名
     * @param displayName 姓名；可为 null
     * @param role 角色；可为 null
     * @param userEnabled 用户是否处于启用状态
     * @param account 账户快照；null 表示该用户尚未开户
     */
    public BankAdminAccountView(String username, String displayName, Role role,
            boolean userEnabled, BankAccountResponse account) {
        this.username = username;
        this.displayName = displayName;
        this.role = role;
        this.userEnabled = userEnabled;
        this.opened = account != null;
        this.accountId = account == null ? null : account.getAccountId();
        this.balance = account == null ? null : account.getBalance();
        this.status = account == null ? null : account.getStatus();
        this.createdAt = account == null ? null : account.getCreatedAt();
        this.updatedAt = account == null ? null : account.getUpdatedAt();
    }

    /** @return 登录名 */
    public String getUsername() {
        return username;
    }

    /** @return 姓名；可能为 null */
    public String getDisplayName() {
        return displayName;
    }

    /** @return 角色；可能为 null */
    public Role getRole() {
        return role;
    }

    /** @return 用户是否启用 */
    public boolean isUserEnabled() {
        return userEnabled;
    }

    /** @return 是否已开户 */
    public boolean isOpened() {
        return opened;
    }

    /** @return 账户号；未开户为 null */
    public String getAccountId() {
        return accountId;
    }

    /** @return 余额；未开户为 null */
    public BigDecimal getBalance() {
        return balance;
    }

    /** @return 账户状态；未开户为 null */
    public BankAccountStatus getStatus() {
        return status;
    }

    /** @return 开户时间；未开户为 null */
    public Date getCreatedAt() {
        return createdAt == null ? null : new Date(createdAt.getTime());
    }

    /** @return 最近变动时间；未开户为 null */
    public Date getUpdatedAt() {
        return updatedAt == null ? null : new Date(updatedAt.getTime());
    }
}
