package edu.seu.vcampus.server.bank;

import edu.seu.vcampus.common.bank.entity.BankAccount;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 银行账户业务号的格式约束。
 *
 * <p>
 * {@code tblBankAccount.baId} 是 {@code VARCHAR(20)}（课程给定表结构）。业务号超长时 MySQL 抛
 * {@code Data too long for column 'baId'}，而服务端把它吞成「服务器内部错误」，客户端只看到
 * 开户失败、看不到原因 —— 这条约束只有真正落库那一刻才会炸，所以在这里钉死。
 *
 * <p>
 * 持久化后端用 Mockito 替身：本用例只关心业务号本身，不该因为要验它而去连库。
 */
class BankAccountIdTest {

    /** 列宽上限，与 {@code sql/vCampus.sql} 里 {@code baId} 的定义一致。 */
    private static final int COLUMN_WIDTH = 20;

    /** 一次并发开户的规模上限，用来验业务号不撞。 */
    private static final int OWNER_COUNT = 50;

    /** 测试用户编号。 */
    private static final String OWNER = "3f0c1e88-0f4e-4c0a-9a2f-5c2e0b7d1111";

    /** 业务号必须装得进 {@code baId} 这一列。 */
    @Test
    void accountIdFitsTheDatabaseColumn() {
        BankService bank = newBank();

        String accountId = bank.openAccount(OWNER).getAccountId();

        assertTrue(accountId.length() <= COLUMN_WIDTH, "业务号 " + accountId + " 有 "
                + accountId.length() + " 个字符，超过 baId 的 " + COLUMN_WIDTH
                + " 字符，落库时会被拒绝");
    }

    /** 业务号的随机部分不能退化成常量：多次开户必须拿到不同的号。 */
    @Test
    void accountIdsDoNotCollide() {
        BankService bank = newBank();

        Set<String> ids = new HashSet<String>();
        int index = 0;
        while (index < OWNER_COUNT) {
            ids.add(bank.openAccount("owner-" + index).getAccountId());
            index = index + 1;
        }
        assertEquals(OWNER_COUNT, ids.size(), "每次开户都应得到一个新的业务号");
    }

    /**
     * 造一个不落库的银行服务：后端替身读回空状态。
     *
     * @return 银行服务
     */
    private static BankService newBank() {
        BankStore store = mock(BankStore.class);
        when(store.loadAccounts()).thenReturn(Collections.<BankAccount>emptyList());
        return new BankService(store);
    }
}
