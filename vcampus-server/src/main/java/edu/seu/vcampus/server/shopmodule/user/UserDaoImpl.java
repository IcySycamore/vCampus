package edu.seu.vcampus.server.shopmodule.user;

import edu.seu.vcampus.common.user.HumanInfo;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.User;
import edu.seu.vcampus.server.db.DbHelper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户数据访问接口实现类.
 */
public class UserDaoImpl implements UserDao {

    @Override
    public User findByUserId(String userId) {
        String sql = "SELECT uUuid, uId, uName, uPwd, uRole FROM tbluser WHERE uId = ?";
        try (Connection conn = DbHelper.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractUserFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<User> findAll() {
        List<User> userList = new ArrayList<>();
        String sql = "SELECT uUuid, uId, uName, uPwd, uRole FROM tbluser";
        try (Connection conn = DbHelper.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                userList.add(extractUserFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return userList;
    }

    @Override
    public boolean addUser(User user) {
        String sql = "INSERT INTO tbluser (uUuid, uId, uName, uPwd, uRole) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DbHelper.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getUuid());
            stmt.setString(2, user.getUserName());
            stmt.setString(3, user.getUserName());
            stmt.setString(4, user.getPassword());
            stmt.setString(5, user.getRole() != null ? user.getRole().name() : Role.STUDENT.name());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public HumanInfo findHumanInfoByUserUuid(String userUuid) {
        // HumanInfo 功能已废弃，返回 null
        return null;
    }

    private User extractUserFromResultSet(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUuid(rs.getString("uUuid"));
        user.setUserName(rs.getString("uId"));
        user.setPassword(rs.getString("uPwd"));
        String roleStr = rs.getString("uRole");
        if (roleStr != null) {
            try {
                user.setRole(Role.valueOf(roleStr));
            } catch (IllegalArgumentException e) {
                user.setRole(Role.STUDENT);
            }
        }
        return user;
    }
}
