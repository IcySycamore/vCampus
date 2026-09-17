package edu.seu.vcampus.client.view.shop;

import edu.seu.vcampus.common.shop.entity.ShopItem;
import java.awt.GridLayout;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证单店铺商品目录的搜索、分类和响应式列数。 */
class ShopCatalogPanelTest {

    /** 三件不同分类商品应支持实时筛选，并随视口宽度调整列数。 */
    @Test
    void catalogFiltersItemsAndAdaptsColumns() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    ShopPanel panel = new ShopPanel(null);
                    display(panel, Arrays.asList(
                            item("S001", "校园文化衫"),
                            item("S015", "32GB U盘"),
                            item("S025", "洗衣液")));
                    JPanel container = field(panel, "itemsContainer", JPanel.class);
                    JLabel count = field(panel, "countLabel", JLabel.class);
                    JTextField search = field(panel, "searchField", JTextField.class);
                    @SuppressWarnings("unchecked")
                    JComboBox<String> category = field(
                            panel, "categoryFilter", JComboBox.class);

                    assertEquals(3, container.getComponentCount());
                    assertEquals("3 件商品", count.getText());
                    assertTrue(container.getComponent(0) instanceof ShopItemCard);

                    search.setText("U盘");
                    assertEquals("1 件商品", count.getText());
                    search.setText("");
                    category.setSelectedIndex(3);
                    assertEquals("1 件商品", count.getText());

                    JScrollPane scroll = field(panel, "scrollPane", JScrollPane.class);
                    GridLayout grid = field(panel, "itemGrid", GridLayout.class);
                    scroll.getViewport().setSize(1100, 500);
                    updateLayout(panel);
                    assertEquals(4, grid.getColumns());
                    scroll.getViewport().setSize(500, 500);
                    updateLayout(panel);
                    assertEquals(2, grid.getColumns());
                } catch (Exception exception) {
                    throw new AssertionError(exception);
                }
            }
        });
    }

    private static ShopItem item(String id, String name) {
        return new ShopItem(id, name, new BigDecimal("10.00"), Integer.valueOf(20),
                name + "商品描述", "SHOP001");
    }

    private static void display(ShopPanel panel, java.util.List<ShopItem> items)
            throws Exception {
        Method method = ShopPanel.class.getDeclaredMethod("displayItems", java.util.List.class);
        method.setAccessible(true);
        method.invoke(panel, items);
    }

    private static void updateLayout(ShopPanel panel) throws Exception {
        Method method = ShopPanel.class.getDeclaredMethod("updateCatalogLayout");
        method.setAccessible(true);
        method.invoke(panel);
    }

    private static <T> T field(Object source, String name, Class<T> type) throws Exception {
        Field field = source.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(source));
    }
}
