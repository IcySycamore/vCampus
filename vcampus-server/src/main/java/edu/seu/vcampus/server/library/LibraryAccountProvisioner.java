package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.library.LibraryPolicy;
import edu.seu.vcampus.common.library.entity.LibraryAccount;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.server.user.AccountProvisioner;
import java.sql.SQLException;
import java.util.Date;

/** 用户建号时为学生和教师同步建立图书馆读者账户。 */
public final class LibraryAccountProvisioner implements AccountProvisioner {
    private final LibraryAccountDao accounts;

    /** @param accounts 图书馆账户数据访问接口 */
    public LibraryAccountProvisioner(LibraryAccountDao accounts) {
        if (accounts == null) {
            throw new IllegalArgumentException("accounts must not be null");
        }
        this.accounts = accounts;
    }

    @Override
    public void provision(String userUuid, String displayName, Role role) {
        int limit = LibraryPolicy.borrowLimit(role == null ? null : role.name());
        if (limit == 0 || userUuid == null || userUuid.trim().length() == 0) {
            return;
        }
        try {
            if (accounts.findByUserUuid(userUuid) != null) {
                return;
            }
            LibraryAccount account = new LibraryAccount(userUuid, limit, new Date());
            if (!accounts.insert(account)) {
                throw new IllegalStateException("图书馆账户建立失败: " + userUuid);
            }
        } catch (SQLException exception) {
            throw failure("图书馆账户建立失败: " + userUuid, exception);
        }
    }

    @Override
    public void revoke(String userUuid) {
        if (userUuid == null || userUuid.trim().length() == 0) {
            return;
        }
        try {
            LibraryAccount account = accounts.findByUserUuid(userUuid);
            if (account != null && !account.isDeleted()
                    && !accounts.softDelete(userUuid, new Date())) {
                throw new IllegalStateException("图书馆账户撤销失败: " + userUuid);
            }
        } catch (SQLException exception) {
            throw failure("图书馆账户撤销失败: " + userUuid, exception);
        }
    }

    private IllegalStateException failure(String message, SQLException cause) {
        return new IllegalStateException(message, cause);
    }
}
