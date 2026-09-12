package edu.seu.vcampus.common.user.entity;

/**
 * 能力（Capability）：权限判定的最小单位，双端共享（见 ADR-0009 D6、报告 §2.3）。
 *
 * <p>
 * 角色到能力的映射集中在 {@link Permissions#can(Role, Capability)}：服务端用它判 403，
 * 客户端用它决定控件可见性。客户端判断只是「显示/隐藏」，<b>不作为安全边界</b>。
 *
 * <p>
 * 维护规则：枚举<b>只能追加，不得重排或删除</b>（避免二进制兼容与语义漂移）； 新增能力必须同时在 {@code PermissionsTest}
 * 的能力矩阵里补一行。
 */
public enum Capability {

    /** 用户管理：查询、编辑、启停、注册、注销、重置密码。 */
    USER_MANAGE,

    /** 学籍：查看全部学生学籍（管理轨）。 */
    STUDENT_VIEW_ALL,

    /** 学籍：提交本人的修改申请（我的轨）。 */
    STUDENT_MODIFY_APPLY,

    /** 学籍：查看待审申请并审核。 */
    STUDENT_MODIFY_AUDIT,

    /** 学籍：新生登记。 */
    STUDENT_REGISTER,

    /** 学籍：注销学籍。 */
    STUDENT_DELETE,

    /** 学籍：修改学籍状态。 */
    STUDENT_CHANGE_STATUS,

    /** 选课：选课与退课（我的轨）。 */
    COURSE_SELECT,

    /** 选课：查看课程名单与成绩（受限管理轨，教师限自己授的课）。 */
    COURSE_GRADE_VIEW_ALL,

    /** 选课：成绩录入（受限管理轨）。 */
    COURSE_GRADE_EDIT,

    /** 选课：课程维护。 */
    COURSE_MANAGE,

    /** 图书馆：借书与归还本人的书。 */
    LIBRARY_BORROW,

    /** 图书馆：借阅管理与代还。 */
    LIBRARY_BORROW_MANAGE,

    /** 图书馆：馆藏维护。 */
    LIBRARY_MANAGE,

    /** 商店：下单与取消本人订单。 */
    SHOP_BUY,

    /** 商店：全量订单查询与状态推进。 */
    SHOP_ORDER_MANAGE,

    /** 商店：商品维护。 */
    SHOP_MANAGE
}
