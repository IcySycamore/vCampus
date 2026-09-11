package edu.seu.vcampus.client.auth;

import edu.seu.vcampus.common.entity.Book;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageType;
import java.awt.Component;
import java.awt.Container;
import java.util.Collections;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 界面额度、按钮状态、旧查询响应、借书失败与归还后的刷新测试。 */
class LibraryQuotaPanelTest {
    @ParameterizedTest
    @CsvSource({"学生,3", "教师,5"})
    void countsOnlyActiveLoansAndDisablesAtLimit(String role, int limit) throws Exception {
        try (LibraryUiFixture ui = new LibraryUiFixture(role)) {
            assertState(ui, false, "已借 —/" + limit + " 本");
            ui.reply(ui.query(), MessageType.SUCCESS, LibraryUiFixture.records(limit - 1, 20));
            assertState(ui, true, "已借 " + (limit - 1) + "/" + limit + " 本 · 剩余可借数量：1 本");
            ui.reply(ui.query(), MessageType.SUCCESS, LibraryUiFixture.records(limit, 20));
            assertState(ui, false, "已借 " + limit + "/" + limit + " 本 · 剩余可借数量：0 本");
            ui.reply(ui.query(), MessageType.SUCCESS, LibraryUiFixture.records(limit + 1, 0));
            assertState(ui, false, "剩余可借数量：0 本");
        }
    }

    @Test
    void ignoresOldQuotaResponseAndDisablesOnFailureOrDisconnect() throws Exception {
        try (LibraryUiFixture ui = new LibraryUiFixture("学生")) {
            Message old = ui.query();
            Message current = ui.query();
            ui.reply(current, MessageType.SUCCESS, LibraryUiFixture.records(3, 0));
            ui.reply(old, MessageType.SUCCESS, LibraryUiFixture.records(0, 0));
            assertState(ui, false, "已借 3/3 本");
            LibraryUiFixture.ui(new Runnable() {
                @Override
                public void run() {
                    assertBorrowRows(ui.panel, 3);
                }
            });
            ui.reply(ui.query(), MessageType.SERVER_ERROR, "无法读取记录");
            assertState(ui, false, "待刷新");
            ui.reply(ui.query(), MessageType.SUCCESS, LibraryUiFixture.records(2, 0));
            assertState(ui, true, "已借 2/3 本");
            LibraryUiFixture.ui(new Runnable() {
                @Override
                public void run() {
                    ui.session.connectionClosed(null);
                }
            });
            assertState(ui, false, "登录并连接服务器");
        }
    }

    @Test
    void borrowResponseRefreshesQuotaAndReturnRestoresButton() throws Exception {
        try (LibraryUiFixture ui = new LibraryUiFixture("学生")) {
            ui.reply(ui.query(), MessageType.SUCCESS, LibraryUiFixture.records(2, 0));
            Message search = new Message(MessageType.LIBRARY_SEARCH, null);
            ui.reply(search, MessageType.SUCCESS,
                    Collections.singletonList(new Book("isbn", "Java", "A", "C", 2, 1)));
            LibraryUiFixture.ui(new Runnable() {
                @Override
                public void run() {
                    selectTables(ui.panel);
                    ((JButton) LibraryUiFixture.find(ui.panel, "libraryBorrow")).doClick();
                }
            });
            Message borrow = ui.take(MessageType.LIBRARY_BORROW);
            assertEquals("001", borrow.getSender());
            assertState(ui, false, "待刷新");
            ui.reply(borrow, MessageType.SUCCESS, LibraryUiFixture.records(1, 0).get(0));
            ui.reply(ui.take(MessageType.LIBRARY_LIST_BORROWS), MessageType.SUCCESS,
                    LibraryUiFixture.records(3, 0));
            assertState(ui, false, "已借 3/3 本");
            LibraryUiFixture.ui(new Runnable() {
                @Override
                public void run() {
                    selectTables(ui.panel);
                    clickReturn(ui.panel);
                }
            });
            Message returned = ui.take(MessageType.LIBRARY_RETURN);
            ui.reply(returned, MessageType.SUCCESS, LibraryUiFixture.records(0, 1).get(0));
            ui.reply(ui.take(MessageType.LIBRARY_LIST_BORROWS), MessageType.SUCCESS,
                    LibraryUiFixture.records(2, 1));
            assertState(ui, true, "已借 2/3 本 · 剩余可借数量：1 本");
        }
    }

    @Test
    void rejectedBorrowRefreshesTheQuotaBeforeEnablingAgain() throws Exception {
        try (LibraryUiFixture ui = new LibraryUiFixture("学生")) {
            ui.reply(ui.query(), MessageType.SUCCESS, LibraryUiFixture.records(2, 0));
            Message rejected = new Message(MessageType.LIBRARY_BORROW, "isbn");
            ui.reply(rejected, MessageType.BAD_REQUEST, "该书暂无可借馆藏");
            assertState(ui, false, "待刷新");
            ui.reply(ui.take(MessageType.LIBRARY_LIST_BORROWS), MessageType.SUCCESS,
                    LibraryUiFixture.records(2, 0));
            assertState(ui, true, "剩余可借数量：1 本");
        }
    }

    private void assertState(final LibraryUiFixture ui, final boolean enabled, final String text)
            throws Exception {
        LibraryUiFixture.ui(new Runnable() {
            @Override
            public void run() {
                JButton button = (JButton) LibraryUiFixture.find(ui.panel, "libraryBorrow");
                JLabel label = (JLabel) LibraryUiFixture.find(ui.panel, "libraryQuota");
                assertEquals(enabled, button.isEnabled());
                assertTrue(label.getText().contains(text), label.getText());
            }
        });
    }

    private void assertBorrowRows(Container parent, int rows) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JTable && "记录号".equals(((JTable) child).getColumnName(0))) {
                assertEquals(rows, ((JTable) child).getRowCount());
            } else if (child instanceof Container) {
                assertBorrowRows((Container) child, rows);
            }
        }
    }

    private void selectTables(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JTable && ((JTable) child).getRowCount() > 0) {
                ((JTable) child).setRowSelectionInterval(0, 0);
            } else if (child instanceof Container) {
                selectTables((Container) child);
            }
        }
    }

    private void clickReturn(Container parent) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JButton && "归还所选".equals(((JButton) child).getText())) {
                ((JButton) child).doClick();
            } else if (child instanceof Container) {
                clickReturn((Container) child);
            }
        }
    }
}
