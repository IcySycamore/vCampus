package edu.seu.vcampus.server;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.server.auth.AuthService;
import edu.seu.vcampus.server.auth.AuthServiceHandler;
import edu.seu.vcampus.server.auth.SessionManager;
import edu.seu.vcampus.server.dispatch.MessageDispatcher;
import edu.seu.vcampus.server.module.student.StudentDaoMemory;
import edu.seu.vcampus.server.module.student.StudentMessageHandler;
import edu.seu.vcampus.server.module.student.StudentService;
import edu.seu.vcampus.server.network.MessageStream;
import edu.seu.vcampus.server.network.ServerSocketListener;
import edu.seu.vcampus.server.thread.ClientThread;
import edu.seu.vcampus.server.thread.ThreadPoolManager;

import java.io.IOException;

/**
 * vCampus 服务器端入口。
 *
 * <p>启动 ServerSocket 监听，循环接受客户端连接；每个连接交给全局线程池，
 * 由 {@link ClientThread} 跑「每客户端一线程」的收发循环（含心跳与连接级鉴权，
 * 见 ADR-0006）。
 *
 * <p>全局只装配一份身份与服务对象：{@link AuthService} 是唯一的认证总入口，
 * 其内部先构造唯一的 {@link SessionManager}；该 token 表同时交给连接线程做连接级
 * 鉴权、交给业务处理器做命令级鉴权。命令分发器同样全局唯一，取自
 * {@link ClientThread#getDispatcher()}，各模块处理器统一登记到它上面。
 *
 * <p>注册 JVM 关机钩子实现优雅关机：收到停机信号（Ctrl+C 等）时先停止监听，
 * 使阻塞中的 accept 退出，从而结束主循环；同时停止线程池接受新任务。
 */
public final class VCampusServerApp {

    /** 当前监听器；由 startServer / stopServer 维护，供集成测试驱动。 */
    private static volatile ServerSocketListener s_listener;

    /** 关机钩子是否已注册（重复启动时只注册一次）。 */
    private static boolean s_hookRegistered;

    /**
     * 私有构造器，禁止实例化入口类。
     */
    private VCampusServerApp() {
    }

    /**
     * 程序入口：以默认端口启动服务器。
     *
     * @param args 命令行参数（暂未使用）
     */
    public static void main(String[] args) {
        try {
            startServer(ServerSocketListener.DEFAULT_PORT);
        } catch (IOException e) {
            System.err.println("服务器启动失败: " + e.getMessage());
        }
    }

    /**
     * 装配全局对象并在指定端口启动服务器，随后阻塞在「接受连接」循环中。
     *
     * <p>装配顺序：全局认证服务（其内部先建唯一的 SessionManager）→ 取用该会话
     * 管理器 → 全局分发器 → 各模块处理器。之后每接受一个连接就交给全局线程池执行
     * {@link ClientThread}，由它跑「每客户端一线程」的收发循环。
     *
     * @param port 监听端口，0 表示由系统分配随机端口
     * @throws IOException 绑定端口失败
     */
    public static void startServer(int port) throws IOException {
        final ServerSocketListener server = new ServerSocketListener();
        s_listener = server;
        registerShutdownHook();

        // 全局唯一装配：先建会话管理器，再建认证服务总入口；
        // 各连接线程与各业务处理器共用这同一份 token 表。
        final AuthService auth = AuthService.getInstance();
        final SessionManager sessions = auth.getSessionManager();
        registerHandlers(ClientThread.getDispatcher(), auth, sessions);

        server.start(port);
        System.out.println("vCampus Server 已启动，监听端口 " + server.getPort());

        try {
            while (server.isRunning()) {
                MessageStream stream;
                try {
                    stream = server.accept();
                } catch (IOException e) {
                    // 单个连接握手失败不应拖垮监听循环：仅关机导致的异常才退出。
                    if (!server.isRunning()) {
                        break;
                    }
                    System.err.println("接受连接失败: " + e.getMessage());
                    continue;
                }
                System.out.println("新客户端连接建立");
                // 每客户端一线程：交给全局线程池执行，连接收尾由 ClientThread 负责。
                ThreadPoolManager.getInstance().execute(
                        new ClientThread(stream, sessions));
            }
        } finally {
            s_listener = null;
        }
    }

    /**
     * 返回实际监听端口，便于集成测试连接。
     *
     * @return 监听端口；未启动时返回 -1
     */
    public static int getPort() {
        final ServerSocketListener server = s_listener;
        return server == null ? -1 : server.getPort();
    }

    /**
     * 停止监听，使阻塞中的 {@link #startServer(int)} 退出循环并返回。
     *
     * @throws IOException 关闭失败
     */
    public static void stopServer() throws IOException {
        final ServerSocketListener server = s_listener;
        if (server != null) {
            server.stop();
        }
    }

    /**
     * 按命令码登记各模块的处理器，统一挂到全局分发器上。
     *
     * @param dispatcher 全局消息分发器
     * @param auth       全局认证服务（总入口）
     * @param sessions   全局会话管理器（与认证服务共用同一实例）
     */
    private static void registerHandlers(MessageDispatcher dispatcher,
            AuthService auth, SessionManager sessions) {
        // 用户模块：100 登录挑战 / 110 登录校验 / 102 注册 / 101 登出
        AuthServiceHandler authHandler = new AuthServiceHandler(auth);
        dispatcher.register(Command.USER_LOGIN, authHandler);
        dispatcher.register(Command.USER_LOGIN_VERIFY, authHandler);
        dispatcher.register(Command.USER_REGISTER, authHandler);
        dispatcher.register(Command.USER_LOGOUT, authHandler);

        // 学籍模块：分发器为单命令码映射，故逐个登记所支持的命令码
        StudentService studentService =
                new StudentService(new StudentDaoMemory());
        StudentMessageHandler studentHandler =
                new StudentMessageHandler(studentService, sessions);
        dispatcher.register(Command.STUDENT_QUERY, studentHandler);
        dispatcher.register(Command.STUDENT_MODIFY_APPLY, studentHandler);
        dispatcher.register(Command.STUDENT_MODIFY_AUDIT, studentHandler);
        dispatcher.register(Command.STUDENT_REGISTER, studentHandler);
        dispatcher.register(Command.STUDENT_DELETE, studentHandler);
        dispatcher.register(Command.STUDENT_CHANGE_STATUS, studentHandler);
    }

    /**
     * 注册优雅关机钩子：JVM 收到停机信号时停止监听，使阻塞中的 accept 退出。
     *
     * <p>线程池仅停止接受新任务（已提交的连接任务自然跑完）。宽限期等待
     * （awaitTermination）待网络组补充。
     */
    private static synchronized void registerShutdownHook() {
        if (s_hookRegistered) {
            return;
        }
        s_hookRegistered = true;
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final ServerSocketListener server = s_listener;
                    if (server != null) {
                        server.stop();
                    }
                    ThreadPoolManager.getInstance().shutdown();
                    System.out.println("vCampus Server 已停止监听，优雅退出");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, "vCampusServer-shutdown"));
    }
}
