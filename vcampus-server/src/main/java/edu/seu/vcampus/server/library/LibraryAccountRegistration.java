package edu.seu.vcampus.server.library;

import edu.seu.vcampus.server.user.AccountProvisioning;

/** 把图书馆账户接入用户生命周期（建号即建读者账户）。 */
final class LibraryAccountRegistration {
    private LibraryAccountRegistration() {
    }

    /**
     * 登记开户钩子。
     *
     * <p>
     * 不再于启动时给既有账号补档：账号的唯一来源是 {@code data/admins.tsv}（角色全是管理员）， 而读者账户只为师生建立，那段「过一遍既有账号」的代码实际一行也不会写。
     *
     * @param service      图书馆服务
     * @param provisioning 开户钩子登记表
     */
    static void register(LibraryService service, AccountProvisioning provisioning) {
        if (service == null || provisioning == null) {
            return;
        }
        provisioning.add(service.getAccountProvisioner());
    }
}
