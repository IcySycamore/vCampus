package edu.seu.vcampus.client;

import edu.seu.vcampus.client.handler.UiCallback;
import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.client.network.ClientSocketListener;
import edu.seu.vcampus.client.network.ClientMessageSender;
import edu.seu.vcampus.client.student.StudentModule;
import edu.seu.vcampus.client.student.StudentService;
import edu.seu.vcampus.client.user.UserModule;
import edu.seu.vcampus.client.user.UserService;
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

    /** 当前用户服务（登录、登出、个人档案）；界面取姓名与角色用。 */
    private static volatile UserService s_userService;

    /** 当前学籍服务（个人信息查询）；未连接时为 null。 */
    private static volatile StudentService s_studentService;

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
     * @return 用户管理客户端服务（登录、登出与会话的统一入口）
     * @throws IOException 连接失败
     */
    public static UserService connect(String host, int port) throws IOException {
        ClientMessageDispatcher dispatcher = new ClientMessageDispatcher();
        ClientSocketListener socket = new ClientSocketListener(host, port, dispatcher);// 收到的消息直接落入分发器
        dispatcher.bindSender(new ClientMessageSender(socket));// 出站走同一条连接
        dispatcher.setUiCallback(new EdtUiCallback());// 处理器改界面时切回 EDT
        UserService userService = UserModule.register(dispatcher);// 模块自装配（含会话随连接失效）
        StudentService studentService = StudentModule.register(dispatcher,
                userService.getSession());// 与用户模块共用同一份会话
        socket.connect();
        s_socket = socket;
        s_userService = userService;
        s_studentService = studentService;
        return userService;
    }

    /**
     * 取当前用户服务（未连接时为 null）。
     *
     * @return 用户服务
     */
    public static UserService getUserService() {
        return s_userService;
    }

    /**
     * 取当前学籍服务（未连接时为 null）。
     *
     * @return 学籍服务
     */
    public static StudentService getStudentService() {
        return s_studentService;
    }

    /**
     * 停止客户端：关闭与服务器的连接（与 server 的 {@code stopServer} 对称）；断开时各模块自行收尾。
     *
     * @throws IOException 关闭失败
     */
    public static void stop() throws IOException {
        ClientSocketListener socket = s_socket;
        s_socket = null;
        s_userService = null;
        s_studentService = null;
        if (socket != null) {
            socket.close();
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
