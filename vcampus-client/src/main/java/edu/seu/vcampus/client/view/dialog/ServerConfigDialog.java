package edu.seu.vcampus.client.view.dialog;

import edu.seu.vcampus.client.network.ClientServerConfig;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * 服务器地址配置对话框：登录页右上角齿轮打开。
 *
 * <p>
 * 演示时服务器会开在公网（端口映射），地址随时可能变，所以这里既能<b>保存</b>（落到
 * {@code data/client.properties}，下次启动生效），也能<b>测试连接</b>（当场探一次 TCP，
 * 免得回到登录页才发现地址写错）。
 */
public class ServerConfigDialog extends JDialog {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 服务器地址输入框。 */
    private final JTextField m_host;

    /** 端口输入框。 */
    private final JTextField m_port;

    /** 提示标签。 */
    private final JLabel m_message = new JLabel(" ");

    /** 测试连接按钮。 */
    private final JButton m_test = new JButton("测试连接");

    /** 连接测试超时，毫秒。 */
    private final int m_timeout;

    /**
     * 构造配置对话框。
     *
     * @param owner   宿主窗口
     * @param current 当前配置
     */
    public ServerConfigDialog(Window owner, ClientServerConfig current) {
        this(owner, current, 2000);
    }

    /**
     * 构造配置对话框并指定连接测试超时（测试用）。
     *
     * @param owner   宿主窗口
     * @param current 当前配置
     * @param timeout 连接测试超时，毫秒
     */
    public ServerConfigDialog(Window owner, ClientServerConfig current, int timeout) {
        super(owner, "服务器设置", ModalityType.APPLICATION_MODAL);
        this.m_timeout = timeout;
        this.m_host = new JTextField(current == null ? "" : current.host(), 18);
        this.m_port = new JTextField(current == null ? "" : String.valueOf(current.port()), 8);
        setContentPane(createContent());
        pack();
        setMinimumSize(new Dimension(420, 280));
        setLocationRelativeTo(owner);
    }

    private JPanel createContent() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(UiTheme.SURFACE);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 18, 24));
        root.add(createForm(), BorderLayout.CENTER);
        root.add(createActions(), BorderLayout.SOUTH);
        return root;
    }

    private JPanel createForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints grid = new GridBagConstraints();
        grid.insets = new Insets(6, 2, 6, 2);
        grid.anchor = GridBagConstraints.WEST;
        grid.fill = GridBagConstraints.HORIZONTAL;
        grid.weightx = 1;
        grid.gridx = 0;
        grid.gridy = 0;
        grid.gridwidth = 2;
        JLabel heading = new JLabel("服务器地址");
        heading.setForeground(UiTheme.TEXT);
        heading.setFont(UiTheme.font(Font.BOLD, 17F));
        form.add(heading, grid);
        grid.gridy = 1;
        JLabel hint = new JLabel("演示时填公网映射的地址与端口；保存后下次连接生效");
        hint.setForeground(UiTheme.MUTED);
        hint.setFont(UiTheme.font(Font.PLAIN, 11F));
        form.add(hint, grid);
        grid.gridy = 2;
        grid.gridwidth = 1;
        grid.weightx = 0.75;
        form.add(m_host, grid);
        grid.gridx = 1;
        grid.weightx = 0.25;
        form.add(m_port, grid);
        grid.gridx = 0;
        grid.gridy = 3;
        grid.gridwidth = 2;
        m_message.setForeground(UiTheme.MUTED);
        m_message.setFont(UiTheme.font(Font.PLAIN, 12F));
        form.add(m_message, grid);
        return form;
    }

    private JPanel createActions() {
        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        m_test.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                testConnection();
            }
        });
        JButton save = UiFactory.primaryButton("保存", "refresh");
        save.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                save();
            }
        });
        JPanel right = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(m_test);
        right.add(save);
        actions.add(right, BorderLayout.EAST);
        getRootPane().setDefaultButton(save);
        return actions;
    }

    /** 校验并保存到配置文件。 */
    private void save() {
        String error = ClientServerConfig.validate(hostText(), portText());
        if (error != null) {
            warn(error);
            return;
        }
        try {
            ClientServerConfig.of(hostText(), portValue()).save();
        } catch (IOException e) {
            warn("保存失败：" + e.getMessage());
            return;
        }
        javax.swing.JOptionPane.showMessageDialog(this,
                "已保存为 " + hostText().trim() + ":" + portValue() + "\n重新登录后按新地址连接。",
                "服务器设置", javax.swing.JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }

    /** 后台探一次 TCP，看看地址是否可达（不建立业务连接）。 */
    private void testConnection() {
        String error = ClientServerConfig.validate(hostText(), portText());
        if (error != null) {
            warn(error);
            return;
        }
        final String host = hostText().trim();
        final int port = portValue();
        m_test.setEnabled(false);
        m_message.setForeground(UiTheme.MUTED);
        m_message.setText("正在测试连接 " + host + ":" + port + " …");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean reachable = canConnect(host, port);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        m_test.setEnabled(true);
                        m_message.setForeground(reachable ? UiTheme.SUCCESS : UiTheme.ACCENT);
                        m_message.setText(reachable ? "连接成功，可以保存"
                                : "连接失败：请检查地址、端口与服务器是否已启动");
                    }
                });
            }
        }, "vcampus-connection-test").start();
    }

    /** 能否在超时内建立 TCP 连接。 */
    private boolean canConnect(String host, int port) {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), m_timeout);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                return false;
            }
        }
    }

    private void warn(String message) {
        m_message.setForeground(UiTheme.ACCENT);
        m_message.setText(message);
    }

    private int portValue() {
        return Integer.parseInt(portText());
    }

    /** @return 地址输入框文本 */
    public String hostText() {
        return m_host.getText().trim();
    }

    /** @return 端口输入框文本 */
    public String portText() {
        return m_port.getText().trim();
    }

    /** @return 提示标签文本（供测试） */
    public String messageText() {
        return m_message.getText();
    }

    /** @return 测试连接按钮（供测试） */
    public JButton testButton() {
        return m_test;
    }
}
