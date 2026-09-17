package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.course.Building;
import edu.seu.vcampus.server.db.DatabaseAccessException;
import edu.seu.vcampus.server.db.DbHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static edu.seu.vcampus.server.db.JdbcSupport.closeQuietly;

/**
 * 教学楼的读写辅助：{@link Building} ↔ {@code tblBuilding}。
 *
 * <p>
 * 单独成类而不并进 {@link CourseCatalogStoreJdbc}：教学楼只有「名字 + 所属学院」两个字段，既不拆子表也不参与
 * 匹配运算，逻辑量太小，塞进目录存取里只会让那个类继续膨胀。
 *
 * <p>
 * 所属学院是可空外键：教学楼可以先建成、后挂学院（先盖楼再分学院），因此 {@code bdCollegeUuid} 允许为空。
 */
final class BuildingStoreJdbc {

    private BuildingStoreJdbc() {
    }

    /**
     * 加载全部教学楼。
     *
     * @return 教学楼列表，不返回 null
     * @throws DatabaseAccessException 查询失败
     */
    static List<Building> load() {
        List<Building> buildings = new ArrayList<Building>();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(
                    "SELECT bdUuid, bdName, bdCollegeUuid FROM tblBuilding");
            rows = statement.executeQuery();
            while (rows.next()) {
                buildings.add(toBuilding(rows));
            }
            return buildings;
        } catch (SQLException e) {
            throw new DatabaseAccessException("加载教学楼失败", e);
        } finally {
            closeQuietly(rows);
            closeQuietly(statement);
            closeQuietly(connection);
        }
    }

    /**
     * 写入或覆盖一个教学楼。
     *
     * @param connection 事务连接
     * @param building   教学楼
     * @throws SQLException 写入失败
     */
    static void save(Connection connection, Building building) throws SQLException {
        if (building == null) {
            return;
        }
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement("INSERT INTO tblBuilding (bdUuid, bdName,"
                    + " bdCollegeUuid) VALUES (?, ?, ?)"
                    + " ON DUPLICATE KEY UPDATE bdName = VALUES(bdName),"
                    + " bdCollegeUuid = VALUES(bdCollegeUuid)");
            statement.setString(1, building.getUuid());
            statement.setString(2, building.getName());
            statement.setString(3, building.getCollegeUuid());
            statement.executeUpdate();
        } finally {
            closeQuietly(statement);
        }
    }

    /**
     * 结果行 → 教学楼。
     *
     * @param rows 已定位到某行的结果集
     * @return 教学楼
     * @throws SQLException 读取失败
     */
    private static Building toBuilding(ResultSet rows) throws SQLException {
        Building building = new Building();
        building.setUuid(rows.getString("bdUuid"));
        building.setName(rows.getString("bdName"));
        building.setCollegeUuid(rows.getString("bdCollegeUuid"));
        return building;
    }
}
