package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.handler.UIUpdateHandler;
import edu.seu.vcampus.client.auth.ClientSession;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageType;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 * 图书检索、借书、还书及个人借阅界面。
 */
public class LibraryPanel extends JPanel implements UIUpdateHandler {

    private static final long serialVersionUID = 1L;
    private static final int SEARCH = 0;
    private static final int BORROW = 1;
    private static final int REFRESH = 2;
    private static final int RETURN = 3;
    private static final String[] SEARCH_FIELDS = {"all", "title", "author", "category"};
    private final JTextField keywordField = new JTextField(22);
    private final JComboBox<String> fieldBox = new JComboBox<String>(
            new String[] {"全部字段", "书名", "作者", "分类"});
    private final DefaultTableModel bookModel = LibraryTableModels.create(
            new String[] {"ISBN", "书名", "作者", "分类", "可借数量"});
    private final DefaultTableModel borrowModel = LibraryTableModels.create(
            new String[] {"记录号", "书名", "借阅日期", "应还日期", "状态"});
    private final JTable bookTable = new JTable(bookModel);
    private final JTable borrowTable = new JTable(borrowModel);
    private final JLabel statusLabel = new JLabel("  当前为界面预览，服务器连接后即可操作");
    private final LibraryQuotaControls quota = new LibraryQuotaControls();
    private ClientSession session;

    /** 创建离线图书馆页面。 */
    public LibraryPanel() {
        setLayout(new BorderLayout(0, 18));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(30, 34, 26, 34));
        add(LibraryViewBuilder.createHeading(), BorderLayout.NORTH);
        LibraryViewBuilder builder = new LibraryViewBuilder(keywordField, fieldBox,
                bookTable, borrowTable, quota.borrowButton);
        add(builder.createTabs(action(SEARCH), action(BORROW), action(REFRESH),
                action(RETURN)), BorderLayout.CENTER);
        add(LibraryViewBuilder.createFooter(statusLabel, quota.label), BorderLayout.SOUTH);
    }

    /** 绑定登录后复用的客户端会话。
     * @param session 客户端会话
     */
    public void attach(ClientSession session) {
        this.session = session;
        quota.attach(session);
        statusLabel.setText(session != null && session.isAuthenticated() && session.isConnected()
                ? "  已连接图书馆服务" : "  连接尚未建立");
    }

    /** 进入页面时加载馆藏与个人借阅记录。 */
    public void refresh() {
        if (session != null && session.isAuthenticated() && session.isConnected()) {
            search();
            send(MessageType.LIBRARY_LIST_BORROWS, null);
        }
    }
    private ActionListener action(final int action) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (action == SEARCH) {
                    search();
                } else if (action == BORROW) {
                    borrowSelected();
                } else if (action == REFRESH) {
                    send(MessageType.LIBRARY_LIST_BORROWS, null);
                } else {
                    returnSelected();
                }
            }
        };
    }
    private void search() {
        int index = fieldBox.getSelectedIndex();
        send(MessageType.LIBRARY_SEARCH,
                new String[] {keywordField.getText().trim(), SEARCH_FIELDS[index]});
    }

    private void borrowSelected() {
        if (!quota.canBorrow()) {
            statusLabel.setText("  " + quota.label.getText());
            return;
        }
        int row = bookTable.getSelectedRow();
        if (row < 0) {
            statusLabel.setText("  请先选择要借阅的图书");
            return;
        }
        send(MessageType.LIBRARY_BORROW, bookModel.getValueAt(row, 0));
    }

    private void returnSelected() {
        int row = borrowTable.getSelectedRow();
        if (row < 0) {
            statusLabel.setText("  请先选择要归还的记录");
            return;
        }
        send(MessageType.LIBRARY_RETURN, borrowModel.getValueAt(row, 0));
    }

    private void send(int command, Object data) {
        if (session == null || !session.isConnected()) {
            quota.attach(null);
            statusLabel.setText("  服务器端未连接，当前仅可预览界面");
            return;
        }
        if (!session.isAuthenticated()) {
            quota.attach(null);
            statusLabel.setText("  登录已失效，请重新登录");
            return;
        }
        statusLabel.setText("  正在发送请求，请稍候…");
        Message request = new Message(command, data);
        quota.started(request);
        new LibraryRequestTask(session, request, statusLabel, quota).execute();
    }

    @Override
    public void handleMessage(final Message message) {
        if (message == null || message.getCommand() < MessageType.LIBRARY_SEARCH
                || message.getCommand() > MessageType.LIBRARY_RETURN) {
            return;
        }
        runOnUi(new Runnable() {
            @Override
            public void run() {
                applyResponse(message);
            }
        });
    }

    @Override
    public void connectionClosed(final Exception cause) {
        runOnUi(new Runnable() {
            @Override
            public void run() {
                quota.attach(null);
                statusLabel.setText(cause == null ? "  连接已关闭" : "  连接中断，请稍后重试");
            }
        });
    }

    private void applyResponse(Message message) {
        if (message.getCommand() == MessageType.LIBRARY_LIST_BORROWS
                && !MessageType.UNAUTHORIZED.equals(message.getStatusCode())
                && !quota.isCurrentQuery(message)) {
            return;
        }
        quota.received(message);
        if (!MessageType.SUCCESS.equals(message.getStatusCode())) {
            if (!MessageType.UNAUTHORIZED.equals(message.getStatusCode())
                    && (message.getCommand() == MessageType.LIBRARY_BORROW
                    || message.getCommand() == MessageType.LIBRARY_RETURN)) {
                send(MessageType.LIBRARY_LIST_BORROWS, null);
            }
            statusLabel.setText("  " + String.valueOf(message.getData()));
            return;
        }
        if (message.getCommand() == MessageType.LIBRARY_SEARCH) {
            int size = LibraryTableModels.showBooks(bookModel, message.getData());
            statusLabel.setText("  共找到 " + size + " 本图书");
        } else if (message.getCommand() == MessageType.LIBRARY_LIST_BORROWS) {
            LibraryTableModels.showBorrows(borrowModel, message.getData());
            statusLabel.setText("  借阅记录已更新");
        } else {
            statusLabel.setText("  操作成功");
            send(MessageType.LIBRARY_LIST_BORROWS, null);
            search();
        }
    }

    private void runOnUi(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }
}
