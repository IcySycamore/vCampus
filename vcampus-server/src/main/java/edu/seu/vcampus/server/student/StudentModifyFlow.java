package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.student.dto.ModifyRequestQuery;
import edu.seu.vcampus.common.student.entity.ModifyRequestStatus;
import edu.seu.vcampus.common.student.entity.StudentModifyRequest;
import edu.seu.vcampus.common.student.entity.StudentProfile;

import java.util.List;
import java.util.Map;

/**
 * 学籍修改的审核流：申请（202）→ 待审列表（207）→ 审核（203）。
 *
 * <p>
 * 从 {@link StudentService} 拆出来的原因有两个：一是它属于独立子域（申请单有自己的存储与状态机，
 * 与学籍记录本体的 CRUD 不是一回事）；二是合并进 {@code StudentService} 会让单文件超过 200 行上限。
 * {@code StudentService} 对外仍提供同名方法，内部委托到这里，调用方感知不到拆分。
 *
 * <p>
 * <b>核心语义</b>：202 只落一条 PENDING 申请，<b>不改学籍</b>；203 通过时才把申请中的字段变更
 * 应用到 {@link StudentProfile}。这样才真正体现「学生申请 → 教务审核」的业务流程。
 */
public class StudentModifyFlow {

    /** 申请单存储。 */
    private final StudentModifyRequestDao m_requests;

    /** 学籍记录存储（审核通过时要把变更落回学籍）。 */
    private final StudentDao m_profiles;

    /**
     * 构造审核流。
     *
     * @param requests 申请单存储
     * @param profiles 学籍记录存储
     */
    public StudentModifyFlow(StudentModifyRequestDao requests, StudentDao profiles) {
        if (requests == null || profiles == null) {
            throw new IllegalArgumentException("requests and profiles must not be null");
        }
        this.m_requests = requests;
        this.m_profiles = profiles;
    }

    /**
     * 提交修改申请（命令 202）。
     *
     * @param profileId     目标学籍记录主键
     * @param applicantUuid 申请人账户 uuid
     * @param changes       要修改的字段（字段名 → 新值）
     * @param reason        申请理由
     * @return 是否提交成功（目标学籍不存在、无可改字段、或字段不被允许时返回 false）
     */
    public boolean apply(Long profileId, String applicantUuid,
            Map<String, String> changes, String reason) {
        if (profileId == null || applicantUuid == null || changes == null
                || changes.isEmpty()) {
            return false;
        }
        if (m_profiles.findById(profileId) == null) {
            return false;
        }
        String encoded = StudentChangeCodec.encode(changes);
        if (encoded.length() == 0) {
            return false;
        }
        StudentModifyRequest request = new StudentModifyRequest(profileId,
                applicantUuid, encoded, reason);
        request.setAppliedAt(System.currentTimeMillis());
        return m_requests.insert(request);
    }

    /**
     * 查询申请单（命令 207），按提交时间倒序分页。
     *
     * @param query 过滤条件（null 表示全部状态）
     * @return 分页结果
     */
    public PageResponse<StudentModifyRequest> list(ModifyRequestQuery query) {
        int pageNumber = query == null ? 1 : query.getPageNumber();
        int pageSize = query == null
                ? PageResponse.DEFAULT_PAGE_SIZE
                : query.getPageSize();
        int normalizedPage = PageResponse.normalizePageNumber(pageNumber);
        int normalizedSize = PageResponse.normalizePageSize(pageSize);
        int offset = PageResponse.offsetOf(normalizedPage, normalizedSize);
        List<StudentModifyRequest> items =
                m_requests.find(query, offset, normalizedSize);
        long total = m_requests.count(query);
        return new PageResponse<StudentModifyRequest>(items, total,
                normalizedPage, normalizedSize);
    }

    /**
     * 审核申请（命令 203）。
     *
     * <p>
     * 通过时把申请中的字段变更应用到学籍记录；驳回时学籍保持原样。只能审核 PENDING 的申请，
     * 重复审核返回 false（避免一条申请被反复应用变更）。
     *
     * @param requestId   申请单主键
     * @param approved    是否通过
     * @param comment     审核意见
     * @param auditorUuid 审核人账户 uuid
     * @return 是否审核成功
     */
    public boolean audit(Long requestId, boolean approved, String comment,
            String auditorUuid) {
        if (requestId == null || auditorUuid == null) {
            return false;
        }
        StudentModifyRequest request = m_requests.findById(requestId);
        if (request == null || request.getStatus() != ModifyRequestStatus.PENDING) {
            return false;
        }
        if (approved) {
            StudentProfile profile = m_profiles.findById(request.getProfileId());
            if (profile == null) {
                return false;
            }
            if (!StudentChangeCodec.applyChanges(profile, request.getChangesJson())) {
                return false;
            }
            if (!m_profiles.update(profile)) {
                return false;
            }
        }
        request.setStatus(approved
                ? ModifyRequestStatus.APPROVED
                : ModifyRequestStatus.REJECTED);
        request.setComment(comment);
        request.setAuditedBy(auditorUuid);
        request.setAuditedAt(System.currentTimeMillis());
        return m_requests.update(request);
    }
}
