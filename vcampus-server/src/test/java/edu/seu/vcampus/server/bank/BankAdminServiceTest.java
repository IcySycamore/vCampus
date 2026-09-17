package edu.seu.vcampus.server.bank;

import edu.seu.vcampus.common.bank.dto.BankAdminAccountView;
import edu.seu.vcampus.common.bank.dto.BankAdminQuery;
import edu.seu.vcampus.common.bank.dto.BankTransactionListResponse;
import edu.seu.vcampus.common.bank.dto.BankTransactionQueryRequest;
import edu.seu.vcampus.common.bank.entity.BankAccountStatus;
import edu.seu.vcampus.common.bank.exception.BankAccountNotOpenedException;
import edu.seu.vcampus.common.bank.security.BankPassword;
import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.server.user.InMemoryUserRepository;
import edu.seu.vcampus.server.user.UserRepository;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 银行管理端服务：联合查询、关键字过滤、分页、冻结与重置密码。 */
class BankAdminServiceTest {

    private static final String ZHAO_UUID = "uuid-zhao";
    private static final String QIAN_UUID = "uuid-qian";

    private InMemoryUserRepository users;
    private BankService bank;
    private BankAdminService admin;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        bank = new BankService();
        admin = new BankAdminService(bank, users);
        save("zhao001", ZHAO_UUID, "赵一", "学生");
        save("qian002", QIAN_UUID, "钱二", "教师");
    }

    private void save(String username, String uuid, String displayName, String role) {
        users.save(new UserRepository.Credential(username, uuid, displayName,
                "salt-placeholder", "hash-placeholder", role, true));
    }

    /** 未开户与已开户的用户同列一页，且账户字段只在已开户时有值。 */
    @Test
    void listsUsersWithAndWithoutAccounts() {
        bank.openAccount(ZHAO_UUID);
        PageResponse<BankAdminAccountView> page = admin.listAccounts(new BankAdminQuery());
        assertEquals(2L, page.getTotal());
        assertEquals("qian002", page.getItems().get(0).getUsername());
        assertFalse(page.getItems().get(0).isOpened());
        assertNull(page.getItems().get(0).getBalance());
        assertEquals("zhao001", page.getItems().get(1).getUsername());
        assertTrue(page.getItems().get(1).isOpened());
        assertEquals(BigDecimal.ZERO, page.getItems().get(1).getBalance());
    }

    /** 关键字同时匹配登录名与姓名。 */
    @Test
    void filtersByKeywordInUsernameOrDisplayName() {
        assertEquals(1L, admin.listAccounts(new BankAdminQuery("qian", 1, 20)).getTotal());
        assertEquals(1L, admin.listAccounts(new BankAdminQuery("赵一", 1, 20)).getTotal());
        assertEquals(0L, admin.listAccounts(new BankAdminQuery("sun", 1, 20)).getTotal());
    }

    /** 分页按登录名升序且总数正确。 */
    @Test
    void pagesAccountsInUsernameOrder() {
        PageResponse<BankAdminAccountView> first =
                admin.listAccounts(new BankAdminQuery(null, 1, 1));
        assertEquals(1, first.getItems().size());
        assertEquals(2L, first.getTotal());
        assertEquals(2L, first.getTotalPages());
        PageResponse<BankAdminAccountView> second =
                admin.listAccounts(new BankAdminQuery(null, 2, 1));
        assertEquals(1, second.getItems().size());
        assertEquals("zhao001", second.getItems().get(0).getUsername());
    }

    /** 未知用户的管理操作一律返回 null，由处理器转404。 */
    @Test
    void unknownUserYieldsNull() {
        assertNull(admin.viewAccount("nobody"));
        assertNull(admin.setFrozen("nobody", true));
        assertNull(admin.listTransactions("nobody", new BankTransactionQueryRequest()));
        assertNull(admin.resetPassword("nobody", new byte[16], new byte[32]));
    }

    /** 管理员冻结账户无需目标用户的银行密码，冻结后本人不能再充值。 */
    @Test
    void freezesAccountWithoutTargetPassword() {
        byte[] salt = BankPassword.newSalt();
        bank.openAccount(ZHAO_UUID, salt,
                BankPassword.derive("bank-pass!!".toCharArray(), salt));
        BankAdminAccountView frozen = admin.setFrozen("zhao001", true);
        assertEquals(BankAccountStatus.FROZEN, frozen.getStatus());
        assertThrows(IllegalStateException.class, new Executable() {
            @Override
            public void execute() {
                bank.recharge(ZHAO_UUID, new BigDecimal("10.00"));
            }
        });
        admin.setFrozen("zhao001", false);
        assertEquals(BankAccountStatus.NORMAL,
                bank.queryAccount(ZHAO_UUID).getStatus());
    }

    /** 重置后旧密码失效、新密码可用，且不需要旧密码。 */
    @Test
    void resetsBankPasswordWithoutOldOne() {
        byte[] oldSalt = BankPassword.newSalt();
        bank.openAccount(ZHAO_UUID, oldSalt,
                BankPassword.derive("old-pass!!".toCharArray(), oldSalt));
        byte[] newSalt = BankPassword.newSalt();
        admin.resetPassword("zhao001", newSalt,
                BankPassword.derive("new-pass!!".toCharArray(), newSalt));
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                bank.freezeAccount(ZHAO_UUID, "old-pass!!".toCharArray());
            }
        });
        assertEquals(BankAccountStatus.FROZEN,
                bank.freezeAccount(ZHAO_UUID, "new-pass!!".toCharArray()).getStatus());
    }

    /** 管理员能按用户名读到目标用户的流水。 */
    @Test
    void listsTargetUserTransactions() {
        bank.openAccount(ZHAO_UUID);
        bank.recharge(ZHAO_UUID, new BigDecimal("50.00"));
        BankTransactionListResponse page =
                admin.listTransactions("zhao001", new BankTransactionQueryRequest());
        assertEquals(1, page.getTransactions().size());
        assertEquals(new BigDecimal("50.00"), page.getTransactions().get(0).getAmount());
    }

    /** 目标用户未开户时沿用未开户异常语义。 */
    @Test
    void transactionsOfUnopenedAccountRaiseNotOpened() {
        assertThrows(BankAccountNotOpenedException.class, new Executable() {
            @Override
            public void execute() {
                admin.listTransactions("zhao001", new BankTransactionQueryRequest());
            }
        });
    }

    /** 管理员不需要银行账户，即使未开户也不出现在账户列表。 */
    @Test
    void omitsAdministrators() {
        save("root", "uuid-root", "管理员", "管理员");
        PageResponse<BankAdminAccountView> page =
                admin.listAccounts(new BankAdminQuery());
        assertEquals(2L, page.getTotal());
        for (BankAdminAccountView row : page.getItems()) {
            assertNotEquals("root", row.getUsername());
        }
    }
}
