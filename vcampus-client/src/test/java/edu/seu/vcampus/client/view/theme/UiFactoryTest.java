package edu.seu.vcampus.client.view.theme;

import java.awt.Color;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 统一界面组件工厂测试。
 */
class UiFactoryTest {

    @Test
    void createsPrimaryButtonWithBrandStyle() {
        JButton button = UiFactory.primaryButton("保存", "settings");

        assertEquals("保存", button.getText());
        assertEquals(Color.WHITE, button.getForeground());
        assertEquals(UiTheme.ACCENT, button.getBackground());
        assertFalse(button.isFocusPainted());
    }

    @Test
    void stylesTableForSingleSelection() {
        JTable table = new JTable(2, 2);

        UiFactory.styleTable(table);

        assertEquals(42, table.getRowHeight());
        assertFalse(table.getShowVerticalLines());
        assertEquals(ListSelectionModel.SINGLE_SELECTION, table.getSelectionModel()
                .getSelectionMode());
    }
}
