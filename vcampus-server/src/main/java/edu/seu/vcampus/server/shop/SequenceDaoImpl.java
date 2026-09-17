package edu.seu.vcampus.server.shop;

import edu.seu.vcampus.server.db.DbHelper;
import edu.seu.vcampus.server.db.DatabaseAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 基于表的持久化全局序列实现。
 */
public class SequenceDaoImpl implements SequenceDao {

    @Override
    public boolean initialize(String name, int startValue) {
        String sql = "INSERT INTO tblGlobalSequence (gsName, gsValue) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE gsValue = VALUES(gsValue)";
        try (Connection conn = DbHelper.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setInt(2, startValue);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseAccessException("初始化序列失败: " + name, e);
        }
    }

    @Override
    public int currentValue(String name) {
        String sql = "SELECT gsValue FROM tblGlobalSequence WHERE gsName = ?";
        try (Connection conn = DbHelper.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("gsValue");
                }
            }
        } catch (SQLException e) {
            throw new DatabaseAccessException("读取序列失败: " + name, e);
        }
        return -1;
    }

    @Override
    public int nextValue(String name) {
        String sql = "SELECT gsValue FROM tblGlobalSequence WHERE gsName = ? FOR UPDATE";
        try (Connection conn = DbHelper.getConnection();
                PreparedStatement select = conn.prepareStatement(sql)) {
            select.setString(1, name);
            try (ResultSet rs = select.executeQuery()) {
                if (rs.next()) {
                    int current = rs.getInt("gsValue");
                    int next = current + 1;
                    String updateSql = "UPDATE tblGlobalSequence SET gsValue = ? WHERE gsName = ?";
                    try (PreparedStatement update = conn.prepareStatement(updateSql)) {
                        update.setInt(1, next);
                        update.setString(2, name);
                        update.executeUpdate();
                    }
                    return current;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseAccessException("递增序列失败: " + name, e);
        }
        return -1;
    }
}
