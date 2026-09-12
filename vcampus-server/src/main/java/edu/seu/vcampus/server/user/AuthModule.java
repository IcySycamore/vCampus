package edu.seu.vcampus.server.user;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.server.network.ServerMessageDispatcher;

import java.io.File;
import java.io.IOException;

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

    /** 当前装配的账户库；供其它模块做 uuid → 姓名 的联查（如学籍列表）。 */
    private static volatile UserRepository s_repository;

    /** 私有构造器，禁止实例化装配入口。 */
    private AuthModule() {
    }

    /**
     * 取当前装配的账户库。
     *
     * <p>
     * 业务模块（如学籍）只存 uuid，要在列表里显示姓名就得反查账户。这里把账户库暴露出去，
     * 免得各模块各造一个仓储实例、拿到的却是另一份数据（内存库单例与文件库并非同一个）。
     *
     * @return 账户库；尚未装配时返回 null
     */
    public static UserRepository repository() {
        return s_repository;
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
        return register(dispatcher, null);
    }

    /**
     * 登记用户管理全部命令，并接入开户钩子（注册成功后为账号建立各模块 1:1 档案）。
     *
     * <p>
     * 空库时「注册需要管理员会话」会形成引导死锁，故此处幂等预置演示账号 （学生 001/1、管理员 003/1）；预置账号同样走开户流程，因此学生 001
     * 会有学籍档案。
     *
     * @param dispatcher 应用共享的消息分发器
     * @param provisioning 开户钩子登记表；null 表示不建立业务档案
     * @return 全服唯一的会话管理器
     * @throws IllegalArgumentException 分发器为 null
     */
    public static SessionManager register(ServerMessageDispatcher dispatcher,
            AccountProvisioning provisioning) {
        if (dispatcher == null) {
            throw new IllegalArgumentException("dispatcher must not be null");
        }
        AuthService auth = AuthService.getInstance();
        seedDemoAccounts(auth);
        return bind(dispatcher, provisioning, auth);
    }

    /**
     * 生产装配：账户库落地本地文件，初始管理员由引导文件导入。
     *
     * <p>
     * 这是服务器入口应调用的方法：账号重启后仍在（{@link FileUserRepository}），
     * 管理员口令改引导文件即可（{@link AdminAccountBootstrap}）。
     *
     * @param dispatcher 应用共享的消息分发器
     * @param provisioning 开户钩子登记表；null 表示不建立业务档案
     * @param usersFile 账户文件
     * @param adminsFile 管理员引导文件
     * @return 全服唯一的会话管理器
     * @throws IOException 账户文件初始化失败
     */
    public static SessionManager bootstrap(ServerMessageDispatcher dispatcher,
            AccountProvisioning provisioning, File usersFile, File adminsFile) throws IOException {
        AuthService auth = new AuthService(new FileUserRepository(usersFile),
                NonceManager.getInstance(), SessionManager.getInstance());
        AdminAccountBootstrap.seed(auth, adminsFile);
        return bind(dispatcher, provisioning, auth);
    }

    /**
     * 生产装配：账户库（文件版）与初始管理员由调用方提供。
     *
     * @param dispatcher 应用共享的消息分发器
     * @param provisioning 开户钩子登记表；null 表示不建立业务档案
     * @param auth 认证服务（其账户库与用户管理服务共用同一份）
     * @return 全服唯一的会话管理器
     * @throws IllegalArgumentException 参数为 null
     */
    public static SessionManager bind(ServerMessageDispatcher dispatcher,
            AccountProvisioning provisioning, AuthService auth) {
        if (dispatcher == null || auth == null) {
            throw new IllegalArgumentException("dispatcher and auth must not be null");
        }
        auth.setProvisioning(provisioning);
        s_repository = auth.repository();
        AuthServiceHandler handler = new AuthServiceHandler(auth,
                new UserAdminService(auth.repository(), provisioning));
        dispatcher.register(Command.USER_LOGIN, handler);
        dispatcher.register(Command.USER_LOGIN_VERIFY, handler);
        dispatcher.register(Command.USER_REGISTER, handler);
        dispatcher.register(Command.USER_LOGOUT, handler);
        dispatcher.register(Command.USER_UNREGISTER, handler);
        dispatcher.register(Command.USER_LIST, handler);
        dispatcher.register(Command.USER_UPDATE, handler);
        dispatcher.register(Command.USER_TOGGLE_ENABLED, handler);
        dispatcher.register(Command.USER_CHANGE_PASSWORD, handler);
        dispatcher.register(Command.USER_BATCH_REGISTER, handler);
        dispatcher.register(Command.USER_BATCH_UNREGISTER, handler);
        return auth.getSessionManager();
    }

    private static void seedDemoAccounts(AuthService auth) {
        seedDemoAccount(auth, DEMO_STUDENT, "学生");
        seedDemoAccount(auth, DEMO_ADMIN, "管理员");
    }

    private static void seedDemoAccount(AuthService auth, String name, String role) {
        try {
            auth.register(name, DEMO_PASSWORD, role);
        } catch (IllegalStateException e) {
            // 账号已存在（重复启动或多次装配），忽略
        }
    }
}
