package edu.seu.vcampus.client.view.component;

import edu.seu.vcampus.common.message.PageResponse;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 分页栏测试：只测与页码有关的判断。
 *
 * <p>
 * 不驱动按钮（ADR-0005：不写 GUI 自动化）。两个按钮的可点状态由
 * {@link PageBarPanel#canGoPrevious()} / {@link PageBarPanel#canGoNext()} 推导，
 * 所以测了判定就等于测了按钮状态，不会出现「按钮亮着但点了没反应」。
 */
class PageBarPanelTest {

    @Test
    void startsAtFirstPageWithDefaults() {
        PageBarPanel bar = newBar();

        assertEquals(1, bar.getPageNumber());
        assertEquals(PageResponse.DEFAULT_PAGE_SIZE, bar.getPageSize());
        assertEquals(1, bar.getTotalPages());
        assertFalse(bar.canGoPrevious());
        assertFalse(bar.canGoNext());
    }

    @Test
    void syncTakesPageAndSizeFromServer() {
        PageBarPanel bar = newBar();

        bar.sync(new PageResponse<String>(new ArrayList<String>(), 240L, 3, 50));

        assertEquals(3, bar.getPageNumber());
        assertEquals(50, bar.getPageSize());
        assertEquals(5, bar.getTotalPages());
        assertTrue(bar.canGoPrevious());
        assertTrue(bar.canGoNext());
    }

    @Test
    void cannotGoBeyondLastPage() {
        PageBarPanel bar = newBar();

        bar.sync(new PageResponse<String>(new ArrayList<String>(), 40L, 2, 20));

        assertEquals(2, bar.getTotalPages());
        assertTrue(bar.canGoPrevious());
        assertFalse(bar.canGoNext());
    }

    @Test
    void nullPageLooksLikeEmptyFirstPage() {
        PageBarPanel bar = newBar();

        bar.sync(new PageResponse<String>(new ArrayList<String>(), 40L, 2, 20));
        bar.sync(null);

        assertEquals(1, bar.getPageNumber());
        assertEquals(1, bar.getTotalPages());
        assertFalse(bar.canGoPrevious());
        assertFalse(bar.canGoNext());
    }

    @Test
    void resetToFirstPageKeepsPageSize() {
        PageBarPanel bar = newBar();

        bar.sync(new PageResponse<String>(new ArrayList<String>(), 200L, 4, 50));
        bar.resetToFirstPage();

        assertEquals(1, bar.getPageNumber());
        assertEquals(50, bar.getPageSize());
        assertFalse(bar.canGoPrevious());
        assertTrue(bar.canGoNext());
    }

    @Test
    void rejectsNullCallback() {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                new PageBarPanel(null);
            }
        });
    }

    /** @return 一个回调为空的空分页栏 */
    private static PageBarPanel newBar() {
        return new PageBarPanel(new Runnable() {
            @Override
            public void run() {
            }
        });
    }
}
