package edu.seu.vcampus.client;

import edu.seu.vcampus.client.view.shell.LoginFrame;
import edu.seu.vcampus.client.view.shell.UiTheme;

import javax.swing.SwingUtilities;

/**
 * vCampus 客户端入口。
 *
 * <p>在 Swing 事件分派线程中应用统一主题并启动登录窗口。
 */
public final class VCampusClientApp {

    /**
     * 私有构造器，禁止实例化入口类。
     */
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
}
