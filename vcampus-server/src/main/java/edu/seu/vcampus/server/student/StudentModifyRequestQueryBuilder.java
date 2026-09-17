package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.dto.ModifyRequestQuery;
import edu.seu.vcampus.common.student.entity.RequestField;

import java.util.List;

/**
 * 把学籍申请单的查询条件翻译成 SQL 片段。
 *
 * <p>
 * 抽成单独一个类，是因为 {@link StudentModifyRequestDaoJdbc} 的 {@code find} 与 {@code count}
 * 必须用<b>同一份</b>过滤条件，否则「本页条数」与「总数」会对不上；把拼接放在一处，两边共享 同一个 {@code where}。绑定参数按出现顺序追加到调用方给的列表里，与占位符一一对应。
 *
 * <p>
 * 关键词用 {@code LIKE} 匹配，数值列（申请单号、学籍序号）先 {@code CAST} 成字符再比，免得审核人
 * 输入「12」时因类型不匹配搜不到。{@link RequestField#ALL} 一次比对多列，与接口注释里 「审批人手里就一个搜索框」的说法一致。
 */
final class StudentModifyRequestQueryBuilder {

    /** 关键词多字段匹配时涉及的列。 */
    private static final String[] SEARCHABLE = { "smrUuid", "CAST(smrId AS CHAR)",
            "CAST(smProfileSeq AS CHAR)", "uUuid", "smChanges", "smReason" };

    private StudentModifyRequestQueryBuilder() {
    }

    /**
     * 拼出 WHERE 子句。
     *
     * @param query  查询条件；null 表示不过滤
     * @param params 出参：按顺序追加绑定参数
     * @return 形如 {@code " WHERE ..."} 的片段；无条件时返回空串
     */
    static String where(ModifyRequestQuery query, List<Object> params) {
        if (query == null) {
            return "";
        }
        StringBuilder sql = new StringBuilder();
        addEquals(sql, params, "uUuid", query.getApplicantUuid());
        addEquals(sql, params, "smProfileSeq", query.getProfileId());
        addEquals(sql, params, "smStatus", name(query.getStatus()));
        addKeyword(sql, params, query);
        return sql.length() == 0 ? "" : " WHERE " + sql.toString();
    }

    /**
     * 拼出 ORDER BY 子句。
     *
     * <p>
     * 末尾固定追加 {@code smrId} 作次级排序：申请时间只精确到秒，同一秒提交的多条若只按时间排， 翻页时顺序会抖动。
     *
     * @param query 查询条件；null 或未指定排序字段时按申请时间倒序
     * @return 形如 {@code " ORDER BY smAppliedAt DESC, smrId DESC"} 的片段
     */
    static String orderBy(ModifyRequestQuery query) {
        String column = sortColumn(query == null ? null : query.getSortBy());
        boolean descending = query == null || query.isDescending();
        return " ORDER BY " + column + (descending ? " DESC" : " ASC") + ", smrId DESC";
    }

    /**
     * 追加等值条件。
     *
     * @param sql    正在拼的语句
     * @param params 绑定参数
     * @param column 列名
     * @param value  值；null 表示不加条件
     */
    private static void addEquals(StringBuilder sql, List<Object> params, String column,
            Object value) {
        if (value == null) {
            return;
        }
        and(sql).append(column).append(" = ?");
        params.add(value);
    }

    /**
     * 追加关键词模糊匹配。
     *
     * @param sql    正在拼的语句
     * @param params 绑定参数
     * @param query  查询条件
     */
    private static void addKeyword(StringBuilder sql, List<Object> params,
            ModifyRequestQuery query) {
        String keyword = query.getKeyword();
        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }
        String like = "%" + keyword.trim() + "%";
        RequestField field = query.getSearchField();
        if (field == null || field == RequestField.ALL || !field.isSearchable()) {
            and(sql).append('(');
            for (int i = 0; i < SEARCHABLE.length; i++) {
                if (i > 0) {
                    sql.append(" OR ");
                }
                sql.append(SEARCHABLE[i]).append(" LIKE ?");
                params.add(like);
            }
            sql.append(')');
            return;
        }
        String column = searchColumn(field);
        if (column != null) {
            and(sql).append(column).append(" LIKE ?");
            params.add(like);
        }
    }

    /**
     * 拼接连接词并返回语句本身，省掉每处都写一遍长度判断。
     *
     * @param sql 正在拼的语句
     * @return 同一个 {@link StringBuilder}
     */
    private static StringBuilder and(StringBuilder sql) {
        return sql.length() == 0 ? sql : sql.append(" AND ");
    }

    /**
     * 枚举 → 落库取值。
     *
     * @param status 状态；null 返回 null
     * @return 枚举名；null 输入返回 null
     */
    private static String name(Enum<?> status) {
        return status == null ? null : status.name();
    }

    /**
     * 可搜索字段 → 列名。
     *
     * @param field 字段
     * @return 列名；不可搜索时返回 null
     */
    private static String searchColumn(RequestField field) {
        switch (field) {
            case REQUEST_ID:
                return "CAST(smrId AS CHAR)";
            case PROFILE_ID:
                return "CAST(smProfileSeq AS CHAR)";
            case APPLICANT_UUID:
                return "uUuid";
            case CHANGES:
                return "smChanges";
            case REASON:
                return "smReason";
            default:
                return null;
        }
    }

    /**
     * 可排序字段 → 列名。
     *
     * @param field 字段；null 或不可排序时按申请时间
     * @return 列名
     */
    private static String sortColumn(RequestField field) {
        if (field == null || !field.isSortable()) {
            return "smAppliedAt";
        }
        switch (field) {
            case REQUEST_ID:
                return "smrId";
            case PROFILE_ID:
                return "smProfileSeq";
            case APPLICANT_UUID:
                return "uUuid";
            case STATUS:
                return "smStatus";
            default:
                return "smAppliedAt";
        }
    }
}
