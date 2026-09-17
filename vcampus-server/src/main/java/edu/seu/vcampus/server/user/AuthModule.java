package edu.seu.vcampus.server.user;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.server.network.ServerMessageDispatcher;

import java.io.File;
import java.io.IOException;

/**
 * 用户管理模块装配入口：登记认证命令并预置演示账号。
 *
 * <p>
 * 本模块处在依赖拓扑的最底层，因此由它负责生产装配并把
 * {@link #repository()} / {@link #authService()} / {@link #sessions()} 三个单例交出去：
 */
public final class AuthModule {

    /** 演示学生账号。 */
    private static final String DEMO_STUDENT = "001";

    /** 演示学生姓名。 */
    private static final String DEMO_STUDENT_NAME = "演示学生";

    /** 演示教师账号（验「教师也有信息查看需求」用）。 */
    private static final String DEMO_TEACHER = "002";

    /** 演示教师姓名。 */
    private static final String DEMO_TEACHER_NAME = "演示教师";

    /** 演示管理员账号。 */
    private static final String DEMO_ADMIN = "003";

    /** 演示管理员姓名。 */
    private static final String DEMO_ADMIN_NAME = "系统管理员";

    /** 演示账号初始密码。 */
    private static final String DEMO_PASSWORD = "1";

    /** 管理员引导文件路径的系统属性名。 */
    public static final String ADMINS_FILE_PROPERTY = "vcampus.admins.file";

    /** 当前装配的账户库；供其它模块做 uuid → 姓名 的联查（如学籍列表）。 */
    private static volatile UserRepository s_repository;

    /** 当前装配的认证服务，供需要独立密码复核的业务模块复用同一账户库。 */
    private static volatile AuthService s_auth;

    /** 私有构造器，禁止实例化装配入口。 */
    private AuthModule() {
    }

    /**
     * 取当前装配的账户库。
     *
     * <p>
     * 业务模块（如学籍）只存 uuid，要在列表里显示姓名就得反查账户。这里把账户库暴露出去， 免得各模块各造一个仓储实例、拿到的却是另一份数据（内存库单例与文件库并非同一个）。
     *
     * @return 账户库；尚未装配时返回 null
     */
    public static UserRepository repository() {
        return s_repository;
    }

    /**
     * 取当前装配的认证服务。
     *
     * @return 认证服务；尚未装配时返回 null
     */
    public static AuthService authService() {
        return s_auth;
    }

    /**
     * 取全服唯一的会话表。
     *
     * <p>
     * 连接线程做连接级鉴权、业务处理器做命令级鉴权、银行解析调用者身份，都必须用这一张表：
     * 登录时签发的 token 落在别处，业务侧就校验不到，表现为「刚登录就 401」。
     *
     * @return 会话管理器
     * @throws IllegalStateException 尚未装配
     */
    public static SessionManager sessions() {
        AuthService auth = s_auth;
        if (auth == null) {
            throw new IllegalStateException("认证模块尚未装配，请先调用 AuthModule.initialize");
        }
        return auth.getSessionManager();
    }

    /**
     * 登记用户管理四条命令，并返回全服唯一的会话表供连接线程与其它模块鉴权复用。
     *
     * <p>
     * 空库时「注册需要管理员会话」会形成引导死锁，故此处幂等预置演示账号；接入数据库初始化脚本后即可移除预置。
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
     * 空库时「注册需要管理员会话」会形成引导死锁，故此处幂等预置演示账号，预置账号同样走开户 流程
     *
     * @param dispatcher   应用共享的消息分发器
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
     * 生产装配：账户库落地 MySQL，随后由引导文件导入初始管理员并登记全部命令。
     *
     * <p>
     * 这是服务器入口唯一应调用的装配方法，管理员口令改 {@code data/admins.tsv} 即可 （{@link AdminAccountBootstrap}）。
     *
     * <p>
     * @param dispatcher   应用共享的消息分发器
     * @param provisioning 开户钩子登记表；null 表示不建立业务档案
     * @return 全服唯一的会话管理器
     * @throws IOException 引导文件读写失败
     */
    public static SessionManager initialize(ServerMessageDispatcher dispatcher,
            AccountProvisioning provisioning) throws IOException {
        AuthService auth = new AuthService(new JdbcUserRepository(),
                NonceManager.getInstance(), SessionManager.getInstance());
        AdminAccountBootstrap.seed(auth, new File(
                System.getProperty(ADMINS_FILE_PROPERTY, AdminAccountBootstrap.DEFAULT_FILE)));
        return bind(dispatcher, provisioning, auth);
    }

    /**
     * 生产装配：账户库与初始管理员由调用方提供。
     *
     * @param dispatcher   应用共享的消息分发器
     * @param provisioning 开户钩子登记表；null 表示不建立业务档案
     * @param auth         认证服务
     * @return 全服唯一的会话管理器
     * @throws IllegalArgumentException 参数为 null
     */
    public static SessionManager bind(ServerMessageDispatcher dispatcher,
            AccountProvisioning provisioning, AuthService auth) {
        if (dispatcher == null || auth == null) {
            throw new IllegalArgumentException("dispatcher and auth must not be null");
        }
        auth.setProvisioning(provisioning);
        s_auth = auth;
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
        seedDemoAccount(auth, DEMO_STUDENT, DEMO_STUDENT_NAME, "学生");
        seedDemoAccount(auth, DEMO_TEACHER, DEMO_TEACHER_NAME, "教师");
        seedDemoAccount(auth, DEMO_ADMIN, DEMO_ADMIN_NAME, "管理员");
    }

    /**
     * 预置一个演示账号
     *
     * <p>
     * 姓名必须显式传入：只传登录名的重载会把姓名默认成登录名，演示账号登录后就会显示成 001/002/003，看上去像「只显示用户名」。
     *
     * @param auth        认证服务
     * @param name        登录名
     * @param displayName 姓名
     * @param role        角色显示名
     */
    private static void seedDemoAccount(AuthService auth, String name, String displayName,
            String role) {
        try {
            auth.register(name, displayName, DEMO_PASSWORD, role);
        } catch (IllegalStateException e) {
            // 账号已存在，忽略
        }
    }
}
