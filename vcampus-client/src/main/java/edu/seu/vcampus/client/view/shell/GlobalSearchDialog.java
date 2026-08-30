package edu.seu.vcampus.client.view.shell;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * 在校园工作台中检索并打开校园功能。
 */
public class GlobalSearchDialog extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final Map<String, String> PAGES = createPages();
    private final JTextField query = new JTextField(24);
    private final DefaultListModel<String> model = new DefaultListModel<String>();
    private final JList<String> results = new JList<String>(model);
    private final StringHandler navigator;

    /**
     * 创建全局搜索窗口。
     *
     * @param owner 父窗口
     * @param initialQuery 初始关键词
     * @param navigator 页面跳转回调
     */
    public GlobalSearchDialog(Window owner, String initialQuery, StringHandler navigator) {
        super(owner, "全局搜索", ModalityType.APPLICATION_MODAL);
        this.navigator = navigator;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(createContent());
        setMinimumSize(new Dimension(520, 430));
        query.setText(initialQuery == null ? "" : initialQuery.trim());
        refreshResults();
        pack();
        setLocationRelativeTo(owner);
    }

    private JPanel createContent() {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(UiTheme.BACKGROUND);
        root.setBorder(BorderFactory.createEmptyBorder(26, 30, 26, 30));
        JLabel title = new JLabel("搜索校园功能");
        title.setForeground(UiTheme.TEXT);
        title.setFont(UiTheme.font(Font.BOLD, 23F));
        root.add(title, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        query.setPreferredSize(new Dimension(420, 42));
        query.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        query.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                refreshResults();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                refreshResults();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                refreshResults();
            }
        });
        center.add(query, BorderLayout.NORTH);
        results.setFont(UiTheme.font(Font.PLAIN, 15F));
        results.setFixedCellHeight(42);
        results.setSelectionBackground(new java.awt.Color(218, 237, 246));
        results.setSelectionForeground(UiTheme.NAVY);
        results.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2) {
                    openSelected();
                }
            }
        });
        JScrollPane scroll = new JScrollPane(results);
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        center.add(scroll, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);
        JButton open = UiFactory.primaryButton("打开所选功能", "search");
        open.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                openSelected();
            }
        });
        root.add(open, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(open);
        return root;
    }

    private void refreshResults() {
        if (model == null) {
            return;
        }
        String keyword = query.getText().trim().toLowerCase();
        model.clear();
        for (String title : PAGES.keySet()) {
            if (keyword.length() == 0 || title.toLowerCase().contains(keyword)) {
                model.addElement(title);
            }
        }
        if (!model.isEmpty()) {
            results.setSelectedIndex(0);
        }
    }

    private void openSelected() {
        String selected = results.getSelectedValue();
        if (selected != null) {
            navigator.handle(PAGES.get(selected));
            dispose();
        }
    }

    private static Map<String, String> createPages() {
        Map<String, String> pages = new LinkedHashMap<String, String>();
        pages.put("校园工作台 · 待办、公告、日程", MainContentPanel.HOME);
        pages.put("用户中心 · 资料、密码与身份信息", MainContentPanel.USER);
        pages.put("学生学籍 · 个人信息与学籍状态", MainContentPanel.STUDENT);
        pages.put("选课与成绩 · 课程安排与学习成果", MainContentPanel.COURSE);
        pages.put("智慧图书馆 · 检索、借阅与归还", MainContentPanel.LIBRARY);
        pages.put("校园商店 · 商品与订单", MainContentPanel.SHOP);
        pages.put("校园银行 · 余额与消费流水", MainContentPanel.BANK);
        return pages;
    }
}
