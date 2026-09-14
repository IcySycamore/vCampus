package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.common.student.dto.StudentModifyRequest;
import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.StudentProfile;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 202 申请单的组装规则：全是纯函数，不碰界面也不发请求，所以可以直接单测。
 *
 * <p>
 * 从这里出去的每一笔都决定「申请单里到底写了什么」——多写一项会造成一次没人申请过的变更，
 * 少写一项则审核通过后学籍根本没变。业务规则集中在这一个文件里，界面只负责把控件取值递进来。
 *
 * <p>
 * 字段名取自 {@code StudentModifyRequest.FIELD_*}（双端共用的白名单常量）：名字写歪了服务端会
 * 直接跳过该字段，申请就变成空单，所以不许各处自己拼字符串。
 */
final class StudentModifyRequests {

    /** 私有构造器，禁止实例化工具类。 */
    private StudentModifyRequests() {
    }

    /**
     * 组装申请单里的「字段名 → 新值」，只保留真正改动过的项。
     *
     * @param current 当前档案
     * @param field 界面上的学术方向
     * @param year 界面上的年份文本
     * @param status 选中的在校状态<b>枚举名</b>；null 或空串表示未选
     * @return 要提交的变更；没有改动时为空 Map
     */
    static Map<String, String> changesOf(StudentProfile current, String field, String year,
            String status) {
        Map<String, String> changes = new LinkedHashMap<String, String>();
        if (current == null) {
            return changes;
        }
        String newField = field == null ? "" : field.trim();
        if (newField.length() > 0 && !newField.equals(orEmpty(current.getField()))) {
            changes.put(StudentModifyRequest.FIELD_FIELD, newField);
        }
        String newYear = year == null ? "" : year.trim();
        if (newYear.length() > 0 && !newYear.equals(String.valueOf(current.getJoinYear()))) {
            changes.put(StudentModifyRequest.FIELD_JOIN_YEAR, newYear);
        }
        String oldStatus = current.getStatus() == null ? "" : current.getStatus().name();
        String newStatus = status == null ? "" : status;
        if (newStatus.length() > 0 && !newStatus.equals(oldStatus)) {
            changes.put(StudentModifyRequest.FIELD_STATUS, newStatus);
        }
        return changes;
    }

    /**
     * 提交前的检查：返回第一条不通过的原因，全部通过时返回 null。
     *
     * <p>
     * 纯规则，不碰界面也不发请求——提交按钮拿它决定「先把原因写在旁边，还是发请求」。
     * 放在这里而不是写进界面，是为了能直接单测：下面每一条都对应一次会失败的提交。
     *
     * @param current 当前档案
     * @param field 界面上的学术方向
     * @param year 界面上的年份文本
     * @param status 选中的在校状态<b>枚举名</b>；空串表示未选
     * @param reason 申请理由
     * @return 原因文本；全部通过时为 null
     */
    static String check(StudentProfile current, String field, String year, String status,
            String reason) {
        if (current == null || current.getId() == null) {
            return "这份档案还没有主键，无法提交申请";
        }
        if (reason == null || reason.trim().length() == 0) {
            return "请填申请理由——教务要据此判断批不批";
        }
        Map<String, String> changes = changesOf(current, field, year, status);
        if (changes.isEmpty()) {
            return "没有改动任何字段，不需要提交申请";
        }
        if (changes.containsKey(StudentModifyRequest.FIELD_JOIN_YEAR) && !isInteger(year)) {
            return "入学年份要填整数";
        }
        return null;
    }

    /**
     * 全部在校状态的显示名。
     *
     * @return 显示名数组，顺序与枚举声明一致
     */
    static String[] statusNames() {
        CampusStatus[] values = CampusStatus.values();
        String[] names = new String[values.length];
        int index = 0;
        while (index < values.length) {
            names[index] = values[index].getDisplayName();
            index = index + 1;
        }
        return names;
    }

    /**
     * 显示名 → 枚举。
     *
     * @param displayName 界面上的中文名；null 或空白表示未选
     * @return 对应枚举；未选或无法识别时返回 null
     */
    static CampusStatus statusOf(String displayName) {
        if (displayName == null || displayName.trim().length() == 0) {
            return null;
        }
        return CampusStatus.fromDisplayName(displayName.trim());
    }

    /**
     * 是否是十进制整数（年份校验用）。
     *
     * @param text 文本
     * @return 能否用 int 解析
     */
    static boolean isInteger(String text) {
        if (text == null) {
            return false;
        }
        try {
            Integer.parseInt(text.trim());
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    /**
     * 空值归一为空串，省得各处判 null。
     *
     * @param value 原值
     * @return 原值；null 时为空串
     */
    static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
