package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.library.entity.BorrowRecord;
import edu.seu.vcampus.common.library.dto.BookQuery;
import edu.seu.vcampus.common.library.dto.BorrowRequest;
import edu.seu.vcampus.common.library.dto.RecordRef;
import edu.seu.vcampus.common.library.LibraryPolicy;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.MessageHandler;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.server.user.SessionManager;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import edu.seu.vcampus.server.network.ServerMessageDispatcher;
import java.sql.SQLException;
import java.util.List;

/**
 * 将图书馆消息协议适配到图书馆业务服务。
 */
public class LibraryMessageHandler implements MessageHandler {

    // 覆盖同一 JVM 内的所有处理器实例；多服务器部署须由数据库实现原子额度检查。
    private static final Object BORROW_LOCK = new Object();

    private final LibraryService service;
    private final SessionManager sessions;

    /**
     * 创建消息处理器。
     *
     * @param service 图书馆业务服务
     * @param sessions 与认证模块共享的会话管理器
     */
    public LibraryMessageHandler(LibraryService service, SessionManager sessions) {
        if (service == null || sessions == null) {
            throw new IllegalArgumentException("service and sessions must not be null");
        }
        this.service = service;
        this.sessions = sessions;
    }

    /**
     * 向服务器统一分发器注册全部图书馆命令，供服务器启动组装时调用。
     * 借阅与管理命令共享同一处理器，使用认证模块已有的会话管理器验证身份。
     *
     * @param dispatcher 服务器共享的消息分发器
     * @param service 图书馆业务服务
     * @param sessions 与认证模块共享的会话管理器
     */
    public static void register(ServerMessageDispatcher dispatcher, LibraryService service,
            SessionManager sessions) {
        LibraryModule.register(dispatcher, sessions, service);
    }

    @Override
    public void handle(Message request, MessageSender sender) {
        sender.send(handle(request));
    }

    /**
     * 处理一条图书馆命令并生成响应消息。
     *
     * @param request 客户端请求
     * @return 响应消息
     */
    public Message handle(Message request) {
        Message response = responseFor(request);
        try {
            response.setData(execute(request));
            response.setStatusCode(StatusCode.SUCCESS);
        } catch (LibraryException exception) {
            response.setStatusCode(exception.getStatusCode());
            response.setData(exception.getMessage());
        } catch (SQLException exception) {
            response.setStatusCode(StatusCode.INTERNAL_ERROR);
            response.setData("图书馆服务暂时不可用");
        } catch (RuntimeException exception) {
            response.setStatusCode(StatusCode.INTERNAL_ERROR);
            response.setData("图书馆服务暂时不可用");
        }
        return response;
    }

    private Object execute(Message request) throws SQLException, LibraryException {
        if (request == null) {
            throw new LibraryException(StatusCode.BAD_REQUEST, "请求不能为空");
        }
        SessionEntry entry = authenticatedSession(request);
        String userId = entry.getUuid();
        if (request.getCommand() >= Command.LIBRARY_CREATE_BOOK
                && request.getCommand() <= Command.LIBRARY_CATALOG_SEARCH) {
            if (!LibraryPolicy.canManage(entry.getRole())) {
                throw new LibraryException(StatusCode.FORBIDDEN, "仅管理员可以管理图书馆藏");
            }
            return service.getCatalog().handle(request);
        }
        if (request.getCommand() == Command.LIBRARY_SEARCH) {
            BookQuery query = LibraryRequestValidator.search(request.getData());
            return service.search(query);
        }
        if (request.getCommand() == Command.LIBRARY_LIST_BORROWS) {
            return service.listBorrows(userId);
        }
        if (request.getCommand() == Command.LIBRARY_BORROW) {
            BorrowRequest borrow = LibraryRequestValidator.borrow(request.getData());
            return borrowWithinLimit(userId, borrow.getIsbn(),
                    LibraryPolicy.borrowLimit(entry.getRole()));
        }
        if (request.getCommand() == Command.LIBRARY_RETURN) {
            RecordRef record = LibraryRequestValidator.record(request.getData());
            synchronized (BORROW_LOCK) {
                return service.returnBook(userId, record.getRecordId());
            }
        }
        throw new LibraryException(StatusCode.BAD_REQUEST, "未知的图书馆命令");
    }

    private BorrowRecord borrowWithinLimit(String userId, String isbn, int maxBorrows)
            throws SQLException, LibraryException {
        if (maxBorrows == 0) {
            throw new LibraryException(StatusCode.FORBIDDEN, "当前身份没有借阅权限");
        }
        // 将数量检查和借书事务串行执行，锁一直保持到借书提交或回滚完成。
        synchronized (BORROW_LOCK) {
            List<BorrowRecord> records = service.listBorrows(userId);
            if (records == null) {
                throw new SQLException("borrow records must not be null");
            }
            int activeCount = 0;
            for (BorrowRecord record : records) {
                if (!record.isReturned()) {
                    activeCount++;
                }
            }
            if (activeCount >= maxBorrows) {
                throw new LibraryException(StatusCode.BAD_REQUEST,
                        "最多同时借阅 " + maxBorrows
                                + " 本图书，请先归还后再借阅");
            }
            return service.borrow(userId, isbn);
        }
    }

    private Message responseFor(Message request) {
        Message response = new Message();
        if (request != null) {
            response.setUid(request.getUid());
            response.setCommand(request.getCommand());
        }
        return response;
    }

    private SessionEntry authenticatedSession(Message request)
            throws LibraryException {
        String token = request.getToken();
        SessionEntry entry = token == null || token.trim().length() == 0
                ? null : sessions.validate(token);
        if (entry == null || entry.getUuid() == null
                || entry.getUuid().trim().length() == 0) {
            throw new LibraryException(StatusCode.UNAUTHORIZED, "登录已失效，请重新登录");
        }
        return entry;
    }

}
