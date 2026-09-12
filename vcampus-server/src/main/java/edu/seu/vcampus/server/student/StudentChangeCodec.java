package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.entity.EnrollmentStatus;
import edu.seu.vcampus.common.student.entity.StudentModifyRequest;
import edu.seu.vcampus.common.student.entity.StudentProfile;

import java.util.Map;

/**
 * 学籍变更内容的编解码：把「字段 → 新值」写成键值文本存进申请单，审核通过时再读回来写进学籍。
 *
 * <p>
 * 与 {@link StudentModifyFlow}（流程与状态机）分开，是因为这里只关心「哪些字段允许改、怎么写怎么读」，
 * 属于纯粹的数据格式问题，跟审核流程无关；拆开后流程类才能舒服地待在 200 行以内。
 *
 * <p>
 * <b>白名单</b>：只有 {@link #FIELD_ENROLL_YEAR} 与 {@link #FIELD_STATUS} 可改。编码与解码两处都
 * 校验白名单，所以即使有人绕过界面直接构造申请单、往文本里塞别的字段，解码时也会被忽略。
 */
final class StudentChangeCodec {

    /** 允许通过申请修改的字段：入学年份（十进制整数）。 */
    static final String FIELD_ENROLL_YEAR = "enrollYear";

    /** 允许通过申请修改的字段：学籍状态（枚举名，如 {@code SUSPENDED}）。 */
    static final String FIELD_STATUS = "status";

    /**
     * 私有构造器，禁止实例化工具类。
     */
    private StudentChangeCodec() {
    }

    /**
     * 把「字段名 → 新值」编码为键值文本，跳过不被允许的字段。
     *
     * @param changes 变更
     * @return 编码结果（无有效字段时为空串）
     */
    static String encode(Map<String, String> changes) {
        if (changes == null || changes.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : changes.entrySet()) {
            String field = entry.getKey() == null ? "" : entry.getKey().trim();
            if (!isAllowed(field) || entry.getValue() == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(StudentModifyRequest.ENTRY_SEPARATOR);
            }
            builder.append(field).append(StudentModifyRequest.KEY_VALUE_SEPARATOR)
                    .append(entry.getValue().trim());
        }
        return builder.toString();
    }

    /**
     * 把键值文本应用到学籍记录。
     *
     * @param profile     目标学籍记录
     * @param changesJson 键值文本
     * @return 是否至少应用了一个字段
     */
    static boolean applyChanges(StudentProfile profile, String changesJson) {
        if (profile == null || changesJson == null
                || changesJson.trim().length() == 0) {
            return false;
        }
        boolean changed = false;
        String[] entries =
                changesJson.split(StudentModifyRequest.ENTRY_SEPARATOR);
        int index = 0;
        while (index < entries.length) {
            String entry = entries[index].trim();
            index = index + 1;
            int split = entry.indexOf(StudentModifyRequest.KEY_VALUE_SEPARATOR);
            if (split <= 0) {
                continue;
            }
            String field = entry.substring(0, split).trim();
            String value = entry.substring(split + 1).trim();
            if (FIELD_ENROLL_YEAR.equals(field)) {
                try {
                    profile.setEnrollYear(Integer.parseInt(value));
                    changed = true;
                } catch (NumberFormatException ignored) {
                    // 非法数字：跳过该字段，不影响其余变更
                }
            } else if (FIELD_STATUS.equals(field)) {
                EnrollmentStatus status = parseStatus(value);
                if (status != null) {
                    profile.setStatus(status);
                    changed = true;
                }
            }
        }
        return changed;
    }

    /**
     * 判断字段是否在学生可申请修改的白名单内。
     *
     * @param field 字段名
     * @return 是否允许
     */
    private static boolean isAllowed(String field) {
        return FIELD_ENROLL_YEAR.equals(field) || FIELD_STATUS.equals(field);
    }

    /**
     * 按枚举名解析学籍状态（忽略大小写）。
     *
     * @param value 枚举名
     * @return 对应状态；无法识别返回 null
     */
    private static EnrollmentStatus parseStatus(String value) {
        EnrollmentStatus[] all = EnrollmentStatus.values();
        int index = 0;
        while (index < all.length) {
            if (all[index].name().equalsIgnoreCase(value)) {
                return all[index];
            }
            index = index + 1;
        }
        return null;
    }
}
