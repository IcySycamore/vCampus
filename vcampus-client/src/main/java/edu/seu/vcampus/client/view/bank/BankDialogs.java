package edu.seu.vcampus.client.view.bank;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;

/**
 * 银行模块弹窗的统一放置约定。
 *
 * <p>所有弹窗一律摆在<b>屏幕正中</b>，而不是相对父窗口摆放：父窗口本身靠左上（或还没定位）时，
 * 「相对父窗口居中」会让弹窗看起来贴在左上角。放置规则集中在这里，新增弹窗只要调用
 * {@link #centerOnScreen(JDialog)} 即可，不必各自重复定位代码。</p>
 *
 * <p>需要铺满屏幕的弹窗（如管理员查看流水）用 {@link #installMaximize(JDialog, JButton)} 挂一个
 * 「最大化 / 还原」切换按钮。</p>
 */
final class BankDialogs {

    /** 最大化前的窗口边界，存放在窗口根面板的客户端属性里。 */
    private static final String RESTORE_BOUNDS = "vcampus.bank.restoreBounds";

    /** 私有构造器，禁止实例化工具类。 */
    private BankDialogs() {
    }

    /**
     * 把窗口摆到屏幕正中（可用区域已扣掉任务栏等系统占用）。
     *
     * <p>必须在 {@code pack()} 之后调用，否则窗口还没有尺寸；无图形环境时静默跳过，
     * 便于在无显示器的测试环境构造界面。</p>
     *
     * @param dialog 已确定尺寸的窗口
     */
    static void centerOnScreen(final JDialog dialog) {
        if (dialog == null || GraphicsEnvironment.isHeadless()) {
            return;
        }
        dialog.setLocationByPlatform(false);
        applyCenteredLocation(dialog);
        // 部分窗口管理器（如 WSLg 的合成器）在窗口首次映射时按自己的规则摆放，会忽略创建前的
        // 定位请求；窗口真正显示后再复一次，作为客户端发起的位置请求。
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                applyCenteredLocation(dialog);
            }
        });
    }

    /** 把窗口重定位到屏幕正中；显示前后都可安全重复调用。 */
    private static void applyCenteredLocation(JDialog dialog) {
        dialog.setLocation(centeredLocation(dialog.getSize(), screenArea()));
    }

    /**
     * 计算把给定尺寸的窗口居中于给定区域时的左上角坐标。
     *
     * <p>窗口大于可用区域时贴住区域左上角而不产生负坐标，避免窗口被推到屏幕外。</p>
     *
     * @param size 窗口尺寸
     * @param area 可用屏幕区域
     * @return 居中后的左上角坐标
     */
    static Point centeredLocation(Dimension size, Rectangle area) {
        if (size == null || area == null) {
            throw new IllegalArgumentException("size and area are required");
        }
        int x = area.x + Math.max(0, (area.width - size.width) / 2);
        int y = area.y + Math.max(0, (area.height - size.height) / 2);
        return new Point(x, y);
    }

    /** @return 当前屏幕的可用区域（已排除任务栏） */
    static Rectangle screenArea() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    }

    /**
     * 显示居中于屏幕的确认框。
     *
     * @param parent 父组件；只用于挂靠模态关系，不参与定位
     * @param title 窗口标题
     * @param content 提示内容（文本或组件）
     * @param messageType 见 {@link JOptionPane} 的 *_MESSAGE 常量
     * @return 是否点击了「确定」
     */
    static boolean confirm(Component parent, String title, Object content, int messageType) {
        JOptionPane pane = new JOptionPane(content, messageType,
                JOptionPane.OK_CANCEL_OPTION);
        JDialog dialog = prepared(pane, parent, title);
        dialog.setVisible(true);
        Object value = pane.getValue();
        return value instanceof Integer
                && ((Integer) value).intValue() == JOptionPane.OK_OPTION;
    }

    /**
     * 显示居中于屏幕的提示框。
     *
     * @param parent 父组件；只用于挂靠模态关系，不参与定位
     * @param title 窗口标题
     * @param content 提示内容（文本或组件）
     * @param messageType 见 {@link JOptionPane} 的 *_MESSAGE 常量
     */
    static void message(Component parent, String title, Object content, int messageType) {
        JOptionPane pane = new JOptionPane(content, messageType);
        prepared(pane, parent, title).setVisible(true);
    }

    /**
     * 给窗口装上「最大化 / 还原」切换按钮。
     *
     * @param dialog 目标窗口
     * @param button 触发切换的按钮；文本会随状态在「最大化」与「还原」之间变化
     */
    static void installMaximize(final JDialog dialog, final JButton button) {
        if (dialog == null || button == null) {
            return;
        }
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                toggleMaximized(dialog, button);
            }
        });
    }

    /** 在「铺满可用屏幕」与「原尺寸」之间切换。 */
    private static void toggleMaximized(JDialog dialog, JButton button) {
        JRootPane root = dialog.getRootPane();
        Object saved = root.getClientProperty(RESTORE_BOUNDS);
        if (saved instanceof Rectangle) {
            dialog.setBounds((Rectangle) saved);
            root.putClientProperty(RESTORE_BOUNDS, null);
            dialog.setResizable(false);
            button.setText("最大化");
        } else {
            root.putClientProperty(RESTORE_BOUNDS, dialog.getBounds());
            dialog.setResizable(true);
            dialog.setBounds(screenArea());
            button.setText("还原");
        }
        dialog.validate();
    }

    /** 由选项面板建出窗口并居中。 */
    private static JDialog prepared(JOptionPane pane, Component parent, String title) {
        JDialog dialog = pane.createDialog(parent, title);
        dialog.setResizable(false);
        centerOnScreen(dialog);
        return dialog;
    }
}
