package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.student.dto.ModifyRequestQuery;
import edu.seu.vcampus.common.student.dto.StudentQuery;
import edu.seu.vcampus.common.student.entity.StudentProfile;
import edu.seu.vcampus.common.user.entity.Capability;
import edu.seu.vcampus.common.user.entity.Permissions;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.SessionEntry;

/**
 * 学籍「读」命令的执行器：201 查询 / 207 待审列表 / 208 学籍列表。
 *
 * <p>
 * 从 {@link StudentMessageHandler} 拆出，一是因为合并后单文件会超过 200 行上限，二是把
 * 「鉴权」与「业务执行」两件事分开：handler 只管 token → 角色 → 能力判定，本类只管拿到
 * 已经通过鉴权的请求后怎么落业务。写命令（202-206）在 {@link StudentWriteExecutor}。
 *
 * <p>
 * <b>请求体兼容</b>：201 在旧版客户端里传的是学籍主键 {@code Long}，新版按设计文档改成了
 * {@link StudentQuery}。本类两种都收，避免旧客户端在灰度期直接报废。
 */
final class StudentCommandExecutor {

    /** 学籍业务服务。 */
    private final StudentService m_service;

    /** 写命令执行器（202 / 203 / 204 / 205 / 206 委托给它）。 */
    private final StudentWriteExecutor m_writes;

    /**
     * 构造读命令执行器。
     *
     * @param service 学籍业务服务
     */
    StudentCommandExecutor(StudentService service) {
        this.m_service = service;
        this.m_writes = new StudentWriteExecutor(service);
    }

    /**
     * 执行一条已经通过鉴权的学籍命令。
     *
     * @param request 请求消息
     * @param response 待填充的响应
     * @param actor 已解析出的会话条目
     */
    void execute(Message request, Message response, SessionEntry actor) {
        int command = request.getCommand();
        if (command == Command.STUDENT_QUERY) {
            doQuery(request, response, actor);
        } else if (command == Command.STUDENT_MODIFY_LIST) {
            doModifyList(request, response);
        } else if (command == Command.STUDENT_LIST) {
            doList(request, response);
        } else if (!m_writes.execute(command, request, response, actor)) {
            response.setStatusCode(StatusCode.BAD_REQUEST);
            response.setData("未知的学籍命令");
        }
    }

    /**
     * 查询学籍（201）：本人可查自己，具备全量查看能力者可查任意人。
     *
     * <p>
     * 这里是「三轨查询」中「我的轨」的落点：请求没给目标主键时按会话 uuid 查自己；给了目标主键
     * 但不是自己又没有 {@code STUDENT_VIEW_ALL}，直接 403，绝不做静默降级（否则会把别人的学籍
     * 当作自己的返回）。
     *
     * @param request 请求
     * @param response 响应
     * @param actor 会话条目
     */
    private void doQuery(Message request, Message response, SessionEntry actor) {
        Long profileId = extractProfileId(request.getData());
        StudentProfile profile = profileId == null
                ? m_service.queryByUserUuid(actor.getUuid())
                : m_service.queryProfile(profileId);
        if (profile == null) {
            response.setStatusCode(StatusCode.NOT_FOUND);
            response.setData("学籍记录不存在");
            return;
        }
        if (!isSelf(profile, actor)
                && !Permissions.can(roleOf(actor), Capability.STUDENT_VIEW_ALL)) {
            response.setStatusCode(StatusCode.FORBIDDEN);
            response.setData("无权限查看他人学籍");
            return;
        }
        response.setStatusCode(StatusCode.SUCCESS);
        response.setData(profile);
    }

    /**
     * 待审申请列表（207）。
     *
     * @param request 请求
     * @param response 响应
     */
    private void doModifyList(Message request, Message response) {
        ModifyRequestQuery query = (ModifyRequestQuery) request.getData();
        PageResponse<?> page = m_service.listModifyRequests(query);
        response.setStatusCode(StatusCode.SUCCESS);
        response.setData(page);
    }

    /**
     * 学籍列表（208），分页。
     *
     * @param request 请求
     * @param response 响应
     */
    private void doList(Message request, Message response) {
        StudentQuery query = null;
        if (request.getData() instanceof StudentQuery) {
            query = (StudentQuery) request.getData();
        }
        PageResponse<?> page = m_service.listStudents(query);
        response.setStatusCode(StatusCode.SUCCESS);
        response.setData(page);
    }

    /**
     * 从请求体里取目标学籍主键，兼容裸主键与查询 DTO。
     *
     * <p>
     * 请求体是别的东西（比如字符串）时抛 {@link ClassCastException}，由 handler 统一转成 400：
     * 「参数类型不对」和「你名下没有学籍」是两回事，不能都回 404。
     *
     * @param data 请求数据
     * @return 目标主键；null 或空查询表示「查我自己」
     */
    private Long extractProfileId(Object data) {
        if (data == null) {
            return null;
        }
        if (data instanceof Long) {
            return (Long) data;
        }
        if (data instanceof StudentQuery) {
            return ((StudentQuery) data).getProfileId();
        }
        throw new ClassCastException("unsupported query payload: " + data.getClass());
    }

    /**
     * 判断学籍记录是否属于当前会话用户。
     *
     * @param profile 学籍记录
     * @param actor 会话条目
     * @return 是否本人
     */
    private static boolean isSelf(StudentProfile profile, SessionEntry actor) {
        return actor.getUuid() != null && actor.getUuid().equals(profile.getUserUuid());
    }

    /**
     * 把会话里的角色文本翻成枚举。
     *
     * @param actor 会话条目
     * @return 角色；无法识别返回 null（能力判定会按「拒绝」处理）
     */
    private static Role roleOf(SessionEntry actor) {
        return Role.fromDisplayName(actor.getRole());
    }
}
