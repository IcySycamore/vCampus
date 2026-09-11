package edu.seu.vcampus.common.entity;

import java.io.Serializable;

/**
 * 学生学籍记录（值对象）。
 *
 * <p>
 * 学籍记录只保留「学籍自己的字段 + 指向用户账户的外键」，不内嵌用户详情。
 * 用户基本信息（姓名、电话等）统一由用户管理模块维护，本模块通过
 * {@code m_user_uuid} 引用 {@code common.user.User} 的全局唯一标识 uuid，
 * 需要详情时凭该 uuid 向用户管理模块查询。
 *
 * <p>
 * 关于软删除：毕业生、退学等场景删除学籍时，只置 {@code m_deleted} 标记，
 * 不物理删除记录。这样选课、成绩、借阅等模块对学号/用户 uuid 的引用不会变成
 * 悬空指针，记录仍可查到。
 */
public class StudentProfile implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 学籍记录主键（数据库自增分配，插入前为 null）。 */
    private Long m_id;

    /** 所属用户账户 uuid（引用 common.user.User 的全局唯一标识）。 */
    private String m_user_uuid;

    /** 入学年份，如 2026。 */
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
     * 构造一条学籍记录，初始为未删除。
     *
     * @param userUuid   所属用户账户 uuid
     * @param enrollYear 入学年份
     * @param status     学籍状态
     */
    public StudentProfile(String userUuid, int enrollYear,
            EnrollmentStatus status) {
        this.m_user_uuid = userUuid;
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

    /** @return 入学年份 */
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
