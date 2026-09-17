package edu.seu.vcampus.common.library.dto;

import edu.seu.vcampus.common.message.PageResponse;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 图书查询 DTO 的默认值与分页复制测试。 */
class BookQueryTest {
    @Test
    void defaultsAndNormalizesPagination() {
        BookQuery query = new BookQuery(null, null, 0, 1000);
        assertNull(query.getKeyword());
        assertNull(query.getField());
        assertEquals(1, query.getPageNumber());
        assertEquals(PageResponse.MAX_PAGE_SIZE, query.getPageSize());
    }

    @Test
    void pageCopyKeepsFiltersAndSize() {
        BookQuery query = new BookQuery(" Java ", "title", 1, 30).withPageNumber(3);
        assertEquals(" Java ", query.getKeyword());
        assertEquals("title", query.getField());
        assertEquals(3, query.getPageNumber());
        assertEquals(30, query.getPageSize());
    }
}
