package edu.seu.vcampus.common.constant;

/**
 * 状态码常量（消息信封 {@code statusCode} 字段）。
 */
public final class StatusCode {

    /** 成功。 */
    public static final String SUCCESS = "200";

    /** 请求参数/格式错误。 */
    public static final String BAD_REQUEST = "400";

    /** 未登录或会话失效。 */
    public static final String UNAUTHORIZED = "401";

    /** 无权限（角色不符）。 */
    public static final String FORBIDDEN = "403";

    /** 资源不存在（无此用户）。 */
    public static final String NOT_FOUND = "404";

    /** 服务器内部错误。 */
    public static final String INTERNAL_ERROR = "500";

    /** 密码错误。 */
    public static final String WRONG_PASSWORD = "P100";

    /** 选定角色与库中角色不符。 */
    public static final String ROLE_MISMATCH = "P101";

    /** 账号已禁用（软删除）。 */
    public static final String USER_DISABLED = "P102";

    /**
     * 私有构造器，禁止实例化常量类。
     */
    private StatusCode() {
    }
}