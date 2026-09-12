package edu.seu.vcampus.server.user;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.server.network.ServerMessageDispatcher;

/**
 * 用户管理模块装配入口：登记认证命令并预置演示账号。
 *
 * <p>
 * 模块自带单例装配（{@link AuthService#getInstance()} 及其依赖），应用组装层只需调用
 * {@link #register(ServerMessageDispatcher)}，不必了解模块内部的构造方式。
 */
public final class AuthModule {

    /** 演示学生账号。 */
    private static final String DEMO_STUDENT = "001";

    /** 演示管理员账号。 */
    private static final String DEMO_ADMIN = "003";

    /** 演示账号初始密码。 */
    private static final String DEMO_PASSWORD = "1";

    /** 私有构造器，禁止实例化装配入口。 */
    private AuthModule() {
    }

    /**
     * 登记用户管理四条命令，并返回全服唯一的会话表供连接线程与其它模块鉴权复用。
     *
     * <p>
     * 空库时「注册需要管理员会话」会形成引导死锁，故此处幂等预置演示账号 （学生 001/1、管理员 003/1）；接入数据库初始化脚本后即可移除预置。
     *
     * @param dispatcher 应用共享的消息分发器
     * @return 全服唯一的会话管理器
     * @throws IllegalArgumentException 分发器为 null
     */
    public static SessionManager register(ServerMessageDispatcher dispatcher) {
        if (dispatcher == null) {
            throw new IllegalArgumentException("dispatcher must not be null");
        }
        AuthService auth = AuthService.getInstance();
        AuthServiceHandler handler = new AuthServiceHandler(auth);
        dispatcher.register(Command.USER_LOGIN, handler);
        dispatcher.register(Command.USER_LOGIN_VERIFY, handler);
        dispatcher.register(Command.USER_REGISTER, handler);
        dispatcher.register(Command.USER_LOGOUT, handler);
        dispatcher.register(Command.USER_PROFILE_QUERY, handler);
        seedDemoAccounts(auth);
        return auth.getSessionManager();
    }

    private static void seedDemoAccounts(AuthService auth) {
        seedDemoAccount(auth, DEMO_STUDENT, "学生", "演示学生");
        // 管理员不采集姓名：它没有人员档案，也不需要显示真实姓名
        seedDemoAccount(auth, DEMO_ADMIN, "管理员", null);
    }

    private static void seedDemoAccount(AuthService auth, String name, String role,
            String realName) {
        try {
            auth.register(name, DEMO_PASSWORD, role, realName);
        } catch (IllegalStateException e) {
            // 账号已存在（重复启动或多次装配），忽略
        }
    }
}
