package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.course.Student;
import edu.seu.vcampus.common.course.Teacher;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.server.user.AccountProvisioner;

import java.util.ArrayList;
import java.util.List;

/**
 * 选课开户钩子：账号建立时同步建好课程模块的师生档案（见 {@link AccountProvisioner}）。
 *
 * <p>
 * 学生与教师在课程模块各有一份 1:1 档案（{@link Student} / {@link Teacher}），按账户 uuid 索引； 管理员不建。档案挂靠的学院取自<b>学院池</b>（由
 * {@link CollegePoolBootstrap} 从引导文件建立）， 专业/研究方向由后续业务再补充。
 *
 * <p>
 * 学院必须真的在库里：它是档案的非空外键目标，挂到不存在的学院会被数据库以 1452 拒掉， 而开户钩子失败会连带回滚整个注册。
 */
public class CourseProvisioner implements AccountProvisioner {

    /** 课程数据访问。 */
    private final CourseDao m_dao;

    /** 学院池：新账号的档案从中挑一所挂靠。 */
    private final List<String> m_college_pool;

    /**
     * 构造选课开户钩子。
     *
     * @param dao         课程数据访问
     * @param collegePool 学院池（学院 uuid 列表），不能为空
     */
    public CourseProvisioner(CourseDao dao, List<String> collegePool) {
        if (dao == null) {
            throw new IllegalArgumentException("dao must not be null");
        }
        if (collegePool == null || collegePool.isEmpty()) {
            throw new IllegalArgumentException("collegePool must not be empty");
        }
        this.m_dao = dao;
        this.m_college_pool = new ArrayList<String>(collegePool);
    }

    /**
     * 从学院池里给这个账号挑一所学院。
     *
     * <p>
     * 按账号 uuid 的散列取模，因此是<b>稳定</b>的：同一个账号每次（含重启后）都是同一所学院， 不依赖任何内存状态；池里只有一所学院时行为就是「全校都挂它」。
     *
     * @param userUuid 账户全局唯一标识
     * @return 学院 uuid
     */
    private String collegeFor(String userUuid) {
        int index = (userUuid.hashCode() & 0x7fffffff) % m_college_pool.size();
        return m_college_pool.get(index);
    }

    /**
     * 为学生/教师建立课程模块档案（幂等）。
     *
     * @param userUuid    账户全局唯一标识
     * @param displayName 姓名（课程模块不落姓名，仅保留签名一致性）
     * @param role        角色；管理员与无法解析的角色不建档
     */
    @Override
    public void provision(String userUuid, String displayName, Role role) {
        if (userUuid == null) {
            return;
        }
        if (role == Role.STUDENT) {
            if (m_dao.findStudent(userUuid) == null) {
                m_dao.saveStudent(new Student(userUuid, collegeFor(userUuid), null));
            }
        } else if (role == Role.TEACHER) {
            if (m_dao.findTeacher(userUuid) == null) {
                m_dao.saveTeacher(new Teacher(userUuid, collegeFor(userUuid)));
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
