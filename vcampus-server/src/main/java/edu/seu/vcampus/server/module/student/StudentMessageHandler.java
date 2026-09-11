package edu.seu.vcampus.server.module.student;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.entity.StudentProfile;
import edu.seu.vcampus.common.handler.MessageHandler;
import edu.seu.vcampus.common.handler.MessageSender;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.server.auth.SessionManager;

/**
 * 学籍消息处理器：处理命令码段 200-299 的全部学籍命令。
 *
 * <p>
 * 按组长确定的范围注册方案，本类作为整段学籍命令的唯一入口，内部再按具体
 * 命令码分支（201 查询 / 202 修改 / 204 登记 / 205 删除）。203 审核流依赖
 * 权限体系（用户管理模块），暂未实现，收到时回 400。
 *
 * <p>
 * 权限：每条请求先按 token 解析角色（见 {@link SessionManager}），
 * 无效回 401、越权回 403；通过后才路由到业务分支。
 *
 * <p>
 * 异步模式：{@link #handle} 通过 {@link MessageSender} 主动发送响应，
 * 不阻塞调用线程。
 */
public class StudentMessageHandler implements MessageHandler {

    /** 学籍业务服务。 */
    private final StudentService m_service;

    /** 会话管理器（auth 模块，用于按 token 解析角色）。 */
    private final SessionManager m_sessions;

    /**
     * 构造学籍消息处理器。
     *
     * @param service  学籍业务服务
     * @param sessions 会话管理器
     */
    public StudentMessageHandler(StudentService service,
            SessionManager sessions) {
        if (service == null) {
            throw new IllegalArgumentException("service must not be null");
        }
        if (sessions == null) {
            throw new IllegalArgumentException("sessions must not be null");
        }
        this.m_service = service;
        this.m_sessions = sessions;
    }

    /**
     * 处理一条学籍命令，并通过 sender 发送响应。
     *
     * @param request 请求消息
     * @param sender  响应发送器
     */
    @Override
    public void handle(Message request, MessageSender sender) {
        Message response = responseFor(request);
        int command = request.getCommand();

        Role role = resolveRole(request.getToken());
        if (role == null) {
            response.setStatusCode(StatusCode.UNAUTHORIZED);
            response.setData("未登录或会话已过期");
            sender.send(response);
            return;
        }
        if (!hasPermission(command, role)) {
            response.setStatusCode(StatusCode.FORBIDDEN);
            response.setData("无权限执行该操作");
            sender.send(response);
            return;
        }

        try {
            if (command == Command.STUDENT_QUERY) {
                doQuery(request, response);
            } else if (command == Command.STUDENT_MODIFY_APPLY) {
                doUpdate(request, response);
            } else if (command == Command.STUDENT_MODIFY_AUDIT) {
                response.setStatusCode(StatusCode.BAD_REQUEST);
                response.setData("审核功能待权限体系合入后实现");
            } else if (command == Command.STUDENT_REGISTER) {
                doRegister(request, response);
            } else if (command == Command.STUDENT_DELETE) {
                doDelete(request, response);
            } else if (command == Command.STUDENT_CHANGE_STATUS) {
                doChangeStatus(request, response);
            } else {
                response.setStatusCode(StatusCode.BAD_REQUEST);
                response.setData("未知的学籍命令");
            }
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
     * 按 token 解析角色；token 无效或过期返回 null。
     *
     * @param token 会话令牌
     * @return 角色，无效返回 null
     */
    private Role resolveRole(String token) {
        if (token == null) {
            return null;
        }
        SessionManager.SessionEntry entry = m_sessions.validate(token);
        if (entry == null) {
            return null;
        }
        return Role.fromDisplayName(entry.getRole());
    }

    /**
     * 判断角色是否有权执行指定学籍命令。
     *
     * @param command 命令码
     * @param role    请求者角色
     * @return 是否有权限
     */
    private boolean hasPermission(int command, Role role) {
        if (command == Command.STUDENT_QUERY) {
            return true;
        }
        if (command == Command.STUDENT_MODIFY_APPLY) {
            return role == Role.STUDENT || role == Role.ADMIN;
        }
        if (command == Command.STUDENT_MODIFY_AUDIT
                || command == Command.STUDENT_REGISTER
                || command == Command.STUDENT_DELETE
                || command == Command.STUDENT_CHANGE_STATUS) {
            return role == Role.ADMIN;
        }
        return true;
    }

    /**
     * 查询学籍（201）：data 为学籍记录主键 id。
     *
     * @param request  请求
     * @param response 响应
     */
    private void doQuery(Message request, Message response) {
        Long id = (Long) request.getData();
        StudentProfile profile = m_service.queryProfile(id);
        if (profile == null) {
            response.setStatusCode(StatusCode.NOT_FOUND);
            response.setData("学籍记录不存在");
            return;
        }
        response.setStatusCode(StatusCode.SUCCESS);
        response.setData(profile);
    }

    /**
     * 更新学籍（202）：data 为完整学籍记录，按主键覆盖。
     *
     * @param request  请求
     * @param response 响应
     */
    private void doUpdate(Message request, Message response) {
        StudentProfile profile = (StudentProfile) request.getData();
        boolean ok = m_service.updateProfile(profile);
        if (!ok) {
            response.setStatusCode(StatusCode.NOT_FOUND);
            response.setData("学籍记录不存在或参数非法");
            return;
        }
        response.setStatusCode(StatusCode.SUCCESS);
    }

    /**
     * 登记学籍（204）：data 为学籍记录。
     *
     * @param request  请求
     * @param response 响应
     */
    private void doRegister(Message request, Message response) {
        StudentProfile profile = (StudentProfile) request.getData();
        boolean ok = m_service.registerStudent(profile);
        if (!ok) {
            response.setStatusCode(StatusCode.BAD_REQUEST);
            response.setData("学籍记录非法");
            return;
        }
        response.setStatusCode(StatusCode.SUCCESS);
    }

    /**
     * 删除学籍（205）：data 为学籍记录主键 id，软删除。
     *
     * @param request  请求
     * @param response 响应
     */
    private void doDelete(Message request, Message response) {
        Long id = (Long) request.getData();
        boolean ok = m_service.deleteStudent(id);
        if (!ok) {
            response.setStatusCode(StatusCode.NOT_FOUND);
            response.setData("学籍记录不存在");
            return;
        }
        response.setStatusCode(StatusCode.SUCCESS);
    }

    /**
     * 修改学籍状态（206）：data 为学籍记录（仅需 id 与 status）。
     *
     * @param request  请求
     * @param response 响应
     */
    private void doChangeStatus(Message request, Message response) {
        StudentProfile profile = (StudentProfile) request.getData();
        if (profile == null) {
            response.setStatusCode(StatusCode.BAD_REQUEST);
            response.setData("参数不能为空");
            return;
        }
        boolean ok = m_service.changeStatus(profile.getId(), profile.getStatus());
        if (!ok) {
            response.setStatusCode(StatusCode.NOT_FOUND);
            response.setData("学籍记录不存在或参数非法");
            return;
        }
        response.setStatusCode(StatusCode.SUCCESS);
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
