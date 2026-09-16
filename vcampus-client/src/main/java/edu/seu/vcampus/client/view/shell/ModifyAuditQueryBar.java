package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.student.dto.ModifyRequestQuery;
import edu.seu.vcampus.common.student.entity.ModifyRequestStatus;
import edu.seu.vcampus.common.student.entity.RequestField;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * 修改审核筛选条：搜索字段 / 关键词 / 状态，并把控件状态翻译成 {@link ModifyRequestQuery}。
 *
 * <p>
 * 与学籍那边的 {@link StudentQueryBar} 同一套做法：「控件 → 查询条件」是一段纯映射逻辑，拆出来
 * 才好单测（ADR-0005 不写 GUI 自动化），页面也不必为这些控件占行数。
 *
 * <p>
 * 默认停在「待审核」——这个页面的日常用法就是清待办，看历史申请是偶尔为之，更常用的那一种设为
 * 默认值，少点一次。
 */
class ModifyAuditQueryBar extends JPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 状态下拉的「不过滤」项。 */
    static final String ALL_STATUSES = "全部状态";

    /** 有待审申请时的提示。 */
    private static final String DEFAULT_HINT = "默认只看待审；点表头可按该列排序";

    /** 一条都没有时的提示：区分「没人提」与「这条链路坏了」。 */
    private static final String EMPTY_HINT =
            "没有符合条件的申请：学生在「我的档案」点「申请修改」提交后才会出现在这里";

    /** 搜索字段下拉。 */
    private final JComboBox<String> m_field = new JComboBox<String>();

    /** 关键词输入框。 */
    private final JTextField m_keyword = new JTextField(10);

    /** 状态下拉。 */
    private final JComboBox<String> m_status = new JComboBox<String>();

    /** 就地提示。 */
    private final JLabel m_hint = new JLabel(DEFAULT_HINT);

    /** 查询回调（查询与重置共用）。 */
    private Runnable m_onQuery;

    /** 建筛选条。 */
    ModifyAuditQueryBar() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 6));
        setOpaque(false);
        add(new JLabel("字段"));
        m_field.setModel(new DefaultComboBoxModel<String>(fieldNames()));
        add(m_field);
        add(new JLabel("关键词"));
        add(m_keyword);
        add(new JLabel("状态"));
        m_status.setModel(new DefaultComboBoxModel<String>(new String[] {
                ModifyRequestStatus.PENDING.getDisplayName(), ALL_STATUSES,
                ModifyRequestStatus.APPROVED.getDisplayName(),
                ModifyRequestStatus.REJECTED.getDisplayName() }));
        add(m_status);
        JButton search = UiFactory.primaryButton("查询", "search");
        search.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                notifyQuery();
            }
        });
        add(search);
        JButton reset = new JButton("重置");
        reset.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                clear();
                notifyQuery();
            }
        });
        add(reset);
        m_hint.setForeground(UiTheme.MUTED);
        add(m_hint);
    }

    /**
     * 按当前控件状态组装查询条件。
     *
     * @param pageNumber 页码（从 1 开始）
     * @param pageSize 每页条数
     * @return 查询条件
     */
    ModifyRequestQuery toQuery(int pageNumber, int pageSize) {
        ModifyRequestQuery query = new ModifyRequestQuery();
        query.setKeyword(m_keyword.getText().trim());
        query.setSearchField(fieldOf(m_field.getSelectedItem()));
        Object status = m_status.getSelectedItem();
        if (status != null && !ALL_STATUSES.equals(status)) {
            query.setStatus(ModifyRequestStatus.fromDisplayName(String.valueOf(status)));
        }
        query.setPageNumber(pageNumber);
        query.setPageSize(pageSize);
        return query;
    }

    /**
     * 设置查询回调。
     *
     * @param onQuery 回调
     */
    void setOnQuery(Runnable onQuery) {
        m_onQuery = onQuery;
    }

    /**
     * 按结果是否为空切换提示语。
     *
     * @param empty 当前结果是否为空
     */
    void showEmpty(boolean empty) {
        m_hint.setText(empty ? EMPTY_HINT : DEFAULT_HINT);
    }

    /** 复位：关键词清空、字段回「全部字段」、状态回「待审核」。 */
    void clear() {
        m_keyword.setText("");
        m_field.setSelectedItem(RequestField.ALL.getDisplayName());
        m_status.setSelectedItem(ModifyRequestStatus.PENDING.getDisplayName());
    }

    /** @return 当前关键词文本 */
    String getKeyword() {
        return m_keyword.getText();
    }

    /**
     * 设关键词（供测试）。
     *
     * @param keyword 关键词
     */
    void setKeyword(String keyword) {
        m_keyword.setText(keyword);
    }

    /**
     * 选搜索字段（供测试）。
     *
     * @param displayName 字段显示名
     */
    void selectField(String displayName) {
        m_field.setSelectedItem(displayName);
    }

    /**
     * 选状态（供测试）。
     *
     * @param displayName 状态显示名或「全部状态」
     */
    void selectStatus(String displayName) {
        m_status.setSelectedItem(displayName);
    }

    /** 触发一次查询回调。 */
    private void notifyQuery() {
        if (m_onQuery != null) {
            m_onQuery.run();
        }
    }

    /**
     * 可搜索字段的显示名列表（从枚举取，加字段时下拉自动跟上）。
     *
     * @return 显示名数组
     */
    private static String[] fieldNames() {
        RequestField[] fields = RequestField.searchable();
        String[] names = new String[fields.length];
        int index = 0;
        while (index < fields.length) {
            names[index] = fields[index].getDisplayName();
            index = index + 1;
        }
        return names;
    }

    /**
     * 把下拉项翻译成搜索字段。
     *
     * @param selected 下拉当前项（可为 null）
     * @return 字段；认不出时返回 ALL（当作不过滤列）
     */
    private static RequestField fieldOf(Object selected) {
        if (selected == null) {
            return RequestField.ALL;
        }
        RequestField field = RequestField.fromDisplayName(String.valueOf(selected));
        return field == null ? RequestField.ALL : field;
    }
}
