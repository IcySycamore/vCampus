package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.StudentProfile;

/**
 * 自助建档规则：新用户填写<b>自己</b>的学籍（组长：新学生注册不该只有管理员能建学籍）。
 *
 * <p>
 * 从 {@link StudentWriteExecutor} 里单独拆出来，一是那边快 200 行了，二是这条规则本身值得单独读：
 * 它是「自助」与「越权」的分界线。三条限制都在这里落实，客户端说什么都不作数——账号 uuid 一律取
 * 会话（请求体里的 uuid 被忽略），在校状态强制为在读（学生不能自己声明毕业/退学），并且只在自己
 * 学籍的学术方向<b>还空着</b>时放行。
 *
 * <p>
 * 填过之后再改就要走 202 申请、203 审核：自助只解决「第一次填写」，不是绕过审核的后门。
 */
final class StudentSelfEnroll {

    /** 学籍业务服务。 */
    private final StudentService m_service;

    /**
     * 构造自助建档规则。
     *
     * @param service 学籍业务服务
     * @throws IllegalArgumentException service 为 null
     */
    StudentSelfEnroll(StudentService service) {
        if (service == null) {
            throw new IllegalArgumentException("service must not be null");
        }
        this.m_service = service;
    }

    /**
     * 该账号现在能不能自助建档：只能是自己，且学籍不存在或学术方向还空着。
     *
     * @param userUuid 会话解析出的账号 uuid
     * @param submitted 客户端提交的学籍；可为 null
     * @return true 表示可以走自助建档
     */
    boolean applies(String userUuid, StudentProfile submitted) {
        if (userUuid == null || userUuid.length() == 0) {
            return false;
        }
        // 只能给自己建档：请求体里指名了别人就是越权。null 视为自己——反正最终以会话 uuid 为准，
        // 客户端不填也不会因此多拿到任何权限。
        if (submitted != null && submitted.getUserUuid() != null
                && !userUuid.equals(submitted.getUserUuid())) {
            return false;
        }
        return fieldOf(m_service.queryByUserUuid(userUuid)).length() == 0;
    }

    /**
     * 执行自助建档：只为这个账号写这些字段。
     *
     * @param userUuid 会话解析出的账号 uuid
     * @param submitted 客户端提交的学籍（只取学术方向与入校年份）
     * @return 是否成功；没填学术方向视为参数不全，返回 false
     */
    boolean apply(String userUuid, StudentProfile submitted) {
        String field = submitted == null ? null : submitted.getField();
        if (field == null || field.trim().length() == 0) {
            return false;
        }
        StudentProfile existing = m_service.queryByUserUuid(userUuid);
        if (existing == null) {
            StudentProfile created = new StudentProfile(userUuid, submitted.getJoinYear(),
                    CampusStatus.ENROLLED);
            created.setField(field.trim());
            return m_service.registerStudent(created);
        }
        existing.setField(field.trim());
        existing.setJoinYear(submitted.getJoinYear());
        existing.setStatus(CampusStatus.ENROLLED);
        return m_service.updateProfile(existing);
    }

    /**
     * 取学术方向，null 一律当空串处理（学籍不存在与没填过在这里是同一件事）。
     *
     * @param profile 学籍；可为 null
     * @return 去掉首尾空白后的学术方向
     */
    private static String fieldOf(StudentProfile profile) {
        if (profile == null || profile.getField() == null) {
            return "";
        }
        return profile.getField().trim();
    }
}
