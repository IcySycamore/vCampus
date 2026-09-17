package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.dto.ModifyRequestQuery;
import edu.seu.vcampus.common.student.entity.ModifyRequestStatus;
import edu.seu.vcampus.common.student.entity.RequestField;
import edu.seu.vcampus.common.student.entity.StudentModifyRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

/**
 * 【数据库版】修改申请单存储：落在 MySQL 的 {@code tblModifyRequest}。
 *
 * <p>
 * 与内存/文件实现行为一致的三件事：主键由数据库分配并回填、按申请时间倒序（时间戳相同的用主键做
 * 次级排序，否则分页时同一条会重复出现或漏掉）、{@code find} 与 {@code count} 用<b>同一份</b>
 * 条件（条件写两遍迟早只改一处，表现是「总条数和实际页数对不上」）。
 *
 * <p>
 * 过滤条件到 SQL 的对应关系以 {@link ModifyRequestMatcher} 为准，排序以
 * {@link ModifyRequestSorter} 为准——那两个类是内存与文件实现共用的判断，本类是把同一套语义翻成
 * SQL。两者的结果一致性由 {@code StudentModifyRequestDaoJdbcTest} 对照验证。
 *
 * <p>
 * 关键词里的 {@code %} 与 {@code _} 会被转义：不转义的话用户敲一个 {@code %} 就等于把整张表
 * 捞出来，看起来像「搜索坏了」。
 */
public class StudentModifyRequestDaoJdbc implements StudentModifyRequestDao {

    /** 选列写全，避免 {@code SELECT *} 被表结构变更悄悄改变映射。 */
    private static final String COLUMNS = "mrId, profileId, applicantUuid, changesJson, reason,"
            + " status, comment, appliedAt, auditedBy, auditedAt";

    /** 申请单表名。 */
    private static final String TABLE = "tblModifyRequest";

    /** 连接来源。 */
    private final DataSource m_source;

    /**
     * 构造数据库版申请单存储。
     *
     * @param source 连接来源
     * @throws IllegalArgumentException source 为 null
     */
    public StudentModifyRequestDaoJdbc(DataSource source) {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        this.m_source = source;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean insert(StudentModifyRequest request) {
        if (request == null || request.getProfileId() == null
                || request.getApplicantUuid() == null || request.getStatus() == null) {
            return false;
        }
        String sql = "INSERT INTO " + TABLE + " (profileId, applicantUuid, changesJson, reason,"
                + " status, comment, appliedAt, auditedBy, auditedAt)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = m_source.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql,
                        Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, request.getProfileId().longValue());
            statement.setString(2, request.getApplicantUuid());
            statement.setString(3, request.getChangesJson());
            statement.setString(4, request.getReason());
            statement.setString(5, request.getStatus().name());
            statement.setString(6, request.getComment());
            statement.setLong(7, request.getAppliedAt());
            statement.setString(8, request.getAuditedBy());
            statement.setLong(9, request.getAuditedAt());
            if (statement.executeUpdate() <= 0) {
                return false;
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    request.setRequestId(Long.valueOf(keys.getLong(1)));
                }
            }
            return true;
        } catch (SQLException exception) {
            fail("新增申请单", exception);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StudentModifyRequest findById(Long requestId) {
        if (requestId == null) {
            return null;
        }
        String sql = "SELECT " + COLUMNS + " FROM " + TABLE + " WHERE mrId = ?";
        try (Connection connection = m_source.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, requestId.longValue());
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? read(rows) : null;
            }
        } catch (SQLException exception) {
            fail("按主键查申请单", exception);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean update(StudentModifyRequest request) {
        if (request == null || request.getRequestId() == null) {
            return false;
        }
        String sql = "UPDATE " + TABLE + " SET profileId = ?, applicantUuid = ?, changesJson = ?,"
                + " reason = ?, status = ?, comment = ?, appliedAt = ?, auditedBy = ?,"
                + " auditedAt = ? WHERE mrId = ?";
        try (Connection connection = m_source.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, request.getProfileId().longValue());
            statement.setString(2, request.getApplicantUuid());
            statement.setString(3, request.getChangesJson());
            statement.setString(4, request.getReason());
            statement.setString(5, request.getStatus().name());
            statement.setString(6, request.getComment());
            statement.setLong(7, request.getAppliedAt());
            statement.setString(8, request.getAuditedBy());
            statement.setLong(9, request.getAuditedAt());
            statement.setLong(10, request.getRequestId().longValue());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            fail("更新申请单", exception);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<StudentModifyRequest> find(ModifyRequestQuery query, int offset, int limit) {
        List<StudentModifyRequest> items = new ArrayList<StudentModifyRequest>();
        if (limit <= 0) {
            return items;
        }
        List<Object> params = new ArrayList<Object>();
        String sql = "SELECT " + COLUMNS + " FROM " + TABLE + whereOf(query, params)
                + orderOf(query) + " LIMIT ? OFFSET ?";
        params.add(Integer.valueOf(limit));
        params.add(Integer.valueOf(Math.max(0, offset)));
        try (Connection connection = m_source.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    items.add(read(rows));
                }
            }
        } catch (SQLException exception) {
            fail("查询申请单", exception);
        }
        return items;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long count(ModifyRequestQuery query) {
        List<Object> params = new ArrayList<Object>();
        String sql = "SELECT COUNT(*) FROM " + TABLE + whereOf(query, params);
        try (Connection connection = m_source.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getLong(1) : 0L;
            }
        } catch (SQLException exception) {
            fail("统计申请单", exception);
            return 0L;
        }
    }

    /**
     * 组装 WHERE 子句（不含 ORDER BY / LIMIT，便于 {@code find} 与 {@code count} 共用）。
     *
     * @param query 过滤条件；null 表示不过滤
     * @param params 出参：按顺序收集占位符的值
     * @return 以 {@code WHERE 1 = 1} 开头的子句
     */
    private static String whereOf(ModifyRequestQuery query, List<Object> params) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (query == null) {
            return where.toString();
        }
        if (query.getStatus() != null) {
            where.append(" AND status = ?");
            params.add(query.getStatus().name());
        }
        if (query.getProfileId() != null) {
            where.append(" AND profileId = ?");
            params.add(query.getProfileId());
        }
        if (query.getApplicantUuid() != null) {
            where.append(" AND applicantUuid = ?");
            params.add(query.getApplicantUuid());
        }
        String keyword = query.getKeyword() == null ? "" : query.getKeyword().trim();
        if (keyword.length() > 0) {
            appendKeyword(where, params, likeOf(keyword), query.getSearchField());
        }
        return where.toString();
    }

    /**
     * 追加关键词条件：指定了搜索列就只比那一列，否则一次比多列。
     *
     * @param where 子句
     * @param params 占位符取值
     * @param like 已加通配符的关键词
     * @param field 搜索字段
     */
    private static void appendKeyword(StringBuilder where, List<Object> params, String like,
            RequestField field) {
        if (field == null || field == RequestField.ALL) {
            where.append(" AND (CAST(mrId AS CHAR) LIKE ? OR CAST(profileId AS CHAR) LIKE ?"
                    + " OR applicantUuid LIKE ? OR changesJson LIKE ? OR reason LIKE ?)");
            int index = 0;
            while (index < 5) {
                params.add(like);
                index = index + 1;
            }
            return;
        }
        String column = columnOf(field);
        if (column == null) {
            return;
        }
        where.append(" AND ").append(column).append(" LIKE ?");
        params.add(like);
    }

    /**
     * 取字段对应的列表达式。
     *
     * @param field 字段
     * @return 列表达式；不可搜索的字段返回 null
     */
    private static String columnOf(RequestField field) {
        if (field == RequestField.REQUEST_ID) {
            return "CAST(mrId AS CHAR)";
        }
        if (field == RequestField.PROFILE_ID) {
            return "CAST(profileId AS CHAR)";
        }
        if (field == RequestField.APPLICANT_UUID) {
            return "applicantUuid";
        }
        if (field == RequestField.CHANGES) {
            return "changesJson";
        }
        if (field == RequestField.REASON) {
            return "reason";
        }
        return null;
    }

    /**
     * 组装 ORDER BY：默认申请时间倒序（清待办时新提交的在最上面），末尾一律补主键做次级排序。
     *
     * @param query 查询条件；null 表示默认排序
     * @return 子句
     */
    private static String orderOf(ModifyRequestQuery query) {
        RequestField key = query == null ? null : query.getSortBy();
        boolean explicit = key != null && key.isSortable();
        String column = "appliedAt";
        if (explicit) {
            if (key == RequestField.REQUEST_ID) {
                column = "mrId";
            } else if (key == RequestField.PROFILE_ID) {
                column = "profileId";
            } else if (key == RequestField.APPLICANT_UUID) {
                column = "applicantUuid";
            } else if (key == RequestField.STATUS) {
                column = "status";
            }
        }
        boolean descending = explicit ? query.isDescending() : true;
        return " ORDER BY " + column + (descending ? " DESC" : " ASC") + ", mrId DESC";
    }

    /**
     * 把关键词转成 LIKE 模式，并转义 {@code %} 与 {@code _}。
     *
     * @param keyword 关键词
     * @return 带通配符的模式
     */
    private static String likeOf(String keyword) {
        String escaped = keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    /**
     * 按顺序绑定占位符。
     *
     * @param statement 语句
     * @param params 取值
     * @throws SQLException 绑定失败
     */
    private static void bind(PreparedStatement statement, List<Object> params)
            throws SQLException {
        int index = 0;
        while (index < params.size()) {
            statement.setObject(index + 1, params.get(index));
            index = index + 1;
        }
    }

    /**
     * 把一行结果集读成申请单。
     *
     * @param rows 当前行
     * @return 申请单
     * @throws SQLException 读取失败
     */
    private static StudentModifyRequest read(ResultSet rows) throws SQLException {
        StudentModifyRequest request = new StudentModifyRequest();
        request.setRequestId(Long.valueOf(rows.getLong("mrId")));
        request.setProfileId(Long.valueOf(rows.getLong("profileId")));
        request.setApplicantUuid(rows.getString("applicantUuid"));
        request.setChangesJson(rows.getString("changesJson"));
        request.setReason(rows.getString("reason"));
        request.setStatus(ModifyRequestStatus.valueOf(rows.getString("status")));
        request.setComment(rows.getString("comment"));
        request.setAppliedAt(rows.getLong("appliedAt"));
        request.setAuditedBy(rows.getString("auditedBy"));
        request.setAuditedAt(rows.getLong("auditedAt"));
        return request;
    }

    /**
     * 记录一次数据库失败。
     *
     * @param action 正在做的事
     * @param exception 异常
     */
    private static void fail(String action, SQLException exception) {
        System.err.println("申请单数据库操作失败（" + action + "）：" + exception.getMessage());
    }
}
