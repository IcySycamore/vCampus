package edu.seu.vcampus.server.module.student;

import edu.seu.vcampus.common.entity.EnrollmentStatus;
import edu.seu.vcampus.common.entity.StudentProfile;

import java.util.List;

/**
 * 学籍业务服务：学籍的登记、查询、更新与软删除。
 *
 * <p>
 * 当前为基本 CRUD 骨架，不含角色权限校验。权限（学生看自己、教务看全部等）
 * 依赖用户管理模块的登录态与角色，待组长合入后再补进各方法（预留 TODO）。
 */
public class StudentService {

    /** 学籍数据访问。 */
    private final StudentDao m_dao;

    /**
     * 构造学籍服务。
     *
     * @param dao 学籍数据访问实现
     */
    public StudentService(StudentDao dao) {
        if (dao == null) {
            throw new IllegalArgumentException("dao must not be null");
        }
        this.m_dao = dao;
    }

    /**
     * 查询一条学籍记录（命令 201）。
     *
     * @param id 学籍记录主键
     * @return 学籍记录，不存在或已删除返回 null
     */
    public StudentProfile queryProfile(Long id) {
        // TODO 权限：学生只能查自己，教务/管理员可查全部。
        if (id == null) {
            return null;
        }
        return m_dao.findById(id);
    }

    /**
     * 按用户 id 查本人的学籍记录（学生“看自己”用）。
     *
     * @param userId 用户账户 id
     * @return 学籍记录，不存在或已删除返回 null
     */
    public StudentProfile queryByUserId(Long userId) {
        // TODO 权限：学生只能查自己的 userId。
        if (userId == null) {
            return null;
        }
        return m_dao.findByUserId(userId);
    }

    /**
     * 修改学籍状态（在读/休学/退学/毕业）。
     *
     * @param id        学籍记录主键
     * @param newStatus 新状态
     * @return 是否成功
     */
    public boolean changeStatus(Long id, EnrollmentStatus newStatus) {
        // TODO 权限：仅教务/管理员可改状态。
        if (id == null || newStatus == null) {
            return false;
        }
        StudentProfile profile = m_dao.findById(id);
        if (profile == null) {
            return false;
        }
        profile.setStatus(newStatus);
        return m_dao.update(profile);
    }

    /**
     * 列出全部未删除的学籍记录（管理端用）。
     *
     * @return 学籍记录列表
     */
    public List<StudentProfile> listAllProfiles() {
        // TODO 权限：仅教务/管理员可调用。
        return m_dao.findAll();
    }

    /**
     * 新生学籍登记（命令 204）。
     *
     * @param profile 学籍记录（userId 必填）
     * @return 是否成功
     */
    public boolean registerStudent(StudentProfile profile) {
        // TODO 权限：仅教务/管理员可登记。
        if (profile == null) {
            return false;
        }
        return m_dao.insert(profile);
    }

    /**
     * 更新学籍记录（命令 202/203）。
     *
     * @param profile 学籍记录（主键必填）
     * @return 是否成功
     */
    public boolean updateProfile(StudentProfile profile) {
        // TODO 权限：学生提交修改申请需教务审核；教务直接改需审批流。
        if (profile == null) {
            return false;
        }
        return m_dao.update(profile);
    }

    /**
     * 软删除学籍记录（命令 205）。
     *
     * @param id 学籍记录主键
     * @return 是否成功
     */
    public boolean deleteStudent(Long id) {
        // TODO 权限：仅管理员可删除。
        return m_dao.softDelete(id);
    }
}
