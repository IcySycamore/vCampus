package edu.seu.vcampus.common.bank.dto;

import edu.seu.vcampus.common.bank.entity.BankTransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** BankTransactionQueryRequest 的分页、筛选和序列化测试。 */
class BankTransactionQueryRequestTest {

    @Test
    void suppliesDefaultPagination() {
        BankTransactionQueryRequest request = new BankTransactionQueryRequest();

        assertEquals(1, request.getPageNumber());
        assertEquals(20, request.getPageSize());
        assertNull(request.getType());
    }

    @Test
    void acceptsAndSerializesCustomQuery() throws Exception {
        BankTransactionQueryRequest request = new BankTransactionQueryRequest(
                3, 50, BankTransactionType.CONSUMPTION);

        BankTransactionQueryRequest copy = BankDtoTestSupport.roundTrip(request);

        assertEquals(3, copy.getPageNumber());
        assertEquals(50, copy.getPageSize());
        assertEquals(BankTransactionType.CONSUMPTION, copy.getType());
    }

    @Test
    void rejectsInvalidPagination() {
        assertInvalidQuery(0, 20);
        assertInvalidQuery(1, 0);
        assertInvalidQuery(1, 101);
    }

    private static void assertInvalidQuery(final int pageNumber, final int pageSize) {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                new BankTransactionQueryRequest(pageNumber, pageSize);
            }
        });
    }
}
