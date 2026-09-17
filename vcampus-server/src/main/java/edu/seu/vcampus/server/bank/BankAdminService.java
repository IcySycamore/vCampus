package edu.seu.vcampus.server.bank;

import edu.seu.vcampus.common.bank.dto.BankAdminAccountView;
import edu.seu.vcampus.common.bank.dto.BankAdminQuery;
import edu.seu.vcampus.common.bank.dto.BankTransactionListResponse;
import edu.seu.vcampus.common.bank.dto.BankTransactionQueryRequest;
import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.server.user.UserRepository;
import edu.seu.vcampus.server.user.UserRepository.Credential;

import java.util.ArrayList;
import java.util.List;

/**
 * 银行管理端业务（管理轨）：把用户信息与账户快照拼成管理员可读的视图。
 *
 * <p>银行账户以用户主键为键存放，单独列出只有一串编号，无法按学号或姓名检索。因此本类以
 * {@link UserRepository#findAll()} 为驱动（按登录名升序，分页稳定），逐个取账户快照。
 * 仓库必须由应用组装层注入：模块自行 new 会拿到与认证不同的实例（见 {@code AuthModule}的警告）。</p>
 *
 * <p>本类不做权限判定，准入由处理器层按 {@code Role.ADMIN} 把关。</p>
 */
public class BankAdminService {

    /** 银行核心服务。 */
    private final BankService m_bank;

    /** 共享用户仓库。 */
    private final UserRepository m_users;

    /**
     * 创建管理端服务。
     *
     * @param bank 银行核心服务
     * @param users 共享用户仓库
     */
    public BankAdminService(BankService bank, UserRepository users) {
        if (bank == null || users == null) {
            throw new IllegalArgumentException("bank and users are required");
        }
        this.m_bank = bank;
        this.m_users = users;
    }

    /**
     * 分页列出全部用户的银行账户。
     *
     * @param query 关键字与分页条件；null 表示不过滤
     * @return 当前页账户视图
     */
    public PageResponse<BankAdminAccountView> listAccounts(BankAdminQuery query) {
        BankAdminQuery condition = query == null ? new BankAdminQuery() : query;
        List<Credential> matched = new ArrayList<Credential>();
        for (Credential credential : m_users.findAll()) {
            if (!isAdmin(credential) && matches(credential, condition.getKeyword())) {
                matched.add(credential);
            }
        }
        int offset = PageResponse.offsetOf(condition.getPageNumber(), condition.getPageSize());
        List<BankAdminAccountView> page = new ArrayList<BankAdminAccountView>();
        for (int i = offset; i < matched.size()
                && page.size() < condition.getPageSize(); i++) {
            page.add(toView(matched.get(i)));
        }
        return new PageResponse<BankAdminAccountView>(page, matched.size(),
                condition.getPageNumber(), condition.getPageSize());
    }

    /**
     * 按用户名查看单个账户。
     *
     * @param username 登录名
     * @return 账户视图；用户不存在返回 null
     */
    public BankAdminAccountView viewAccount(String username) {
        Credential credential = username == null ? null : m_users.findByUsername(username);
        return credential == null ? null : toView(credential);
    }

    /**
     * 分页查询指定用户的资金流水。
     *
     * @param username 登录名
     * @param query 分页与类型条件
     * @return 流水分页；用户不存在返回 null，未开户抛未开户异常
     */
    public BankTransactionListResponse listTransactions(String username,
            BankTransactionQueryRequest query) {
        Credential credential = username == null ? null : m_users.findByUsername(username);
        if (credential == null) {
            return null;
        }
        return m_bank.listTransactions(credential.getUuid(), query);
    }

    /**
     * 冻结或解冻指定用户的账户，不要求目标用户的银行密码。
     *
     * @param username 登录名
     * @param frozen true 冻结、false 解冻
     * @return 变更后的账户视图；用户不存在返回 null
     */
    public BankAdminAccountView setFrozen(String username, boolean frozen) {
        Credential credential = username == null ? null : m_users.findByUsername(username);
        if (credential == null) {
            return null;
        }
        m_bank.adminSetFrozen(credential.getUuid(), frozen);
        return toView(credential);
    }

    /**
     * 重置指定用户的银行密码，不校验旧密码。
     *
     * @param username 登录名
     * @param salt 盐
     * @param hash 加盐摘要
     * @return 账户视图；用户不存在返回 null
     */
    public BankAdminAccountView resetPassword(String username, byte[] salt, byte[] hash) {
        Credential credential = username == null ? null : m_users.findByUsername(username);
        if (credential == null) {
            return null;
        }
        m_bank.adminResetPassword(credential.getUuid(), salt, hash);
        return toView(credential);
    }

    /** 把一条用户凭证与账户快照拼成管理视图。 */
    private BankAdminAccountView toView(Credential credential) {
        return new BankAdminAccountView(credential.getUsername(),
                credential.getDisplayName(), Role.fromDisplayName(credential.getRole()),
                credential.isEnabled(), m_bank.findAccount(credential.getUuid()));
    }

    /** 管理员不需要银行账户，不参与账户管理列表。 */
    private boolean isAdmin(Credential credential) {
        return Role.ADMIN == Role.fromDisplayName(credential.getRole());
    }

    /** 关键词匹配登录名或姓名；空关键词匹配全部。 */
    private boolean matches(Credential credential, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }
        String trimmed = keyword.trim();
        return contains(credential.getUsername(), trimmed)
                || contains(credential.getDisplayName(), trimmed);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }
}
