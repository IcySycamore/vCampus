package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.student.dto.ModifyAuditRequest;
import edu.seu.vcampus.common.student.dto.StudentDeleteRequest;
import edu.seu.vcampus.common.student.dto.StudentModifyRequest;
import edu.seu.vcampus.common.student.dto.StudentStatusRequest;
import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.StudentProfile;
import edu.seu.vcampus.common.user.entity.Capability;
import edu.seu.vcampus.common.user.entity.Permissions;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.common.user.entity.SessionEntry;

/**
 * 学籍「写」命令的执行器：202 申请 / 203 审核 / 204 登记 / 205 删除 / 206 改状态。
 *
 * <p>
 * 与 {@link StudentCommandExecutor}（读命令 201 / 207 / 208）分开，一是各自都在 200 行以内，
 * 二是读写两条链路的失败语义不同：读多半是 404，写还牵扯「参数缺失 400」「重复操作 400」。
 * 分开之后每条链路的状态码策略可以单独读、单独改。
 *
 * <p>
 * <b>请求体兼容</b>：205 / 206 在旧版客户端里传的是裸主键 / {@link StudentProfile}，新版按设计
 * 文档改成了 DTO。本类两种都收，避免旧客户端在灰度期直接报废。
 */
final class StudentWriteExecutor {

    /** 学籍业务服务。 */
    private final StudentService m_service;

    /** 自助建档规则（没有登记权限的人走这条路）。 */
    private final StudentSelfEnroll m_self_enroll;

    /**
     * 构造写命令执行器。
     *
     * @param service 学籍业务服务
     */
    StudentWriteExecutor(StudentService service) {
        this.m_service = service;
        this.m_self_enroll = new StudentSelfEnroll(service);
    }

    /**
     * 尝试执行一条写命令。
     *
     * @param command 命令码
     * @param request 请求消息
     * @param response 待填充的响应
     * @param actor 已解析出的会话条目
     * @return 是否由本执行器处理了该命令（false 表示不是写命令）
     */
    boolean execute(int command, Message request, Message response, SessionEntry actor) {
        if (command == Command.STUDENT_MODIFY_APPLY) {
            doApply(request, response, actor);
        } else if (command == Command.STUDENT_MODIFY_AUDIT) {
            doAudit(request, response, actor);
        } else if (command == Command.STUDENT_REGISTER) {
            doRegister(request, response, actor);
        } else if (command == Command.STUDENT_DELETE) {
            doDelete(request, response);
        } else if (command == Command.STUDENT_CHANGE_STATUS) {
            doChangeStatus(request, response);
        } else {
            return false;
        }
        return true;
    }

    /**
     * 提交修改申请（202）：只落一条待审申请，审核通过后学籍才会变。
     *
     * @param request 请求
     * @param response 响应
     * @param actor 会话条目（申请人取会话 uuid，不信任请求体）
     */
    private void doApply(Message request, Message response, SessionEntry actor) {
        StudentModifyRequest dto = (StudentModifyRequest) request.getData();
        if (dto == null || dto.getProfileId() == null) {
            response.setStatusCode(StatusCode.BAD_REQUEST);
            response.setData("参数不能为空");
            return;
        }
        boolean ok = m_service.applyModification(dto.getProfileId(), actor.getUuid(),
                dto.getChanges(), dto.getReason());
        if (!ok) {
            response.setStatusCode(StatusCode.BAD_REQUEST);
            response.setData("申请提交失败：学籍不存在或没有可修改的字段");
            return;
        }
        response.setStatusCode(StatusCode.SUCCESS);
    }

    /**
     * 审核修改申请（203）：通过时把变更真正应用到学籍。
     *
     * @param request 请求
     * @param response 响应
     * @param actor 会话条目（审核人）
     */
    private void doAudit(Message request, Message response, SessionEntry actor) {
        ModifyAuditRequest dto = (ModifyAuditRequest) request.getData();
        if (dto == null || dto.getRequestId() == null) {
            response.setStatusCode(StatusCode.BAD_REQUEST);
            response.setData("参数不能为空");
            return;
        }
        boolean ok = m_service.auditModification(dto.getRequestId(), dto.isApproved(),
                dto.getComment(), actor.getUuid());
        if (!ok) {
            response.setStatusCode(StatusCode.BAD_REQUEST);
            response.setData("审核失败：申请不存在或已被审核过");
            return;
        }
        response.setStatusCode(StatusCode.SUCCESS);
    }

    /**
     * 登记学籍（204）：有登记权限的按请求体登记他人，没有的走自助建档。
     *
     * <p>
     * 没有登记权限的人有两种失败，语义不同：不是「填自己的、且自己的学籍还没填过」回 <b>403</b>
     * （没这个权限，见 {@link StudentSelfEnroll#applies}）；资格过了但没填学术方向回 <b>400</b>
     * （参数不全）。
     *
     * @param request 请求
     * @param response 响应
     * @param actor 会话条目（自助建档时以它的 uuid 为准，不信任请求体）
     */
    private void doRegister(Message request, Message response, SessionEntry actor) {
        StudentProfile submitted = (StudentProfile) request.getData();
        if (submitted == null) {
            response.setStatusCode(StatusCode.BAD_REQUEST);
            response.setData("参数不能为空");
            return;
        }
        Role role = Role.fromDisplayName(actor.getRole());
        if (Permissions.can(role, Capability.STUDENT_REGISTER)) {
            if (!m_service.registerStudent(submitted)) {
                response.setStatusCode(StatusCode.BAD_REQUEST);
                response.setData("学籍记录非法");
                return;
            }
            response.setStatusCode(StatusCode.SUCCESS);
            return;
        }
        if (!m_self_enroll.applies(actor.getUuid(), submitted)) {
            response.setStatusCode(StatusCode.FORBIDDEN);
            response.setData("无权登记学籍：只能填写自己的学籍，且学籍尚未填写过");
            return;
        }
        if (!m_self_enroll.apply(actor.getUuid(), submitted)) {
            response.setStatusCode(StatusCode.BAD_REQUEST);
            response.setData("学籍记录非法");
            return;
        }
        response.setStatusCode(StatusCode.SUCCESS);
    }

    /**
     * 删除学籍（205）：兼容 {@link StudentDeleteRequest} 与裸主键两种请求体。
     *
     * @param request 请求
     * @param response 响应
     */
    private void doDelete(Message request, Message response) {
        Object data = request.getData();
        Long profileId = null;
        if (data instanceof StudentDeleteRequest) {
            profileId = ((StudentDeleteRequest) data).getProfileId();
        } else if (data instanceof Long) {
            profileId = (Long) data;
        }
        if (profileId == null) {
            response.setStatusCode(StatusCode.BAD_REQUEST);
            response.setData("参数不能为空");
            return;
        }
        if (!m_service.deleteStudent(profileId)) {
            response.setStatusCode(StatusCode.NOT_FOUND);
            response.setData("学籍记录不存在");
            return;
        }
        response.setStatusCode(StatusCode.SUCCESS);
    }

    /**
     * 修改学籍状态（206）：兼容 {@link StudentStatusRequest} 与 {@link StudentProfile} 两种请求体。
     *
     * @param request 请求
     * @param response 响应
     */
    private void doChangeStatus(Message request, Message response) {
        Object data = request.getData();
        Long profileId = null;
        CampusStatus status = null;
        if (data instanceof StudentStatusRequest) {
            profileId = ((StudentStatusRequest) data).getProfileId();
            status = ((StudentStatusRequest) data).getStatus();
        } else if (data instanceof StudentProfile) {
            profileId = ((StudentProfile) data).getId();
            status = ((StudentProfile) data).getStatus();
        }
        if (profileId == null || status == null) {
            response.setStatusCode(StatusCode.BAD_REQUEST);
            response.setData("参数不能为空");
            return;
        }
        if (!m_service.changeStatus(profileId, status)) {
            response.setStatusCode(StatusCode.NOT_FOUND);
            response.setData("学籍记录不存在或参数非法");
            return;
        }
        response.setStatusCode(StatusCode.SUCCESS);
    }
}
