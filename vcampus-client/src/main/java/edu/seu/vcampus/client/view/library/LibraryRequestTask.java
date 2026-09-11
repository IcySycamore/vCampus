package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.auth.ClientSession;
import edu.seu.vcampus.common.message.Message;
import java.util.concurrent.ExecutionException;
import javax.swing.JLabel;
import javax.swing.SwingWorker;

/** 在后台发送图书馆请求，网络写入不阻塞界面操作。 */
final class LibraryRequestTask extends SwingWorker<Void, Void> {
    private final ClientSession session;
    private final Message request;
    private final JLabel status;
    private final LibraryQuotaControls quota;

    LibraryRequestTask(ClientSession session, Message request, JLabel status,
            LibraryQuotaControls quota) {
        this.session = session;
        this.request = request;
        this.status = status;
        this.quota = quota;
    }

    @Override
    protected Void doInBackground() throws Exception {
        session.send(request);
        return null;
    }

    @Override
    protected void done() {
        try {
            get();
        } catch (InterruptedException exception) {
            quota.failed(request);
            Thread.currentThread().interrupt();
            status.setText("  请求已取消");
        } catch (ExecutionException exception) {
            quota.failed(request);
            status.setText(session.isAuthenticated()
                    ? "  发送失败，请检查连接后重试" : "  登录已失效，请重新登录");
        }
    }
}
