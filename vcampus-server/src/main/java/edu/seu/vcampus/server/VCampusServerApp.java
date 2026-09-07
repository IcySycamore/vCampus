package edu.seu.vcampus.server;

import edu.seu.vcampus.server.network.MessageStream;
import edu.seu.vcampus.server.network.ServerSocketListener;

import java.io.IOException;

/**
 * vCampus 服务器端入口。
 *
 * <p>启动 ServerSocket 监听，循环接受客户端连接。每接一个连接后创建消息流，
 * 交由线程池处理收发循环（线程池 ClientThreadMan 由网络小组接入，见 ADR-0006）。
 *
 * <p>注册 JVM 关机钩子实现优雅关机：收到停机信号（Ctrl+C 等）时先停止监听，
 * 使阻塞中的 accept 退出，从而结束主循环、释放资源；线程池接入后补全线程池优雅关闭。
 */
public final class VCampusServerApp {

    /**
     * 私有构造器，禁止实例化入口类。
     */
    private VCampusServerApp() {
    }

    /**
     * 程序入口。
     *
     * @param args 命令行参数（暂未使用）
     */
    public static void main(String[] args) {
        final ServerSocketListener listener = new ServerSocketListener();
        registerShutdownHook(listener);

        try {
            listener.start(ServerSocketListener.DEFAULT_PORT);
            System.out.println("vCampus Server 已启动，监听端口 " + listener.getPort());

            while (listener.isRunning()) {
                MessageStream stream = listener.accept();
                System.out.println("新客户端连接建立");
                // 线程池 ClientThreadMan 接入后，在此把 stream 交给线程池处理；
                // 当前线程池未实现，先关闭连接避免资源泄漏。
                stream.close();
            }
        } catch (IOException e) {
            // 正常关机时 listener.stop() 会使 accept 抛异常退出循环，不打印堆栈；
            // 只有非关机导致的异常才打印。
            if (listener.isRunning()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 注册优雅关机钩子：JVM 收到停机信号时停止监听，使阻塞中的 accept 退出。
     *
     * <p>线程池 ClientThreadMan 接入后，在此补全线程池的优雅关闭：
     * shutdown() → awaitTermination(宽限期) → shutdownNow()。
     *
     * @param listener 监听器
     */
    private static void registerShutdownHook(final ServerSocketListener listener) {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    listener.stop();
                    System.out.println("vCampus Server 已停止监听，优雅退出");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, "vCampusServer-shutdown"));
    }
}
