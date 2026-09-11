package edu.seu.vcampus.common.bank.dto;

import edu.seu.vcampus.common.bank.entity.BankTransaction;
import edu.seu.vcampus.common.bank.entity.BankTransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** BankTransactionListResponse 的分页、封装和序列化测试。 */
class BankTransactionListResponseTest {

    @Test
    void copiesListAndCalculatesTotalPages() {
        List<BankTransaction> source = new ArrayList<BankTransaction>();
        source.add(transaction("T001"));
        final BankTransactionListResponse response = new BankTransactionListResponse(
                source, 2, 10, 21);

        source.clear();

        assertEquals(1, response.getTransactions().size());
        assertEquals(2, response.getPageNumber());
        assertEquals(10, response.getPageSize());
        assertEquals(21, response.getTotalCount());
        assertEquals(3, response.getTotalPages());
        assertThrows(UnsupportedOperationException.class, new Executable() {
            @Override
            public void execute() {
                response.getTransactions().add(transaction("T002"));
            }
        });
    }

    @Test
    void serializesTransactionsAndPagination() throws Exception {
        BankTransactionListResponse response = new BankTransactionListResponse(
                Arrays.asList(transaction("T001")), 1, 20, 1);

        BankTransactionListResponse copy = BankDtoTestSupport.roundTrip(response);

        assertEquals("T001", copy.getTransactions().get(0).getTransactionId());
        assertEquals(1, copy.getTotalCount());
        assertEquals(1, copy.getTotalPages());
    }

    @Test
    void emptyResultHasZeroPages() {
        BankTransactionListResponse response = new BankTransactionListResponse(
                new ArrayList<BankTransaction>(), 1, 20, 0);

        assertEquals(0, response.getTotalPages());
    }

    @Test
    void rejectsInvalidListAndPagination() {
        assertInvalid(null, 1, 20, 0);
        assertInvalid(Arrays.asList(transaction("T001")), 0, 20, 1);
        assertInvalid(Arrays.asList(transaction("T001")), 1, 0, 1);
        assertInvalid(Arrays.asList(transaction("T001")), 1, 101, 1);
        assertInvalid(Arrays.asList(transaction("T001")), 1, 20, 0);
        assertInvalid(Arrays.asList(transaction("T001"), null), 1, 20, 2);
        assertInvalid(Arrays.asList(transaction("T001"), transaction("T002")),
                1, 1, 2);
    }

    private static void assertInvalid(final List<BankTransaction> transactions,
            final int pageNumber, final int pageSize, final long totalCount) {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                new BankTransactionListResponse(transactions, pageNumber,
                        pageSize, totalCount);
            }
        });
    }

    private static BankTransaction transaction(String transactionId) {
        return new BankTransaction(transactionId, "A001", BankTransactionType.RECHARGE,
                BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE,
                null, "充值", new Date(1000L));
    }
}
