package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageHandler;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.server.network.ServerMessageDispatcher;
import edu.seu.vcampus.server.user.SessionManager;

/** 图书馆模块装配：复用入口的分发器、会话表及数据库业务服务。 */
public final class LibraryModule {
    private LibraryModule() {
    }

    /**
     * 登记图书馆支持的命令。
     * @param dispatcher 共享分发器
     * @param sessions 认证模块的共享会话表
     * @param service 注入数据源和 DAO 的业务服务；null 时明确报告数据库未配置
     */
    public static void register(ServerMessageDispatcher dispatcher, SessionManager sessions,
            LibraryService service) {
        if (dispatcher == null || sessions == null) {
            throw new IllegalArgumentException("dispatcher and sessions must not be null");
        }
        MessageHandler handler = service == null ? unavailable(sessions)
                : new LibraryMessageHandler(service, sessions);
        dispatcher.register(Command.LIBRARY_SEARCH, Command.LIBRARY_RETURN, handler);
        dispatcher.register(Command.LIBRARY_CREATE_BOOK, Command.LIBRARY_CATALOG_SEARCH, handler);
    }

    private static MessageHandler unavailable(final SessionManager sessions) {
        return new MessageHandler() {
            @Override
            public void handle(Message request, MessageSender sender) {
                Message response = new Message(request.getCommand(), "图书馆数据库尚未配置");
                response.setUid(request.getUid());
                response.setStatusCode(sessions.validate(request.getToken()) == null
                        ? StatusCode.UNAUTHORIZED : StatusCode.INTERNAL_ERROR);
                sender.send(response);
            }
        };
    }
}
