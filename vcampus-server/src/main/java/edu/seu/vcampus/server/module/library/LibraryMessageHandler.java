package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageType;

import java.sql.SQLException;

/**
 * 将图书馆消息协议适配到图书馆业务服务。
 */
public class LibraryMessageHandler {

    private final LibraryService service;

    /**
     * 创建消息处理器。
     *
     * @param service 图书馆业务服务
     */
    public LibraryMessageHandler(LibraryService service) {
        if (service == null) {
            throw new IllegalArgumentException("service must not be null");
        }
        this.service = service;
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
        if (request.getCommand() == MessageType.LIBRARY_SEARCH) {
            String[] filters = (String[]) request.getData();
            return service.search(filters[0], filters.length > 1 ? filters[1] : "all");
        }
        if (request.getCommand() == MessageType.LIBRARY_LIST_BORROWS) {
            return service.listBorrows(request.getSender());
        }
        if (request.getCommand() == MessageType.LIBRARY_BORROW) {
            return service.borrow(request.getSender(), (String) request.getData());
        }
        if (request.getCommand() == MessageType.LIBRARY_RETURN) {
            Number recordId = (Number) request.getData();
            return service.returnBook(request.getSender(), recordId.longValue());
        }
        throw new IllegalArgumentException("未知的图书馆命令");
    }

    private Message responseFor(Message request) {
        Message response = new Message();
        if (request != null) {
            response.setUid(request.getUid());
            response.setCommand(request.getCommand());
        }
        return response;
    }

    private String safeMessage(RuntimeException exception) {
        return exception.getMessage() == null ? "请求格式不正确" : exception.getMessage();
    }
}
