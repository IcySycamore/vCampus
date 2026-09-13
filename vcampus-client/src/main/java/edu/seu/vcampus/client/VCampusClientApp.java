package edu.seu.vcampus.client;

import edu.seu.vcampus.client.api.ClientApis;
import edu.seu.vcampus.client.handler.UiCallback;
import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.client.network.ClientSocketListener;
import edu.seu.vcampus.client.network.ClientMessageSender;
import edu.seu.vcampus.client.view.shell.LoginFrame;
import edu.seu.vcampus.client.view.theme.UiTheme;

import javax.swing.SwingUtilities;

import java.io.IOException;

/**
 * vCampus 客户端入口：启动登录界面，并负责把「连接层 + 分发器 + 各业务模块」装配起来 ——与 server 的
 * {@code VCampusServerApp} 对称（那边装配模块并启动监听，这边装配模块并建立连接）。
 *
 * <p>
 * 装配关系：收到的消息由 {@link ClientSocketListener} 直接交给 {@link ClientMessageDispatcher}
 * （收消息与连接事件的落点）；出站由分发器经 {@link ClientMessageSender} 走同一条连接。 各模块自带接线规则，入口只需调用
 * {@code XxxModule.register(dispatcher)}。
 */
public final class VCampusClientApp {

    /** 当前连接；由 {@link #connect(String, int)} 建立、{@link #stop()} 关闭。 */
    private static volatile ClientSocketListener s_socket;
    private static volatile ClientApis s_apis;

    /** 私有构造器，禁止实例化入口类。 */
    private VCampusClientApp() {
    }

    /**
     * 程序入口。
     *
     * @param args 命令行参数（暂未使用）
     */
    public static void main(String[] args) {
        UiTheme.applyNimbus();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new LoginFrame().setVisible(true);
            }
        });
    }

    /**
     * 建立到服务器的连接并装配各客户端模块（与 server 的 {@code startServer} 对称）。
     *
     * @param host 服务器地址
     * @param port 服务器端口
     * @return 各模块客户端 API 的只读容器（登录、业务操作与身份的统一入口）
     * @throws IOException 连接失败
     */
    public static ClientApis connect(String host, int port) throws IOException {
        ClientMessageDispatcher dispatcher = new ClientMessageDispatcher();
        ClientSocketListener socket = new ClientSocketListener(host, port, dispatcher);
        dispatcher.bindSender(new ClientMessageSender(socket));// 出站走同一条连接
        dispatcher.setUiCallback(new EdtUiCallback());// 处理器改界面时切回 EDT
        ClientApis apis = ClientApis.create(dispatcher);// 各模块自装配（含会话随连接失效）
        stopAsync();
        s_socket = socket;
        s_apis = apis;
        try {
            socket.connect();
            return apis;
        } catch (IOException exception) {
            stopAsync(apis);
            throw exception;
        }
    }

    /**
     * 停止客户端：关闭与服务器的连接（与 server 的 {@code stopServer} 对称）；断开时各模块自行收尾。
     *
     * @throws IOException 关闭失败
     */
    public static synchronized void stop() throws IOException {
        ClientSocketListener socket = s_socket;
        s_socket = null;
        s_apis = null;
        if (socket != null) {
            socket.close();
        }
    }

    /**
     * 关闭指定装配实例的连接，过期窗口不能关闭新登录的连接。
     * @param owner 创建该窗口的 API 容器
     */
    public static synchronized void stopAsync(ClientApis owner) {
        if (s_apis == owner) {
            stopAsync();
        }
    }

    /** 异步关闭当前连接；先摘下引用，避免关闭随后建立的新连接。 */
    public static synchronized void stopAsync() {
        final ClientSocketListener socket = s_socket;
        s_socket = null;
        s_apis = null;
        if (socket != null) {
            Thread closer = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        socket.close();
                    } catch (IOException exception) {
                        System.err.println("关闭连接失败: " + exception.getMessage());
                    }
                }
            }, "vcampus-connection-close");
            closer.setDaemon(true);
            closer.start();
        }
    }

    /**
     * 停止客户端且不向调用方抛异常（供界面退出路径调用）。
     */
    public static void stopQuietly() {
        try {
            stop();
        } catch (IOException e) {
            System.err.println("关闭连接失败: " + e.getMessage());
        }
    }

    /** 界面回调实现：把处理器的界面更新派发到 Swing 事件线程。 */
    private static final class EdtUiCallback implements UiCallback {
        @Override
        public void run(Runnable task) {
            if (SwingUtilities.isEventDispatchThread()) {
                task.run();
            } else {
                SwingUtilities.invokeLater(task);
            }
        }
    }
}
