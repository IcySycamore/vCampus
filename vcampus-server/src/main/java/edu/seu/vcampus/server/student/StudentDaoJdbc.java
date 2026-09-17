package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.PersonCategory;
import edu.seu.vcampus.common.student.entity.StudentProfile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

/**
 * 【数据库版】学籍存储：档案落在 MySQL 的 {@code tblCampusProfile}。
 *
 * <p>
 * 与 {@link StudentDaoFile} 的差别只有「存在哪」：同样的软删除语义、同样的 id 回填、同样只返回
 * 未删除记录。业务层只依赖 {@link StudentDao}，换实现不用改一行（见 ADR-0002）。
 *
 * <p>
 * <b>本类不做过滤、排序与分页</b>：那些在 {@link StudentService} 里做，而且必须在补齐姓名之后做
 * ——{@code realName} 是联查用户模块得来的展示字段，本表没有这一列（见
 * {@code docs/学籍模块数据库对接说明.md} §3.2）。把过滤下推到 SQL 会让「按姓名搜」失效，
 * 所以这里只负责原样取回。
 *
 * <p>
 * 出错一律记日志并返回 null / false：接口没有声明受检异常，而「数据库连不上」与「这条记录不存在」
 * 在调用方眼里都表现为「拿不到」，界面上会给出对应的提示，比抛异常中断整个请求更稳。
 */
public class StudentDaoJdbc implements StudentDao {

    /** 选列一律写全，避免用 {@code SELECT *} 时被表结构变更悄悄改变映射。 */
    private static final String COLUMNS =
            "srId, userUuid, personCategory, joinYear, status, field, studentNo, deleted";

    /** 档案表名。 */
    private static final String TABLE = "tblCampusProfile";

    /** 连接来源。 */
    private final DataSource m_source;

    /**
     * 构造数据库版学籍存储。
     *
     * @param source 连接来源（生产用 {@link StudentDataSource}，测试可换）
     * @throws IllegalArgumentException source 为 null
     */
    public StudentDaoJdbc(DataSource source) {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        this.m_source = source;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StudentProfile findById(Long id) {
        if (id == null) {
            return null;
        }
        String sql = "SELECT " + COLUMNS + " FROM " + TABLE + " WHERE srId = ? AND deleted = 0";
        try (Connection connection = m_source.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id.longValue());
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? read(rows) : null;
            }
        } catch (SQLException exception) {
            fail("按主键查学籍", exception);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StudentProfile findByUserUuid(String userUuid) {
        if (userUuid == null) {
            return null;
        }
        String sql = "SELECT " + COLUMNS + " FROM " + TABLE
                + " WHERE userUuid = ? AND deleted = 0";
        try (Connection connection = m_source.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userUuid);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? read(rows) : null;
            }
        } catch (SQLException exception) {
            fail("按账户查学籍", exception);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<StudentProfile> findAll() {
        List<StudentProfile> all = new ArrayList<StudentProfile>();
        String sql = "SELECT " + COLUMNS + " FROM " + TABLE + " WHERE deleted = 0 ORDER BY srId";
        try (Connection connection = m_source.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                all.add(read(rows));
            }
        } catch (SQLException exception) {
            fail("列全部学籍", exception);
        }
        return all;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * 主键由数据库自增分配并<b>回填</b>到传入对象：调用方拿不到主键，后续的改状态与修改申请都会
     * 落空。{@code realName} 是联查展示字段，写库时忽略。
     */
    @Override
    public boolean insert(StudentProfile profile) {
        if (!writable(profile)) {
            return false;
        }
        String sql = "INSERT INTO " + TABLE
                + " (userUuid, personCategory, joinYear, status, field, studentNo, deleted)"
                + " VALUES (?, ?, ?, ?, ?, ?, 0)";
        try (Connection connection = m_source.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql,
                        Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, profile.getUserUuid());
            statement.setString(2, profile.getPersonCategory().name());
            statement.setInt(3, profile.getJoinYear());
            statement.setString(4, profile.getStatus().name());
            statement.setString(5, profile.getField());
            statement.setString(6, profile.getStudentNo());
            if (statement.executeUpdate() <= 0) {
                return false;
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    profile.setId(Long.valueOf(keys.getLong(1)));
                }
            }
            return true;
        } catch (SQLException exception) {
            fail("新增学籍", exception);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean update(StudentProfile profile) {
        if (!writable(profile) || profile.getId() == null) {
            return false;
        }
        String sql = "UPDATE " + TABLE + " SET userUuid = ?, personCategory = ?, joinYear = ?,"
                + " status = ?, field = ?, studentNo = ?, deleted = ? WHERE srId = ?";
        try (Connection connection = m_source.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, profile.getUserUuid());
            statement.setString(2, profile.getPersonCategory().name());
            statement.setInt(3, profile.getJoinYear());
            statement.setString(4, profile.getStatus().name());
            statement.setString(5, profile.getField());
            statement.setString(6, profile.getStudentNo());
            statement.setInt(7, profile.isDeleted() ? 1 : 0);
            statement.setLong(8, profile.getId().longValue());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            fail("更新学籍", exception);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * 只置删除标记，不 DELETE 数据行：注销要可追溯，而且误注销只能靠改数据恢复。
     */
    @Override
    public boolean softDelete(Long id) {
        if (id == null) {
            return false;
        }
        String sql = "UPDATE " + TABLE + " SET deleted = 1 WHERE srId = ? AND deleted = 0";
        try (Connection connection = m_source.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id.longValue());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            fail("注销学籍", exception);
            return false;
        }
    }

    /**
     * 把一行结果集读成档案。
     *
     * <p>
     * {@code realName} 有意留空：它不是本表的列，由服务端联查用户模块后填充。
     *
     * @param rows 当前行
     * @return 档案
     * @throws SQLException 读取失败
     */
    private static StudentProfile read(ResultSet rows) throws SQLException {
        StudentProfile profile = new StudentProfile();
        profile.setId(Long.valueOf(rows.getLong("srId")));
        profile.setUserUuid(rows.getString("userUuid"));
        profile.setPersonCategory(PersonCategory.valueOf(rows.getString("personCategory")));
        profile.setJoinYear(rows.getInt("joinYear"));
        profile.setStatus(CampusStatus.valueOf(rows.getString("status")));
        profile.setField(rows.getString("field"));
        profile.setStudentNo(rows.getString("studentNo"));
        if (rows.getInt("deleted") != 0) {
            profile.markDeleted();
        }
        return profile;
    }

    /**
     * 判定一条档案能否写库：三个 NOT NULL 列（账户、类别、状态）缺任何一个都不写。
     *
     * <p>
     * 宁可返回失败也不让数据库抛约束错误：约束错误的日志里只有列名，看不出是哪条业务操作触发的。
     *
     * @param profile 档案
     * @return 可写返回 true
     */
    private static boolean writable(StudentProfile profile) {
        return profile != null && profile.getUserUuid() != null
                && profile.getPersonCategory() != null && profile.getStatus() != null;
    }

    /**
     * 记录一次数据库失败。
     *
     * @param action 正在做的事（用于日志定位）
     * @param exception 异常
     */
    private static void fail(String action, SQLException exception) {
        System.err.println("学籍数据库操作失败（" + action + "）：" + exception.getMessage());
    }
}
