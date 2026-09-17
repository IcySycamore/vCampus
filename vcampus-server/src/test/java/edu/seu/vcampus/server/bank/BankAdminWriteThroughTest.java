package edu.seu.vcampus.server.bank;

import edu.seu.vcampus.common.bank.entity.BankAccount;
import edu.seu.vcampus.common.bank.entity.BankAccountStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 管理端操作必须落到持久化后端。
 *
 * <p>
 * {@code adminSetFrozen} 与 {@code adminResetPassword} 曾经改完内存对象就直接 return，一次 {@code m_store.*} 都没调
 * —— 对照用户端的 {@code setFrozen} 是写全的。内存模式下完全看不出来（界面读的就是内存， store 也是空实现），jdbc
 * 模式下「冻结成功」「改密成功」的响应是真的，重启后消失。
 *
 * <p>
 * 它们能躲过既有测试，是因为 {@link BankStore} 一直没有测试替身，没法断言「写到底有没有发生」，只能连真库端到端跑。 这个类补上那个替身，把契约钉在单元测试里。
 */
class BankAdminWriteThroughTest {

    private static final String OWNER_UUID = "7f4c2a10-94ad-4b42-8cae-51fd93e6b001";

    private BankStore store;
    private BankService bank;

    /** 用假后端起一个已开户的服务。 */
    @BeforeEach
    void setUp() {
        store = mock(BankStore.class);
        bank = new BankService(store);
        bank.openAccount(OWNER_UUID);
    }

    /** 管理端冻结应把状态写下去，而不是只改内存。 */
    @Test
    void adminFreezeWritesAccountDown() {
        bank.adminSetFrozen(OWNER_UUID, true);

        ArgumentCaptor<BankAccount> captured = ArgumentCaptor.forClass(BankAccount.class);
        verify(store).updateAccount(captured.capture());
        assertEquals(BankAccountStatus.FROZEN, captured.getValue().getStatus(),
                "冻结后的状态应当被写下去");
        verify(store).updateCredential(anyString(), any(BankCredentialRecord.class));
    }

    /** 冻结与解冻各写一次，解冻同样不能漏。 */
    @Test
    void adminUnfreezeAlsoWritesAccountDown() {
        bank.adminSetFrozen(OWNER_UUID, true);
        bank.adminSetFrozen(OWNER_UUID, false);

        verify(store, times(2)).updateAccount(any(BankAccount.class));
    }

    /** 管理端重置密码应把新凭据写下去。 */
    @Test
    void adminResetPasswordWritesCredentialDown() {
        // 盐 16 字节、摘要 32 字节，与 BankCredential 的格式约束一致
        bank.adminResetPassword(OWNER_UUID, new byte[16], new byte[32]);

        ArgumentCaptor<BankCredentialRecord> captured = ArgumentCaptor
                .forClass(BankCredentialRecord.class);
        verify(store, atLeastOnce()).updateCredential(anyString(), captured.capture());
        assertNotNull(captured.getValue(), "重置后的凭据不能为空");
    }
}
