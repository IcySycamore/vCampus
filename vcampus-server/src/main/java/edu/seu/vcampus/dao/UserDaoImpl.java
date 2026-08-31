package edu.seu.vcampus.dao;

import edu.seu.vcampus.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDaoImpl implements UserDao {

    @Override
    public User findByUsername(String uId) {
        // 使用实际字段 uId 查询
        String sql = "SELECT * FROM tbluser WHERE uId = ?";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, uId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                        rs.getString("uId"),
                        rs.getString("uName"),
                        rs.getInt("uAge"),
                        rs.getString("uSex"),
                        rs.getString("uPwd"),
                        rs.getString("uRole")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean addUser(User user) {
        String sql = "INSERT INTO tbluser(uId, uName, uAge, uSex, uPwd, uRole) VALUES(?, ?, ?, ?, ?, ?)";
        try (Connection conn = DbHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, user.getuId());
            pstmt.setString(2, user.getuName());
            pstmt.setInt(3, user.getuAge());
            pstmt.setString(4, user.getuSex());
            pstmt.setString(5, user.getuPwd());
            pstmt.setString(6, user.getuRole());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM tbluser";
        try (Connection conn = DbHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                list.add(new User(
                    rs.getString("uId"),
                    rs.getString("uName"),
                    rs.getInt("uAge"),
                    rs.getString("uSex"),
                    rs.getString("uPwd"),
                    rs.getString("uRole")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}