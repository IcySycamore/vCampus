package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.library.LibraryService;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.library.entity.BorrowRecord;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;

/** 展示借阅额度；身份和上限始终读取图书馆 API，不缓存会话字段。 */
final class LibraryQuotaControls {
    final JLabel label = new JLabel();
    final JButton borrowButton = UiFactory.secondaryButton("借阅所选", "borrow");
    private final LibraryService api;
    private boolean loaded;
    private boolean changing;
    private int activeCount;

    LibraryQuotaControls(LibraryService api) {
        this.api = api;
        label.setForeground(UiTheme.NAVY);
        label.setName("libraryQuota");
        borrowButton.setName("libraryBorrow");
        render();
    }

    void loading() {
        loaded = false;
        render();
    }

    void changing(boolean value) {
        changing = value;
        loading();
    }

    void show(List<BorrowRecord> records) {
        activeCount = 0;
        for (BorrowRecord record : records) {
            if (!record.isReturned()) {
                activeCount++;
            }
        }
        loaded = true;
        render();
    }

    boolean canBorrow() {
        return api != null && api.isLoggedIn() && loaded && !changing
                && activeCount < api.borrowLimit();
    }

    private void render() {
        int limit = api == null ? 0 : api.borrowLimit();
        if (api == null || !api.isLoggedIn()) {
            label.setText("登录并连接服务器后查看借阅额度");
        } else if (!loaded) {
            label.setText("已借 —/" + limit + " 本 · 剩余可借数量：待刷新");
        } else {
            label.setText("已借 " + activeCount + "/" + limit + " 本 · 剩余可借数量："
                    + Math.max(0, limit - activeCount) + " 本");
        }
        borrowButton.setEnabled(canBorrow());
        borrowButton.setToolTipText(label.getText());
    }
}
