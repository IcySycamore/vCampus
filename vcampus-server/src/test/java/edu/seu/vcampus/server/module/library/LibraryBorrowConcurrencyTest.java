package edu.seu.vcampus.server.module.library;

import edu.seu.vcampus.common.entity.BorrowRecord;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageType;
import edu.seu.vcampus.server.auth.SessionManager;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 同一 JVM 内多个消息处理器竞争最后一个借阅名额。 */
class LibraryBorrowConcurrencyTest {
    @ParameterizedTest
    @CsvSource({"学生,3", "教师,5"})
    void concurrentRequestsCannotExceedTheLastSlot(String role, final int limit) throws Exception {
        final AtomicInteger active = new AtomicInteger(limit - 1);
        LibraryService service = mock(LibraryService.class);
        when(service.listBorrows("001")).thenAnswer(new Answer<List<BorrowRecord>>() {
            @Override
            public List<BorrowRecord> answer(InvocationOnMock invocation) {
                return LibraryBorrowLimitTest.records(active.get(), 0);
            }
        });
        when(service.borrow("001", "isbn")).thenAnswer(new Answer<BorrowRecord>() {
            @Override
            public BorrowRecord answer(InvocationOnMock invocation) throws Exception {
                Thread.sleep(100);
                active.incrementAndGet();
                return new BorrowRecord();
            }
        });
        SessionManager sessions = new SessionManager();
        String token = sessions.create("uuid", "001", role);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Message> first = pool.submit(borrow(
                    new LibraryMessageHandler(service, sessions), token, start));
            Future<Message> second = pool.submit(borrow(
                    new LibraryMessageHandler(service, sessions), token, start));
            start.countDown();
            String a = first.get(5, TimeUnit.SECONDS).getStatusCode();
            String b = second.get(5, TimeUnit.SECONDS).getStatusCode();
            assertTrue((MessageType.SUCCESS.equals(a) && MessageType.BAD_REQUEST.equals(b))
                    || (MessageType.SUCCESS.equals(b) && MessageType.BAD_REQUEST.equals(a)));
            assertEquals(limit, active.get());
            verify(service).borrow("001", "isbn");
        } finally {
            pool.shutdownNow();
        }
    }

    private Callable<Message> borrow(final LibraryMessageHandler handler, final String token,
            final CountDownLatch start) {
        return new Callable<Message>() {
            @Override
            public Message call() throws Exception {
                start.await();
                Message request = new Message(MessageType.LIBRARY_BORROW, "isbn");
                request.setToken(token);
                return handler.handle(request);
            }
        };
    }
}
