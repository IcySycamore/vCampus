package edu.seu.vcampus.client.view.component;

import edu.seu.vcampus.common.student.entity.StudentField;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 表头点击排序测试：只测「点哪一列 → 按什么字段、什么方向」这段判断。
 *
 * <p>
 * 不构造鼠标事件（ADR-0005：不写 GUI 自动化）——判断逻辑本身放在
 * {@link TableSortBinder#selectColumn(int)} 里，直接调它即可；鼠标监听只做「坐标 → 列下标」。
 */
class TableSortBinderTest {

    /**
     * 第一次点某列：按该列升序，并在表头留下记号。
     */
    @Test
    void firstClickSortsAscending() {
        JTable table = newTable();
        TableSortBinder<StudentField> binder = newBinder(table);

        assertTrue(binder.selectColumn(1));

        assertEquals(StudentField.REAL_NAME, binder.getField());
        assertFalse(binder.isDescending());
        assertEquals("姓名" + TableSortBinder.ASC_MARK, headerOf(table, 1));
    }

    /**
     * 同一列再点一次翻转方向，记号跟着换。
     */
    @Test
    void secondClickFlipsDirection() {
        JTable table = newTable();
        TableSortBinder<StudentField> binder = newBinder(table);

        binder.selectColumn(1);
        binder.selectColumn(1);

        assertTrue(binder.isDescending());
        assertEquals("姓名" + TableSortBinder.DESC_MARK, headerOf(table, 1));
    }

    /**
     * 换一列排序：新列从升序开始，旧列的记号必须抹掉。
     *
     * <p>
     * 抹掉旧记号这条很容易漏：留着的话界面上会同时出现两个 ▲，用户无从判断到底按哪列排的。
     */
    @Test
    void switchingColumnResetsDirectionAndClearsOldMark() {
        JTable table = newTable();
        TableSortBinder<StudentField> binder = newBinder(table);

        binder.selectColumn(1);
        binder.selectColumn(1);
        binder.selectColumn(0);

        assertEquals(StudentField.PROFILE_ID, binder.getField());
        assertFalse(binder.isDescending(), "换列应从升序重新开始");
        assertEquals("姓名", headerOf(table, 1), "旧列的记号应被抹掉");
        assertEquals("主键" + TableSortBinder.ASC_MARK, headerOf(table, 0));
    }

    /**
     * 映射返回 null 的列点不动，也不触发重查。
     */
    @Test
    void unsortableColumnIsIgnored() {
        JTable table = newTable();
        TableSortBinder<StudentField> binder = newBinder(table);
        final int[] calls = new int[1];
        binder.setOnSortChanged(new Runnable() {
            @Override
            public void run() {
                calls[0] = calls[0] + 1;
            }
        });

        assertFalse(binder.selectColumn(2), "备注列不可排序");
        assertNull(binder.getField());
        assertEquals(0, calls[0], "点了不可排序的列不该白跑一次查询");
        assertEquals("备注", headerOf(table, 2));
    }

    /**
     * 可排序的列会触发一次重查回调。
     */
    @Test
    void sortableColumnTriggersCallback() {
        JTable table = newTable();
        TableSortBinder<StudentField> binder = newBinder(table);
        final int[] calls = new int[1];
        binder.setOnSortChanged(new Runnable() {
            @Override
            public void run() {
                calls[0] = calls[0] + 1;
            }
        });

        binder.selectColumn(0);

        assertEquals(1, calls[0]);
    }

    /**
     * 参数为 null 直接拒绝，避免后面点表头时抛一个和原因无关的空指针。
     */
    @Test
    void rejectsNullArguments() {
        final JTable table = newTable();
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                new TableSortBinder<StudentField>(null, columnMap());
            }
        });
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                new TableSortBinder<StudentField>(table, null);
            }
        });
    }

    /**
     * 造一张三列表格：主键 / 姓名可排序，备注不可排序。
     *
     * @return 表格
     */
    private static JTable newTable() {
        return new JTable(new DefaultTableModel(new String[] { "主键", "姓名", "备注" }, 0));
    }

    /**
     * 造一个绑定器（未加监听）。
     *
     * @param table 目标表格
     * @return 绑定器
     */
    private static TableSortBinder<StudentField> newBinder(JTable table) {
        return new TableSortBinder<StudentField>(table, columnMap());
    }

    /**
     * 列 → 字段：第 0、1 列可排序，第 2 列不可。
     *
     * @return 映射
     */
    private static TableSortBinder.ColumnMap<StudentField> columnMap() {
        return new TableSortBinder.ColumnMap<StudentField>() {
            @Override
            public StudentField fieldOf(int column) {
                if (column == 0) {
                    return StudentField.PROFILE_ID;
                }
                if (column == 1) {
                    return StudentField.REAL_NAME;
                }
                return null;
            }
        };
    }

    /**
     * 取某列表头当前文字。
     *
     * @param table 表格
     * @param column 列下标
     * @return 表头文字
     */
    private static String headerOf(JTable table, int column) {
        return String.valueOf(table.getColumnModel().getColumn(column).getHeaderValue());
    }
}
