package edu.seu.vcampus.common.message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PageResponse 分页语义与参数归一化测试。
 */
class PageResponseTest {

    /** 页码/页长的默认值与上限。 */
    @Test
    void normalizesPagingParameters() {
        assertEquals(1, PageResponse.normalizePageNumber(0));
        assertEquals(1, PageResponse.normalizePageNumber(-5));
        assertEquals(3, PageResponse.normalizePageNumber(3));
        assertEquals(PageResponse.DEFAULT_PAGE_SIZE, PageResponse.normalizePageSize(0));
        assertEquals(PageResponse.DEFAULT_PAGE_SIZE, PageResponse.normalizePageSize(20));
        assertEquals(PageResponse.MAX_PAGE_SIZE, PageResponse.normalizePageSize(999));
    }

    /** 偏移量按归一化后的参数计算。 */
    @Test
    void computesOffset() {
        assertEquals(0, PageResponse.offsetOf(1, 20));
        assertEquals(40, PageResponse.offsetOf(3, 20));
        assertEquals(0, PageResponse.offsetOf(0, 0));
    }

    /** 总页数与下一页判定。 */
    @Test
    void computesTotalPages() {
        PageResponse<String> page = new PageResponse<String>(Arrays.asList("a"), 41L, 2, 20);
        assertEquals(3, page.getTotalPages());
        assertTrue(page.hasNext());
        assertFalse(new PageResponse<String>(Arrays.asList("a"), 20L, 1, 20).hasNext());
    }

    /** 空结果的总页数为 0，且没有下一页。 */
    @Test
    void emptyPageHasNoNext() {
        PageResponse<String> empty = PageResponse.empty();
        assertTrue(empty.isEmpty());
        assertEquals(0, empty.getTotalPages());
        assertFalse(empty.hasNext());
        assertEquals(0L, empty.getTotal());
    }

    /** null 列表与负数总数归一为安全值。 */
    @Test
    void toleratesInvalidInput() {
        PageResponse<String> page = new PageResponse<String>(null, -3L, 1, 20);
        assertTrue(page.getItems().isEmpty());
        assertEquals(0L, page.getTotal());
    }

    /** 记录列表是只读视图，防止调用方改页内数据。 */
    @Test
    void itemsAreUnmodifiable() {
        List<String> source = new ArrayList<String>();
        source.add("a");
        final PageResponse<String> page = new PageResponse<String>(source, 1L, 1, 20);
        source.add("b");// 外部再改不影响快照
        assertEquals(1, page.getItems().size());
        assertThrows(UnsupportedOperationException.class, new Executable() {
            @Override
            public void execute() {
                page.getItems().add("c");
            }
        });
    }
}
