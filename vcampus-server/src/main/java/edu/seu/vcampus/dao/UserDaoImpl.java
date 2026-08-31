package edu.seu.vcampus.dao;

import edu.seu.vcampus.model.User;
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
    public User findByUsername(String username) {
        String sql = "SELECT uId, uName, uAge, uSex, uPwd, salt, uRole "
                + "FROM tbluser WHERE uId = ?";
        try (Connection conn = DbHelper.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
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
        String sql = "SELECT uId, uName, uAge, uSex, uPwd, salt, uRole "
                + "FROM tbluser";
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
        String sql = "INSERT INTO tbluser (uId, uName, uAge, uSex, uPwd, salt, uRole) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DbHelper.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getuId());
            stmt.setString(2, user.getuName());
            stmt.setInt(3, user.getuAge());
            stmt.setString(4, user.getuSex());
            stmt.setString(5, user.getuPwd());
            stmt.setString(6, user.getSalt());
            stmt.setString(7, user.getuRole());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private User extractUserFromResultSet(ResultSet rs) throws SQLException {
        User user = new User();
        user.setuId(rs.getString("uId"));
        user.setuName(rs.getString("uName"));
        user.setuAge(rs.getInt("uAge"));
        user.setuSex(rs.getString("uSex"));
        user.setuPwd(rs.getString("uPwd"));
        user.setSalt(rs.getString("salt"));
        user.setuRole(rs.getString("uRole"));
        return user;
    }
}