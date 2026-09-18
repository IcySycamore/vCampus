package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.library.LibraryPolicy;
import edu.seu.vcampus.common.library.dto.BookRef;
import edu.seu.vcampus.common.library.dto.BookQuery;
import edu.seu.vcampus.common.library.dto.BorrowRequest;
import edu.seu.vcampus.common.library.dto.RecordRef;
import edu.seu.vcampus.common.library.dto.ReservationRef;
import edu.seu.vcampus.common.library.entity.BorrowRecord;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import java.sql.SQLException;
import java.util.List;

/** 路由普通读者命令并串行保护借阅额度与库存更新。 */
final class LibraryReaderCommands {
    private static final Object CIRCULATION_LOCK = new Object();
    private final LibraryService service;
    private final LibraryFinePayment payment;

    LibraryReaderCommands(LibraryService service, LibraryFinePayment payment) {
        this.service = service;
        this.payment = payment;
    }

    Object execute(Message request, SessionEntry entry)
            throws SQLException, LibraryException {
        int command = request.getCommand();
        String userId = entry.getUuid();
        if (command == Command.LIBRARY_SEARCH) {
            BookQuery query = LibraryRequestValidator.search(request.getData());
            return service.search(query);
        }
        if (command == Command.LIBRARY_LIST_BORROWS) {
            return service.listBorrows(userId);
        }
        if (command == Command.LIBRARY_POPULAR_BORROWS) {
            return service.listPopular(5);
        }
        if (command == Command.LIBRARY_ACCOUNT_QUERY) {
            requireBorrower(entry);
            return service.queryAccount(userId);
        }
        if (command == Command.LIBRARY_LIST_RESERVATIONS) {
            requireBorrower(entry);
            return service.listReservations(userId);
        }
        requireBorrower(entry);
        synchronized (CIRCULATION_LOCK) {
            return mutate(request, userId, LibraryPolicy.borrowLimit(entry.getRole()));
        }
    }

    private Object mutate(Message request, String userId, int limit)
            throws SQLException, LibraryException {
        int command = request.getCommand();
        if (command == Command.LIBRARY_BORROW) {
            BorrowRequest borrow = LibraryRequestValidator.borrow(request.getData());
            ensureLimit(userId, limit);
            return service.borrow(userId, borrow.getIsbn());
        }
        if (command == Command.LIBRARY_RETURN) {
            return service.returnBook(userId, record(request).getRecordId());
        }
        if (command == Command.LIBRARY_RENEW) {
            return service.renew(userId, record(request).getRecordId());
        }
        if (command == Command.LIBRARY_RESERVE) {
            BookRef book = LibraryRequestValidator.book(request.getData());
            return service.reserve(userId, book.getIsbn());
        }
        if (command == Command.LIBRARY_CANCEL_RESERVATION) {
            ReservationRef reservation = LibraryRequestValidator.reservation(
                    request.getData());
            return service.cancelReservation(userId, reservation.getReservationId());
        }
        if (command == Command.LIBRARY_PAY_FINE) {
            return service.payFine(userId, record(request).getRecordId(), payment);
        }
        throw new LibraryException(StatusCode.BAD_REQUEST, "未知的图书馆命令");
    }

    private RecordRef record(Message request) throws LibraryException {
        return LibraryRequestValidator.record(request.getData());
    }

    private void ensureLimit(String userId, int limit)
            throws SQLException, LibraryException {
        List<BorrowRecord> records = service.listBorrows(userId);
        if (records == null) {
            throw new SQLException("borrow records must not be null");
        }
        int active = 0;
        for (BorrowRecord record : records) {
            if (!record.isReturned()) {
                active++;
            }
        }
        if (active >= limit) {
            throw new LibraryException(StatusCode.BAD_REQUEST,
                    "最多同时借阅 " + limit + " 本图书，请先归还后再借阅");
        }
    }

    private void requireBorrower(SessionEntry entry) throws LibraryException {
        if (LibraryPolicy.borrowLimit(entry.getRole()) == 0) {
            throw new LibraryException(StatusCode.FORBIDDEN, "当前身份没有借阅权限");
        }
    }
}
