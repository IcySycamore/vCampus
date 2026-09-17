package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.library.entity.BorrowRecord;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

/** 校验罚款归属并协调银行扣款与借阅记录落库。 */
final class LibraryFineService {
    private final DataSource dataSource;
    private final BorrowDao borrows;

    LibraryFineService(DataSource dataSource, BorrowDao borrows) {
        LibraryValues.requireDependencies("fine", dataSource, borrows);
        this.dataSource = dataSource;
        this.borrows = borrows;
    }

    BorrowRecord pay(String userId, long recordId, char[] password,
            LibraryFinePayment payment)
            throws SQLException, LibraryException {
        String user = LibraryValues.text(userId, "用户 ID");
        if (payment == null) {
            throw new LibraryException(StatusCode.INTERNAL_ERROR,
                    "图书罚款支付服务尚未配置");
        }
        BorrowRecord record = findOwned(user, recordId);
        if (!record.isReturned()) {
            throw badRequest("请先归还逾期图书，再缴纳滞纳金");
        }
        if (!record.hasUnpaidFine()) {
            throw badRequest("该借阅记录没有待缴滞纳金");
        }
        String transactionId = payment.pay(user, record.getFineAmount(),
                "LIBRARY_FINE:" + recordId, password);
        return savePayment(user, recordId, transactionId);
    }

    private BorrowRecord findOwned(String user, long id)
            throws SQLException, LibraryException {
        try (Connection connection = dataSource.getConnection()) {
            BorrowRecord record = borrows.findById(connection, id);
            requireOwned(record, user);
            return record;
        }
    }

    private BorrowRecord savePayment(String user, long id, String transactionId)
            throws SQLException, LibraryException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                BorrowRecord record = borrows.findById(connection, id);
                requireOwned(record, user);
                if (!record.isFinePaid()
                        && !borrows.markFinePaid(connection, id, transactionId)) {
                    throw new SQLException("fine payment update was not completed");
                }
                connection.commit();
                record.setFinePaid(true);
                record.setFineTransactionId(transactionId);
                return record;
            } catch (SQLException exception) {
                LibraryValues.rollback(connection, exception);
                throw exception;
            } catch (LibraryException exception) {
                LibraryValues.rollback(connection, exception);
                throw exception;
            }
        }
    }

    private void requireOwned(BorrowRecord record, String user) throws LibraryException {
        if (record == null) {
            throw new LibraryException(StatusCode.NOT_FOUND, "借阅记录不存在");
        }
        if (!user.equals(record.getUserId())) {
            throw new LibraryException(StatusCode.FORBIDDEN,
                    "不能操作其他用户的借阅记录");
        }
    }

    private LibraryException badRequest(String message) {
        return new LibraryException(StatusCode.BAD_REQUEST, message);
    }
}
