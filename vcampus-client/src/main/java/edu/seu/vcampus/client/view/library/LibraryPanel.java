package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.handler.UIUpdateHandler;
import edu.seu.vcampus.client.network.ClientSocket;
import edu.seu.vcampus.client.view.shell.UiTheme;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageType;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
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
    private ClientSocket client;
    private String userId;

    /** 创建离线图书馆页面。 */
    public LibraryPanel() {
        setLayout(new BorderLayout(0, 18));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(30, 34, 26, 34));
        add(createHeading(), BorderLayout.NORTH);
        LibraryViewBuilder builder = new LibraryViewBuilder(keywordField, fieldBox,
                bookTable, borrowTable);
        add(builder.createTabs(event -> search(), event -> borrowSelected(),
                event -> send(MessageType.LIBRARY_LIST_BORROWS, null),
                event -> returnSelected()), BorderLayout.CENTER);
        styleStatus();
        add(statusLabel, BorderLayout.SOUTH);
    }

    /**
     * 绑定已创建的客户端连接。
     *
     * @param client 客户端连接
     * @param userId 当前用户 ID
     */
    public void attach(ClientSocket client, String userId) {
        this.client = client;
        this.userId = userId;
        statusLabel.setText(client != null && client.isConnected()
                ? "  已连接图书馆服务" : "  连接尚未建立");
    }

    private JPanel createHeading() {
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JPanel text = new JPanel(new BorderLayout(0, 5));
        text.setOpaque(false);
        JLabel title = new JLabel("智慧图书馆");
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.font(Font.BOLD, 28F));
        JLabel subtitle = new JLabel("发现好书，管理你的每一次阅读");
        subtitle.setForeground(UiTheme.MUTED);
        subtitle.setFont(UiTheme.font(Font.PLAIN, 15F));
        text.add(title, BorderLayout.NORTH);
        text.add(subtitle, BorderLayout.SOUTH);
        heading.add(text, BorderLayout.WEST);
        return heading;
    }

    private void styleStatus() {
        statusLabel.setOpaque(true);
        statusLabel.setForeground(UiTheme.MUTED);
        statusLabel.setBackground(new Color(234, 241, 245));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(9, 10, 9, 10));
    }

    private void search() {
        int index = fieldBox.getSelectedIndex();
        send(MessageType.LIBRARY_SEARCH,
                new String[] {keywordField.getText().trim(), SEARCH_FIELDS[index]});
    }

    private void borrowSelected() {
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
        if (client == null || !client.isConnected()) {
            statusLabel.setText("  服务器端未连接，当前仅可预览界面");
            return;
        }
        Message request = new Message(command, data);
        request.setSender(userId);
        try {
            client.send(request);
            statusLabel.setText("  请求已发送，请稍候…");
        } catch (IOException exception) {
            statusLabel.setText("  发送失败：" + exception.getMessage());
        }
    }

    @Override
    public void handleMessage(final Message message) {
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
                statusLabel.setText(cause == null ? "  连接已关闭" : "  连接中断，请稍后重试");
            }
        });
    }

    private void applyResponse(Message message) {
        if (!MessageType.SUCCESS.equals(message.getStatusCode())) {
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
