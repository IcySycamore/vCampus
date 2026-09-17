package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.network.ClientServerConfig;
import edu.seu.vcampus.client.view.component.GradientPanel;
import edu.seu.vcampus.client.view.component.IconTextFieldPanel;
import edu.seu.vcampus.client.view.component.RoundedPanel;
import edu.seu.vcampus.client.view.dialog.ServerConfigDialog;
import edu.seu.vcampus.client.view.theme.ResponsiveTypography;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * 登录窗口：左侧深色品牌区 + 右侧登录卡。
 *
 * <p>
 * <b>没有匿名入口</b>：这一页只提供「账号 + 密码 → 登录」一条路。曾经有个「离线预览」按钮，
 * 点一下就用「预览用户」的身份把主窗口开出来，等于免登录进门；它和配套的无会话构造器都已删除。
 *
 * <p>
 * <b>也没有身份选择</b>：早先这里有「学生 / 教师 / 管理员」三选一，但登录请求带过去的角色服务端
 * 根本不看（身份以服务端下发的会话为准），选定值与真实身份不符也不影响登录结果。一个不影响任何 行为的控件只会误导人 —— 它让人以为选了「管理员」就是管理员。
 */
public class LoginFrame extends JFrame {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 登录名输入框。 */
    private final JTextField userIdField = new JTextField(20);

    /** 密码输入框。 */
    private final JPasswordField passwordField = new JPasswordField(20);

    /** 表单提示（错误信息与进行中状态）。 */
    private final JLabel messageLabel = new JLabel(" ");

    /** 登录流程。 */
    private final LoginFlow loginFlow = new LoginFlow(this, messageLabel);

    /** 登录按钮（测试要确认它是这一页唯一的入口，且空表单不会被放行）。 */
    private JButton loginButton;

    /** 创建登录窗口。 */
    public LoginFrame() {
        this(" ");
    }

    /**
     * 创建显示提示的登录窗口。
     *
     * @param message 登录提示（如「连接已断开，请重新登录」）
     */
    public LoginFrame(String message) {
        super("vCampus 虚拟校园");
        setUndecorated(true);// 自绘标题栏：齿轮（服务器设置）+ 最小化 + 关闭
        messageLabel.setText(message);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 650));
        setSize(1080, 760);
        setLocationRelativeTo(null);
        setContentPane(createContent());
        ResponsiveTypography.install(this, 1080, 1.2F);
    }

    private JPanel createContent() {
        GradientPanel root = new GradientPanel(new Color(13, 24, 45), new Color(32, 28, 48));
        root.setLayout(new BorderLayout());
        root.setBorder(BorderFactory.createLineBorder(new Color(52, 62, 88)));
        root.add(new LoginTitleBar(this, new Runnable() {
            @Override
            public void run() {
                openServerConfig();
            }
        }), BorderLayout.NORTH);
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        JPanel shell = new JPanel(new GridLayout(1, 2));
        shell.setOpaque(false);
        shell.setPreferredSize(new Dimension(880, 590));
        shell.add(new LoginBrandPanel());
        shell.add(createLoginCard());
        center.add(shell);
        ResponsiveTypography.installScaledSize(this, shell, 880, 590, 1080, 1.2F);
        root.add(center, BorderLayout.CENTER);
        return root;
    }

    /** 打开服务器地址配置（右上角齿轮）。 */
    private void openServerConfig() {
        new ServerConfigDialog(this, ClientServerConfig.load()).setVisible(true);
    }

    private JPanel createLoginCard() {
        RoundedPanel card = new RoundedPanel(new GridBagLayout(), 24, UiTheme.SURFACE);
        card.setBorder(BorderFactory.createEmptyBorder(38, 46, 32, 46));
        GridBagConstraints grid = new GridBagConstraints();
        grid.gridx = 0;
        grid.fill = GridBagConstraints.HORIZONTAL;
        grid.weightx = 1;
        grid.insets = new Insets(2, 2, 2, 2);
        card.add(heading(), grid);
        grid.gridy = 1;
        card.add(subtitle(), grid);
        grid.gridy = 2;
        grid.insets = new Insets(26, 2, 4, 2);
        card.add(fieldLabel("账号"), grid);
        grid.gridy = 3;
        grid.insets = new Insets(0, 2, 12, 2);
        card.add(new IconTextFieldPanel("user", userIdField), grid);
        grid.gridy = 4;
        grid.insets = new Insets(2, 2, 4, 2);
        card.add(fieldLabel("密码"), grid);
        grid.gridy = 5;
        grid.insets = new Insets(0, 2, 10, 2);
        card.add(new IconTextFieldPanel("lock", passwordField), grid);
        grid.gridy = 6;
        grid.insets = new Insets(0, 2, 8, 2);
        messageLabel.setForeground(UiTheme.ACCENT);
        card.add(messageLabel, grid);
        grid.gridy = 7;
        grid.insets = new Insets(2, 2, 0, 2);
        JButton button = UiFactory.primaryButton("登  录", "user");
        button.setPreferredSize(new Dimension(300, 44));
        button.addActionListener(new LoginAction());
        card.add(button, grid);
        grid.gridy = 8;
        grid.insets = new Insets(22, 2, 0, 2);
        card.add(hintPanel(), grid);
        loginButton = button;
        getRootPane().setDefaultButton(button);
        return card;
    }

    /**
     * 登录按钮（供测试断言：登录页只有这一个入口）。
     *
     * @return 登录按钮
     */
    JButton loginButton() {
        return loginButton;
    }

    /**
     * 表单提示文本（供测试断言：空表单不发起登录，只给提示）。
     *
     * @return 提示文本
     */
    String message() {
        return messageLabel.getText();
    }

    private JLabel heading() {
        JLabel label = new JLabel("欢迎回来");
        label.setForeground(UiTheme.TEXT);
        label.setFont(UiTheme.font(Font.BOLD, 26F));
        return label;
    }

    private JLabel subtitle() {
        JLabel label = new JLabel("登录 vCampus 虚拟校园系统");
        label.setForeground(UiTheme.MUTED);
        label.setFont(UiTheme.font(Font.PLAIN, 13F));
        return label;
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UiTheme.MUTED);
        label.setFont(UiTheme.font(Font.BOLD, 12F));
        return label;
    }

    /**
     * 卡片底部说明：账号从哪来、连的是哪台服务器。
     *
     * @return 说明区面板
     */
    private JPanel hintPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 4));
        panel.setOpaque(false);
        JLabel account = new JLabel("账号由管理员统一分配，系统不提供自助注册", SwingConstants.CENTER);
        account.setForeground(UiTheme.MUTED);
        account.setFont(UiTheme.font(Font.PLAIN, 12F));
        panel.add(account);
        ClientServerConfig config = ClientServerConfig.load();
        JLabel server = new JLabel(
                "当前服务器 " + config.host() + ":" + config.port() + "（右上角齿轮可修改）",
                SwingConstants.CENTER);
        server.setForeground(new Color(150, 156, 170));
        server.setFont(UiTheme.font(Font.PLAIN, 11F));
        panel.add(server);
        return panel;
    }

    /**
     * 登录中禁用整张表单，避免连点重复建连。
     *
     * @param busy 是否正在登录
     */
    void setBusy(boolean busy) {
        getRootPane().getDefaultButton().setEnabled(!busy);
        userIdField.setEnabled(!busy);
        passwordField.setEnabled(!busy);
    }

    private final class LoginAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            String userId = userIdField.getText().trim();
            char[] password = passwordField.getPassword();
            if (userId.length() == 0 || password.length == 0) {
                Arrays.fill(password, '\0');
                messageLabel.setText("请输入账号和密码");
                return;
            }
            passwordField.setText("");
            messageLabel.setText("正在连接服务器…");
            loginFlow.start(userId, new String(password));
            Arrays.fill(password, '\0');
        }
    }
}
