package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.view.theme.UiIcons;
import edu.seu.vcampus.client.view.theme.UiTheme;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 登录窗口的自绘标题栏：左侧品牌，右侧「齿轮 / 最小化 / 关闭」。
 *
 * <p>
 * 之所以自绘：系统标题栏里塞不进自定义按钮，而演示时需要在登录页直接改服务器地址（公网映射后地址会变）。
 * 代价是窗口失去系统边框，因此本类额外承担两件事——<b>拖动窗口</b>与<b>刻画最小化/关闭符号</b> （符号自绘而非用字符，避免字体缺字形显示成方块）。
 */
public class LoginTitleBar extends JPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 普通按钮悬停底色。 */
    private static final Color HOVER = new Color(255, 255, 255, 38);

    /** 关闭按钮悬停底色。 */
    private static final Color CLOSE_HOVER = new Color(232, 17, 35);

    /** 被操作的窗口。 */
    private final Window m_window;

    /** 拖动起点。 */
    private Point m_origin;

    /**
     * 构造标题栏。
     *
     * @param window     目标窗口（用于拖动 / 最小化 / 关闭）
     * @param onSettings 点击齿轮的回调
     */
    public LoginTitleBar(final Window window, final Runnable onSettings) {
        this.m_window = window;
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 18, 0, 8));
        add(createBrand(), BorderLayout.WEST);
        add(createButtons(onSettings), BorderLayout.EAST);
        installDrag();
    }

    /** 左侧品牌文字（深色底上用浅色）。 */
    private JLabel createBrand() {
        JLabel brand = new JLabel("vCampus 虚拟校园");
        brand.setForeground(new Color(150, 166, 196));
        brand.setFont(UiTheme.font(Font.BOLD, 12F));
        return brand;
    }

    /** 右侧按钮组：齿轮在最小化与关闭的左侧。 */
    private JPanel createButtons(final Runnable onSettings) {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        buttons.setOpaque(false);
        JButton gear = createGearButton();
        gear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (onSettings != null) {
                    onSettings.run();
                }
            }
        });
        buttons.add(gear);
        buttons.add(new CaptionButton(CaptionButton.MINIMIZE, m_window));
        buttons.add(new CaptionButton(CaptionButton.CLOSE, m_window));
        return buttons;
    }

    /** 齿轮按钮（配置服务器地址）。 */
    private JButton createGearButton() {
        final JButton gear = new JButton(UiIcons.load("settings-light", 18));
        gear.setPreferredSize(new Dimension(38, 30));
        gear.setContentAreaFilled(false);
        gear.setBorderPainted(false);
        gear.setFocusPainted(false);
        gear.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        gear.setToolTipText("配置服务器地址与端口");
        gear.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                gear.setContentAreaFilled(true);
                gear.setOpaque(true);
                gear.setBackground(HOVER);
            }

            @Override
            public void mouseExited(MouseEvent event) {
                gear.setContentAreaFilled(false);
            }
        });
        return gear;
    }

    /** 按住标题栏拖动窗口（无系统边框后的替代）。 */
    private void installDrag() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                m_origin = event.getPoint();
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent event) {
                if (m_origin == null || m_window == null) {
                    return;
                }
                Point location = m_window.getLocation();
                m_window.setLocation(location.x + event.getX() - m_origin.x,
                        location.y + event.getY() - m_origin.y);
            }
        });
    }

    /**
     * 自绘的最小化 / 关闭按钮。
     *
     * <p>
     * 符号用 {@link Graphics2D} 画出来，而不是用「—」「✕」这类字符：不同机器字体覆盖不一， 缺字形时会显示成方块（用户反馈过右上角图标显示不正确）。
     */
    static final class CaptionButton extends JButton {

        /** 序列化版本号。 */
        private static final long serialVersionUID = 1L;

        /** 最小化类型。 */
        static final int MINIMIZE = 0;

        /** 关闭类型。 */
        static final int CLOSE = 1;

        /** 按钮类型。 */
        private final int m_type;

        /** 被操作的窗口。 */
        private final transient Window m_window;

        /**
         * 构造标题按钮。
         *
         * @param type   类型：{@link #MINIMIZE} 或 {@link #CLOSE}
         * @param window 目标窗口
         */
        CaptionButton(int type, Window window) {
            this.m_type = type;
            this.m_window = window;
            setPreferredSize(new Dimension(40, 30));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(type == CLOSE ? "关闭" : "最小化");
            addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    perform();
                }
            });
        }

        /** 执行最小化或关闭。 */
        private void perform() {
            if (m_window == null) {
                return;
            }
            if (m_type == CLOSE) {
                System.exit(0);// 与登录窗口原有 EXIT_ON_CLOSE 行为一致
            } else if (m_window instanceof Frame) {
                ((Frame) m_window).setState(Frame.ICONIFIED);
            } else {
                m_window.setVisible(false);
            }
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D canvas = (Graphics2D) graphics.create();
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            if (getModel().isRollover() || getModel().isPressed()) {
                canvas.setColor(m_type == CLOSE ? CLOSE_HOVER : HOVER);
                canvas.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
            }
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;
            canvas.setColor(Color.WHITE);
            canvas.setStroke(new BasicStroke(1.4F));
            if (m_type == CLOSE) {
                canvas.drawLine(centerX - 5, centerY - 5, centerX + 5, centerY + 5);
                canvas.drawLine(centerX + 5, centerY - 5, centerX - 5, centerY + 5);
            } else {
                canvas.drawLine(centerX - 6, centerY + 4, centerX + 6, centerY + 4);
            }
            canvas.dispose();
        }
    }
}
