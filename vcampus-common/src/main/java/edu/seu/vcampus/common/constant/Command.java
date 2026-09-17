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

    /** 修改在校状态（在读/在编/休学/毕业 等）。 */
    public static final int STUDENT_CHANGE_STATUS = 206;

    /** 待审的学籍修改申请列表（分页）。 */
    public static final int STUDENT_MODIFY_LIST = 207;

    /** 学籍列表查询（分页，管理端）。 */
    public static final int STUDENT_LIST = 208;

    /** 学籍命令码段终止。 */
    public static final int STUDENT_SEGMENT_END = 299;

    /** 选课命令码段 300-399。 */
    /** 查询课程列表（登录即可）。 */
    public static final int COURSE_LIST = 300;

    /** 选课（学生）。 */
    public static final int COURSE_SELECT = 301;

    /** 退课（学生）。 */
    public static final int COURSE_DROP = 302;

    /** 查询本人成绩（登录即可）。 */
    public static final int SCORE_QUERY = 303;

    /** 录入/修改成绩（教师/管理员）。 */
    public static final int SCORE_SAVE = 304;

    /** 查询本人授课课程（教师）。 */
    public static final int COURSE_TEACHING_LIST = 305;

    /** 手动排课（管理员）。 */
    public static final int COURSE_SCHEDULE = 306;

    /** 查询本人偏好时间槽（教师）。 */
    public static final int COURSE_PREFERENCE_GET = 307;

    /** 设置本人偏好时间槽（教师）。 */
    public static final int COURSE_PREFERENCE_SET = 308;

    /** 查询教室列表（管理员）。 */
    public static final int COURSE_CLASSROOM_LIST = 309;

    /** 添加课程（管理员）。 */
    public static final int COURSE_ADD = 310;

    /** 修改课程（管理员）。 */
    public static final int COURSE_UPDATE = 311;

    /** 删除课程（管理员）。 */
    public static final int COURSE_DELETE = 312;

    /** 认领课程（教师）。 */
    public static final int COURSE_CLAIM = 313;

    /** 查询全部教师（含研究方向，管理员排课用）。 */
    public static final int COURSE_TEACHER_LIST = 314;

    /** 查询本人已选课程（学生课表）。 */
    public static final int COURSE_MY_SELECTIONS = 315;

    /** 查询本人可用时间槽（教师）。 */
    public static final int COURSE_AVAILABLE_GET = 316;

    /** 设置本人可用时间槽（教师）。 */
    public static final int COURSE_AVAILABLE_SET = 317;

    /** 图书馆命令码段 400-499。 */
    /** 检索馆藏。 */
    public static final int LIBRARY_SEARCH = 400;

    /** 查询我的借阅记录。 */
    public static final int LIBRARY_LIST_BORROWS = 401;

    /** 借书。 */
    public static final int LIBRARY_BORROW = 402;

    /** 还书。 */
    public static final int LIBRARY_RETURN = 403;

    /** 续借。 */
    public static final int LIBRARY_RENEW = 404;

    /** 录入馆藏（避开设计稿预留的 404～406）。 */
    public static final int LIBRARY_CREATE_BOOK = 408;

    /** 修改馆藏。 */
    public static final int LIBRARY_UPDATE_BOOK = 409;

    /** 逻辑下架馆藏。 */
    public static final int LIBRARY_WITHDRAW_BOOK = 410;

    /** 管理员查询全部馆藏。 */
    public static final int LIBRARY_CATALOG_SEARCH = 411;

    /** 图书无可借馆藏时提交预约。 */
    public static final int LIBRARY_RESERVE = 412;

    /** 查询我的预约。 */
    public static final int LIBRARY_LIST_RESERVATIONS = 413;

    /** 取消我的预约。 */
    public static final int LIBRARY_CANCEL_RESERVATION = 414;

    /** 从校园银行账户缴纳一笔借阅记录的逾期滞纳金。 */
    public static final int LIBRARY_PAY_FINE = 415;

    /** 查询当前用户的图书馆读者账户。 */
    public static final int LIBRARY_ACCOUNT_QUERY = 416;

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
