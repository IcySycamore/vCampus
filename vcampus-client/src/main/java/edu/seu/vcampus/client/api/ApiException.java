package edu.seu.vcampus.client.api;

/**
 * 客户端 API 调用失败异常（非受检）。
 *
 * <p>
 * 设计目标是「界面里不写 try/catch、不写线程」：服务层把网络异常、超时、服务端拒绝统一
 * 收敛成本异常抛出，界面只在统一的失败回调里处理一次。
 *
 * <p>
 * 与既有的 {@code client.user.AuthException} 的关系：本类是它的父类，登录相关代码不必改动
 * 就能被同一套失败处理逻辑接住。新增模块一律抛 {@code ApiException}。
 */
public class ApiException extends RuntimeException {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 服务端状态码；网络层失败（无响应）时为 null。 */
    private final String m_status_code;

    /**
     * 构造异常。
     *
     * @param statusCode 服务端状态码；无响应时传 null
     * @param message 可读的错误说明
     */
    public ApiException(String statusCode, String message) {
        super(message);
        this.m_status_code = statusCode;
    }

    /**
     * 构造异常并保留根因。
     *
     * @param statusCode 服务端状态码；无响应时传 null
     * @param message 可读的错误说明
     * @param cause 根因
     */
    public ApiException(String statusCode, String message, Throwable cause) {
        super(message, cause);
        this.m_status_code = statusCode;
    }

    /**
     * 获取服务端状态码。
     *
     * @return 状态码；网络层失败时为 null
     */
    public String getStatusCode() {
        return m_status_code;
    }

    /**
     * 是否为「无权限」。
     *
     * @return 状态码为 403 时返回 true
     */
    public boolean isForbidden() {
        return "403".equals(m_status_code);
    }

    /**
     * 是否为「未登录或会话过期」。
     *
     * @return 状态码为 401 时返回 true
     */
    public boolean isUnauthorized() {
        return "401".equals(m_status_code);
    }
}
