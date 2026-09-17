package edu.seu.vcampus.server.bank;

import edu.seu.vcampus.common.bank.dto.BankCampusPasswordChallengeRequest;
import edu.seu.vcampus.common.bank.dto.BankCampusPasswordVerifyRequest;
import edu.seu.vcampus.common.bank.dto.BankPasswordChangeRequest;
import edu.seu.vcampus.common.bank.entity.BankAccountStatus;
import edu.seu.vcampus.common.bank.security.BankPassword;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import edu.seu.vcampus.common.util.Sha256Util;
import edu.seu.vcampus.server.network.ServerMessageDispatcher;
import edu.seu.vcampus.server.user.AuthService;
import edu.seu.vcampus.server.user.InMemoryUserRepository;
import edu.seu.vcampus.server.user.NonceManager;
import edu.seu.vcampus.server.user.SessionManager;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** 修改银行密码的独立校园复核、银行密码校验和一次性 token 生命周期测试。 */
class BankPasswordChangeFlowTest {
    @Test
    void verifiesCampusThenBankPasswordAndConsumesToken() {
        InMemoryUserRepository users = new InMemoryUserRepository();
        final SessionManager sessions = new SessionManager();
        AuthService auth = new AuthService(users, new NonceManager(), sessions);
        auth.register("bank-flow", "流程用户", "campus-old", "学生");
        SessionEntry primaryEntry = sessions.validate(
                sessions.create(users.findByUsername("bank-flow").getUuid(), "bank-flow", "流程用户", "学生"));
        String primary = sessions.create(primaryEntry.getUuid(), "bank-flow", "流程用户", "学生");
        BankService bank = new BankService();
        char[] old = "bank-old".toCharArray();
        byte[] oldSalt = BankPassword.newSalt();
        bank.openAccount(primaryEntry.getUuid(), oldSalt, BankPassword.derive(old, oldSalt));
        ServerMessageDispatcher dispatcher = new ServerMessageDispatcher();
        BankModule.register(dispatcher, bank, auth, new BankIdentityResolver() {
            @Override public String resolveOwnerUuid(Message request) {
                SessionEntry entry = sessions.validate(request.getToken());
                return entry == null ? null : entry.getUuid();
            }
        });

        Message challengeRequest = message(Command.BANK_PASSWORD_VERIFY_CHALLENGE, primary,
                new BankCampusPasswordChallengeRequest("bank-flow"));
        Message challengeResponse = dispatch(dispatcher, challengeRequest);
        assertEquals(StatusCode.SUCCESS, challengeResponse.getStatusCode());
        LoginChallenge challenge = (LoginChallenge) challengeResponse.getData();
        Message badCampus = dispatch(dispatcher, message(Command.BANK_PASSWORD_VERIFY, primary,
                new BankCampusPasswordVerifyRequest("bank-flow", "invalid-proof")));
        assertEquals(StatusCode.BANK_CAMPUS_PASSWORD_INVALID, badCampus.getStatusCode());
        challengeResponse = dispatch(dispatcher, challengeRequest);
        challenge = (LoginChallenge) challengeResponse.getData();
        String proof = Sha256Util.sha256Hex(challenge.m_nonce
                + Sha256Util.sha256Hex(challenge.m_salt + "campus-old"));
        Message verifyResponse = dispatch(dispatcher, message(Command.BANK_PASSWORD_VERIFY, primary,
                new BankCampusPasswordVerifyRequest("bank-flow", proof)));
        assertEquals(StatusCode.SUCCESS, verifyResponse.getStatusCode());
        String verificationToken = (String) verifyResponse.getData();
        byte[] newSalt = BankPassword.newSalt();
        char[] next = "bank-new".toCharArray();
        Message changed = dispatch(dispatcher, message(Command.BANK_PASSWORD_CHANGE, primary,
                new BankPasswordChangeRequest("bank-flow", verificationToken, old, newSalt,
                        BankPassword.derive(next, newSalt))));
        assertEquals(StatusCode.SUCCESS, changed.getStatusCode());
        assertEquals(BankAccountStatus.NORMAL, bank.queryAccount(primaryEntry.getUuid()).getStatus());
        assertEquals(null, sessions.validate(verificationToken));

        Message replay = dispatch(dispatcher, message(Command.BANK_PASSWORD_CHANGE, primary,
                new BankPasswordChangeRequest("bank-flow", verificationToken, old, newSalt,
                        BankPassword.derive(next, newSalt))));
        assertEquals(StatusCode.BANK_CAMPUS_PASSWORD_INVALID, replay.getStatusCode());
        assertNotNull(bank.queryAccount(primaryEntry.getUuid()));
    }

    private static Message message(int command, String token, Object data) {
        Message message = new Message(command, data);
        message.setToken(token);
        return message;
    }

    private static Message dispatch(ServerMessageDispatcher dispatcher, Message request) {
        final List<Message> replies = new ArrayList<Message>();
        dispatcher.dispatch(request, new MessageSender() {
            @Override public void send(Message response) { replies.add(response); }
        });
        assertEquals(1, replies.size());
        return replies.get(0);
    }
}
