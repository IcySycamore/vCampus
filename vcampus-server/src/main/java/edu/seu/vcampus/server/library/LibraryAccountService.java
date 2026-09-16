package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.library.entity.LibraryAccount;
import java.sql.SQLException;

/** 查询图书馆账户并执行读者账户级规则。 */
final class LibraryAccountService {
    private final LibraryAccountDao accounts;

    LibraryAccountService(LibraryAccountDao accounts) {
        LibraryValues.requireDependencies("account", accounts);
        this.accounts = accounts;
    }

    LibraryAccount query(String userUuid) throws SQLException, LibraryException {
        LibraryAccount account = accounts.findByUserUuid(
                LibraryValues.text(userUuid, "用户 ID"));
        if (account == null || account.isDeleted()) {
            throw new LibraryException(StatusCode.NOT_FOUND, "图书馆账户不存在");
        }
        return account;
    }

    void ensureCanBorrow(String userUuid, int activeCount)
            throws SQLException, LibraryException {
        LibraryAccount account = ensureOperational(userUuid);
        if (activeCount >= account.getBorrowLimit()) {
            throw new LibraryException(StatusCode.BAD_REQUEST,
                    "最多同时借阅 " + account.getBorrowLimit() + " 本图书，请先归还后再借阅");
        }
    }

    LibraryAccount ensureOperational(String userUuid)
            throws SQLException, LibraryException {
        LibraryAccount account = query(userUuid);
        if (!account.isOperational()) {
            throw new LibraryException(StatusCode.FORBIDDEN, "图书馆账户已暂停使用");
        }
        return account;
    }

    LibraryAccountProvisioner provisioner() {
        return new LibraryAccountProvisioner(accounts);
    }
}
