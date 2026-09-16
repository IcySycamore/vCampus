package edu.seu.vcampus.client.bank;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.client.user.UserService;
import edu.seu.vcampus.common.bank.dto.BankOpenRequest;
import edu.seu.vcampus.common.bank.security.BankPassword;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import java.util.Arrays;

/** 一次性校园密码复核；临时 UserService 不替换主窗口共享会话。 */
final class BankOpening {
    private final UserService verification;
    BankOpening(ClientMessageDispatcher dispatcher) {
        verification = new UserService(dispatcher);
    }
    BankOpenRequest verify(UserService users, String name, char[] login, char[] password) {
        SessionEntry current = users.currentSession();
        if (current == null) { throw new ApiException(StatusCode.UNAUTHORIZED); }
        if (!current.getUsername().equals(name) || login == null || login.length == 0) {
            throw new ApiException(StatusCode.BAD_REQUEST, "请输入当前校园账号及登录密码");
        }
        byte[] salt = BankPassword.newSalt();
        byte[] hash;
        try {
            hash = BankPassword.derive(password, salt);
        } catch (IllegalArgumentException e) {
            throw new ApiException(StatusCode.BAD_REQUEST, e.getMessage());
        }
        try {
            verification.login(name, Role.fromDisplayName(current.getRole()), new String(login));
            if (verification.currentSession() == null
                    || !current.getUuid().equals(verification.currentSession().getUuid())) {
                throw new ApiException(StatusCode.FORBIDDEN);
            }
            return new BankOpenRequest(name, verification.currentToken(), salt, hash);
        } finally {
            Arrays.fill(hash, (byte) 0);
        }
    }
    void close() {
        if (verification.currentToken() != null) {
            try { verification.logout(); } catch (ApiException ignored) {
                // 开户服务端已消费令牌时，注销会回 401；临时本地会话仍会清除。
            }
        }
    }
}
