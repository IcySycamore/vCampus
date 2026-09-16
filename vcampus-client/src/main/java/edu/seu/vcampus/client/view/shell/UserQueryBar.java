package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.common.user.dto.UserQuery;
import edu.seu.vcampus.common.user.entity.Role;

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
 * 用户管理筛选条：关键词 / 角色 / 状态，并把控件状态翻译成 {@link UserQuery}。
 *
 * <p>
 * 「控件 → 查询条件」是一段纯映射逻辑，因此从页面里拆出来单独成类，便于单测； 本类不发任何请求，只把「查询」按钮的点击转成 {@link Runnable} 回调。
 */
public class UserQueryBar extends JPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 角色下拉的「不限」项。 */
    private static final String ALL_ROLES = "全部角色";

    /** 状态下拉的「不限」项。 */
    private static final String ANY_STATE = "全部状态";

    /** 关键词输入框。 */
    private final JTextField m_keyword = new JTextField(12);

    /** 角色过滤。 */
    private final JComboBox<String> m_role = new JComboBox<String>();

    /** 状态过滤。 */
    private final JComboBox<String> m_enabled = new JComboBox<String>();

    /** 查询回调。 */
    private Runnable m_onQuery;

    /**
     * 创建筛选条。
     */
    public UserQueryBar() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 6));
        setOpaque(false);
        add(new JLabel("关键词"));
        add(m_keyword);
        m_role.setModel(new DefaultComboBoxModel<String>(
                new String[] { ALL_ROLES, "学生", "教师", "管理员" }));
        add(m_role);
        m_enabled.setModel(new DefaultComboBoxModel<String>(
                new String[] { ANY_STATE, "启用", "禁用" }));
        add(m_enabled);
        JButton search = UiFactory.primaryButton("查询", "search");
        search.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (m_onQuery != null) {
                    m_onQuery.run();
                }
            }
        });
        add(search);
    }

    /**
     * 按当前控件状态组装查询条件。
     *
     * @param pageNumber 页码（从 1 开始）
     * @param pageSize   每页条数
     * @return 查询条件
     */
    public UserQuery toQuery(int pageNumber, int pageSize) {
        Role role = null;
        Object selectedRole = m_role.getSelectedItem();
        if (selectedRole != null && !ALL_ROLES.equals(selectedRole)) {
            role = Role.fromDisplayName(String.valueOf(selectedRole));
        }
        Boolean enabled = null;
        Object selectedState = m_enabled.getSelectedItem();
        if ("启用".equals(selectedState)) {
            enabled = Boolean.TRUE;
        } else if ("禁用".equals(selectedState)) {
            enabled = Boolean.FALSE;
        }
        return new UserQuery(m_keyword.getText(), role, enabled, pageNumber, pageSize);
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
     * 设置关键词（供调用方在条件变化后重置）。
     *
     * @param keyword 关键词
     */
    public void setKeyword(String keyword) {
        m_keyword.setText(keyword);
    }

    /** @return 当前关键词文本 */
    public String getKeyword() {
        return m_keyword.getText();
    }

    /**
     * 选择角色过滤项（供测试）。
     *
     * @param displayName 角色显示名或「全部角色」
     */
    public void selectRole(String displayName) {
        m_role.setSelectedItem(displayName);
    }

    /**
     * 选择状态过滤项（供测试）。
     *
     * @param state 「全部状态」「启用」或「禁用」
     */
    public void selectState(String state) {
        m_enabled.setSelectedItem(state);
    }
}
