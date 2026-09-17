package edu.seu.vcampus.server.bank;

import edu.seu.vcampus.common.bank.entity.BankAccount;
import edu.seu.vcampus.common.bank.entity.BankTransaction;

import java.util.List;

/**
 * 银行账户与流水的持久化后端。
 *
 * <p>
 * {@link BankService} 仍在内存里持有账户锁与业务规则，本接口只负责「把变更写下去、启动时读回来」。
 * 这样做的原因是银行的一整套并发语义（同一账户上的余额变动与流水记录必须原子）建立在 {@code synchronized (record)} 上，改成数据库事务会牵动
 * {@code BankMessageHandler} 与商店扣款 路径；当前部署是单服务端实例，进程内锁足够，落库只解决「重启丢数据」。
 *
 * <p>
 * 缺省实现是 {@link BankStoreMemory}（不持久化，行为与改造前一致）；{@code -Dvcampus.store=jdbc} 时装配
 * {@link BankStoreJdbc}。方法都按「调用方已持有账户锁」的假设编写，实现不必自己加锁。
 */
public interface BankStore {

    /**
     * 加载全部账户，用于服务启动时恢复内存状态。
     *
     * @return 账户列表，不返回 null
     */
    List<BankAccount> loadAccounts();

    /**
     * 读取某账户的密码凭据。
     *
     * @param accountId 账户业务编号
     * @return 凭据快照；账户不存在或从未设置密码时返回 null
     */
    BankCredentialRecord loadCredential(String accountId);

    /**
     * 读取某账户的全部流水，按发生时间升序。
     *
     * @param accountId 账户业务编号
     * @return 流水列表，不返回 null
     */
    List<BankTransaction> loadTransactions(String accountId);

    /**
     * 读取历史流水中最大的流水序号，供服务启动时恢复进程内计数器。
     *
     * <p>
     * 流水号形如 {@code T-<n>}，由 {@code BankService} 的进程内计数器分配。不恢复的话服务重启后 会从 1 重新开始，与库里已有流水撞主键。
     *
     * @return 最大序号；没有任何流水时返回 0
     */
    long loadMaxSequence();

    /**
     * 写入新开的账户及其凭据。
     *
     * @param account    新账户
     * @param credential 密码凭据，可为 null（开户时未设置密码）
     * @return 写入成功为 true，账户已存在为 false
     */
    boolean insertAccount(BankAccount account, BankCredentialRecord credential);

    /**
     * 保存账户的余额与状态。
     *
     * @param account 已变更的账户
     * @return 命中记录为 true
     */
    boolean updateAccount(BankAccount account);

    /**
     * 保存账户的密码凭据与挂失状态。
     *
     * @param accountId  账户业务编号
     * @param credential 新凭据
     * @return 命中记录为 true
     */
    boolean updateCredential(String accountId, BankCredentialRecord credential);

    /**
     * 追加一条资金流水。
     *
     * @param transaction 已生成的流水
     * @return 写入成功为 true
     */
    boolean appendTransaction(BankTransaction transaction);
}
