package edu.seu.vcampus.client.api;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;

/**
 * 状态码 → 中文文案的唯一映射处（见 ADR-0009 D2）。
 *
 * <p>
 * 服务端常常只回状态码、{@code data} 为空，因此文案必须由客户端集中提供， 不允许各页面各写一套 if/else（登录页原先的
 * {@code messageFor} 已收敛到这里）。
 *
 * <p>
 * {@code Lxxx} 是<b>客户端本地</b>状态码，只表示「请求没能到达/没能拿到有效响应」， 不出现在线上协议里，也不与服务端码冲突。
 */
public final class ApiErrors {

    /** 本地码：与服务器的连接已断开。 */
    public static final String LOCAL_NETWORK = "L100";

    /** 本地码：等待响应超时。 */
    public static final String LOCAL_TIMEOUT = "L101";

    /** 本地码：等待被中断（操作取消）。 */
    public static final String LOCAL_INTERRUPTED = "L102";

    /** 本地码：响应载荷格式不符合预期。 */
    public static final String LOCAL_MALFORMED = "L103";

    /** 私有构造器，禁止实例化常量映射类。 */
    private ApiErrors() {
    }

    /**
     * 把状态码翻译成给用户看的文案。
     *
     * @param statusCode 状态码；null 表示无法判定
     * @return 中文文案，未知码给出带码兜底文案
     */
    public static String messageFor(String statusCode) {
        if (statusCode == null) {
            return "操作失败，请稍后重试";
        }
        if (StatusCode.SUCCESS.equals(statusCode)) {
            return "操作成功";
        }
        if (StatusCode.BAD_REQUEST.equals(statusCode)) {
            return "请求不合法，或数据已存在";
        }
        if (StatusCode.UNAUTHORIZED.equals(statusCode)) {
            return "登录状态已失效，请重新登录";
        }
        if (StatusCode.FORBIDDEN.equals(statusCode)) {
            return "当前身份没有该操作权限";
        }
        if (StatusCode.NOT_FOUND.equals(statusCode)) {
            return "目标记录不存在";
        }
        if (StatusCode.INTERNAL_ERROR.equals(statusCode)) {
            return "服务器内部错误，请稍后重试";
        }
        if (StatusCode.WRONG_PASSWORD.equals(statusCode)) {
            return "原密码不正确";
        }
        if (StatusCode.ROLE_MISMATCH.equals(statusCode)) {
            return "所选身份与该账号不符";
        }
        if (StatusCode.USER_DISABLED.equals(statusCode)) {
            return "该账号已被禁用，请联系管理员";
        }
        if (Command.BANK_ACCOUNT_NOT_OPENED.equals(statusCode)) {
            return "请先开通校园银行账户";
        }
        if (LOCAL_NETWORK.equals(statusCode)) {
            return "与服务器的连接已断开，请重新登录";
        }
        if (LOCAL_TIMEOUT.equals(statusCode)) {
            return "服务器无响应（超时）";
        }
        if (LOCAL_INTERRUPTED.equals(statusCode)) {
            return "操作已取消";
        }
        if (LOCAL_MALFORMED.equals(statusCode)) {
            return "服务器响应格式异常";
        }
        return "操作失败（" + statusCode + "）";
    }
}
