package edu.seu.vcampus.server.db;

/**
 * 数据库访问失败异常。
 */
public class DatabaseAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DatabaseAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
