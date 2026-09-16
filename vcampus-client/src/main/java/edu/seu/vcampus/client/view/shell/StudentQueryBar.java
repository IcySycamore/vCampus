package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.student.dto.StudentQuery;
import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.PersonCategory;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * 学籍管理筛选条：关键词 / 人员类别 / 在校状态，并把控件状态翻译成 {@link StudentQuery}。
 *
 * <p>
 * 与用户管理的 {@link UserQueryBar} 同一套做法：「控件 → 查询条件」是一段纯映射逻辑，从页面里
 * 拆出来单独成类才好单测（ADR-0005 不写 GUI 自动化）。本类不发任何请求，只把按钮点击转成回调，
 * 页码与每页条数由调用方（列表页）传进来，本类不持有分页状态。
 *
 * <p>
 * <b>关键词是「多字段模糊匹配」而不是「选一个字段再填值」</b>：教务念着学号、姓名、账户或专业
 * 来找人都是同一件事，真让他先选字段只会多一次点击；搜索范围在界面上写明（见 {@code HINT}），
 * 用户不用猜。年份也在比对范围内——「2026 级的有哪些人」是很常见的问法。
 */
public class StudentQueryBar extends JPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 类别下拉的「不过滤」项。 */
    static final String ALL_CATEGORIES = "全部类别";

    /** 状态下拉的「不过滤」项。 */
    static final String ALL_STATUSES = "全部状态";

    /** 关键词覆盖范围的说明：写在界面上，用户不必猜能搜什么。 */
    private static final String HINT = "关键词可匹配学号 / 姓名 / 账户 / 专业·方向 / 入校年份";

    /** 关键词输入框。 */
    private final JTextField m_keyword = new JTextField(12);

    /** 人员类别下拉。 */
    private final JComboBox<String> m_category = new JComboBox<String>();

    /** 在校状态下拉。 */
    private final JComboBox<String> m_status = new JComboBox<String>();

    /** 查询回调。 */
    private Runnable m_onQuery;

    /** 重置回调。 */
    private Runnable m_onReset;

    /**
     * 创建筛选条。
     */
    public StudentQueryBar() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 6));
        setOpaque(false);
        add(new JLabel("关键词"));
        add(m_keyword);
        m_category.setModel(new DefaultComboBoxModel<String>(new String[] { ALL_CATEGORIES,
                PersonCategory.STUDENT.getDisplayName(), PersonCategory.TEACHER.getDisplayName() }));
        add(m_category);
        m_status.setModel(new DefaultComboBoxModel<String>(statusNames()));
        add(m_status);
        add(primary("查询", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (m_onQuery != null) {
                    m_onQuery.run();
                }
            }
        }));
        JButton reset = new JButton("重置");
        reset.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                clear();
                if (m_onReset != null) {
                    m_onReset.run();
                }
            }
        });
        add(reset);
        JLabel hint = new JLabel(HINT);
        hint.setForeground(UiTheme.MUTED);
        hint.setFont(UiTheme.font(Font.PLAIN, 12F));
        add(hint);
    }

    /**
     * 按当前控件状态组装查询条件。
     *
     * <p>
     * 「全部类别 / 全部状态」翻译成 null 而不是枚举里的某个值：null 才是「不过滤」，随便挑一个
     * 具体值当「全部」用，会在接口层悄悄少查一半记录。
     *
     * @param pageNumber 页码（从 1 开始）
     * @param pageSize   每页条数
     * @return 查询条件
     */
    public StudentQuery toQuery(int pageNumber, int pageSize) {
        StudentQuery query = new StudentQuery();
        query.setKeyword(m_keyword.getText().trim());
        Object category = m_category.getSelectedItem();
        if (category != null && !ALL_CATEGORIES.equals(category)) {
            query.setPersonCategory(PersonCategory.fromDisplayName(String.valueOf(category)));
        }
        Object status = m_status.getSelectedItem();
        if (status != null && !ALL_STATUSES.equals(status)) {
            query.setStatus(CampusStatus.fromDisplayName(String.valueOf(status)));
        }
        query.setPageNumber(pageNumber);
        query.setPageSize(pageSize);
        return query;
    }

    /**
     * 把三个控件复位到「不过滤」。
     */
    public void clear() {
        m_keyword.setText("");
        m_category.setSelectedItem(ALL_CATEGORIES);
        m_status.setSelectedItem(ALL_STATUSES);
    }

    /**
     * 设置查询按钮的回调。
     *
     * @param onQuery 查询回调
     */
    public void setOnQuery(Runnable onQuery) {
        m_onQuery = onQuery;
    }

    /**
     * 设置重置按钮的回调。
     *
     * @param onReset 重置回调
     */
    public void setOnReset(Runnable onReset) {
        m_onReset = onReset;
    }

    /** @return 当前关键词文本 */
    public String getKeyword() {
        return m_keyword.getText();
    }

    /**
     * 设置关键词（供调用方或测试改条件）。
     *
     * @param keyword 关键词
     */
    public void setKeyword(String keyword) {
        m_keyword.setText(keyword);
    }

    /**
     * 选择人员类别（供测试）。
     *
     * @param displayName 类别显示名或「全部类别」
     */
    public void selectCategory(String displayName) {
        m_category.setSelectedItem(displayName);
    }

    /**
     * 选择在校状态（供测试）。
     *
     * @param displayName 状态显示名或「全部状态」
     */
    public void selectStatus(String displayName) {
        m_status.setSelectedItem(displayName);
    }

    /**
     * 状态下拉的全部选项：「全部状态」+ 各状态的显示名。
     *
     * <p>
     * 逐个从枚举取值而不是手写字符串数组：状态增删时这里自动跟上，不会出现「枚举加了但下拉里没有」。
     *
     * @return 显示名数组
     */
    private static String[] statusNames() {
        CampusStatus[] all = CampusStatus.values();
        String[] names = new String[all.length + 1];
        names[0] = ALL_STATUSES;
        int index = 0;
        while (index < all.length) {
            names[index + 1] = all[index].getDisplayName();
            index = index + 1;
        }
        return names;
    }

    /**
     * 造一个主按钮。
     *
     * @param text 文案
     * @param listener 点击回调
     * @return 按钮
     */
    private static JButton primary(String text, ActionListener listener) {
        JButton button = UiFactory.primaryButton(text, "search");
        button.addActionListener(listener);
        return button;
    }
}
