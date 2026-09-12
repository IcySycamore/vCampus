package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.PersonCategory;
import edu.seu.vcampus.common.student.entity.StudentProfile;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.server.user.AccountProvisioner;

import java.util.Calendar;

/**
 * 在校人员档案开户钩子：账号建立时同步建好档案（见 {@link AccountProvisioner}）。
 *
 * <p>
 * 对<b>在校人员</b>（学生、教师）建档，管理员不建——管理员是系统运维角色，不是校园成员，
 * {@link PersonCategory} 里也没有对应取值。建档后本人登录即可用 201 查到自己的档案与姓名，
 * 不必让业务代码到处判「还没有档案记录」。
 *
 * <p>
 * 教师也建档是「教师的研究方向和学生的专业对应」这条需求的前提：教师档案若不存在，
 * 按方向检索就永远只命中学生，教师那一侧是空的。
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
     * 为在校人员（学生、教师）建立档案：年份取当前年份，状态为在校。
     *
     * @param userUuid 账户全局唯一标识
     * @param userName 登录名（未使用）
     * @param role 角色；管理员与无法解析的角色不建档
     */
    @Override
    public void provision(String userUuid, String userName, Role role) {
        PersonCategory category = categoryOf(role);
        if (category == null || userUuid == null) {
            return;
        }
        if (m_dao.findByUserUuid(userUuid) != null) {
            return;
        }
        StudentProfile profile = new StudentProfile(userUuid, category, currentYear(),
                CampusStatus.ENROLLED);
        if (!m_dao.insert(profile)) {
            throw new IllegalStateException("在校档案建立失败: " + userUuid);
        }
    }

    /**
     * 角色 → 人员类别。
     *
     * @param role 登录角色
     * @return 人员类别；管理员或 null 角色返回 null（不建档）
     */
    private static PersonCategory categoryOf(Role role) {
        if (role == Role.STUDENT) {
            return PersonCategory.STUDENT;
        }
        return role == Role.TEACHER ? PersonCategory.TEACHER : null;
    }

    /**
     * 撤销账号时软删除档案（保留历史引用，不做物理删除）。
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
