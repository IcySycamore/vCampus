package edu.seu.vcampus.common.constant;

/**
 * 命令码常量
 *
 * <p>
 * 各模块按段划分命令码
 */
public final class Command {

    /** 心跳：网络层保活命令，非业务命令码（客户端每 5 秒发一次）。 */
    public static final int HEARTBEAT = 1;

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

    /** 修改密码（本人改密走 proof 校验；管理员重置他人密码需 USER_MANAGE）。 */
    public static final int USER_CHANGE_PASSWORD = 109;

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

    /** 图书馆命令码段 400-499。 */
    /** 检索馆藏。 */
    public static final int LIBRARY_SEARCH = 400;

    /** 查询我的借阅记录。 */
    public static final int LIBRARY_LIST_BORROWS = 401;

    /** 借书。 */
    public static final int LIBRARY_BORROW = 402;

    /** 还书。 */
    public static final int LIBRARY_RETURN = 403;

    /** 查询当前用户的银行账户及余额。 */
    public static final int BANK_ACCOUNT_QUERY = 601;

    /** 为当前用户的银行账户充值。 */
    public static final int BANK_RECHARGE = 602;

    /** 分页查询当前用户的银行资金流水。 */
    public static final int BANK_TRANSACTION_LIST = 603;

    /** 为当前已认证用户显式开户；重复请求返回已有账户。 */
    public static final int BANK_ACCOUNT_OPEN = 604;

    /** 银行业务状态码：用户尚未开户，响应载荷为未开户异常。 */
    public static final String BANK_ACCOUNT_NOT_OPENED = "B100";

    /**
     * 私有构造器，禁止实例化常量类。
     */
    private Command() {
    }
}
