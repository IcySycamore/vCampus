package edu.seu.vcampus.common.bank.dto;

import edu.seu.vcampus.common.message.PageResponse;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** BankAdminQuery的分页归一化与序列化测试。 */
class BankAdminQueryTest {

    /** 默认条件使用共享分页默认值。 */
    @Test
    void defaultsToSharedPagingDefaults() {
        BankAdminQuery query = new BankAdminQuery();
        assertNull(query.getKeyword());
        assertEquals(PageResponse.DEFAULT_PAGE_NUMBER, query.getPageNumber());
        assertEquals(PageResponse.DEFAULT_PAGE_SIZE, query.getPageSize());
    }

    /** 越界分页被归一化而不是抛异常（与 UserQuery一致）。 */
    @Test
    void normalizesOutOfRangePaging() {
        BankAdminQuery query = new BankAdminQuery("zhao", 0, 100000);
        assertEquals(PageResponse.DEFAULT_PAGE_NUMBER, query.getPageNumber());
        assertEquals(PageResponse.MAX_PAGE_SIZE, query.getPageSize());
    }

    /** 序列化往返保留全部条件。 */
    @Test
    void roundTripsCondition() throws IOException, ClassNotFoundException {
        BankAdminQuery copy = BankDtoTestSupport.roundTrip(new BankAdminQuery("zhao", 3, 5));
        assertEquals("zhao", copy.getKeyword());
        assertEquals(3, copy.getPageNumber());
        assertEquals(5, copy.getPageSize());
    }
}
