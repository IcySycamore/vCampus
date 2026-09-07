package edu.seu.vcampus.common.entity;

/**
 * 学籍状态枚举。
 *
 * <p>
 * 描述一名学生在校期间学籍所处的阶段。状态与软删除标记（见
 * {@link StudentProfile#isDeleted()}）是两个正交维度：状态表示学生在读还是离校，
 * 软删除表示学籍记录是否已从系统中移除（仅标记，记录仍保留）。
 */
public enum EnrollmentStatus {

    /** 在读。 */
    ENROLLED,

    /** 休学。 */
    SUSPENDED,

    /** 退学。 */
    WITHDRAWN,

    /** 毕业。 */
    GRADUATED
}
