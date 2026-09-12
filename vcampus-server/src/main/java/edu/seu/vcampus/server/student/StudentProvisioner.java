package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.entity.EnrollmentStatus;
import edu.seu.vcampus.common.student.entity.StudentProfile;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.server.user.AccountProvisioner;

import java.util.Calendar;

/**
 * 学籍开户钩子：账号建立时同步建好学籍档案（见 {@link AccountProvisioner}）。
 *
 * <p>
 * 只对角色为「学生」的账号建档案——教师与管理员没有学籍。建档后学生可立即查到自己的学籍， 不需要业务代码到处判「还没有学籍记录」。
 *
 * <p>
 * 幂等：已存在档案（含被软删除的）时不重复插入，保证重复注册/重试/演示账号预置都不会产生第二条。
 */
public class StudentProvisioner implements AccountProvisioner {

    /** 学籍数据访问。 */
    private final StudentDao m_dao;

    /**
     * 构造学籍开户钩子。
     *
     * @param dao 学籍数据访问
     * @throws IllegalArgumentException dao 为 null
     */
    public StudentProvisioner(StudentDao dao) {
        if (dao == null) {
            throw new IllegalArgumentException("dao must not be null");
        }
        this.m_dao = dao;
    }

    /**
     * 为学生账号建立学籍档案（入学年份取当前年份，状态为在读）。
     *
     * @param userUuid 账户全局唯一标识
     * @param userName 登录名（未使用）
     * @param role 角色；非学生不建档
     */
    @Override
    public void provision(String userUuid, String userName, Role role) {
        if (role != Role.STUDENT || userUuid == null) {
            return;
        }
        if (m_dao.findByUserUuid(userUuid) != null) {
            return;
        }
        StudentProfile profile = new StudentProfile(userUuid, currentYear(),
                EnrollmentStatus.ENROLLED);
        if (!m_dao.insert(profile)) {
            throw new IllegalStateException("学籍档案建立失败: " + userUuid);
        }
    }

    /**
     * 撤销账号时软删除学籍档案（保留历史引用，不做物理删除）。
     *
     * @param userUuid 账户全局唯一标识
     */
    @Override
    public void revoke(String userUuid) {
        if (userUuid == null) {
            return;
        }
        StudentProfile profile = m_dao.findByUserUuid(userUuid);
        if (profile != null && profile.getId() != null && !profile.isDeleted()) {
            m_dao.softDelete(profile.getId());
        }
    }

    private int currentYear() {
        return Calendar.getInstance().get(Calendar.YEAR);
    }
}
