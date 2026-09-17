package edu.seu.vcampus.client.view.shop;

import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.shop.entity.ShopItem;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

/**
 * 商品卡片组件。
 */
public class ShopItemCard extends JPanel {

    /**
     * 购买监听器。
     */
    public interface PurchaseListener {
        /**
         * 当用户点击购买按钮时调用。
         *
         * @param item 商品
         * @param quantity 购买数量
         */
        void onPurchase(ShopItem item, int quantity);
    }

    private final ShopItem item;
    private final PurchaseListener listener;
    private final JSpinner quantitySpinner;

    /**
     * 创建商品卡片。
     *
     * @param item 商品信息
     * @param listener 购买监听器
     */
    public ShopItemCard(ShopItem item, PurchaseListener listener) {
        this.item = item;
        this.listener = listener;

        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        setPreferredSize(new Dimension(250, 200));
        setMaximumSize(new Dimension(250, 200));

        // 顶部：商品名称
        JLabel nameLabel = new JLabel(item.getSiName());
        nameLabel.setFont(UiTheme.font(java.awt.Font.BOLD, 16));
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(nameLabel, BorderLayout.NORTH);

        // 中间：商品信息
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JLabel priceLabel = new JLabel("价格: ¥" + item.getSiPrice());
        priceLabel.setFont(UiTheme.font(java.awt.Font.PLAIN, 14));
        priceLabel.setForeground(UiTheme.ACCENT);
        priceLabel.setAlignmentX(CENTER_ALIGNMENT);
        infoPanel.add(priceLabel);

        infoPanel.add(Box.createVerticalStrut(5));

        JLabel stockLabel = new JLabel("库存: " + item.getSiStock());
        stockLabel.setFont(UiTheme.font(java.awt.Font.PLAIN, 12));
        stockLabel.setForeground(UiTheme.MUTED);
        stockLabel.setAlignmentX(CENTER_ALIGNMENT);
        infoPanel.add(stockLabel);

        if (item.getSiDesc() != null && !item.getSiDesc().trim().isEmpty()) {
            infoPanel.add(Box.createVerticalStrut(5));
            JLabel descLabel = new JLabel("<html>" + item.getSiDesc() + "</html>");
            descLabel.setFont(UiTheme.font(java.awt.Font.PLAIN, 12));
            descLabel.setForeground(UiTheme.MUTED);
            descLabel.setAlignmentX(CENTER_ALIGNMENT);
            infoPanel.add(descLabel);
        }

        add(infoPanel, BorderLayout.CENTER);

        // 底部：数量选择和购买按钮
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));

        JLabel quantityLabel = new JLabel("数量:");
        quantityLabel.setFont(UiTheme.font(java.awt.Font.PLAIN, 12));
        actionPanel.add(quantityLabel);

        // 数量选择器（最小1，最大为库存数量）
        int maxQuantity = item.getSiStock() > 0 ? item.getSiStock() : 1;
        quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, maxQuantity, 1));
        quantitySpinner.setPreferredSize(new Dimension(60, 25));
        actionPanel.add(quantitySpinner);

        JButton buyButton = UiFactory.primaryButton("购买", null);
        buyButton.setEnabled(item.getSiStock() > 0);
        final ShopItem finalItem = item;
        final PurchaseListener finalListener = listener;
        buyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (finalListener != null) {
                    int quantity = (Integer) quantitySpinner.getValue();
                    finalListener.onPurchase(finalItem, quantity);
                }
            }
        });
        actionPanel.add(buyButton);

        add(actionPanel, BorderLayout.SOUTH);
    }
}
