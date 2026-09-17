package edu.seu.vcampus.server.bank;

import edu.seu.vcampus.common.bank.entity.BankAccount;
import edu.seu.vcampus.common.bank.entity.BankTransaction;

import java.util.Collections;
import java.util.List;

/**
 * 不持久化的银行后端，保持改造前的纯内存行为。
 *
 * <p>
 * 缺省装配用它，因此没接数据库的机器上（含绝大多数单元测试）行为与改造前逐字一致：所有读取都
 * 返回空、所有写入都返回成功，真正的状态仍只在 {@link BankService} 的内存表里。
 */
public final class BankStoreMemory implements BankStore {

    /** @return 空列表，内存版没有可恢复的历史 */
    @Override
    public List<BankAccount> loadAccounts() {
        return Collections.emptyList();
    }

    /** @return 永远为 null，内存版不存凭据 */
    @Override
    public BankCredentialRecord loadCredential(String accountId) {
        return null;
    }

    /** @return 空列表，内存版不存流水 */
    @Override
    public List<BankTransaction> loadTransactions(String accountId) {
        return Collections.emptyList();
    }

    /** @return 恒为 0，内存版没有可恢复的流水序号 */
    @Override
    public long loadMaxSequence() {
        return 0L;
    }

    /** @return 恒为 true，内存版无需写入 */
    @Override
    public boolean insertAccount(BankAccount account, BankCredentialRecord credential) {
        return true;
    }

    /** @return 恒为 true，内存版无需写入 */
    @Override
    public boolean updateAccount(BankAccount account) {
        return true;
    }

    /** @return 恒为 true，内存版无需写入 */
    @Override
    public boolean updateCredential(String accountId, BankCredentialRecord credential) {
        return true;
    }

    /** @return 恒为 true，内存版无需写入 */
    @Override
    public boolean appendTransaction(BankTransaction transaction) {
        return true;
    }
}
