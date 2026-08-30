package edu.seu.vcampus.server.module.library;

/**
 * 可安全返回给客户端的图书馆业务异常。
 */
public class LibraryException extends Exception {

    private static final long serialVersionUID = 1L;
    private final String statusCode;

    /**
     * 创建业务异常。
     *
     * @param statusCode 协议状态码
     * @param message 用户可读提示
     */
    public LibraryException(String statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    /** @return 协议状态码 */
    public String getStatusCode() {
        return statusCode;
    }
}
