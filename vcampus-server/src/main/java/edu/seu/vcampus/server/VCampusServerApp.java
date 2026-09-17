package edu.seu.vcampus.server;

import edu.seu.vcampus.server.db.DbHelper;

import edu.seu.vcampus.common.constant.NetworkConstant;
import edu.seu.vcampus.common.network.MessageStream;
import edu.seu.vcampus.server.bank.BankModule;
import edu.seu.vcampus.server.course.CourseModule;
import edu.seu.vcampus.server.library.LibraryModule;
import edu.seu.vcampus.server.library.LibraryService;
import edu.seu.vcampus.server.network.ServerMessageDispatcher;
import edu.seu.vcampus.server.network.ServerMessageReceiverThread;
import edu.seu.vcampus.server.network.ServerSocketListener;
import edu.seu.vcampus.server.shop.ShopModule;
import edu.seu.vcampus.server.student.StudentModule;
import edu.seu.vcampus.server.thread.ThreadPoolManager;
import edu.seu.vcampus.server.user.AccountProvisioning;
import edu.seu.vcampus.server.user.AuthModule;
import edu.seu.vcampus.server.user.SessionManager;

import java.io.IOException;
import java.sql.SQLException;

/**
 * vCampus 服务器端入口。
 *
 * <p>
 * 启动 ServerSocket 监听，循环接受客户端连接；每个连接交给全局线程池， 由 {@link ServerMessageReceiverThread}
 * 跑「每客户端一线程」的收发循环（含心跳与连接级鉴权， 见 ADR-0006）。
 *
 * <p>
 * 装配按<b>依赖拓扑</b>自上而下走一遍，各业务模块自带单例，入口不 new 任何 DAO：
 *
 * <pre>
 * 账号（无依赖）→ 课程 / 学籍 / 银行（只用到账号）→ 图书馆 / 商店（还要银行）
 * </pre>
 *
 * <p>
 * 模块自装配的前提是「单例必须真的是同一个」：会话表全服唯一（登录签发的 token 才能在业务侧 校验到）、银行账户池全服唯一（商店与罚款才能在同一个池里扣钱）、账户库全服唯一
 * （学籍列表才能反查到姓名）。这些约束由各模块的单例访问器保证，入口只负责按序调用。
 *
 * <p>
 * 注册 JVM 关机钩子实现优雅关机：收到停机信号（Ctrl+C 等）时先停止监听， 使阻塞中的 accept 退出，从而结束主循环；同时停止线程池接受新任务。
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
     * <p>
     * 默认端口取自 {@link NetworkConstant#DEFAULT_PORT}（端口的唯一权威源）， 服务端不再自定义端口常量。
     *
     * @param args 命令行参数（暂未使用）
     */
    public static void main(String[] args) {
        try {
            // 缺库就直接起不来：不提供内存/文件回退，避免静默降级到一条「重启即失」的路径
            DbHelper.requireAvailable();
            startServer(NetworkConstant.DEFAULT_PORT);
        } catch (IOException e) {
            System.err.println("服务器启动失败: " + e.getMessage());
            System.exit(1);
        } catch (RuntimeException e) {
            System.err.println("服务器启动失败: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * 按生产装配在指定端口启动服务器，随后阻塞在「接受连接」循环中。
     *
     * @param port 监听端口，0 表示由系统分配随机端口
     * @throws IOException 绑定端口失败
     */
    public static void startServer(int port) throws IOException {
        runServer(port, null);
    }

    /**
     * 启动服务器，并以外部注入的图书馆服务覆盖图书馆模块的<span>单例</span>（供集成测试注入替身）。
     *
     * @param port    监听端口，0 表示随机端口
     * @param library 注入的图书馆业务服务，不能为 null
     * @throws IOException 启动或监听失败
     */
    public static void startServer(int port, LibraryService library) throws IOException {
        if (library == null) {
            throw new IllegalArgumentException("library must not be null");
        }
        runServer(port, library);
    }

    /**
     * 按依赖拓扑装配并在指定端口启动服务器，随后阻塞在「接受连接」循环中。
     *
     * @param port            监听端口，0 表示由系统分配随机端口
     * @param injectedLibrary 注入的图书馆服务；null 表示用图书馆模块单例
     * @throws IOException 绑定端口失败
     */
    private static void runServer(int port, LibraryService injectedLibrary) throws IOException {
        final ServerSocketListener server = new ServerSocketListener();
        s_listener = server;
        registerShutdownHook();

        final ServerMessageDispatcher dispatcher = ServerMessageReceiverThread.getDispatcher();
        // 开户钩子登记表用于「管理员建号后同步建立各模块 1:1 档案」
        final AccountProvisioning provisioning = new AccountProvisioning();

        // 顺序即依赖拓扑，不能调乱：账号在最前（会话表与账户库由它交出去），
        // 银行要在图书馆、商店之前（两者都从它取账户池）。
        final SessionManager sessions = AuthModule.initialize(dispatcher, provisioning);
        CourseModule.register(dispatcher, sessions, provisioning);
        StudentModule.register(dispatcher, sessions, provisioning);
        BankModule.register(dispatcher, sessions);
        LibraryModule.register(dispatcher, sessions, provisioning, injectedLibrary);
        ShopModule.register(dispatcher, sessions);

        // 演示数据：仅当开启 -Dvcampus.demo.seed=true 时注入（账号/馆藏/借阅，含逾期）
        seedDemoData();

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
                // 每客户端一线程：交给全局线程池执行，连接收尾由 ServerMessageReceiverThread 负责。
                ThreadPoolManager.getInstance()
                        .execute(new ServerMessageReceiverThread(stream, sessions));
            }
        } finally {
            s_listener = null;
        }
    }

    /**
     * 开关开启时注入演示数据；失败只告警，不阻断启动。
     *
     * <p>
     * 数据访问一律取各模块单例：种子必须写进各模块真正在用的那份目录与账户池， 否则注入的数据在界面上看不见 —— 与「第二个 BankService」是同一类坑。
     */
    private static void seedDemoData() {
        try {
            DemoDataSeeder.seedIfEnabled(AuthModule.repository(), AuthModule.authService(),
                    LibraryModule.bookDao(), LibraryModule.borrowDao(),
                    LibraryModule.accountDao(), CourseModule.courseDao(),
                    CourseModule.scoreDao());
        } catch (SQLException e) {
            System.err.println("演示种子注入失败: " + e.getMessage());
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
     * 注册优雅关机钩子：JVM 收到停机信号时停止监听，使阻塞中的 accept 退出。
     *
     * <p>
     * 线程池仅停止接受新任务（已提交的连接任务自然跑完）。宽限期等待 （awaitTermination）待网络组补充。
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
