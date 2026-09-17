package edu.seu.vcampus.common.constant;

/**
 * 状态码常量（消息信封 {@code statusCode} 字段）。
 */
public final class StatusCode {

    /** 成功 */
    public static final String SUCCESS = "200";

    /** 请求参数/格式错误 */
    public static final String BAD_REQUEST = "400";

    /** 未登录或会话失效 */
    public static final String UNAUTHORIZED = "401";

    /** 无权限 */
    public static final String FORBIDDEN = "403";

    /** 资源不存在 */
    public static final String NOT_FOUND = "404";

    /** 服务器内部错误 */
    public static final String INTERNAL_ERROR = "500";

    /** 密码错误 */
    public static final String WRONG_PASSWORD = "P100";

    /** 选定角色与库中角色不符 */
    public static final String ROLE_MISMATCH = "P101";

    /** 账号已禁用 */
    public static final String USER_DISABLED = "P102";

    /** 银行修改密码时校园密码 proof 不正确或一次性验证已失效。 */
    public static final String BANK_CAMPUS_PASSWORD_INVALID = "B101";

    /** 银行当前密码不正确。 */
    public static final String BANK_PASSWORD_INVALID = "B102";

    /** 银行密码错误次数过多，暂时锁定验证。 */
    public static final String BANK_PASSWORD_LOCKED = "B103";

    /** 银行新密码摘要或密码参数不符合协议。 */
    public static final String BANK_PASSWORD_POLICY = "B104";

    /**
     * 私有构造
     */
    private StatusCode() {
    }
}
