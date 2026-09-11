package edu.seu.vcampus.common.message;

/**
 * 客户端与服务器端约定的消息类型和图书馆命令。
 */
public final class MessageType {

    /** 400-499 为图书馆模块保留的命令码区间。 */
    public static final int LIBRARY_SEARCH = 400;
    public static final int LIBRARY_LIST_BORROWS = 401;
    public static final int LIBRARY_BORROW = 402;
    public static final int LIBRARY_RETURN = 403;
    public static final String SUCCESS = "200";
    public static final String BAD_REQUEST = "400";
    public static final String UNAUTHORIZED = "401";
    public static final String FORBIDDEN = "403";
    public static final String NOT_FOUND = "404";
    public static final String SERVER_ERROR = "500";

    /** 禁止实例化常量类。 */
    private MessageType() {
    }
}
