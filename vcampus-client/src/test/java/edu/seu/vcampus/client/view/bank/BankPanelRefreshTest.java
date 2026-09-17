package edu.seu.vcampus.client.view.bank;

import edu.seu.vcampus.client.bank.BankService;
import edu.seu.vcampus.common.bank.dto.BankAccountResponse;
import edu.seu.vcampus.common.bank.dto.BankTransactionListResponse;
import edu.seu.vcampus.common.bank.dto.BankTransactionQueryRequest;
import edu.seu.vcampus.common.bank.entity.BankAccountStatus;
import edu.seu.vcampus.common.bank.entity.BankTransaction;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 验证外部资金变动触发的银行页面自动刷新。 */
class BankPanelRefreshTest {

    /** 加载期间收到刷新通知时，应在当前余额和流水加载完成后自动补刷。 */
    @Test
    void refreshRequestIsQueuedWhileBankPageIsBusy() throws Exception {
        final BankService api = mock(BankService.class);
        final CountDownLatch accountStarted = new CountDownLatch(1);
        final CountDownLatch releaseAccount = new CountDownLatch(1);
        final CountDownLatch transactionsLoaded = new CountDownLatch(2);
        final BankAccountResponse account = new BankAccountResponse("B-1",
                new BigDecimal("100.00"), BankAccountStatus.NORMAL, null, null);
        final BankTransactionListResponse transactions =
                new BankTransactionListResponse(
                        Collections.<BankTransaction>emptyList(), 1, 20, 0);
        when(api.queryMyAccount()).thenAnswer(new Answer<BankAccountResponse>() {
            @Override
            public BankAccountResponse answer(InvocationOnMock invocation) throws Throwable {
                accountStarted.countDown();
                releaseAccount.await(2, TimeUnit.SECONDS);
                return account;
            }
        });
        when(api.listMyTransactions(any(BankTransactionQueryRequest.class)))
                .thenAnswer(new Answer<BankTransactionListResponse>() {
                    @Override
                    public BankTransactionListResponse answer(InvocationOnMock invocation) {
                        transactionsLoaded.countDown();
                        return transactions;
                    }
                });

        final BankPanel[] panel = new BankPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                panel[0] = new BankPanel(api);
            }
        });
        assertTrue(accountStarted.await(2, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                panel[0].refreshData();
            }
        });
        releaseAccount.countDown();

        assertTrue(transactionsLoaded.await(4, TimeUnit.SECONDS));
    }
}
