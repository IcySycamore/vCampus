package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.dto.ModifyRequestQuery;
import edu.seu.vcampus.common.student.entity.ModifyRequestStatus;
import edu.seu.vcampus.common.student.entity.StudentModifyRequest;
import edu.seu.vcampus.server.db.DatabaseAccessException;
import edu.seu.vcampus.server.db.DbHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static edu.seu.vcampus.server.db.JdbcSupport.closeQuietly;

/**
 * 学籍修改申请单数据访问：落表 {@code tblStudentModifyRequest}。
 *
 * <p>
 * 主键按全库 约定是 {@code smrUuid}；表里另有一个自增的 {@code smrId}，只用来回填实体里的 {@code requestId}（既有接口与客户端都按这个 Long
 * 单号办事），不参与对外寻址。
 *
 * <p>
 * 过滤与排序的 SQL 拼接交给 {@link StudentModifyRequestQueryBuilder}，好让 {@link #find} 与 {@link #count}
 * 用的是同一份条件。
 */
public class StudentModifyRequestDaoJdbc implements StudentModifyRequestDao {

    /** 查询列清单。 */
    private static final String COLUMNS = "smrId, smrUuid, uUuid, smProfileSeq, smChanges,"
            + " smReason, smStatus, smComment, smAppliedAt, smAuditedBy, smAuditedAt";

    @Override
    public boolean insert(StudentModifyRequest request) {
        if (request == null) {
            return false;
        }
        String sql = "INSERT INTO tblStudentModifyRequest (smrUuid, uUuid, smProfileSeq,"
                + " smChanges, smReason, smStatus, smComment, smAppliedAt, smAuditedBy,"
                + " smAuditedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet keys = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, request.getApplicantUuid());
            setNullableLong(statement, 3, request.getProfileId());
            statement.setString(4,
                    request.getChangesJson() == null ? "" : request.getChangesJson());
            statement.setString(5, request.getReason());
            statement.setString(6, statusName(request.getStatus()));
            statement.setString(7, request.getComment());
            statement.setTimestamp(8, appliedAt(request));
            statement.setString(9, request.getAuditedBy());
            statement.setTimestamp(10, auditedAt(request));
            if (statement.executeUpdate() <= 0) {
                return false;
            }
            keys = statement.getGeneratedKeys();
            if (keys.next()) {
                request.setRequestId(Long.valueOf(keys.getLong(1)));// 既有接口按 Long 单号办事
            }
            return true;
        } catch (SQLException e) {
            throw new DatabaseAccessException("新增申请单失败: " + request.getApplicantUuid(), e);
        } finally {
            closeQuietly(keys);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    @Override
    public StudentModifyRequest findById(Long requestId) {
        if (requestId == null) {
            return null;
        }
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement("SELECT " + COLUMNS
                    + " FROM tblStudentModifyRequest WHERE smrId = ?");
            statement.setLong(1, requestId.longValue());
            rows = statement.executeQuery();
            return rows.next() ? toRequest(rows) : null;
        } catch (SQLException e) {
            throw new DatabaseAccessException("查询申请单失败: " + requestId, e);
        } finally {
            closeQuietly(rows);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    @Override
    public boolean update(StudentModifyRequest request) {
        if (request == null || request.getRequestId() == null) {
            return false;
        }
        String sql = "UPDATE tblStudentModifyRequest SET smProfileSeq = ?, smChanges = ?,"
                + " smReason = ?, smStatus = ?, smComment = ?, smAppliedAt = ?, smAuditedBy = ?,"
                + " smAuditedAt = ? WHERE smrId = ?";
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(sql);
            setNullableLong(statement, 1, request.getProfileId());
            statement.setString(2,
                    request.getChangesJson() == null ? "" : request.getChangesJson());
            statement.setString(3, request.getReason());
            statement.setString(4, statusName(request.getStatus()));
            statement.setString(5, request.getComment());
            statement.setTimestamp(6, appliedAt(request));
            statement.setString(7, request.getAuditedBy());
            statement.setTimestamp(8, auditedAt(request));
            statement.setLong(9, request.getRequestId().longValue());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseAccessException("更新申请单失败: " + request.getRequestId(), e);
        } finally {
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    @Override
    public List<StudentModifyRequest> find(ModifyRequestQuery query, int offset, int limit) {
        List<StudentModifyRequest> found = new ArrayList<StudentModifyRequest>();
        List<Object> params = new ArrayList<Object>();
        String sql = "SELECT " + COLUMNS + " FROM tblStudentModifyRequest"
                + StudentModifyRequestQueryBuilder.where(query, params)
                + StudentModifyRequestQueryBuilder.orderBy(query);
        if (limit > 0) {
            sql = sql + " LIMIT ? OFFSET ?";
        }
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(sql);
            int index = 1;
            for (Object value : params) {
                statement.setObject(index++, value);
            }
            if (limit > 0) {
                statement.setInt(index++, limit);
                statement.setInt(index, Math.max(0, offset));
            }
            rows = statement.executeQuery();
            while (rows.next()) {
                found.add(toRequest(rows));
            }
            return found;
        } catch (SQLException e) {
            throw new DatabaseAccessException("查询申请单列表失败", e);
        } finally {
            closeQuietly(rows);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    @Override
    public long count(ModifyRequestQuery query) {
        List<Object> params = new ArrayList<Object>();
        String sql = "SELECT COUNT(*) FROM tblStudentModifyRequest"
                + StudentModifyRequestQueryBuilder.where(query, params);
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(sql);
            int index = 1;
            for (Object value : params) {
                statement.setObject(index++, value);
            }
            rows = statement.executeQuery();
            return rows.next() ? rows.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new DatabaseAccessException("统计申请单失败", e);
        } finally {
            closeQuietly(rows);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    /**
     * 结果行 → 申请单。
     *
     * @param rows 已定位到某行的结果集
     * @return 申请单
     * @throws SQLException 读取失败
     */
    private static StudentModifyRequest toRequest(ResultSet rows) throws SQLException {
        StudentModifyRequest request = new StudentModifyRequest();
        request.setRequestId(Long.valueOf(rows.getLong("smrId")));
        request.setApplicantUuid(rows.getString("uUuid"));
        long profileSeq = rows.getLong("smProfileSeq");
        request.setProfileId(rows.wasNull() ? null : Long.valueOf(profileSeq));
        request.setChangesJson(rows.getString("smChanges"));
        request.setReason(rows.getString("smReason"));
        request.setStatus(status(rows.getString("smStatus")));
        request.setComment(rows.getString("smComment"));
        Timestamp applied = rows.getTimestamp("smAppliedAt");
        request.setAppliedAt(applied == null ? 0L : applied.getTime());
        request.setAuditedBy(rows.getString("smAuditedBy"));
        Timestamp audited = rows.getTimestamp("smAuditedAt");
        request.setAuditedAt(audited == null ? 0L : audited.getTime());
        return request;
    }

    /**
     * 申请单的提交时间：实体没给就用当前时间，免得 NOT NULL 列写不进去。
     *
     * @param request 申请单
     * @return 落在秒精度上的时间戳
     */
    private static Timestamp appliedAt(StudentModifyRequest request) {
        long millis = request.getAppliedAt() <= 0L
                ? System.currentTimeMillis()
                : request.getAppliedAt();
        return new Timestamp(millis);
    }

    /**
     * 申请单的审核时间：未审核（0）时写 NULL。
     *
     * @param request 申请单
     * @return 时间戳；未审核返回 null
     */
    private static Timestamp auditedAt(StudentModifyRequest request) {
        return request.getAuditedAt() <= 0L ? null : new Timestamp(request.getAuditedAt());
    }

    /**
     * 绑定可空的 Long 参数。
     *
     * @param statement 语句
     * @param index     参数下标
     * @param value     值；null 写 SQL NULL
     * @throws SQLException 绑定失败
     */
    private static void setNullableLong(PreparedStatement statement, int index, Long value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.BIGINT);
        } else {
            statement.setLong(index, value.longValue());
        }
    }

    /**
     * 状态枚举 → 落库取值。
     *
     * @param status 状态；null 视作待审核
     * @return 枚举名
     */
    private static String statusName(ModifyRequestStatus status) {
        return status == null ? ModifyRequestStatus.PENDING.name() : status.name();
    }

    /**
     * 库中文本 → 状态。
     *
     * @param text 枚举名；无法识别时视作待审核
     * @return 状态
     */
    private static ModifyRequestStatus status(String text) {
        if (text == null) {
            return ModifyRequestStatus.PENDING;
        }
        try {
            return ModifyRequestStatus.valueOf(text);
        } catch (IllegalArgumentException e) {
            return ModifyRequestStatus.PENDING;
        }
    }

}
