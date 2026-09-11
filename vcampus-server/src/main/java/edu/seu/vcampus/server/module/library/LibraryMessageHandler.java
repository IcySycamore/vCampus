package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.entity.BorrowRecord;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageType;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.handler.MessageHandler;
import edu.seu.vcampus.common.handler.MessageSender;
import edu.seu.vcampus.server.auth.SessionManager;
import edu.seu.vcampus.server.dispatch.MessageDispatcher;

import java.sql.SQLException;
import java.util.List;

/**
 * 将图书馆消息协议适配到图书馆业务服务。
 */
public class LibraryMessageHandler implements MessageHandler {

    private static final int STUDENT_MAX_BORROWS = 3;
    private static final int TEACHER_MAX_BORROWS = 5;
    private static final int DEFAULT_MAX_BORROWS = 10;
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
     * 四条命令共享同一处理器，使用认证模块已有的会话管理器验证身份。
     *
     * @param dispatcher 服务器共享的消息分发器
     * @param service 图书馆业务服务
     * @param sessions 与认证模块共享的会话管理器
     */
    public static void register(MessageDispatcher dispatcher, LibraryService service,
            SessionManager sessions) {
        if (dispatcher == null) {
            throw new IllegalArgumentException("dispatcher must not be null");
        }
        LibraryMessageHandler handler = new LibraryMessageHandler(service, sessions);
        dispatcher.register(MessageType.LIBRARY_SEARCH, handler);
        dispatcher.register(MessageType.LIBRARY_LIST_BORROWS, handler);
        dispatcher.register(MessageType.LIBRARY_BORROW, handler);
        dispatcher.register(MessageType.LIBRARY_RETURN, handler);
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
            response.setStatusCode(MessageType.SUCCESS);
        } catch (LibraryException exception) {
            response.setStatusCode(exception.getStatusCode());
            response.setData(exception.getMessage());
        } catch (SQLException exception) {
            response.setStatusCode(MessageType.SERVER_ERROR);
            response.setData("图书馆服务暂时不可用");
        } catch (RuntimeException exception) {
            response.setStatusCode(MessageType.BAD_REQUEST);
            response.setData(safeMessage(exception));
        }
        return response;
    }

    private Object execute(Message request) throws SQLException, LibraryException {
        if (request == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        SessionManager.SessionEntry entry = authenticatedSession(request);
        String userId = entry.getUsername();
        if (request.getCommand() == MessageType.LIBRARY_SEARCH) {
            String[] filters = (String[]) request.getData();
            return service.search(filters[0], filters.length > 1 ? filters[1] : "all");
        }
        if (request.getCommand() == MessageType.LIBRARY_LIST_BORROWS) {
            return service.listBorrows(userId);
        }
        if (request.getCommand() == MessageType.LIBRARY_BORROW) {
            return borrowWithinLimit(userId, (String) request.getData(),
                    borrowLimit(entry.getRole()));
        }
        if (request.getCommand() == MessageType.LIBRARY_RETURN) {
            Number recordId = (Number) request.getData();
            synchronized (BORROW_LOCK) {
                return service.returnBook(userId, recordId.longValue());
            }
        }
        throw new IllegalArgumentException("未知的图书馆命令");
    }

    private int borrowLimit(String role) {
        if (Role.STUDENT.getDisplayName().equals(role)) {
            return STUDENT_MAX_BORROWS;
        }
        if (Role.TEACHER.getDisplayName().equals(role)) {
            return TEACHER_MAX_BORROWS;
        }
        return DEFAULT_MAX_BORROWS;
    }

    private BorrowRecord borrowWithinLimit(String userId, String isbn, int maxBorrows)
            throws SQLException, LibraryException {
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
                throw new LibraryException(MessageType.BAD_REQUEST,
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

    private SessionManager.SessionEntry authenticatedSession(Message request)
            throws LibraryException {
        String token = request.getToken();
        SessionManager.SessionEntry entry = token == null || token.trim().length() == 0
                ? null : sessions.validate(token);
        if (entry == null || entry.getUsername() == null
                || entry.getUsername().trim().length() == 0) {
            throw new LibraryException(StatusCode.UNAUTHORIZED, "登录已失效，请重新登录");
        }
        return entry;
    }

    private String safeMessage(RuntimeException exception) {
        return exception.getMessage() == null ? "请求格式不正确" : exception.getMessage();
    }
}
