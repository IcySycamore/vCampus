package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.student.dto.ModifyRequestQuery;
import edu.seu.vcampus.common.student.dto.StudentQuery;
import edu.seu.vcampus.common.student.entity.EnrollmentStatus;
import edu.seu.vcampus.common.student.entity.StudentModifyRequest;
import edu.seu.vcampus.common.student.entity.StudentProfile;
import edu.seu.vcampus.server.user.UserRepository;

import java.util.List;
import java.util.Map;

/**
 * 学籍业务服务：档案的增删改查与分页列表，以及审核流（202 / 203 / 207）的对外入口。
 * 本层不判权限（由 handler 按 Permissions 放行），但负责把展示用姓名联查补齐。
 */
public class StudentService {

    /** 学籍数据访问。 */
    private final StudentDao m_dao;

    /** 修改申请的审核流（202 / 203 / 207）。 */
    private final StudentModifyFlow m_modify_flow;

    /** 展示用姓名的联查器（查档案时按 uuid 去用户模块补姓名）。 */
    private final StudentProfileDecorator m_decorator;

    /** 构造服务（不联查姓名，仅供单模块测试使用）。
     *
     * @param dao 学籍数据访问实现
     */
    public StudentService(StudentDao dao) {
        this(dao, new StudentModifyRequestDaoMemory());
    }

    /** 构造服务（不联查姓名）。
     *
     * @param dao 学籍数据访问实现
     * @param requests 修改申请单存储
     */
    public StudentService(StudentDao dao, StudentModifyRequestDao requests) {
        this(dao, requests, null);
    }

    /**
     * 构造服务。
     *
     * @param dao 学籍数据访问实现
     * @param requests 修改申请单存储
     * @param users 用户凭证存储（用于给档案补姓名；可为 null，此时不联查）
     */
    public StudentService(StudentDao dao, StudentModifyRequestDao requests,
            UserRepository users) {
        if (dao == null) {
            throw new IllegalArgumentException("dao must not be null");
        }
        if (requests == null) {
            throw new IllegalArgumentException("requests must not be null");
        }
        this.m_dao = dao;
        this.m_modify_flow = new StudentModifyFlow(requests, dao);
        this.m_decorator = new StudentProfileDecorator(users);
    }

    /**
     * 查询一条学籍记录（命令 201），并把姓名一并联查出来。
     * @param id 学籍记录主键
     * @return 学籍记录，不存在或已删除返回 null
     */
    public StudentProfile queryProfile(Long id) {
        return id == null ? null : m_decorator.decorate(m_dao.findById(id));
    }

    /**
     * 按账户 uuid 查本人的学籍记录（学生「看自己」用），同样补上姓名。
     *
     * @param userUuid 用户账户 uuid
     * @return 学籍记录，不存在或已删除返回 null
     */
    public StudentProfile queryByUserUuid(String userUuid) {
        return userUuid == null ? null : m_decorator.decorate(m_dao.findByUserUuid(userUuid));
    }

    /**
     * 修改学籍状态（在读/休学/退学/毕业）。
     * @param id 学籍记录主键
     * @param newStatus 新状态
     * @return 是否成功
     */
    public boolean changeStatus(Long id, EnrollmentStatus newStatus) {
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
        return m_decorator.decorate(m_dao.findAll());
    }

    /**
     * 新生学籍登记（命令 204）。
     * @param profile 学籍记录（账户 uuid 必填）
     * @return 是否成功
     */
    public boolean registerStudent(StudentProfile profile) {
        if (profile == null) {
            return false;
        }
        return m_dao.insert(profile);
    }

    /**
     * 更新学籍记录（命令 202/203）。
     * @param profile 学籍记录（主键必填）
     * @return 是否成功
     */
    public boolean updateProfile(StudentProfile profile) {
        if (profile == null) {
            return false;
        }
        return m_dao.update(profile);
    }

    /**
     * 软删除学籍记录（命令 205）。
     * @param id 学籍记录主键
     * @return 是否成功
     */
    public boolean deleteStudent(Long id) {
        return m_dao.softDelete(id);
    }

    /**
     * 按条件分页查询学籍列表（命令 208）；不判权限，调用方须先确认有 STUDENT_VIEW_ALL。
     * @param query 过滤条件（null 表示全部）
     * @return 分页结果
     */
    public PageResponse<StudentProfile> listStudents(StudentQuery query) {
        int pageNumber = query == null ? 1 : query.getPageNumber();
        int pageSize = query == null
                ? PageResponse.DEFAULT_PAGE_SIZE
                : query.getPageSize();
        int[] normalized = PageResponse.normalize(pageNumber, pageSize);
        int offset = (normalized[0] - 1) * normalized[1];
        List<StudentProfile> items = m_decorator.decorate(m_dao.find(query, offset,
                normalized[1]));
        long total = m_dao.count(query);
        return new PageResponse<StudentProfile>(items, total, normalized[0],
                normalized[1]);
    }

    /**
     * 提交学籍修改申请（命令 202）：只落一条待审申请，不直接改学籍。
     * @param profileId 目标学籍记录主键
     * @param applicantUuid 申请人账户 uuid（由会话解析，不取自请求体）
     * @param changes 要修改的字段
     * @param reason 申请理由
     * @return 是否提交成功
     */
    public boolean applyModification(Long profileId, String applicantUuid,
            Map<String, String> changes, String reason) {
        return m_modify_flow.apply(profileId, applicantUuid, changes, reason);
    }

    /**
     * 查询修改申请单（命令 207）。
     * @param query 过滤条件（null 表示全部状态）
     * @return 分页结果
     */
    public PageResponse<StudentModifyRequest> listModifyRequests(
            ModifyRequestQuery query) {
        return m_modify_flow.list(query);
    }

    /**
     * 审核修改申请（命令 203）：通过时把变更应用到学籍。

     * @param requestId 申请单主键
     * @param approved 是否通过
     * @param comment 审核意见
     * @param auditorUuid 审核人账户 uuid
     * @return 是否审核成功
     */
    public boolean auditModification(Long requestId, boolean approved,
            String comment, String auditorUuid) {
        return m_modify_flow.audit(requestId, approved, comment, auditorUuid);
    }
}
