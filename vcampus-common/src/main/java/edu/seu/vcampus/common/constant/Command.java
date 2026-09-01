package edu.seu.vcampus.common.constant;

/**
 * 命令码常量（用户管理段 100–199）。
 *
 * <p>
 * 各模块按段划分命令码；此处为用户管理模块的保留段。
 */
public final class Command {

    /** 登录。 */
    public static final int USER_LOGIN = 100;

    /** 登出。 */
    public static final int USER_LOGOUT = 101;

    /** 注册。 */
    public static final int USER_REGISTER = 102;

    /** 批量注册。 */
    public static final int USER_BATCH_REGISTER = 103;

    /** 注销（软删除）。 */
    public static final int USER_UNREGISTER = 104;

    /** 批量注销。 */
    public static final int USER_BATCH_UNREGISTER = 105;

    /** 分页/过滤查询用户。 */
    public static final int USER_LIST = 106;

    /** 编辑用户（不改角色）。 */
    public static final int USER_UPDATE = 107;

    /** 启用/禁用用户。 */
    public static final int USER_TOGGLE_ENABLED = 108;

    /** 获取盐（登录/注册握手）。 */
    public static final int USER_SALT_REQUEST = 109;

    /**
     * 私有构造器，禁止实例化常量类。
     */
    private Command() {
    }
}