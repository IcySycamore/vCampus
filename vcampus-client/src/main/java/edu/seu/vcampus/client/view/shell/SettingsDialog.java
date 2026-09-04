package edu.seu.vcampus.client.view.shell;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 校园工作台界面设置窗口。
 */
public class SettingsDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final String[] FONT_SIZES = {"标准 100%", "较大 115%", "超大 130%"};
    private final JComboBox<String> fontSize = new JComboBox<String>(FONT_SIZES);

    /**
     * 创建设置窗口。
     *
     * @param owner 父窗口
     */
    public SettingsDialog(Window owner) {
        super(owner, "界面设置", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(createContent());
        setMinimumSize(new Dimension(430, 300));
        pack();
        setLocationRelativeTo(owner);
        selectCurrentScale();
    }

    private JPanel createContent() {
        JPanel root = new JPanel(new BorderLayout(0, 20));
        root.setBackground(UiTheme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        JLabel title = new JLabel("界面设置");
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.font(Font.BOLD, 24F));
        root.add(title, BorderLayout.NORTH);
        RoundedPanel form = new RoundedPanel(new GridBagLayout(), 20, UiTheme.SURFACE);
        form.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        GridBagConstraints grid = new GridBagConstraints();
        grid.insets = new Insets(8, 6, 8, 6);
        grid.anchor = GridBagConstraints.WEST;
        JLabel label = new JLabel("界面字号");
        label.setForeground(UiTheme.NAVY);
        label.setFont(UiTheme.font(Font.BOLD, 14F));
        form.add(label, grid);
        grid.gridx = 1;
        grid.weightx = 1;
        grid.fill = GridBagConstraints.HORIZONTAL;
        form.add(fontSize, grid);
        grid.gridx = 0;
        grid.gridy = 1;
        grid.gridwidth = 2;
        JLabel hint = new JLabel("窗口最大化时仍会在此基础上自动放大");
        hint.setForeground(UiTheme.MUTED);
        form.add(hint, grid);
        root.add(form, BorderLayout.CENTER);
        JButton save = UiFactory.primaryButton("保存设置", "settings");
        save.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                save();
            }
        });
        root.add(save, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(save);
        return root;
    }

    private void selectCurrentScale() {
        float scale = ResponsiveTypography.getUserScale();
        fontSize.setSelectedIndex(scale >= 1.25F ? 2 : scale >= 1.1F ? 1 : 0);
    }

    private void save() {
        float[] scales = {1F, 1.15F, 1.3F};
        ResponsiveTypography.setUserScale(scales[fontSize.getSelectedIndex()]);
        dispose();
    }
}
