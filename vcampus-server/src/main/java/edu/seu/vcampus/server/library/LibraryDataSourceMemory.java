package edu.seu.vcampus.server.library;

import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.DataSource;

/** 内存 DAO 占位阶段使用的无操作事务连接。 */
public final class LibraryDataSourceMemory implements DataSource {
    private PrintWriter m_log_writer;
    private int m_login_timeout;

    @Override
    public Connection getConnection() {
        return connection();
    }

    @Override
    public Connection getConnection(String username, String password) {
        return connection();
    }

    @Override
    public PrintWriter getLogWriter() {
        return m_log_writer;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        m_log_writer = out;
    }

    @Override
    public void setLoginTimeout(int seconds) {
        m_login_timeout = seconds;
    }

    @Override
    public int getLoginTimeout() {
        return m_login_timeout;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("memory data source has no parent logger");
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface != null && iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw new SQLException("not a wrapper for " + iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return iface != null && iface.isInstance(this);
    }

    private Connection connection() {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[] {Connection.class}, new MemoryConnection());
    }

    private static final class MemoryConnection implements InvocationHandler {
        private boolean m_auto_commit = true;
        private boolean m_closed;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if ("close".equals(name)) {
                m_closed = true;
                return null;
            }
            if ("isClosed".equals(name)) {
                return m_closed;
            }
            if ("setAutoCommit".equals(name)) {
                m_auto_commit = ((Boolean) args[0]).booleanValue();
                return null;
            }
            if ("getAutoCommit".equals(name)) {
                return m_auto_commit;
            }
            if ("commit".equals(name) || "rollback".equals(name)) {
                return null;
            }
            if ("isValid".equals(name)) {
                return !m_closed;
            }
            if ("isWrapperFor".equals(name)) {
                return args[0] != null && ((Class<?>) args[0]).isInstance(proxy);
            }
            if ("unwrap".equals(name) && args[0] != null
                    && ((Class<?>) args[0]).isInstance(proxy)) {
                return proxy;
            }
            if ("toString".equals(name)) {
                return "LibraryMemoryConnection";
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(name)) {
                return proxy == args[0];
            }
            throw new SQLFeatureNotSupportedException("unsupported connection method: " + name);
        }
    }
}
