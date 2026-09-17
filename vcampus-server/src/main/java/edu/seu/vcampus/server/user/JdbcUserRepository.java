package edu.seu.vcampus.server.user;

import edu.seu.vcampus.server.db.DatabaseAccessException;
import edu.seu.vcampus.server.db.DbHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户凭证存储：落表 {@code tblUserCredential}。

 *
 * <p>
 * 表结构对应 {@code sql/vCampus.sql} 的 {@code tblUserCredential} 加上 {@code sql/vCampus-extend.sql}
 * 补的两列：{@code ucName}（姓名快照）、{@code ucEnabled}（启用位）。 存储的始终是 {@code sha256(salt + 口令)}，<b>不落明文口令</b>。
 *
 * <p>
 * 异常：接口方法未声明受检异常，故此处把 {@link SQLException} 统一包装成 {@link DatabaseAccessException} 抛出。
 */
public class JdbcUserRepository implements UserRepository {

    /** 查询列清单。 */
    private static final String COLUMNS = "ucUsername, ucUuid, ucName, ucSalt, ucHash, ucRole, ucEnabled";

    @Override
    public void save(String username, String uuid, String salt, String hash, String role) {
        save(new Credential(username, uuid, username, salt, hash, role, true));
    }

    @Override
    public void save(Credential credential) {
        String sql = "INSERT INTO tblUserCredential"
                + " (ucUsername, ucUuid, ucName, ucSalt, ucHash, ucRole, ucEnabled)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE ucUuid = VALUES(ucUuid), ucName = VALUES(ucName),"
                + " ucSalt = VALUES(ucSalt), ucHash = VALUES(ucHash),"
                + " ucRole = VALUES(ucRole), ucEnabled = VALUES(ucEnabled)";
        execute(sql, credential.getUsername(), credential.getUuid(), credential.getDisplayName(),
                credential.getSalt(), credential.getHash(), credential.getRole(),
                credential.isEnabled() ? Integer.valueOf(1) : Integer.valueOf(0));
    }

    @Override
    public Credential findByUsername(String username) {
        return findOne("SELECT " + COLUMNS + " FROM tblUserCredential WHERE ucUsername = ?",
                username);
    }

    @Override
    public Credential findByUuid(String uuid) {
        return findOne("SELECT " + COLUMNS + " FROM tblUserCredential WHERE ucUuid = ?", uuid);
    }

    @Override
    public boolean exists(String username) {
        return findByUsername(username) != null;
    }

    @Override
    public List<Credential> findAll() {
        List<Credential> found = new ArrayList<Credential>();
        String sql = "SELECT " + COLUMNS + " FROM tblUserCredential ORDER BY ucUsername";
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(sql);
            rows = statement.executeQuery();
            while (rows.next()) {
                found.add(toCredential(rows));
            }
            return found;
        } catch (SQLException e) {
            throw new DatabaseAccessException("查询全部用户凭证失败", e);
        } finally {
            close(rows, statement, connection);
        }
    }

    @Override
    public void update(String username, String displayName) {
        execute("UPDATE tblUserCredential SET ucName = ? WHERE ucUsername = ?", displayName,
                username);
    }

    @Override
    public void setEnabled(String username, boolean enabled) {
        execute("UPDATE tblUserCredential SET ucEnabled = ? WHERE ucUsername = ?",
                enabled ? Integer.valueOf(1) : Integer.valueOf(0), username);
    }

    @Override
    public void updateCredential(String username, String salt, String hash) {
        execute("UPDATE tblUserCredential SET ucSalt = ?, ucHash = ? WHERE ucUsername = ?", salt,
                hash, username);
    }

    @Override
    public void delete(String username) {
        int affected = execute("DELETE FROM tblUserCredential WHERE ucUsername = ?", username);
        if (affected == 0) {
            System.out.println("[JdbcUserRepository] 删除未命中行: " + username);
        }
    }

    /**
     * 查一条凭证。
     *
     * @param sql   查询语句（含一个占位符）
     * @param param 绑定的查询值
     * @return 凭证；无记录返回 null
     */
    private Credential findOne(String sql, String param) {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet rows = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(sql);
            statement.setString(1, param);
            rows = statement.executeQuery();
            return rows.next() ? toCredential(rows) : null;
        } catch (SQLException e) {
            throw new DatabaseAccessException("查询用户凭证失败: " + param, e);
        } finally {
            close(rows, statement, connection);
        }
    }

    /**
     * 结果行 → 凭证。
     *
     * @param rows 已定位到某行的结果集
     * @return 凭证对象；姓名为空时回落登录名
     * @throws SQLException 读取失败
     */
    private Credential toCredential(ResultSet rows) throws SQLException {
        String username = rows.getString("ucUsername");
        String name = rows.getString("ucName");
        return new Credential(username, rows.getString("ucUuid"),
                name == null || name.length() == 0 ? username : name, rows.getString("ucSalt"),
                rows.getString("ucHash"), rows.getString("ucRole"), rows.getBoolean("ucEnabled"));
    }

    /**
     * 执行一条更新语句。
     *
     * @param sql    语句
     * @param params 按占位符顺序的绑定参数
     * @return 受影响行数
     */
    private int execute(String sql, Object... params) {
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            connection = DbHelper.getConnection();
            statement = connection.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseAccessException("更新用户凭证失败", e);
        } finally {
            close(null, statement, connection);
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
