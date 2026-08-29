package edu.seu.vcampus.server;

import edu.seu.vcampus.server.network.MessageStream;
import edu.seu.vcampus.server.network.ServerSocketListener;

import java.io.IOException;

/**
 * vCampus 服务器端入口。
 *
 * <p>启动 ServerSocket 监听，循环接受客户端连接。每接一个连接后创建消息流，
 * 交由线程池处理收发循环（线程池 ClientThreadMan 由网络小组接入，见 ADR-0006）。
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
        ServerSocketListener listener = new ServerSocketListener();
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
            e.printStackTrace();
        }
    }
}
