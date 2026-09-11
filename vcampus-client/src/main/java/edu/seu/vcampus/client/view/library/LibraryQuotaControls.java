package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.auth.ClientSession;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.entity.BorrowRecord;
import edu.seu.vcampus.common.message.Message;
import edu.seu.vcampus.common.message.MessageType;
import edu.seu.vcampus.common.user.Role;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;

/** 根据登录身份和服务器借阅记录展示额度，服务器仍负责最终校验。 */
final class LibraryQuotaControls {
    final JLabel label = new JLabel();
    final JButton borrowButton = UiFactory.secondaryButton("借阅所选", "borrow");
    private ClientSession session;
    private Message latestQuery;
    private int pendingChanges;
    private int activeCount;
    private int limit;
    private boolean loaded;

    LibraryQuotaControls() {
        label.setForeground(UiTheme.NAVY);
        label.setName("libraryQuota");
        borrowButton.setName("libraryBorrow");
        render();
    }

    void attach(ClientSession value) {
        session = value;
        String role = value == null ? null : value.getRole();
        limit = Role.STUDENT.getDisplayName().equals(role) ? 3
                : Role.TEACHER.getDisplayName().equals(role) ? 5 : 10;
        latestQuery = null;
        pendingChanges = 0;
        loaded = false;
        render();
    }

    void started(Message request) {
        if (request.getCommand() == MessageType.LIBRARY_LIST_BORROWS) {
            latestQuery = request;
            loaded = false;
        } else if (isChange(request)) {
            pendingChanges++;
            latestQuery = null;
            loaded = false;
        }
        render();
    }

    void received(Message response) {
        if (MessageType.UNAUTHORIZED.equals(response.getStatusCode())) {
            attach(null);
            return;
        }
        if (isChange(response)) {
            pendingChanges = Math.max(0, pendingChanges - 1);
            loaded = false;
        } else if (response.getCommand() == MessageType.LIBRARY_LIST_BORROWS
                && isCurrentQuery(response)) {
            loaded = false;
            if (MessageType.SUCCESS.equals(response.getStatusCode()) && pendingChanges == 0) {
                activeCount = 0;
                for (Object value : (List<?>) response.getData()) {
                    if (!((BorrowRecord) value).isReturned()) {
                        activeCount++;
                    }
                }
                loaded = true;
            }
        }
        render();
    }

    void failed(Message request) {
        if (isChange(request)) {
            pendingChanges = Math.max(0, pendingChanges - 1);
            loaded = false;
        } else if (request == latestQuery) {
            latestQuery = null;
            loaded = false;
        }
        render();
    }

    boolean canBorrow() {
        return connected() && loaded && pendingChanges == 0 && activeCount < limit;
    }

    boolean isCurrentQuery(Message response) {
        return latestQuery != null && response.getUid() != null
                && response.getUid().equals(latestQuery.getUid());
    }

    private boolean connected() {
        return session != null && session.isAuthenticated() && session.isConnected();
    }

    private boolean isChange(Message message) {
        return message.getCommand() == MessageType.LIBRARY_BORROW
                || message.getCommand() == MessageType.LIBRARY_RETURN;
    }

    private void render() {
        if (!connected()) {
            label.setText("登录并连接服务器后查看借阅额度");
        } else if (!loaded) {
            label.setText("已借 —/" + limit + " 本 · 剩余可借数量：待刷新");
        } else {
            label.setText("已借 " + activeCount + "/" + limit + " 本 · 剩余可借数量："
                    + Math.max(0, limit - activeCount) + " 本");
        }
        borrowButton.setEnabled(canBorrow());
        borrowButton.setToolTipText(canBorrow() ? "借阅所选图书" : label.getText());
    }
}
