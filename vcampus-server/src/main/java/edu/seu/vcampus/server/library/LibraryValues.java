package edu.seu.vcampus.server.library;

import edu.seu.vcampus.common.library.LibraryPolicy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;

/** 图书馆服务共享的值规范化、日期和事务工具。 */
final class LibraryValues {
    private LibraryValues() {
    }

    static void requireDependencies(String owner, Object... dependencies) {
        for (Object dependency : dependencies) {
            if (dependency == null) {
                throw new IllegalArgumentException(owner + " dependencies must not be null");
            }
        }
    }

    static String text(String value, String label) {
        if (value == null || value.trim().length() == 0) {
            throw new IllegalArgumentException(label + "不能为空");
        }
        return value.trim();
    }

    static Date addDays(Date value, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(value);
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTime();
    }

    static Date dueDate(Date borrowedAt) {
        return addDays(borrowedAt, LibraryPolicy.LOAN_DAYS);
    }

    static void rollback(Connection connection, Exception cause) throws SQLException {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            cause.addSuppressed(rollbackFailure);
        }
    }
}
