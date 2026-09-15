package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.common.constant.StatusCode;
import edu.seu.vcampus.common.library.entity.BorrowRecord;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

/** 验证额度展示、刷新失败和乱序后台结果，不依赖旧会话或消息回调。 */
class LibraryQuotaPanelTest {
    @Test
    void searchFieldsIncludeIsbn() throws Exception {
        LibraryUiFixture fixture = new LibraryUiFixture("学生");
        JComboBox<?> fields = (JComboBox<?>) LibraryUiFixture.find(
                fixture.panel, "librarySearchField");
        assertEquals("ISBN", fields.getItemAt(fields.getItemCount() - 1));
    }

    @ParameterizedTest
    @CsvSource({"学生,30", "student,30", "STUDENT,30",
        "教师,30", "teacher,30", "TEACHER,30"})
    void countsOnlyActiveLoansAndDisablesAtLimit(String role, int limit) throws Exception {
        final LibraryUiFixture fixture = new LibraryUiFixture(role);
        doReturn(LibraryUiFixture.records(limit - 1, 20))
                .when(fixture.api).listMyBorrows();
        fixture.refresh();
        state(fixture, true, "剩余可借数量：1 本");
        doReturn(LibraryUiFixture.records(limit, 20))
                .when(fixture.api).listMyBorrows();
        fixture.refresh();
        state(fixture, false, "已借 " + limit + "/" + limit + " 本");
    }

    @Test
    void failedRefreshDoesNotReusePreviouslyLoadedQuota() throws Exception {
        final LibraryUiFixture fixture = new LibraryUiFixture("学生");
        fixture.refresh();
        state(fixture, true, "剩余可借数量：30 本");
        doThrow(new ApiException(StatusCode.INTERNAL_ERROR))
                .when(fixture.api).listMyBorrows();
        fixture.refresh();
        state(fixture, false, "待刷新");
    }

    @Test
    void olderBackgroundResultCannotOverwriteANewerFullQuota() throws Exception {
        final LibraryUiFixture fixture = new LibraryUiFixture("学生");
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        doAnswer(new Answer<List<BorrowRecord>>() {
            @Override
            public List<BorrowRecord> answer(InvocationOnMock call) throws Exception {
                entered.countDown();
                assertTrue(release.await(5, TimeUnit.SECONDS));
                return LibraryUiFixture.records(0, 0);
            }
        }).doReturn(LibraryUiFixture.records(30, 0))
                .when(fixture.api).listMyBorrows();
        try {
            fixture.refresh();
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            fixture.refresh();
            state(fixture, false, "已借 30/30 本");
        } finally {
            release.countDown();
        }
        state(fixture, false, "已借 30/30 本");
    }

    private void state(final LibraryUiFixture fixture, final boolean enabled, final String text)
            throws Exception {
        LibraryUiFixture.await(new Runnable() {
            @Override
            public void run() {
                JButton button = (JButton) LibraryUiFixture.find(fixture.panel, "libraryBorrow");
                JLabel label = (JLabel) LibraryUiFixture.find(fixture.panel, "libraryQuota");
                assertEquals(enabled, button.isEnabled());
                assertTrue(label.getText().contains(text), label.getText());
            }
        });
    }
}
