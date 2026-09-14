package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.StudentProfile;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTextField;

/**
 * 修改态下那四个控件的持有者：学术方向、入学年份、在校状态、申请理由。
 *
 * <p>
 * <b>不是</b>面板：这些控件由 {@link ProfileRowsPanel} 直接摆进档案表格，原地替换对应的「值」格，
 * 所以这里只负责「持有 + 预填 + 取值」，不参与布局。
 *
 * <p>
 * 与 {@link StudentModifyRequests} 的分工：那边是纯规则（改哪些字段算数、提交前怎么校验），
 * 这边只是界面控件。字段对同包直接可见是有意的——它们本来就要被表格拿去排版，
 * 再绕一层 getter 只会让两边都变长。
 */
final class StudentModifyForm {

    /** 学术方向输入框。 */
    final JTextField field = new JTextField();

    /** 入校年份输入框。 */
    final JTextField year = new JTextField();

    /** 在校状态下拉：显示中文，提交时换成枚举名。 */
    final JComboBox<String> status = new JComboBox<String>();

    /** 申请理由输入框。 */
    final JTextField reason = new JTextField();

    /**
     * 用当前档案预填。
     *
     * @param profile 档案
     */
    void prefill(StudentProfile profile) {
        field.setText(StudentModifyRequests.orEmpty(profile.getField()));
        year.setText(String.valueOf(profile.getJoinYear()));
        status.setModel(new DefaultComboBoxModel<String>(StudentModifyRequests.statusNames()));
        if (profile.getStatus() == null) {
            // 档案没有状态时不预选，否则「没动过」会被当成一次变更。
            status.setSelectedIndex(-1);
        } else {
            status.setSelectedItem(profile.getStatus().getDisplayName());
        }
        reason.setText("");
    }

    /** @return 学术方向文本 */
    String fieldText() {
        return field.getText();
    }

    /** @return 年份文本 */
    String yearText() {
        return year.getText();
    }

    /**
     * 选中的在校状态。
     *
     * @return 枚举名（如 {@code SUSPENDED}）；未选为空串
     */
    String statusName() {
        Object selected = status.getSelectedItem();
        CampusStatus value = StudentModifyRequests.statusOf(
                selected == null ? null : String.valueOf(selected));
        return value == null ? "" : value.name();
    }

    /** @return 申请理由，已去首尾空白 */
    String reason() {
        return reason.getText().trim();
    }
}
