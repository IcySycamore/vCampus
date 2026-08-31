package edu.seu.vcampus.dao;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DbHelper {
    private static String url;
    private static String user;
    private static String password;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("com.mysql.jdbc.Driver");
            } catch (ClassNotFoundException ex) {
                System.err.println("错误：未找到 MySQL 数据库驱动！");
            }
        }

        try (InputStream in = DbHelper.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                System.err.println("错误：未找到 db.properties 配置文件！");
            } else {
                Properties props = new Properties();
                props.load(in);
                url = props.getProperty("db.url");
                user = props.getProperty("db.user");
                password = props.getProperty("db.password");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * 打印数据库中的所有表名
     */
    public static void printAllTables() {
        String sql = "SHOW TABLES";
        System.out.println("\n========== 当前 vcampus 数据库中的所有表 ==========");
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            boolean hasTable = false;
            while (rs.next()) {
                hasTable = true;
                String tableName = rs.getString(1);
                System.out.println(" 📄 " + tableName);
            }

            if (!hasTable) {
                System.out.println("（数据库中没有任何表，请先导入 SQL 脚本建表）");
            }
            System.out.println("==================================================\n");

        } catch (SQLException e) {
            System.err.println("查询表清单失败：");
            e.printStackTrace();
        }
    }

    /**
     * 查看指定表的数据记录
     */
    public static void printTableData(String tableName) {
        String sql = "SELECT * FROM " + tableName;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            System.out.println("========== 数据表: " + tableName + " 内容 ==========");

            for (int i = 1; i <= columnCount; i++) {
                System.out.print(metaData.getColumnName(i) + "\t\t");
            }
            System.out.println("\n------------------------------------------------");

            boolean hasData = false;
            while (rs.next()) {
                hasData = true;
                for (int i = 1; i <= columnCount; i++) {
                    System.out.print(rs.getString(i) + "\t\t");
                }
                System.out.println();
            }

            if (!hasData) {
                System.out.println("（该表中暂无数据）");
            }
            System.out.println("================================================\n");

        } catch (SQLException e) {
            System.err.println("查询表数据失败，请检查表名 [" + tableName + "] 是否正确。");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println("正在尝试连接数据库...");
        try (Connection conn = DbHelper.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("恭喜！vcampus-server 成功连接到 MySQL 数据库！");

                // 1. 打印当前数据库所有的表
                printAllTables();

                // 2. 打印 tbluser 表里的具体数据记录
                printTableData("tbluser");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}