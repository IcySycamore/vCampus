package edu.seu.vcampus.client.view.shop;

import edu.seu.vcampus.client.view.component.RoundedPanel;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiIcons;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.shop.entity.ShopItem;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
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

/**
 * 商品卡片组件。
 */
public class ShopItemCard extends RoundedPanel {

    static final int MINIMUM_WIDTH = 226;
    static final int CARD_HEIGHT = 220;

    private static final long serialVersionUID = 1L;

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
    private final JSpinner quantitySpinner;

    /**
     * 创建商品卡片。
     *
     * @param item 商品信息
     * @param listener 购买监听器
     */
    public ShopItemCard(ShopItem item, PurchaseListener listener) {
        super(new BorderLayout(0, 12), 8, Color.WHITE);
        this.item = item;

        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        setPreferredSize(new Dimension(MINIMUM_WIDTH, CARD_HEIGHT));
        setMinimumSize(new Dimension(MINIMUM_WIDTH, CARD_HEIGHT));

        add(createMetaRow(), BorderLayout.NORTH);
        add(createDetails(), BorderLayout.CENTER);

        JPanel actionPanel = transparent(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JLabel quantityLabel = new JLabel("数量");
        quantityLabel.setFont(UiTheme.font(Font.PLAIN, 12));
        quantityLabel.setForeground(UiTheme.MUTED);
        actionPanel.add(quantityLabel);

        int stock = item.getSiStock() == null ? 0 : item.getSiStock().intValue();
        int maxQuantity = stock > 0 ? stock : 1;
        quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, maxQuantity, 1));
        quantitySpinner.setPreferredSize(new Dimension(58, 34));
        actionPanel.add(quantitySpinner);

        JButton buyButton = UiFactory.primaryButton("加入订单", "shop");
        buyButton.setEnabled(stock > 0);
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

    private JPanel createMetaRow() {
        JPanel row = transparent(new BorderLayout(8, 0));
        Color storeColor = ShopCategoryCatalog.colorFor(item);
        JLabel store = new JLabel(ShopCategoryCatalog.nameFor(item),
                UiIcons.load("shop", 20), JLabel.LEFT);
        store.setIconTextGap(7);
        store.setFont(UiTheme.font(Font.BOLD, 12));
        store.setForeground(storeColor);
        row.add(store, BorderLayout.WEST);

        int stock = item.getSiStock() == null ? 0 : item.getSiStock().intValue();
        JLabel stockLabel = new JLabel(stockText(stock));
        stockLabel.setFont(UiTheme.font(Font.BOLD, 11));
        stockLabel.setForeground(stock <= 5 ? UiTheme.ACCENT : UiTheme.SUCCESS);
        row.add(stockLabel, BorderLayout.EAST);
        return row;
    }

    private JPanel createDetails() {
        JPanel details = transparent();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));

        JLabel name = new JLabel(item.getSiName());
        name.setFont(UiTheme.font(Font.BOLD, 17));
        name.setForeground(UiTheme.TEXT);
        name.setAlignmentX(LEFT_ALIGNMENT);
        details.add(name);
        details.add(Box.createVerticalStrut(7));

        String description = item.getSiDesc() == null ? "" : item.getSiDesc();
        JLabel desc = new JLabel("<html><body style='width:190px'>"
                + description + "</body></html>");
        desc.setFont(UiTheme.font(Font.PLAIN, 12));
        desc.setForeground(UiTheme.MUTED);
        desc.setAlignmentX(LEFT_ALIGNMENT);
        details.add(desc);
        details.add(Box.createVerticalGlue());

        JLabel price = new JLabel("¥" + item.getSiPrice());
        price.setFont(UiTheme.font(Font.BOLD, 21));
        price.setForeground(UiTheme.ACCENT);
        price.setAlignmentX(LEFT_ALIGNMENT);
        details.add(price);
        return details;
    }

    private static String stockText(int stock) {
        if (stock <= 0) {
            return "暂时缺货";
        }
        return stock <= 5 ? "仅剩 " + stock + " 件" : "库存 " + stock;
    }

    private static JPanel transparent() {
        return transparent(new FlowLayout());
    }

    private static JPanel transparent(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }
}
