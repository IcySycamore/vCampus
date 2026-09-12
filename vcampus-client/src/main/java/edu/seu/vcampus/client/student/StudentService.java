package edu.seu.vcampus.client.student;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.client.user.ClientSession;
import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.student.dto.ModifyAuditRequest;
import edu.seu.vcampus.common.student.dto.ModifyRequestQuery;
import edu.seu.vcampus.common.student.dto.StudentDeleteRequest;
import edu.seu.vcampus.common.student.dto.StudentModifyRequest;
import edu.seu.vcampus.common.student.dto.StudentQuery;
import edu.seu.vcampus.common.student.dto.StudentStatusRequest;
import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.StudentProfile;

/**
 * 学籍客户端服务：给界面提供查询、申请、审核、登记、改状态、删除的同步方法。
 *
 * <p>
 * 调用约定：方法同步阻塞并直接返回实体/分页结果；失败统一抛 {@link ApiException}（非受检），
 * 界面不必 try/catch，也不必接触 {@code Message} / 命令码 / 分发器；身份一律由服务端按会话解析，
 * 查询自己的学籍不传 uuid。权限判定不在本层：按角色隐藏控件由界面负责，服务端 403 是最终防线。
 */
public class StudentService {

    /** 请求发送与响应校验。 */
    private final StudentApiClient m_client;

    /** 内存会话（取其 token 填充请求；与用户模块共用同一个会话对象）。 */
    private final ClientSession m_session;

    /**
     * 构造服务，使用默认 5 秒超时。
     *
     * @param dispatcher 消息分发器
     * @param session 内存会话
     */
    public StudentService(ClientMessageDispatcher dispatcher, ClientSession session) {
        this(dispatcher, session, 5000L);
    }

    /**
     * 构造服务。
     *
     * @param dispatcher 消息分发器
     * @param session 内存会话
     * @param timeoutMillis 请求超时，毫秒
     */
    public StudentService(ClientMessageDispatcher dispatcher, ClientSession session,
            long timeoutMillis) {
        if (dispatcher == null) {
            throw new IllegalArgumentException("dispatcher must not be null");
        }
        if (session == null) {
            throw new IllegalArgumentException("session must not be null");
        }
        this.m_session = session;
        this.m_client = new StudentApiClient(dispatcher, timeoutMillis);
    }

    /**
     * 查询「我的」学籍（命令 201）。
     *
     * <p>
     * 不传任何身份信息：服务端按会话里的 uuid 查。这样学生端不可能通过改参数查到别人。
     *
     * @return 本人学籍
     * @throws ApiException 未登录、无学籍记录或网络失败
     */
    public StudentProfile queryMyProfile() {
        return (StudentProfile) send(Command.STUDENT_QUERY, new StudentQuery()).getData();
    }

    /**
     * 按主键查看指定学籍（命令 201，管理端使用）。
     *
     * @param profileId 学籍记录主键
     * @return 学籍记录
     * @throws ApiException 记录不存在、无权限或网络失败
     */
    public StudentProfile queryProfile(long profileId) {
        Message response = send(Command.STUDENT_QUERY,
                StudentQuery.byProfileId(Long.valueOf(profileId)));
        return (StudentProfile) response.getData();
    }

    /**
     * 分页查询学籍列表（命令 208，管理端使用）。
     *
     * @param query 过滤条件（关键字、状态、分页）
     * @return 分页结果
     * @throws ApiException 无权限（403）或网络失败
     */
    @SuppressWarnings("unchecked")
    public PageResponse<StudentProfile> listStudents(StudentQuery query) {
        Message response = send(Command.STUDENT_LIST, query);
        return (PageResponse<StudentProfile>) response.getData();
    }

    /**
     * 提交学籍修改申请（命令 202，学生使用）。
     *
     * <p>
     * 调用成功后学籍<b>不会立刻变</b>，只是多了一条待审申请；审核通过后才会生效。界面应当
     * 提示「已提交，等待教务审核」。
     *
     * @param request 申请内容（目标学籍、要改的字段、理由）
     * @throws ApiException 参数非法、学籍不存在或网络失败
     */
    public void applyModification(StudentModifyRequest request) {
        send(Command.STUDENT_MODIFY_APPLY, request);
    }

    /**
     * 分页查询修改申请单（命令 207，教务使用）。
     *
     * @param query 过滤条件（状态、目标学籍、分页）
     * @return 分页结果
     * @throws ApiException 无权限（403）或网络失败
     */
    @SuppressWarnings("unchecked")
    public PageResponse<StudentModifyRequest> listModifyRequests(ModifyRequestQuery query) {
        Message response = send(Command.STUDENT_MODIFY_LIST, query);
        return (PageResponse<StudentModifyRequest>) response.getData();
    }

    /**
     * 审核修改申请（命令 203，教务使用）。
     *
     * @param requestId 申请单编号（字符串形式，界面直接传输入框内容即可）
     * @param approved 是否通过
     * @param comment 审核意见
     * @throws ApiException 编号非法、申请不存在、已被审核过或网络失败
     */
    public void auditModification(String requestId, boolean approved, String comment) {
        Long id = StudentApiClient.parseId(requestId);
        send(Command.STUDENT_MODIFY_AUDIT,
                new ModifyAuditRequest(id, Boolean.valueOf(approved), comment));
    }

    /**
     * 登记学籍（命令 204，管理员使用）。
     *
     * @param profile 学籍记录
     * @throws ApiException 记录非法、无权限（403）或网络失败
     */
    public void registerStudent(StudentProfile profile) {
        send(Command.STUDENT_REGISTER, profile);
    }

    /**
     * 修改学籍状态（命令 206，教务使用）。
     *
     * @param profileId 学籍记录主键
     * @param status 新状态
     * @throws ApiException 记录不存在、无权限（403）或网络失败
     */
    public void changeStatus(long profileId, CampusStatus status) {
        StudentStatusRequest request = new StudentStatusRequest();
        request.setProfileId(Long.valueOf(profileId));
        request.setStatus(status);
        send(Command.STUDENT_CHANGE_STATUS, request);
    }

    /**
     * 删除学籍（命令 205，管理员使用，软删除）。
     *
     * @param profileId 学籍记录主键
     * @throws ApiException 记录不存在、无权限（403）或网络失败
     */
    public void deleteStudent(long profileId) {
        StudentDeleteRequest request = new StudentDeleteRequest();
        request.setProfileId(Long.valueOf(profileId));
        send(Command.STUDENT_DELETE, request);
    }

    /**
     * 发送请求（薄壳，真正的校验在 {@link StudentApiClient}）。
     *
     * @param command 命令码
     * @param data 请求数据
     * @return 成功响应
     */
    private Message send(int command, Object data) {
        return m_client.call(command, data, m_session.getToken());
    }
}
