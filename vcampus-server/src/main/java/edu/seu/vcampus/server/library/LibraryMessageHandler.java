package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.library.LibraryPolicy;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageHandler;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import edu.seu.vcampus.server.user.SessionManager;
import java.sql.SQLException;

/** 将图书馆消息协议适配到图书馆业务服务。 */
public class LibraryMessageHandler implements MessageHandler {
    private final LibraryService m_service;
    private final SessionManager m_sessions;
    private final LibraryReaderCommands m_reader;

    /**
     * 创建未配置罚款支付的兼容处理器。
     * 
     * @param service  图书馆业务服务
     * @param sessions 认证模块共享会话表
     */
    public LibraryMessageHandler(LibraryService service, SessionManager sessions) {
        this(service, sessions, null);
    }

    /**
     * 创建完整图书馆处理器。
     * 
     * @param service  图书馆业务服务
     * @param sessions 认证模块共享会话表
     * @param payment  校园银行罚款支付接口
     */
    public LibraryMessageHandler(LibraryService service, SessionManager sessions,
            LibraryFinePayment payment) {
        if (service == null || sessions == null) {
            throw new IllegalArgumentException("service and sessions must not be null");
        }
        m_service = service;
        m_sessions = sessions;
        m_reader = new LibraryReaderCommands(service, payment);
    }

    @Override
    public void handle(Message request, MessageSender sender) {
        sender.send(createResponse(request));
    }

    /**
     * 处理图书馆命令并生成响应。
     * 
     * @param request 客户端请求
     * @return 响应消息
     */
    public Message createResponse(Message request) {
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
        SessionEntry entry = requireSession(request.getToken());
        if (request.getCommand() >= Command.LIBRARY_CREATE_BOOK
                && request.getCommand() <= Command.LIBRARY_CATALOG_SEARCH) {
            boolean manager = LibraryPolicy.canManage(entry.getRole());
            boolean savesMetadata = request.getCommand() == Command.LIBRARY_UPDATE_BOOK
                    && LibraryPolicy.borrowLimit(entry.getRole()) > 0;
            if (!manager && !savesMetadata) {
                throw new LibraryException(StatusCode.FORBIDDEN, "仅管理员可以录入、下架或查看已下架馆藏");
            }
            return m_service.getCatalog().handle(request);
        }
        return m_reader.execute(request, entry);
    }

    private Message responseFor(Message request) {
        Message response = new Message();
        if (request != null) {
            response.setUid(request.getUid());
            response.setCommand(request.getCommand());
        }
        return response;
    }

    private SessionEntry requireSession(String token) throws LibraryException {
        SessionEntry entry = token == null || token.trim().length() == 0
                ? null
                : m_sessions.validate(token);
        if (entry == null || entry.getUuid() == null
                || entry.getUuid().trim().length() == 0) {
            throw new LibraryException(StatusCode.UNAUTHORIZED, "登录已失效，请重新登录");
        }
        return entry;
    }
}
