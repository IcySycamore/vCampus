package edu.seu.vcampus.client.view.library;

import javax.swing.JTable;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证 ISBN、作者等长文本单元格的自动滚动渲染。 */
class LibraryMarqueeCellRendererTest {
    @Test
    void advancesAndRendersWithTheCurrentTable() {
        LibraryMarqueeCellRenderer renderer = new LibraryMarqueeCellRenderer();
        JTable table = new JTable(1, 1);
        assertSame(renderer, renderer.getTableCellRendererComponent(table,
                "97871115580987654321", false, false, 0, 0));
        assertEquals(0, renderer.offset());
        renderer.advance();
        assertTrue(renderer.offset() > 0);
    }
}
