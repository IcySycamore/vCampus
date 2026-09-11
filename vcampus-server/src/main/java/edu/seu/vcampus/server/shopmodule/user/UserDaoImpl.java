package edu.seu.vcampus.server.shopmodule.user;

import edu.seu.vcampus.common.user.Admin;
import edu.seu.vcampus.common.user.HumanInfo;
import edu.seu.vcampus.common.user.Role;
import edu.seu.vcampus.common.user.Student;
import edu.seu.vcampus.common.user.Teacher;
import edu.seu.vcampus.common.user.User;
import edu.seu.vcampus.server.db.DbHelper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 用户数据访问接口实现类.
 */
public class UserDaoImpl implements UserDao {

    @Override
    public User findByUserId(String userId) {
        String sql = "SELECT * FROM tbluser WHERE uId = ?";
        try (Connection conn = DbHelper.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = extractUserFromResultSet(rs);
                    HumanInfo humanInfo = findHumanInfoByUserUuid(user.getUuid().toString());
                    if (humanInfo != null) {
                        user.setHumanInfo(humanInfo);
                    }
                    return user;
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
        String sql = "SELECT uId, uName, uAge, uSex, uPwd, uRole "
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
        HumanInfo info = user.getHumanInfo();
        if (info == null) {
            return false;
        }
        String userUuid = user.getUuid() == null
            ? UUID.randomUUID().toString() : user.getUuid().toString();
        String humanUuid = info.getUuid() == null
            ? UUID.randomUUID().toString() : info.getUuid().toString();
        user.setUuid(UUID.fromString(userUuid));
        info.setUuid(UUID.fromString(humanUuid));
        String userSql = "INSERT INTO tbluser (uUuid, uId, uName, uAge, uSex, uPwd, uRole) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        String humanSql = "INSERT INTO tblHumanInfo (hiUuid, hiId, hiName, hiTel, "
            + "hiHomeAddress, hiWorkAddress, hiAge, hiGender, hiDepartment, hiMajor, hiTitle) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String mappingSql = "INSERT INTO tblUserHumanInfo (uUuid, hiUuid) VALUES (?, ?)";
        try (Connection conn = DbHelper.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(userSql)) {
            stmt.setString(1, userUuid);
                stmt.setString(2, info.getId());
                stmt.setString(3, info.getName());
                stmt.setInt(4, info.getAge());
                stmt.setString(5, toSex(info.getGender()));
                stmt.setString(6, user.getPassword());
                stmt.setString(7, toRole(user));
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(humanSql)) {
                stmt.setString(1, humanUuid);
                stmt.setString(2, info.getId());
                stmt.setString(3, info.getName());
                stmt.setString(4, info.getTel());
                stmt.setString(5, info.getHomeAddress());
                stmt.setString(6, info.getWorkAddress());
                stmt.setInt(7, info.getAge());
                stmt.setString(8, info.getGender() == null ? null : info.getGender().name());
                stmt.setString(9, info.getDepartment() == null ? null : info.getDepartment().name());
                stmt.setString(10, info.getMajor() == null ? null : info.getMajor().name());
                stmt.setString(11, info.getTitle() == null ? null : info.getTitle().name());
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(mappingSql)) {
                stmt.setString(1, userUuid);
                stmt.setString(2, humanUuid);
                stmt.executeUpdate();
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public HumanInfo findHumanInfoByUserUuid(String userUuid) {
        String sql = "SELECT h.* FROM tblHumanInfo h "
                + "JOIN tblUserHumanInfo m ON m.hiUuid = h.hiUuid "
                + "WHERE m.uUuid = ?";
        try (Connection conn = DbHelper.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userUuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractHumanInfo(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private User extractUserFromResultSet(ResultSet rs) throws SQLException {
        String id = rs.getString("uId");
        HumanInfo info = new HumanInfo(id, rs.getString("uName"), null, null, null,
                rs.getInt("uAge"), toGender(rs.getString("uSex")));
        String uuid = rs.getString("uUuid");
        User user = createUser(rs.getString("uRole"), info, rs.getString("uPwd"));
        if (uuid != null) {
            user.setUuid(UUID.fromString(uuid));
        }
        return user;
    }

    private HumanInfo extractHumanInfo(ResultSet rs) throws SQLException {
        HumanInfo info = new HumanInfo(rs.getString("hiId"), rs.getString("hiName"),
                rs.getString("hiTel"), rs.getString("hiHomeAddress"),
                rs.getString("hiWorkAddress"), rs.getInt("hiAge"),
                toGender(rs.getString("hiGender")));
        info.setUuid(UUID.fromString(rs.getString("hiUuid")));
        info.setDepartment(enumValue(edu.seu.vcampus.common.user.Department.class,
                rs.getString("hiDepartment")));
        info.setMajor(enumValue(edu.seu.vcampus.common.user.Major.class,
                rs.getString("hiMajor")));
        info.setTitle(enumValue(edu.seu.vcampus.common.user.Title.class,
                rs.getString("hiTitle")));
        return info;
    }

    private <T extends Enum<T>> T enumValue(Class<T> type, String value) {
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String toSex(HumanInfo.Gender gender) {
        if (gender == HumanInfo.Gender.MALE) {
            return "男";
        }
        if (gender == HumanInfo.Gender.FEMALE) {
            return "女";
        }
        return "其他";
    }

    private String toRole(User user) {
        if (user instanceof Admin) {
            return Role.ADMIN.getDisplayName();
        }
        if (user instanceof Teacher) {
            return Role.TEACHER.getDisplayName();
        }
        return Role.STUDENT.getDisplayName();
    }

    private User createUser(String roleName, HumanInfo info, String password) {
        Role role = Role.fromDisplayName(roleName);
        if (role == Role.TEACHER) {
            return new Teacher(info, info.getId(), password);
        }
        if (role == Role.ADMIN) {
            return new Admin(info, info.getId(), password, true);
        }
        return new Student(info, info.getId(), password);
    }

    private HumanInfo.Gender toGender(String sex) {
        if ("男".equals(sex)) {
            return HumanInfo.Gender.MALE;
        }
        if ("女".equals(sex)) {
            return HumanInfo.Gender.FEMALE;
        }
        return HumanInfo.Gender.OTHER;
    }
}