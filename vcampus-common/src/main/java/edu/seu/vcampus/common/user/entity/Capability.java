package edu.seu.vcampus.common.user.entity;

/**
 * 能力枚举：权限判定的最小单位，按模块号段分组。
 *
 * <p>
 * 界面用它决定控件可见性（{@code Permissions.can(role, capability)}），服务端用它做准入判定
 * ——服务端的 403 才是最终防线，界面隐藏只是为了不给出无效操作。
 *
 * <p>
 * <b>维护规则</b>：本枚举<b>只能追加、不得重排</b>（序号不落库，但重排会产生无谓的 diff 噪音，
 * 也容易让阅读者误判新旧）。新增能力时必须同步在 {@link Permissions} 的权限矩阵里补一行，
 * 缺省语义为「拒绝」。
 */
public enum Capability {

    // ---------- 用户管理 100-199 ----------

    /** 用户查询 / 编辑 / 启停 / 注册 / 注销 / 重置密码。 */
    USER_MANAGE,

    // ---------- 学生学籍 200-299 ----------

    /** 查看他人学籍：学籍列表与详情（仅自己的学籍不需要本能力，登录即可）。 */
    STUDENT_VIEW_ALL,

    /** 提交本人的学籍修改申请。 */
    STUDENT_MODIFY_APPLY,

    /** 修改申请的待审列表与审核（通过 / 驳回）。 */
    STUDENT_MODIFY_AUDIT,

    /** 新生学籍登记。 */
    STUDENT_REGISTER,

    /** 注销学籍（软删除）。 */
    STUDENT_DELETE,

    /** 修改在校状态（在读 / 在编 / 休学 / 毕业 等）。 */
    STUDENT_CHANGE_STATUS,

    // ---------- 选课系统 300-399 ----------

    /** 选课 / 退课。 */
    COURSE_SELECT,

    /** 查看课程名单与成绩。 */
    COURSE_GRADE_VIEW_ALL,

    /** 录入课程成绩。 */
    COURSE_GRADE_EDIT,

    /** 课程维护。 */
    COURSE_MANAGE,

    // ---------- 图书馆 400-499 ----------

    /** 借书 / 归还本人的书。 */
    LIBRARY_BORROW,

    /** 借阅管理与代还（跨用户操作）。 */
    LIBRARY_BORROW_MANAGE,

    /** 馆藏维护。 */
    LIBRARY_MANAGE,

    // ---------- 校园商店 500-599 ----------

    /** 下单 / 取消本人的订单。 */
    SHOP_BUY,

    /** 全量订单查询与状态推进。 */
    SHOP_ORDER_MANAGE,

    /** 商品维护。 */
    SHOP_MANAGE
}
