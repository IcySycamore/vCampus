package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.library.LibraryService;
import edu.seu.vcampus.common.library.LibraryPolicy;
import edu.seu.vcampus.common.library.dto.BookQuery;
import edu.seu.vcampus.common.library.entity.Book;
import edu.seu.vcampus.common.library.entity.BorrowRecord;
import edu.seu.vcampus.common.library.entity.BookReservation;
import edu.seu.vcampus.common.library.entity.PopularBorrow;
import edu.seu.vcampus.common.message.PageResponse;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

/** 页面只依赖业务 API；协议和真实会话在 client.library 测试中验证。 */
final class LibraryUiFixture {
    final LibraryService api = mock(LibraryService.class);
    LibraryPanel panel;

    /** 借阅列表的数据源；测试改这里，不得重新打桩，理由见 {@link #setBorrows}。 */
    private volatile List<BorrowRecord> m_borrows = records(0, 0);

    /** 借阅列表要抛的失败；null 表示正常返回。 */
    private volatile ApiException m_borrowsFailure;

    /** 借阅列表的自定义应答（按调用次序消费，用尽后一直用最后一个）。 */
    private final List<Answer<List<BorrowRecord>>> m_borrowAnswers = new ArrayList<Answer<List<BorrowRecord>>>();

    /** 应答队列的游标。 */
    private final AtomicInteger m_borrowAnswerCursor = new AtomicInteger();

    LibraryUiFixture(String role) throws Exception {
        when(api.isLoggedIn()).thenReturn(true);
        when(api.borrowLimit()).thenReturn(LibraryPolicy.borrowLimit(role));
        when(api.canManageCatalog()).thenReturn(LibraryPolicy.canManage(role));
        when(api.listMyBorrows()).thenAnswer(new Answer<List<BorrowRecord>>() {
            @Override
            public List<BorrowRecord> answer(InvocationOnMock call) throws Throwable {
                return nextBorrows(call);
            }
        });
        when(api.listMyReservations()).thenReturn(Collections.<BookReservation>emptyList());
        when(api.listPopularBorrows()).thenReturn(Collections.<PopularBorrow>emptyList());
        when(api.searchBooks(any(BookQuery.class))).thenReturn(new PageResponse<Book>(
                Collections.singletonList(new Book("9787302423287", "Java", "Author",
                        "计算机", 4, 2)),
                1, 1, 20));
        when(api.searchCatalog(any(BookQuery.class))).thenReturn(PageResponse.<Book>empty());
        ui(new Runnable() {
            @Override
            public void run() {
                panel = new LibraryPanel(api);
            }
        });
    }

    void refresh() throws Exception {
        ui(new Runnable() {
            @Override
            public void run() {
                panel.refresh();
            }
        });
    }

    /**
     * 换掉借阅列表的数据。
     *
     * <p>
     * 必须从这里换，不能在测试里对同一个 mock 重新打桩：面板的刷新跑在后台线程上，而 Mockito 的 「待定应答」是**按 mock 共享**的 —— 后台线程此刻调用 mock
     * 上的任何方法（例如预约面板调 {@code listMyReservations()}），都会把这条待定应答抢走，于是预约面板收到借阅记录、 抛
     * ClassCastException，额度也跟着不刷新。这个竞态只在机器慢到后台线程还没跑完时才现形， 本地跑不出来、CI 上偶发。
     *
     * @param records 借阅记录
     */
    void setBorrows(List<BorrowRecord> records) {
        m_borrowsFailure = null;
        m_borrows = records;
    }

    /**
     * 让借阅列表返回失败。
     *
     * @param error 失败原因
     */
    void failBorrows(ApiException error) {
        m_borrowsFailure = error;
    }

    /**
     * 排队一组自定义应答（用于「先后两次刷新拿到不同结果」这类用例）。
     *
     * @param answers 按调用次序生效的应答；超出次数后一直用最后一个
     */
    @SafeVarargs
    final void queueBorrowAnswers(Answer<List<BorrowRecord>>... answers) {
        synchronized (m_borrowAnswers) {
            m_borrowsFailure = null;
            m_borrowAnswers.clear();
            for (Answer<List<BorrowRecord>> answer : answers) {
                m_borrowAnswers.add(answer);
            }
            m_borrowAnswerCursor.set(0);
        }
    }

    /**
     * 算本次借阅查询的结果。
     *
     * @param call 本次调用
     * @return 借阅记录
     * @throws Throwable 测试要求失败时抛出
     */
    private List<BorrowRecord> nextBorrows(InvocationOnMock call) throws Throwable {
        ApiException failure = m_borrowsFailure;
        if (failure != null) {
            throw failure;
        }
        Answer<List<BorrowRecord>> answer = null;
        synchronized (m_borrowAnswers) {
            if (!m_borrowAnswers.isEmpty()) {
                int last = m_borrowAnswers.size() - 1;
                int index = Math.min(m_borrowAnswerCursor.getAndIncrement(), last);
                answer = m_borrowAnswers.get(index);
            }
        }
        // 应答在锁外执行：其中一个应答可能故意阻塞等测试放行，握着锁会让并发的第二次刷新一起卡住
        return answer == null ? m_borrows : answer.answer(call);
    }

    static List<BorrowRecord> records(int active, int returned) {
        List<BorrowRecord> records = new ArrayList<BorrowRecord>();
        for (int i = 0; i < active + returned; i++) {
            BorrowRecord record = new BorrowRecord("uuid-001", "9787302423287", "Java",
                    new Date(0), new Date(1));
            record.setId((long) i + 1);
            if (i >= active) {
                record.setReturnedAt(new Date());
            }
            records.add(record);
        }
        return records;
    }

    static Component find(Container parent, String name) {
        for (Component child : parent.getComponents()) {
            if (name.equals(child.getName())) {
                return child;
            }
            if (child instanceof Container) {
                Component found = find((Container) child, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    static void ui(Runnable task) throws Exception {
        SwingUtilities.invokeAndWait(task);
    }

    static void await(final Runnable assertion) throws Exception {
        long deadline = System.nanoTime() + 5000000000L;
        final AssertionError[] error = new AssertionError[1];
        do {
            ui(new Runnable() {
                @Override
                public void run() {
                    try {
                        assertion.run();
                        error[0] = null;
                    } catch (AssertionError failure) {
                        error[0] = failure;
                    }
                }
            });
            if (error[0] == null) {
                return;
            }
            Thread.sleep(10);
        } while (System.nanoTime() < deadline);
        throw error[0];
    }
}
