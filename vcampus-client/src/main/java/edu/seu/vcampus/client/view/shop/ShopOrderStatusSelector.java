package edu.seu.vcampus.client.view.shop;

import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.shop.entity.ShopOrderStatus;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

/** “我的订单”中的待支付、已支付分段切换控件。 */
final class ShopOrderStatusSelector extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final Color IDLE_BACKGROUND = new Color(235, 244, 248);

    interface Listener {
        void onStatusSelected(ShopOrderStatus status);
    }

    private final JToggleButton unpaidButton;
    private final JToggleButton paidButton;

    ShopOrderStatusSelector(final Listener listener) {
        super(new GridLayout(1, 2, 2, 0));
        setBackground(UiTheme.BORDER);
        setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));

        unpaidButton = createButton("待支付", ShopOrderStatus.UNPAID, listener);
        paidButton = createButton("已支付", ShopOrderStatus.PAID, listener);
        ButtonGroup group = new ButtonGroup();
        group.add(unpaidButton);
        group.add(paidButton);
        add(unpaidButton);
        add(paidButton);

        unpaidButton.setSelected(true);
        refreshColors();
    }

    void setControlsEnabled(boolean enabled) {
        unpaidButton.setEnabled(enabled);
        paidButton.setEnabled(enabled);
    }

    JToggleButton getUnpaidButton() {
        return unpaidButton;
    }

    JToggleButton getPaidButton() {
        return paidButton;
    }

    private JToggleButton createButton(String text, final ShopOrderStatus status,
            final Listener listener) {
        JToggleButton button = new JToggleButton(text);
        button.setFocusPainted(false);
        button.setFont(UiTheme.font(Font.BOLD, 13F));
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                refreshColors();
                listener.onStatusSelected(status);
            }
        });
        return button;
    }

    private void refreshColors() {
        style(unpaidButton);
        style(paidButton);
    }

    private static void style(JToggleButton button) {
        button.setForeground(button.isSelected() ? Color.WHITE : UiTheme.NAVY);
        button.setBackground(button.isSelected() ? UiTheme.ACCENT : IDLE_BACKGROUND);
    }
}
