package edu.seu.vcampus.server.library;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

/**
 * 内存版的连接来源：给出一条可提交、可回滚的占位连接。
 *
 * <p>
 * 内存 DAO 不看连接内容，但上层的事务代码需要一条能 {@code setAutoCommit} / {@code commit} / {@code rollback}
 * 的对象才不会崩，于是用动态代理提供。语义是「事务被接受，但不产生效果」—— 内存状态下本来就没有跨表原子性可言。
 *
 * <p>
 * <b>这个实现只适用于内存后端。</b> jdbc 模式必须注入 {@link LibraryConnectionSourceJdbc}， 否则上层的事务会落在假连接上、而下层 DAO 各写各的
 * —— 那正是迁移前遗留的接缝错位。
 */
public final class LibraryConnectionSourceMemory implements LibraryConnectionSource {

    @Override
    public Connection getConnection() {
        return connection();
    }

    private Connection connection() {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[] { Connection.class }, new MemoryConnection());
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
