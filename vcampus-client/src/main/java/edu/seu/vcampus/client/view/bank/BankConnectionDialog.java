package edu.seu.vcampus.client.view.bank;

import edu.seu.vcampus.client.view.theme.UiTheme;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.Timer;

/** 充值前的两秒连接提示，仅用于界面流程，不连接外部银行。 */
final class BankConnectionDialog {
    private BankConnectionDialog() { }

    static void show(final Window owner, String cardNumber, final Runnable onReady) {
        final JDialog dialog = new JDialog(owner, "银行连接", Dialog.ModalityType.DOCUMENT_MODAL);
        JPanel content = new JPanel(new BorderLayout(0, 20));
        content.setBackground(UiTheme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(26, 28, 26, 28));
        JLabel message = new JLabel("正在获取卡号为 " + cardNumber + " 的银行的连接");
        message.setForeground(UiTheme.TEXT);
        content.add(message, BorderLayout.CENTER);
        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        content.add(progress, BorderLayout.SOUTH);
        dialog.setContentPane(content);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setResizable(false);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        final Timer timer = new Timer(2000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dialog.dispose();
                if (owner.isDisplayable()) { onReady.run(); }
            }
        });
        timer.setRepeats(false);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) { timer.start(); }
            @Override
            public void windowClosed(WindowEvent event) { timer.stop(); }
        });
        dialog.setVisible(true);
    }
}
