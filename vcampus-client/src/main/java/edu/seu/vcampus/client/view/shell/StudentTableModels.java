package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.common.student.entity.StudentField;
import edu.seu.vcampus.common.student.entity.StudentProfile;

import java.util.List;
import javax.swing.table.DefaultTableModel;

/**
 * 学籍表格的列定义与回填：把 {@link StudentProfile} 列表铺进 {@link DefaultTableModel}。
 *
 * <p>
 * 单独成类而不是写在页面里，一是「学籍管理」与将来的其它页面共用同一套列，
 * 二是「实体 → 单元格文本」的转换可以单测，页面本身则薄到不必测（ADR-0005：不写 GUI 自动化）。
 *
 * <p>
 * 单元格一律是纯文本：主键用于「修改状态 / 注销」时定位目标行，故意展示出来，
 * 避免出现「看到一行但不知道该行的主键」的死角。学号单纯给人看（教务对账、口头报号），
 * 不参与定位与任何查询。
 */
final class StudentTableModels {

    /** 表头。最后一列按人员类别区分「专业」与「研究方向」两种叫法。 */
    private static final String[] COLUMNS = { "主键", "学号", "人员类别", "姓名", "专业 / 研究方向",
            "年份", "在校状态" };

    /** 私有构造器，禁止实例化工具类。 */
    private StudentTableModels() {
    }

    /**
     * 建一个只读的表格模型。
     *
     * @return 表格模型
     */
    static DefaultTableModel create() {
        return new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    /**
     * 回填表格。
     *
     * @param model 表格模型
     * @param profiles 学籍列表；null 视为空
     */
    static void fill(DefaultTableModel model, List<StudentProfile> profiles) {
        model.setRowCount(0);
        if (profiles == null) {
            return;
        }
        int index = 0;
        while (index < profiles.size()) {
            model.addRow(rowOf(profiles.get(index)));
            index = index + 1;
        }
    }

    /**
     * 取某一列对应的排序字段，供表头点击排序使用。
     *
     * <p>
     * 返回 null 表示该列不参与排序（这里没有这种列，但接口允许——另一张表的「审核意见」就属于
     * 点不动的列）。映射与 {@link #COLUMNS} 的次序一体：两者必须同时改，否则点「姓名」会按学号排。
     *
     * @param column 视图列下标
     * @return 排序字段；越界返回 null
     */
    static StudentField sortFieldOf(int column) {
        if (column < 0 || column >= COLUMNS.length) {
            return null;
        }
        if (column == 0) {
            return StudentField.PROFILE_ID;
        }
        if (column == 1) {
            return StudentField.STUDENT_NO;
        }
        if (column == 2) {
            return StudentField.CATEGORY;
        }
        if (column == 3) {
            return StudentField.REAL_NAME;
        }
        if (column == 4) {
            return StudentField.FIELD;
        }
        if (column == 5) {
            return StudentField.JOIN_YEAR;
        }
        return StudentField.STATUS;
    }

    /**
     * 把一条学籍转成一行单元格。
     *
     * @param profile 学籍
     * @return 单元格数组
     */
    private static Object[] rowOf(StudentProfile profile) {
        return new Object[] { idText(profile), orDash(profile.getStudentNo()), categoryText(profile),
                orDash(profile.getRealName()), orDash(profile.getField()),
                String.valueOf(profile.getJoinYear()), statusText(profile) };
    }

    /**
     * 主键文本。
     *
     * @param profile 学籍
     * @return 主键；缺主键时给占位符（尚未落库的档案）
     */
    private static String idText(StudentProfile profile) {
        return profile.getId() == null ? "-" : profile.getId().toString();
    }

    /**
     * 人员类别文本。
     *
     * @param profile 学籍
     * @return 类别显示名；实体把未标记的档案已按学生归一，这里取不到 null
     */
    private static String categoryText(StudentProfile profile) {
        return profile.getPersonCategory().getDisplayName();
    }

    /**
     * 在校状态文本。
     *
     * @param profile 学籍
     * @return 状态显示名
     */
    private static String statusText(StudentProfile profile) {
        return profile.getStatus() == null ? "-" : profile.getStatus().getDisplayName();
    }

    /**
     * 空值统一显示成短横线，避免表格里出现空洞。
     *
     * @param value 原值
     * @return 原值；空白时为 "-"
     */
    private static String orDash(String value) {
        return value == null || value.trim().length() == 0 ? "-" : value;
    }
}
