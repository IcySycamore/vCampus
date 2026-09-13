package edu.seu.vcampus.server.user;

import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.user.dto.BatchResult;
import edu.seu.vcampus.common.user.dto.UserQuery;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.User;
import edu.seu.vcampus.server.user.UserRepository.Credential;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户管理服务（命令 104、106、107、108）：账户的分页查询、编辑、启停与注销。
 *
 * <p>
 * 与 {@link AuthService}（认证：注册/登录/改密）分工：本类只处理「管理轨」的账户维护，
 * 不涉及口令与令牌。权限判定在处理器层（{@code Capability.USER_MANAGE}），本类只做业务规则。
 *
 * <p>
 * 注销是「删账户 + 撤销各模块档案」的组合：先按 uuid 撤销档案，再删除账户， 顺序保证不会出现「档案指向已不存在的账户」。
 */
public class UserAdminService {

    /** 用户凭证存储。 */
    private final UserRepository m_users;

    /** 开户钩子登记表（注销时撤销各模块档案）。 */
    private final AccountProvisioning m_provisioning;

    /**
     * 构造用户管理服务。
     *
     * @param users 用户凭证存储
     * @param provisioning 开户钩子登记表；null 表示不撤销档案
     * @throws IllegalArgumentException users 为 null
     */
    public UserAdminService(UserRepository users, AccountProvisioning provisioning) {
        if (users == null) {
            throw new IllegalArgumentException("users must not be null");
        }
        this.m_users = users;
        this.m_provisioning = provisioning;
    }

    /**
     * 分页查询用户（命令 106），支持关键词（登录名/姓名）、角色与启用状态过滤。
     *
     * @param query 查询条件；null 表示查询全部（默认分页）
     * @return 分页结果
     */
    public PageResponse<User> listUsers(UserQuery query) {
        UserQuery condition = query == null ? new UserQuery() : query;
        List<Credential> matched = new ArrayList<Credential>();
        for (Credential credential : m_users.findAll()) {
            if (matches(credential, condition)) {
                matched.add(credential);
            }
        }
        int offset = PageResponse.offsetOf(condition.getPageNumber(), condition.getPageSize());
        List<User> page = new ArrayList<User>();
        for (int i = offset; i < matched.size() && page.size() < condition.getPageSize(); i++) {
            page.add(toUser(matched.get(i)));
        }
        return new PageResponse<User>(page, matched.size(), condition.getPageNumber(),
                condition.getPageSize());
    }

    /**
     * 编辑用户（命令 107）：只改姓名，<b>不改角色</b>。
     *
     * @param userName 目标登录名
     * @param displayName 新姓名；null 或空表示不改
     * @return 是否命中用户
     */
    public boolean updateUser(String userName, String displayName) {
        if (userName == null || m_users.findByUsername(userName) == null) {
            return false;
        }
        if (displayName != null && displayName.trim().length() > 0) {
            m_users.update(userName, displayName.trim());
        }
        return true;
    }

    /**
     * 启用/禁用用户（命令 108）。禁用后该账号无法登录（状态码 P102）。
     *
     * @param userName 目标登录名
     * @param enabled 目标状态
     * @return 是否命中用户
     */
    public boolean setEnabled(String userName, boolean enabled) {
        if (userName == null || !m_users.exists(userName)) {
            return false;
        }
        m_users.setEnabled(userName, enabled);
        return true;
    }

    /**
     * 注销账户（命令 104）：撤销各模块档案后删除账户。
     *
     * @param userName 目标登录名
     * @return 是否命中用户
     */
    public boolean unregister(String userName) {
        Credential credential = m_users.findByUsername(userName);
        if (credential == null) {
            return false;
        }
        if (m_provisioning != null) {
            m_provisioning.revoke(credential.getUuid());
        }
        m_users.delete(userName);
        return true;
    }

    /**
     * 批量注销（命令 105）：逐条执行，逐条记账，不做全批回滚。
     *
     * @param userNames 登录名列表；null 或空返回全成功 0 条
     * @return 批量结果（不存在的账号计入失败明细）
     */
    public BatchResult unregisterAll(List<String> userNames) {
        int success = 0;
        List<BatchResult.Failure> failures = new ArrayList<BatchResult.Failure>();
        if (userNames != null) {
            for (String userName : userNames) {
                if (userName == null || userName.trim().length() == 0) {
                    continue;
                }
                if (unregister(userName.trim())) {
                    success++;
                } else {
                    failures.add(new BatchResult.Failure(userName, "账号不存在"));
                }
            }
        }
        return new BatchResult(success, failures);
    }

    private boolean matches(Credential credential, UserQuery query) {
        Role role = query.getRole();
        if (role != null && !role.getDisplayName().equals(credential.getRole())) {
            return false;
        }
        Boolean enabled = query.getEnabled();
        if (enabled != null && enabled.booleanValue() != credential.isEnabled()) {
            return false;
        }
        String keyword = query.getKeyword();
        if (keyword == null || keyword.trim().length() == 0) {
            return true;
        }
        String trimmed = keyword.trim();
        return contains(credential.getUsername(), trimmed)
                || contains(credential.getDisplayName(), trimmed);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }

    private User toUser(Credential credential) {
        User user = new User(credential.getUsername(), credential.getDisplayName(), null,
                Role.fromDisplayName(credential.getRole()));
        user.setUuid(credential.getUuid());
        user.setEnabled(credential.isEnabled());
        return user;
    }
}
