package edu.seu.vcampus.server.user;

import edu.seu.vcampus.common.user.entity.Role;

import java.util.ArrayList;
import java.util.List;

/**
 * 开户钩子登记表：把「账户建立/撤销」广播给各模块（见 {@link AccountProvisioner}）。
 *
 * <p>
 * 各模块在自装配时把自己的钩子登记进来（{@code XxxModule.register(..., provisioning)}），
 * 用户模块不反向依赖任何业务模块。
 *
 * <p>
 * 失败语义：{@link #provision} 中任一模块失败，会先<b>回滚</b>之前已成功的模块，再抛出异常；
 * 调用方（{@link AuthService#register}）随后撤销刚写入的账户——保证不会留下「账户存在但档案缺失」
 * 或「档案存在但账户不存在」的中间态。
 */
public final class AccountProvisioning {

    /** 已登记的钩子。装配期单线程登记，运行期只读。 */
    private final List<AccountProvisioner> m_provisioners = new ArrayList<AccountProvisioner>();

    /**
     * 登记一个开户钩子。
     *
     * @param provisioner 开户钩子
     * @throws IllegalArgumentException 钩子为 null
     */
    public synchronized void add(AccountProvisioner provisioner) {
        if (provisioner == null) {
            throw new IllegalArgumentException("provisioner must not be null");
        }
        m_provisioners.add(provisioner);
    }

    /**
     * 为新账户建立全部 1:1 档案；任一步失败即回滚已建档案并抛出。
     *
     * @param userUuid 账户全局唯一标识
     * @param userName 登录名
     * @param role 角色
     * @throws RuntimeException 某个模块建档案失败（已回滚）
     */
    public void provision(String userUuid, String userName, Role role) {
        List<AccountProvisioner> done = new ArrayList<AccountProvisioner>();
        for (AccountProvisioner provisioner : snapshot()) {
            try {
                provisioner.provision(userUuid, userName, role);
                done.add(provisioner);
            } catch (RuntimeException e) {
                rollback(done, userUuid);
                throw e;
            }
        }
    }

    /**
     * 撤销账户时清理各模块档案：单个模块失败只记录，不影响其它模块与账户删除结果。
     *
     * @param userUuid 账户全局唯一标识
     */
    public void revoke(String userUuid) {
        for (AccountProvisioner provisioner : snapshot()) {
            try {
                provisioner.revoke(userUuid);
            } catch (RuntimeException e) {
                System.err.println("撤销账户档案失败(" + provisioner.getClass().getSimpleName()
                        + "): " + e.getMessage());
            }
        }
    }

    /** @return 已登记的钩子数量（装配自检用） */
    public synchronized int size() {
        return m_provisioners.size();
    }

    private synchronized List<AccountProvisioner> snapshot() {
        return new ArrayList<AccountProvisioner>(m_provisioners);
    }

    private void rollback(List<AccountProvisioner> done, String userUuid) {
        for (int i = done.size() - 1; i >= 0; i--) {
            try {
                done.get(i).revoke(userUuid);
            } catch (RuntimeException e) {
                System.err.println("回滚账户档案失败: " + e.getMessage());
            }
        }
    }
}
