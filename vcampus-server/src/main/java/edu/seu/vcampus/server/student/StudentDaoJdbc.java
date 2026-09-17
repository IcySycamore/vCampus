package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.PersonCategory;
import edu.seu.vcampus.common.student.entity.StudentProfile;
import edu.seu.vcampus.server.db.DatabaseAccessException;
import edu.seu.vcampus.server.db.DbHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 【MySQL 版】学籍档案存储：落表 {@code tblStudentProfile}。
 *
 * <p>
 * 与 {@link StudentDaoFile} 实现同一个 {@link StudentDao}，可在装配处按开关二选一。表结构见
 * {@code sql/vCampus-extend.sql}：{@code spId}（自增辅助序号，对应实体的 {@code m_id}；对外标识是 {@code uUuid}）、
 * {@code uUuid}（用户账户 uuid，唯一）、类别、学号、入学年份、状态、方向与软删除位。
 *
 * <p>
 * <b>姓名不落库</b>：{@code StudentProfile.realName} 是联表展示字段，由服务端按 uuid 反查用户 模块填充，因此读写本表时一律忽略它。
 *
 * <p>
 * 枚举以 {@code name()} 落库（如 {@code STUDENT}、{@code ENROLLED}），读回时用 {@code valueOf} 还原；无法识别的取值按 null
 * 处理，避免一条脏数据让整个列表查不出来。
 */
public class StudentDaoJdbc implements StudentDao {

    /** 查询列清单。 */
    private static final String COLUMNS = "spId, uUuid, spCategory, spStudentNo, spJoinYear, spStatus, spField";

    @Override
    public StudentProfile findById(Long id) {
        if (id == null) {
            return null;
        }
        return findOne("SELECT " + COLUMNS + " FROM tblStudentProfile"
                + " WHERE spId = ? AND spDeleted = 0", id);
    }

    @Override
    public StudentProfile findByUserUuid(String userUuid) {
        if (userUuid == null) {
            return null;
        }
        return findOne("SELECT " + COLUMNS + " FROM tblStudentProfile"
                + " WHERE uUuid = ? AND spDeleted = 0", userUuid);
    }

    @Override
    public List<StudentProfile> findAll() {
        List<StudentProfile> found = new ArrayList<StudentProfile>();
        String sql = "SELECT " + COLUMNS + " FROM tblStudentProfile"
                + " WHERE spDeleted = 0 ORDER BY spId";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(sql);
            rows = statement.executeQuery();
            while (rows.next()) {
                found.add(toProfile(rows));
            }
            return found;
        } catch (SQLException e) {
            throw new DatabaseAccessException("查询全部学籍档案失败", e);
        } finally {
            close(rows, statement, connection);
        }
    }

    @Override
    public boolean insert(StudentProfile profile) {
        if (profile == null) {
            return false;
        }
        String sql = "INSERT INTO tblStudentProfile"
                + " (uUuid, spCategory, spStudentNo, spJoinYear, spStatus, spField, spDeleted)"
                + " VALUES (?, ?, ?, ?, ?, ?, 0)";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet keys = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, profile.getUserUuid());
            statement.setString(2, name(profile.getPersonCategory()));
            statement.setString(3, profile.getStudentNo());
            statement.setInt(4, profile.getJoinYear());
            statement.setString(5, name(profile.getStatus()));
            statement.setString(6, profile.getField());
            if (statement.executeUpdate() <= 0) {
                return false;
            }
            // 回填自增辅助序号：既有接口按 Long id 定位记录，因此仍需回填
            keys = statement.getGeneratedKeys();
            if (keys.next()) {
                profile.setId(Long.valueOf(keys.getLong(1)));
            }
            return true;
        } catch (SQLException e) {
            throw new DatabaseAccessException("新增学籍档案失败: " + profile.getUserUuid(), e);
        } finally {
            close(keys, statement, connection);
        }
    }

    @Override
    public boolean update(StudentProfile profile) {
        if (profile == null || profile.getId() == null) {
            return false;
        }
        String sql = "UPDATE tblStudentProfile SET uUuid = ?, spCategory = ?, spStudentNo = ?,"
                + " spJoinYear = ?, spStatus = ?, spField = ?, spDeleted = ? WHERE spId = ?";
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(sql);
            statement.setString(1, profile.getUserUuid());
            statement.setString(2, name(profile.getPersonCategory()));
            statement.setString(3, profile.getStudentNo());
            statement.setInt(4, profile.getJoinYear());
            statement.setString(5, name(profile.getStatus()));
            statement.setString(6, profile.getField());
            statement.setInt(7, profile.isDeleted() ? 1 : 0);
            statement.setLong(8, profile.getId().longValue());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseAccessException("更新学籍档案失败: " + profile.getId(), e);
        } finally {
            close(null, statement, connection);
        }
    }

    @Override
    public boolean softDelete(Long id) {
        if (id == null) {
            return false;
        }
        String sql = "UPDATE tblStudentProfile SET spDeleted = 1 WHERE spId = ? AND spDeleted = 0";
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(sql);
            statement.setLong(1, id.longValue());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseAccessException("软删除学籍档案失败: " + id, e);
        } finally {
            close(null, statement, connection);
        }
    }

    /**
     * 查一条档案。
     *
     * @param sql   查询语句（含一个占位符）
     * @param param 绑定的查询值
     * @return 档案；无记录返回 null
     */
    private StudentProfile findOne(String sql, Object param) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(sql);
            statement.setObject(1, param);
            rows = statement.executeQuery();
            return rows.next() ? toProfile(rows) : null;
        } catch (SQLException e) {
            throw new DatabaseAccessException("查询学籍档案失败: " + param, e);
        } finally {
            close(rows, statement, connection);
        }
    }

    /**
     * 结果行 → 档案对象。
     *
     * @param rows 已定位到某行的结果集
     * @return 档案；姓名字段留空（由上层联查填充）
     * @throws SQLException 读取失败
     */
    private StudentProfile toProfile(ResultSet rows) throws SQLException {
        StudentProfile profile = new StudentProfile();
        profile.setId(Long.valueOf(rows.getLong("spId")));
        profile.setUserUuid(rows.getString("uUuid"));
        profile.setPersonCategory(category(rows.getString("spCategory")));
        profile.setStudentNo(rows.getString("spStudentNo"));
        profile.setJoinYear(rows.getInt("spJoinYear"));
        profile.setStatus(status(rows.getString("spStatus")));
        profile.setField(rows.getString("spField"));
        profile.restore();// 查询一律带 spDeleted = 0，读到即未删除
        return profile;
    }

    /**
     * 枚举 → 库中文本。
     *
     * @param value 枚举值；可为 null
     * @return 枚举名；null 输入返回 null
     */
    private static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    /**
     * 库中文本 → 人员类别。
     *
     * @param text 库中取值；null 或无法识别时视作学生
     * @return 人员类别
     */
    private static PersonCategory category(String text) {
        if (text == null) {
            return PersonCategory.STUDENT;
        }
        try {
            return PersonCategory.valueOf(text);
        } catch (IllegalArgumentException e) {
            return PersonCategory.STUDENT;
        }
    }

    /**
     * 库中文本 → 在校状态。
     *
     * @param text 库中取值；null 或无法识别时返回 null
     * @return 在校状态
     */
    private static CampusStatus status(String text) {
        if (text == null) {
            return null;
        }
        try {
            return CampusStatus.valueOf(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 释放数据库资源（逐项关闭，单项失败不影响其它）。
     *
     * @param rows       结果集；可为 null
     * @param statement  语句；可为 null
     * @param connection 连接；可为 null
     */
    private void close(ResultSet rows, PreparedStatement statement, Connection connection) {
        if (rows != null) {
            try {
                rows.close();
            } catch (SQLException ignored) {
                // 关闭失败不影响业务结果
            }
        }
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException ignored) {
                // 同上
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // 同上
            }
        }
    }
}
