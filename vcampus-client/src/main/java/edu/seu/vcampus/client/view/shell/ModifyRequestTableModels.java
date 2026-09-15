package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.common.student.entity.StudentModifyRequest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.swing.table.DefaultTableModel;

/**
 * 学籍修改申请表格的列定义与回填。
 *
 * <p>
 * 与 {@link StudentTableModels} 分开：申请单是「行为记录」，列与学籍档案完全不同，
 * 混在一个类里会让「改了一处列宽、动了另一张表」这种意外变得可能。
 *
 * <p>
 * 申请人一列展示账户 uuid：学籍模块只存 uuid，姓名归用户模块维护，而 207 的响应里
 * 不带姓名（不像 201/208 会联查补名）。要让这里显示姓名，需要在服务端给申请单也做一次
 * 姓名补齐——属服务端改动，暂不在本次范围。
 */
final class ModifyRequestTableModels {

    /** 表头。 */
    private static final String[] COLUMNS = { "申请单号", "学籍主键", "申请人", "变更内容", "理由",
            "状态", "申请时间" };

    /** 私有构造器，禁止实例化工具类。 */
    private ModifyRequestTableModels() {
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
     * @param requests 申请单列表；null 视为空
     */
    static void fill(DefaultTableModel model, List<StudentModifyRequest> requests) {
        model.setRowCount(0);
        if (requests == null) {
            return;
        }
        int index = 0;
        while (index < requests.size()) {
            model.addRow(rowOf(requests.get(index)));
            index = index + 1;
        }
    }

    /**
     * 把一条申请单转成一行单元格。
     *
     * @param request 申请单
     * @return 单元格数组
     */
    private static Object[] rowOf(StudentModifyRequest request) {
        return new Object[] { idText(request.getRequestId()), idText(request.getProfileId()),
                orDash(request.getApplicantUuid()), orDash(request.getChangesJson()),
                orDash(request.getReason()),
                request.getStatus() == null ? "-" : request.getStatus().getDisplayName(),
                timeText(request.getAppliedAt()) };
    }

    /**
     * 主键文本。
     *
     * @param id 主键
     * @return 主键文本；为 null 时给占位符
     */
    private static String idText(Long id) {
        return id == null ? "-" : id.toString();
    }

    /**
     * 时间戳 → 「年-月-日 时:分」。
     *
     * @param millis 毫秒时间戳；非正数视为未记录
     * @return 可读时间
     */
    private static String timeText(long millis) {
        if (millis <= 0L) {
            return "-";
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
        return format.format(new Date(millis));
    }

    /**
     * 空值统一显示成短横线。
     *
     * @param value 原值
     * @return 原值；空白时为 "-"
     */
    private static String orDash(String value) {
        return value == null || value.trim().length() == 0 ? "-" : value;
    }
}
