package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.course.Timeslot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static edu.seu.vcampus.server.db.JdbcSupport.closeQuietly;

/**
 * 时间槽的读写辅助：把 {@code Set<Timeslot>} 落到 {@code tblTimeslot} 的某个归属上。
 *
 * <p>
 * 从 {@link CourseStoreJdbc} 拆出来，是因为教师、学生、课程、教室四类实体都要读写自己的可用与 偏好时间槽，逻辑完全一样；放在一处也免得每类实体各写一遍「先删后插」。
 *
 * <p>
 * 时间槽没有自己的对外标识 —— 它是一组值，{@link Timeslot#equals} 按「星期 + 起止分钟」比较 —— 所以归属方用
 * {@code (ownerType, ownerUuid)} 定位，主键 {@code tsUuid} 只是为了表结构完整。 「可用」与「偏好」是两类归属类型，删除时可分别处理。
 */
final class TimeslotStoreJdbc {

    private TimeslotStoreJdbc() {
    }

    /**
     * 覆盖某归属方的一类时间槽（先删后插）。
     *
     * @param connection 事务连接
     * @param ownerType  归属类型
     * @param ownerUuid  归属对象 UUID
     * @param timeslots  时间槽集合；可为 null 或空
     * @throws SQLException 写入失败
     */
    static void replace(Connection connection, String ownerType, String ownerUuid,
            Set<Timeslot> timeslots) throws SQLException {
        delete(connection, ownerType, ownerUuid);
        if (timeslots == null || timeslots.isEmpty()) {
            return;
        }
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement("INSERT INTO tblTimeslot (tsUuid, tsOwnerType,"
                    + " tsOwnerUuid, tsWeekday, tsStartMinute, tsEndMinute)"
                    + " VALUES (?, ?, ?, ?, ?, ?)");
            for (Timeslot slot : timeslots) {
                statement.setString(1, UUID.randomUUID().toString());
                statement.setString(2, ownerType);
                statement.setString(3, ownerUuid);
                statement.setInt(4, slot.getWeekday());
                statement.setInt(5, slot.getStartMinute());
                statement.setInt(6, slot.getEndMinute());
                statement.addBatch();
            }
            statement.executeBatch();
        } finally {
            closeQuietly(statement);
        }
    }

    /**
     * 读取某归属方的一类时间槽。
     *
     * @param connection 事务连接
     * @param ownerType  归属类型
     * @param ownerUuid  归属对象 UUID
     * @return 时间槽集合，不返回 null
     * @throws SQLException 查询失败
     */
    static Set<Timeslot> load(Connection connection, String ownerType, String ownerUuid)
            throws SQLException {
        Set<Timeslot> slots = new HashSet<Timeslot>();
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            statement = connection.prepareStatement("SELECT tsWeekday, tsStartMinute, tsEndMinute"
                    + " FROM tblTimeslot WHERE tsOwnerType = ? AND tsOwnerUuid = ?");
            statement.setString(1, ownerType);
            statement.setString(2, ownerUuid);
            rows = statement.executeQuery();
            while (rows.next()) {
                slots.add(new Timeslot(rows.getInt("tsWeekday"), rows.getInt("tsStartMinute"),
                        rows.getInt("tsEndMinute")));
            }
            return slots;
        } finally {
            closeQuietly(rows);
            closeQuietly(statement);
        }
    }

    /**
     * 删掉某归属方的一类时间槽。
     *
     * @param connection 事务连接
     * @param ownerType  归属类型
     * @param ownerUuid  归属对象 UUID
     * @throws SQLException 删除失败
     */
    static void delete(Connection connection, String ownerType, String ownerUuid)
            throws SQLException {
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement("DELETE FROM tblTimeslot"
                    + " WHERE tsOwnerType = ? AND tsOwnerUuid = ?");
            statement.setString(1, ownerType);
            statement.setString(2, ownerUuid);
            statement.executeUpdate();
        } finally {
            closeQuietly(statement);
        }
    }

}
