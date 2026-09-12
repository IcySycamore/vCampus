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
    private static final long serialVersionUID = 3L;

    /** 学籍记录主键（数据库自增分配，插入前为 null）。 */
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
     * 显示；写库时忽略本字段，因此它不会污染学籍表结构。
     */
    private String m_real_name;

    /** 入学年份（学生）或入职年份（教师），如 2026。 */
    private int m_enroll_year;

    /** 学籍状态（在读/休学/退学/毕业）。 */
    private EnrollmentStatus m_status;

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
     * @param enrollYear 入学年份
     * @param status 学籍状态
     */
    public StudentProfile(String userUuid, int enrollYear, EnrollmentStatus status) {
        this(userUuid, PersonCategory.STUDENT, enrollYear, status);
    }

    /**
     * 构造一条在校人员档案，初始为未删除。
     *
     * @param userUuid 所属用户账户 uuid
     * @param category 人员类别（学生或教师）
     * @param enrollYear 入学年份（学生）或入职年份（教师）
     * @param status 学籍/在职状态
     */
    public StudentProfile(String userUuid, PersonCategory category, int enrollYear,
            EnrollmentStatus status) {
        this.m_user_uuid = userUuid;
        this.m_person_category = category == null ? PersonCategory.STUDENT : category;
        this.m_enroll_year = enrollYear;
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

    /**
     * 取界面显示用的称呼：优先真实姓名，未采集时回退账户 uuid。
     *
     * <p>
     * 界面拿到就能直接贴上去，不必自己写「姓名为空就显示别的」这种分支；连 uuid 都没有时返回空串。
     *
     * @return 显示名
     */
    public String getDisplayName() {
        if (m_real_name != null && m_real_name.trim().length() > 0) {
            return m_real_name.trim();
        }
        return m_user_uuid == null ? "" : m_user_uuid;
    }

    /** @param category 人员类别（学生或教师） */
    public void setPersonCategory(PersonCategory category) {
        this.m_person_category = category;
    }

    /** @return 入学年份（学生）或入职年份（教师） */
    public int getEnrollYear() {
        return m_enroll_year;
    }

    /** @param enrollYear 入学年份 */
    public void setEnrollYear(int enrollYear) {
        this.m_enroll_year = enrollYear;
    }

    /** @return 学籍状态 */
    public EnrollmentStatus getStatus() {
        return m_status;
    }

    /** @param status 学籍状态 */
    public void setStatus(EnrollmentStatus status) {
        this.m_status = status;
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
