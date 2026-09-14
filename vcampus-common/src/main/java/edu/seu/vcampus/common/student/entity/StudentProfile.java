package edu.seu.vcampus.common.student.entity;

import java.io.Serializable;

/**
 * 在校人员档案（值对象）：学生学籍与教师职工信息共用这一套结构。
 *
 * <p>
 * <b>只覆盖师生</b>：管理员是系统运维角色，不是校园成员，没有学籍或职工档案。这一点由
 * {@link PersonCategory} 的取值集合强制——那里根本没有「管理员」这个取值，因此不必在业务
 * 代码里写「角色是管理员则拒绝建档」这种容易漏的运行时判断。
 *
 * <p>
 * 档案只保留自己的字段 + 指向用户账户的外键，不内嵌用户详情。姓名等基本信息统一由用户管理
 * 模块维护，本模块通过 {@code m_user_uuid} 引用 {@code common.user.User} 的全局唯一标识 uuid，
 * 需要详情时凭该 uuid 向用户管理模块查询。
 *
 * <p>
 * 关于软删除：毕业生、退学等场景删除学籍时，只置 {@code m_deleted} 标记， 不物理删除记录。这样选课、成绩、借阅等模块对学号/用户
 * uuid 的引用不会变成 悬空指针，记录仍可查到。
 */
public class StudentProfile implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 4L;

    /** 档案主键（数据库自增分配，插入前为 null）。 */
    private Long m_id;

    /** 所属用户账户 uuid（引用 common.user.User 的全局唯一标识）。 */
    private String m_user_uuid;

    /** 人员类别（学生/教师）。管理员不在取值范围内，见 {@link PersonCategory}。 */
    private PersonCategory m_person_category;

    /**
     * 真实姓名（<b>联表展示字段，不落库</b>）。
     *
     * <p>
     * 学籍表只存账户 uuid，姓名归用户模块维护。查询时由服务端按 uuid 联查填充，仅用于界面
     * 显示；写库时忽略本字段，因此它不会污染学籍表结构。服务端保证填充后非空（账户查不到
     * 时用 uuid 顶上），界面拿到即可直接显示。
     */
    private String m_real_name;

    /** 入校年份：学生为入学年份、教师为入职年份，如 2026。 */
    private int m_join_year;

    /** 在校状态（在读 / 在编 / 休学 / 毕业 等，见 {@link CampusStatus}）。 */
    private CampusStatus m_status;

    /**
     * 学术方向：学生为专业，教师为研究方向。
     *
     * <p>
     * 两边共用一个字段而不是各建一个，是为了让「按方向检索」只需一个索引：查「计算机」时既能
     * 查到该专业的学生，也能查到该方向的教师。教师的招生方向与学生的专业本就是同一套词汇，
     * 分开存反而要在查询时做两次再合并。
     */
    private String m_field;

    /** 软删除标记：true 表示已删除（记录仍保留）。 */
    private boolean m_deleted;

    /**
     * 构造一条空学籍记录。
     */
    public StudentProfile() {
    }

    /**
     * 构造一条学生档案（类别默认为学生），初始为未删除。
     *
     * @param userUuid 所属用户账户 uuid
     * @param joinYear 入学年份
     * @param status 学籍状态
     */
    public StudentProfile(String userUuid, int joinYear, CampusStatus status) {
        this(userUuid, PersonCategory.STUDENT, joinYear, status);
    }

    /**
     * 构造一条在校人员档案，初始为未删除。
     *
     * @param userUuid 所属用户账户 uuid
     * @param category 人员类别（学生或教师）
     * @param joinYear 入学年份（学生）或入职年份（教师）
     * @param status 学籍/在职状态
     */
    public StudentProfile(String userUuid, PersonCategory category, int joinYear,
            CampusStatus status) {
        this.m_user_uuid = userUuid;
        this.m_person_category = category == null ? PersonCategory.STUDENT : category;
        this.m_join_year = joinYear;
        this.m_status = status;
        this.m_deleted = false;
    }

    /** @return 学籍记录主键 */
    public Long getId() {
        return m_id;
    }

    /** @param id 学籍记录主键 */
    public void setId(Long id) {
        this.m_id = id;
    }

    /** @return 所属用户账户 uuid */
    public String getUserUuid() {
        return m_user_uuid;
    }

    /** @param userUuid 所属用户账户 uuid */
    public void setUserUuid(String userUuid) {
        this.m_user_uuid = userUuid;
    }

    /**
     * @return 人员类别；未设置时视为学生（兼容只关心学籍的旧调用方）
     */
    public PersonCategory getPersonCategory() {
        return m_person_category == null ? PersonCategory.STUDENT : m_person_category;
    }

    /** @return 真实姓名（联表填充）；未采集或未填充返回 null */
    public String getRealName() {
        return m_real_name;
    }

    /** @param realName 真实姓名（服务端联查填充，不由客户端提交） */
    public void setRealName(String realName) {
        this.m_real_name = realName;
    }

    /** @param category 人员类别（学生或教师） */
    public void setPersonCategory(PersonCategory category) {
        this.m_person_category = category;
    }

    /** @return 入学年份（学生）或入职年份（教师） */
    public int getJoinYear() {
        return m_join_year;
    }

    /** @param joinYear 入学年份 */
    public void setJoinYear(int joinYear) {
        this.m_join_year = joinYear;
    }

    /** @return 学籍状态 */
    public CampusStatus getStatus() {
        return m_status;
    }

    /** @param status 学籍状态 */
    public void setStatus(CampusStatus status) {
        this.m_status = status;
    }
    /**
     * 取学术方向（学生为专业，教师为研究方向）。
     *
     * @return 专业/研究方向；未登记返回 null
     */
    public String getField() {
        return m_field;
    }

    /**
     * 设置学术方向。
     *
     * @param field 学生传专业，教师传研究方向
     */
    public void setField(String field) {
        this.m_field = field;
    }
    /** @return 是否已软删除 */
    public boolean isDeleted() {
        return m_deleted;
    }

    /**
     * 软删除：置删除标记，不物理删除记录。
     */
    public void markDeleted() {
        this.m_deleted = true;
    }

    /**
     * 恢复一条被软删除的记录。
     */
    public void restore() {
        this.m_deleted = false;
    }
}
