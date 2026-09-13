package edu.seu.vcampus.client.api;

/**
 * 客户端 API 的统一失败异常（非受检），吸收原 {@code AuthException}（见 ADR-0009 D2）。
 *
 * <p>
 * 携带状态码：服务端拒绝时是协议状态码（{@code StatusCode} / {@code Command.BANK_ACCOUNT_NOT_OPENED} 等），
 * 本地失败时是 {@link ApiErrors} 的 {@code Lxxx} 局部码（不上线协议）。
 * 文案统一由 {@link ApiErrors#messageFor(String)} 提供，页面直接显示 {@code getMessage()}。
 *
 * <p>
 * 非受检是刻意的：模块 API 不声明受检异常，页面就不需要写 try/catch——异常统一由
 * {@code UiTasks} 兜住并提示。
 */
public class ApiException extends RuntimeException {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 本地状态码前缀（超时/断线/被中断/响应格式异常）。 */
    private static final String LOCAL_PREFIX = "L";

    /** 状态码；无法判定时为 null。 */
    private final String m_status_code;

    /**
     * 按状态码构造异常，文案自动取 {@link ApiErrors#messageFor(String)}。
     *
     * @param statusCode 状态码
     */
    public ApiException(String statusCode) {
        this(statusCode, ApiErrors.messageFor(statusCode));
    }

    /**
     * 构造异常并指定文案。
     *
     * @param statusCode 状态码
     * @param message 描述
     */
    public ApiException(String statusCode, String message) {
        super(message);
        this.m_status_code = statusCode;
    }

    /** @return 状态码；可能是本地码 Lxxx 或 null */
    public String getStatusCode() {
        return m_status_code;
    }

    /**
     * 是否由本地原因造成（连接断开、超时、被中断、响应异常），而非服务端拒绝。
     *
     * @return true 表示本地失败
     */
    public boolean isLocal() {
        return m_status_code != null && m_status_code.startsWith(LOCAL_PREFIX);
    }
}
