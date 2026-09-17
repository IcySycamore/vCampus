package edu.seu.vcampus.server;

import edu.seu.vcampus.server.db.DbHelper;

import edu.seu.vcampus.common.constant.NetworkConstant;
import edu.seu.vcampus.common.network.MessageStream;

import edu.seu.vcampus.server.user.AuthModule;
import edu.seu.vcampus.server.user.SessionManager;
import edu.seu.vcampus.server.bank.BankModule;
import edu.seu.vcampus.server.student.StudentModule;
import edu.seu.vcampus.server.course.CourseModule;
import edu.seu.vcampus.server.library.LibraryModule;
import edu.seu.vcampus.server.shop.ShopModule;

import edu.seu.vcampus.server.network.ServerMessageDispatcher;
import edu.seu.vcampus.server.network.ServerMessageReceiverThread;
import edu.seu.vcampus.server.network.ServerSocketListener;
import edu.seu.vcampus.server.thread.ThreadPoolManager;
import edu.seu.vcampus.server.user.AccountProvisioning;

import java.io.IOException;

/**
 * vCampus 服务器端入口。
 *
 * <p>
 * 启动 ServerSocket 监听，循环接受客户端连接；每个连接交给全局线程池， 由 {@link ServerMessageReceiverThread} 跑「每客户端一线程」的收发循环
 *
 * <p>
 * 装配按<b>依赖拓扑</b>自上而下走一遍，各业务模块自带单例
 *
 * <pre>
 * 账号 → 课程 / 学籍 / 银行 → 图书馆 / 商店
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
     * 启动服务器：按依赖拓扑完成全部装配，随后阻塞在「接受连接」循环中。
     *
     * <p>
     * 装配只在这里发生，没有第二个入口、也不接受外部注入：各模块的单例由各模块自己持有， 入口只负责按序把它们接上。测试要走真实路径，就不应该能从这里塞进替身 ——
     * 能注入就意味着「本地绿了、生产走的是另一条路」，而那正是此前两个缺陷长期没人发现的原因。
     *
     * @param port 监听端口，0 表示由系统分配随机端口
     * @throws IOException 绑定端口失败
     */
    public static void startServer(int port) throws IOException {
        final ServerSocketListener server = new ServerSocketListener();
        s_listener = server;
        registerShutdownHook();

        final ServerMessageDispatcher dispatcher = ServerMessageReceiverThread.getDispatcher();
        // 开户钩子登记表用于「管理员建号后同步建立各模块 1:1 档案」
        final AccountProvisioning provisioning = new AccountProvisioning();

        // 账号在最前
        // 银行要在图书馆、商店之前
        final SessionManager sessions = AuthModule.initialize(dispatcher, provisioning);
        CourseModule.register(dispatcher, sessions, provisioning);
        StudentModule.register(dispatcher, sessions, provisioning);
        BankModule.register(dispatcher, sessions);
        LibraryModule.register(dispatcher, sessions, provisioning);
        ShopModule.register(dispatcher, sessions);
        // 引导文件里的账号放到最后导入：钩子刚才才登记完，提前导入的话那些账号拿不到各模块档案
        AuthModule.bootstrapAccounts();

        server.start(port);
        System.out.println("vCampus Server start, listening on port: " + server.getPort());

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
