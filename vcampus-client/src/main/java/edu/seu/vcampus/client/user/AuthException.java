package edu.seu.vcampus.client.user;

/**
 * 认证业务异常：携带服务器返回的状态码（见 {@code StatusCode}）。
 */
public class AuthException extends RuntimeException {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 服务器返回状态码；本地异常时为 null。 */
    private final String statusCode;

    /**
     * 构造认证异常。
     *
     * @param statusCode 状态码
     * @param message    描述
     */
    public AuthException(String statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    /** @return 服务器返回状态码 */
    public String getStatusCode() {
        return statusCode;
    }
}
