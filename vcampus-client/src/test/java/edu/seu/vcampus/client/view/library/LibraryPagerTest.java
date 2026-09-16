package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.common.message.PageResponse;
import java.util.Collections;
import javax.swing.JButton;
import javax.swing.JLabel;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 分页条的页码、总数与边界按钮测试。 */
class LibraryPagerTest {
    @Test
    void movesOnlyWithinAvailablePages() {
        final int[] reloads = new int[1];
        LibraryPager pager = new LibraryPager("test", new Runnable() {
            @Override
            public void run() {
                reloads[0]++;
            }
        });
        pager.show(new PageResponse<Object>(Collections.emptyList(), 41, 1, 20));
        JButton previous = (JButton) LibraryUiFixture.find(pager, "testPrevious");
        JButton next = (JButton) LibraryUiFixture.find(pager, "testNext");
        JLabel label = (JLabel) LibraryUiFixture.find(pager, "testPage");
        assertFalse(previous.isEnabled());
        assertTrue(next.isEnabled());
        next.doClick();
        assertEquals(2, pager.getPageNumber());
        assertEquals(1, reloads[0]);
        pager.show(new PageResponse<Object>(Collections.emptyList(), 41, 2, 20));
        assertTrue(previous.isEnabled());
        assertTrue(label.getText().contains("共 41 项"));
    }
}
