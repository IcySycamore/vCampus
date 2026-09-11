package edu.seu.vcampus.common.constant;

/**
 * 命令码常量
 *
 * <p>
 * 各模块按段划分命令码
 */
public final class Command {

    
    /** 用户管理号段100-199 */
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

    /** 登录验证（挑战-应答第③步，回 proof）。 */
    public static final int USER_LOGIN_VERIFY = 110;

    /** 学籍命令码段起始。 */
    public static final int STUDENT_SEGMENT_START = 200;

    /** 查询学籍。 */
    public static final int STUDENT_QUERY = 201;

    /** 提交修改申请。 */
    public static final int STUDENT_MODIFY_APPLY = 202;

    /** 审核修改（确认/驳回）。 */
    public static final int STUDENT_MODIFY_AUDIT = 203;

    /** 新生学籍登记。 */
    public static final int STUDENT_REGISTER = 204;

    /** 删除学籍（软删除）。 */
    public static final int STUDENT_DELETE = 205;

    /** 修改学籍状态（在读/休学/退学/毕业）。 */
    public static final int STUDENT_CHANGE_STATUS = 206;

    /** 学籍命令码段终止。 */
    public static final int STUDENT_SEGMENT_END = 299;

    /**
     * 私有构造器，禁止实例化常量类。
     */
    private Command() {
    }
}