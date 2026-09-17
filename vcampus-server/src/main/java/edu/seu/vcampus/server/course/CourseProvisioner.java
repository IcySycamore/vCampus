package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.course.Student;
import edu.seu.vcampus.common.course.Teacher;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.server.user.AccountProvisioner;

/**
 * 选课开户钩子：账号建立时同步建好课程模块的师生档案（见 {@link AccountProvisioner}）。
 *
 * <p>学生与教师在课程模块各有一份 1:1 档案（{@link Student} / {@link Teacher}），按账户 uuid 索引；
 * 管理员不建。档案统一挂到演示学院，专业/研究方向由后续业务再补充——本模块是内存骨架，
 * 重启后档案随演示课表一并重建。
 */
public class CourseProvisioner implements AccountProvisioner {

    /** 课程数据访问。 */
    private final CourseDao m_dao;

    /** 默认学院 uuid（新账号档案挂靠的学院）。 */
    private final String m_default_college_uuid;

    /**
     * 构造选课开户钩子。
     *
     * @param dao 课程数据访问
     * @param defaultCollegeUuid 默认学院 uuid
     */
    public CourseProvisioner(CourseDao dao, String defaultCollegeUuid) {
        if (dao == null) {
            throw new IllegalArgumentException("dao must not be null");
        }
        this.m_dao = dao;
        this.m_default_college_uuid = defaultCollegeUuid;
    }

    /**
     * 为学生/教师建立课程模块档案（幂等）。
     *
     * @param userUuid 账户全局唯一标识
     * @param displayName 姓名（课程模块不落姓名，仅保留签名一致性）
     * @param role 角色；管理员与无法解析的角色不建档
     */
    @Override
    public void provision(String userUuid, String displayName, Role role) {
        if (userUuid == null) {
            return;
        }
        if (role == Role.STUDENT) {
            if (m_dao.findStudent(userUuid) == null) {
                m_dao.saveStudent(new Student(userUuid, m_default_college_uuid, null));
            }
        } else if (role == Role.TEACHER) {
            if (m_dao.findTeacher(userUuid) == null) {
                m_dao.saveTeacher(new Teacher(userUuid, m_default_college_uuid));
            }
        }
    }

    /**
     * 撤销账户时清理课程模块档案（内存骨架，重启即重建，这里仅移除教师索引）。
     *
     * @param userUuid 账户全局唯一标识
     */
    @Override
    public void revoke(String userUuid) {
        if (userUuid != null) {
            m_dao.deleteTeacher(userUuid);
        }
    }
}
