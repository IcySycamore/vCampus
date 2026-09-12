package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.student.entity.StudentProfile;
import edu.seu.vcampus.common.message.MessageHandler;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.user.entity.Capability;
import edu.seu.vcampus.common.user.entity.Permissions;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import edu.seu.vcampus.server.user.SessionManager;

/**
 * 学籍消息处理器：处理命令码段 200-299 的全部学籍命令。
 *
 * <p>
 * 本类只做三件事：把 token 解析成会话、按 {@link Permissions} 判能力、把请求交给
 * {@link StudentCommandExecutor} 落业务。命令覆盖 201 查询 / 202 提交修改申请 / 203 审核申请 /
 * 204 登记 / 205 删除 / 206 改状态 / 207 待审列表 / 208 学籍列表；未登记的命令码由分发器统一回 400。
 *
 * <p>
 * 权限：每条请求先按 token 解析角色（见 {@link SessionManager}）， 无效回 401、越权回 403；通过后才路由到业务分支。
 *
 * <p>
 * 异步模式：{@link #handle} 通过 {@link MessageSender} 主动发送响应， 不阻塞调用线程。
 */
public class StudentMessageHandler implements MessageHandler {

    /** 会话管理器（auth 模块，用于按 token 解析会话）。 */
    private final SessionManager m_sessions;

    /** 命令执行器（鉴权通过后的业务落地）。 */
    private final StudentCommandExecutor m_executor;

    /**
     * 构造学籍消息处理器。
     *
     * @param service 学籍业务服务
     * @param sessions 会话管理器
     */
    public StudentMessageHandler(StudentService service, SessionManager sessions) {
        if (service == null) {
            throw new IllegalArgumentException("service must not be null");
        }
        if (sessions == null) {
            throw new IllegalArgumentException("sessions must not be null");
        }
        this.m_sessions = sessions;
        this.m_executor = new StudentCommandExecutor(service);
    }

    /**
     * 处理一条学籍命令，并通过 sender 发送响应。
     *
     * @param request 请求消息
     * @param sender 响应发送器
     */
    @Override
    public void handle(Message request, MessageSender sender) {
        Message response = responseFor(request);
        int command = request.getCommand();

        SessionEntry entry = resolveSession(request.getToken());
        if (entry == null) {
            response.setStatusCode(StatusCode.UNAUTHORIZED);
            response.setData("未登录或会话已过期");
            sender.send(response);
            return;
        }
        if (!hasPermission(command, Role.fromDisplayName(entry.getRole()))) {
            response.setStatusCode(StatusCode.FORBIDDEN);
            response.setData("无权限执行该操作");
            sender.send(response);
            return;
        }

        try {
            m_executor.execute(request, response, entry);
        } catch (ClassCastException exception) {
            response.setStatusCode(StatusCode.BAD_REQUEST);
            response.setData("参数类型不正确");
        } catch (RuntimeException exception) {
            response.setStatusCode(StatusCode.INTERNAL_ERROR);
            response.setData(safeMessage(exception));
        }
        sender.send(response);
    }

    /**
     * 按 token 解析会话；token 无效或过期返回 null。
     *
     * @param token 会话令牌
     * @return 会话条目，无效返回 null
     */
    private SessionEntry resolveSession(String token) {
        if (token == null) {
            return null;
        }
        return m_sessions.validate(token);
    }

    /**
     * 判断角色是否有权执行指定学籍命令。
     *
     * <p>
     * 判定表集中在 {@link Permissions}，本方法只做命令码 → 能力的映射，避免权限规则散落在
     * 每个模块里各写一遍（那正是越权漏洞最常见的来源）。
     *
     * @param command 命令码
     * @param role 请求者角色
     * @return 是否有权限
     */
    private boolean hasPermission(int command, Role role) {
        Capability capability = capabilityFor(command);
        return capability == null || Permissions.can(role, capability);
    }

    /**
     * 命令码 → 所需能力。
     *
     * <p>
     * 返回 null 表示「登录即可」。201 查询是这条规则的唯一使用者：学生要能查自己的学籍，所以
     * 不能要求 {@code STUDENT_VIEW_ALL}；「只能查自己」的限制由执行器在拿到目标记录后再判。
     *
     * @param command 命令码
     * @return 所需能力；无需特定能力返回 null
     */
    private Capability capabilityFor(int command) {
        if (command == Command.STUDENT_QUERY) {
            return null;
        }
        if (command == Command.STUDENT_MODIFY_APPLY) {
            return Capability.STUDENT_MODIFY_APPLY;
        }
        if (command == Command.STUDENT_MODIFY_AUDIT
                || command == Command.STUDENT_MODIFY_LIST) {
            return Capability.STUDENT_MODIFY_AUDIT;
        }
        if (command == Command.STUDENT_LIST) {
            return Capability.STUDENT_VIEW_ALL;
        }
        if (command == Command.STUDENT_REGISTER) {
            return Capability.STUDENT_REGISTER;
        }
        if (command == Command.STUDENT_DELETE) {
            return Capability.STUDENT_DELETE;
        }
        if (command == Command.STUDENT_CHANGE_STATUS) {
            return Capability.STUDENT_CHANGE_STATUS;
        }
        return null;
    }

    /**
     * 构造响应信封：回填 uid 与命令码，便于客户端关联请求。
     *
     * @param request 请求
     * @return 响应
     */
    private Message responseFor(Message request) {
        Message response = new Message();
        response.setUid(request.getUid());
        response.setCommand(request.getCommand());
        return response;
    }

    /**
     * 提取异常信息，null 时给兜底文案。
     *
     * @param exception 异常
     * @return 信息文本
     */
    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null ? "服务器内部错误" : message;
    }
}
