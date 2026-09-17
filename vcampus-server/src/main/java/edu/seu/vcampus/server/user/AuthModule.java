package edu.seu.vcampus.server.user;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.server.network.ServerMessageDispatcher;

import java.io.File;
import java.io.IOException;

/**
 * 用户管理模块装配入口。
 *
 * <p>
 * 本模块处在依赖拓扑的最底层（不依赖任何业务模块），因此由它负责生产装配并把 {@link #repository()} / {@link #authService()} /
 * {@link #sessions()} 三个单例交出去： 学籍、选课、银行都只存 uuid，要反查姓名或做鉴权时取的就是这三个实例。自建一份仓储会拿到
 * 另一份数据，症状是「列表里姓名全是空」或「刚登录就 401」。
 *
 * <p>
 * <b>账号只有一个来源</b>：启动时由 {@link AdminAccountBootstrap} 从 {@code data/admins.tsv}
 * 导入，其余账号一律由管理员在界面上建。代码里不再预置任何演示账号 —— 预置账号曾经把 「001 / 密码 1」这类测试用身份写进真库，既与管理员自建的账号撞名，也让「系统里默认有哪些人」
 * 变成一件只有读代码才知道的事。
 */
public final class AuthModule {

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
     * 业务模块（如学籍）只存 uuid，要在列表里显示姓名就得反查账户。这里把账户库暴露出去， 免得各模块各造一个仓储实例、拿到的却是另一份数据。
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
     * 连接线程做连接级鉴权、业务处理器做命令级鉴权、银行解析调用者身份，都必须用这一张表： 登录时签发的 token 落在别处，业务侧就校验不到，表现为「刚登录就 401」。
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
     * 生产装配：账户库落地 MySQL，随后由引导文件导入初始管理员并登记全部命令。
     *
     * <p>
     * 这是服务器入口唯一应调用的装配方法（另一个入口 {@link #bind} 只给测试注入替身用）。管理 员口令改 {@code data/admins.tsv}
     * 即可（{@link AdminAccountBootstrap}）。
     *
     * <p>
     * 不再提供文件版回退：缺库属于配置错误，取连接时就该失败，而不是静默换一条 「重启即失」的路径。
     *
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
     * 装配：账户库与初始管理员由调用方提供（测试注入替身走这条）。
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
}
