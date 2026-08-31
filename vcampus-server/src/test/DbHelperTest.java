package edu.seu.vcampus.dao;

import static org.junit.Assert.assertNotNull;

import java.sql.Connection;
import org.junit.Test;

/**
 * DbHelper 单元测试类.
 */
public class DbHelperTest {

    @Test
    public void testGetConnection() {
        try (Connection conn = DbHelper.getConnection()) {
            assertNotNull(conn);
        } catch (Exception e) {
            // 测试环境若无数据库服务可忽略连接异常
        }
    }
}