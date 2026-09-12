package edu.seu.vcampus.common.message;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 分页响应测试：边界值（空集合、越界页号、超限页大小）是分页代码最容易出错的地方。
 */
class PageResponseTest {

    /**
     * 空分页的总页数至少为 1（界面不需要为 0 页做特判）。
     */
    @Test
    void emptyPageHasSingleTotalPage() {
        PageResponse<String> page = PageResponse.empty(1, 20);
        assertEquals(0L, page.getTotal());
        assertNotNull(page.getItems());
        assertEquals(0, page.getItems().size());
        assertEquals(1, page.getTotalPages());
        assertFalse(page.hasNext());
        assertFalse(page.hasPrevious());
    }

    /**
     * 每页条数非法时回落到默认值，页号非法时回落到第 1 页。
     */
    @Test
    void normalizeFallsBackToDefaults() {
        int[] zero = PageResponse.normalize(0, 0);
        assertEquals(1, zero[0]);
        assertEquals(PageResponse.DEFAULT_PAGE_SIZE, zero[1]);

        int[] negative = PageResponse.normalize(-5, -1);
        assertEquals(1, negative[0]);
        assertEquals(PageResponse.DEFAULT_PAGE_SIZE, negative[1]);
    }

    /**
     * 每页条数超过上限时被压到上限（防止客户端一次拖走整张表）。
     */
    @Test
    void normalizeCapsPageSize() {
        int[] capped = PageResponse.normalize(3, 100000);
        assertEquals(3, capped[0]);
        assertEquals(PageResponse.MAX_PAGE_SIZE, capped[1]);
    }

    /**
     * 总页数按向上取整计算，且中间页同时有上一页和下一页。
     */
    @Test
    void middlePageFlags() {
        List<String> items = new ArrayList<String>(Arrays.asList("a", "b"));
        PageResponse<String> page = new PageResponse<String>(items, 25L, 2, 10);
        assertEquals(3, page.getTotalPages());
        assertTrue(page.hasNext());
        assertTrue(page.hasPrevious());
    }

    /**
     * 最后一页没有下一页；正好整除时也不应多出一页。
     */
    @Test
    void lastPageHasNoNext() {
        List<String> items = new ArrayList<String>(Arrays.asList("a"));
        PageResponse<String> last = new PageResponse<String>(items, 20L, 2, 10);
        assertEquals(2, last.getTotalPages());
        assertFalse(last.hasNext());
        assertTrue(last.hasPrevious());

        PageResponse<String> exact = new PageResponse<String>(items, 20L, 2, 10);
        assertFalse(exact.hasNext());
    }

    /**
     * 页号越界（大于总页数）时没有下一页，但仍有上一页，便于界面回退。
     */
    @Test
    void beyondLastPage() {
        PageResponse<String> page = new PageResponse<String>(
                new ArrayList<String>(), 5L, 99, 10);
        assertFalse(page.hasNext());
        assertTrue(page.hasPrevious());
    }

    /**
     * 首屏（第 1 页）没有上一页，但还有下一页。
     */
    @Test
    void firstPageHasNoPrevious() {
        PageResponse<String> page = new PageResponse<String>(
                new ArrayList<String>(), 25L, 1, 10);
        assertFalse(page.hasPrevious());
        assertTrue(page.hasNext());
    }
}
