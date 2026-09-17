package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageHandler;
import edu.seu.vcampus.common.message.MessageSender;
import edu.seu.vcampus.common.user.entity.Capability;
import edu.seu.vcampus.common.user.entity.Permissions;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.SessionEntry;
import edu.seu.vcampus.server.user.SessionManager;
import edu.seu.vcampus.server.user.UserRepository;

/**
 * 选课消息处理器：处理命令码段 300-399 的全部选课命令。
 *
 * <p>本类只做三件事：把 token 解析成会话、按 {@link Permissions} 判能力、把请求交给
 * {@link CourseCommandExecutor} 落业务。学生只被授予选课/退课能力；教师可查看自己授课的课程并
 * 安排偏好时间槽；管理员负责排课。未授予的能力一律回 403，越权到服务端才被拦下。
 */
public class CourseMessageHandler implements MessageHandler {

    /** 会话管理器（auth 模块，用于按 token 解析会话）。 */
    private final SessionManager m_sessions;

    /** 命令执行器（鉴权通过后的业务落地）。 */
    private final CourseCommandExecutor m_executor;

    /**
     * 构造选课消息处理器。
     *
     * @param dao 课程数据访问
     * @param management 课程管理服务
     * @param service 选课业务服务
     * @param users 用户凭证存储（写命令把登录名解析成 uuid 用）
     * @param sessions 会话管理器
     */
    public CourseMessageHandler(CourseDao dao, CourseManagementService management,
            CourseService service, UserRepository users, SessionManager sessions) {
        if (sessions == null) {
            throw new IllegalArgumentException("sessions must not be null");
        }
        this.m_sessions = sessions;
        this.m_executor = new CourseCommandExecutor(dao, management, service, users);
    }

    /**
     * 处理一条选课命令，并通过 sender 发送响应。
     *
     * @param request 请求消息
     * @param sender 响应发送器
     */
    @Override
    public void handle(Message request, MessageSender sender) {
        Message response = responseFor(request);
        SessionEntry entry = resolveSession(request.getToken());
        if (entry == null) {
            response.setStatusCode(StatusCode.UNAUTHORIZED);
            response.setData("未登录或会话已过期");
            sender.send(response);
            return;
        }
        if (!hasPermission(request.getCommand(), Role.fromDisplayName(entry.getRole()))) {
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
            response.setData(exception.getMessage() == null ? "服务器内部错误"
                    : exception.getMessage());
        }
        sender.send(response);
    }

    private SessionEntry resolveSession(String token) {
        return token == null ? null : m_sessions.validate(token);
    }

    private boolean hasPermission(int command, Role role) {
        Capability capability = capabilityFor(command);
        return capability == null || Permissions.can(role, capability);
    }

    private Capability capabilityFor(int command) {
        if (command == Command.COURSE_SELECT || command == Command.COURSE_DROP) {
            return Capability.COURSE_SELECT;
        }
        if (command == Command.SCORE_SAVE) {
            return Capability.COURSE_GRADE_EDIT;
        }
        if (command == Command.COURSE_SCHEDULE || command == Command.COURSE_CLASSROOM_LIST
                || command == Command.COURSE_ADD || command == Command.COURSE_UPDATE
                || command == Command.COURSE_DELETE || command == Command.COURSE_TEACHER_LIST) {
            return Capability.COURSE_MANAGE;
        }
        if (command == Command.COURSE_PREFERENCE_SET || command == Command.COURSE_AVAILABLE_SET) {
            return Capability.COURSE_PREFERENCE_EDIT;
        }
        if (command == Command.COURSE_CLAIM) {
            return Capability.COURSE_CLAIM;
        }
        return null;
    }

    private Message responseFor(Message request) {
        Message response = new Message();
        response.setUid(request.getUid());
        response.setCommand(request.getCommand());
        return response;
    }
}
