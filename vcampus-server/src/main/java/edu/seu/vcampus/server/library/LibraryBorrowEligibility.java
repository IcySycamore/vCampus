package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.library.entity.BorrowRecord;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/** 校验逾期、欠费、读者账户状态及同时借阅额度。 */
final class LibraryBorrowEligibility {
    private final BorrowDao borrows;
    private final LibraryAccountService accounts;

    LibraryBorrowEligibility(BorrowDao borrows, LibraryAccountService accounts) {
        LibraryValues.requireDependencies("eligibility", borrows, accounts);
        this.borrows = borrows;
        this.accounts = accounts;
    }

    void requireEligible(String userId, Date now)
            throws SQLException, LibraryException {
        List<BorrowRecord> records = borrows.findByUser(userId);
        if (records == null) {
            throw new SQLException("borrow records must not be null");
        }
        int activeCount = 0;
        for (BorrowRecord record : records) {
            if (!record.isReturned() && now.after(record.getDueAt())) {
                throw rejected("存在逾期未还图书，请先归还并缴纳滞纳金");
            }
            if (!record.isReturned()) {
                activeCount++;
            }
            if (record.hasUnpaidFine()) {
                throw rejected("存在未缴滞纳金，请先完成缴费");
            }
        }
        accounts.ensureCanBorrow(userId, activeCount);
    }

    void requireOperational(String userId) throws SQLException, LibraryException {
        accounts.ensureOperational(userId);
    }

    private LibraryException rejected(String message) {
        return new LibraryException(StatusCode.BAD_REQUEST, message);
    }
}
