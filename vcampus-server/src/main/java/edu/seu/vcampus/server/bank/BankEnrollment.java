package edu.seu.vcampus.server.bank;

import edu.seu.vcampus.common.bank.dto.BankAccountResponse;
import edu.seu.vcampus.common.bank.dto.BankOpenRequest;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import edu.seu.vcampus.server.user.SessionManager;

/** 使用现有共享会话管理器核验独立登录凭据，不修改用户模块。 */
final class BankEnrollment {
    private BankEnrollment() { }
    static BankAccountResponse open(BankService bank, String ownerUuid,
            String originalToken, BankOpenRequest request) {
        SessionManager sessions = SessionManager.getInstance();
        synchronized (sessions) {
            String token = request.getVerificationToken();
            if (token == null || token.equals(originalToken)) {
                throw new IllegalArgumentException("请重新验证校园登录密码");
            }
            SessionEntry verified = sessions.validate(token);
            if (verified == null || !ownerUuid.equals(verified.getUuid())
                    || !verified.getUsername().equals(request.getUsername())) {
                throw new IllegalArgumentException("校园身份验证失败，请重新填写");
            }
            try {
                return bank.openAccount(ownerUuid, request.getSalt(), request.getHash());
            } finally {
                sessions.invalidate(token);
            }
        }
    }
}
