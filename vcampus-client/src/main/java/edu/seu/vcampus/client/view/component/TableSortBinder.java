package edu.seu.vcampus.client.view.component;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;

/**
 * 表头点击排序：把「点了哪一列」翻译成「按哪个字段、朝哪个方向排」。
 *
 * <p>
 * 与 {@link PageBarPanel} 一样只做「显示与意图上报」：本类不改数据也不发请求，排完把回调一跑，
 * 由页面带着新的字段与方向重新查询。
 *
 * <p>
 * <b>为什么排序不在这里做</b>：列表是分页的，本地只能排「当前这一页」——页内有序、页间乱序，
 * 翻到第二页就会发现开头的数字比第一页的结尾还大。所以真正的排序交给服务端，本类只产生
 * 「按第几列、什么方向」这个意图。
 *
 * <p>
 * 表头文字用 ▲ / ▼ 标出当前排序列与方向：没有这个记号，用户点完看不出到底排没排、朝哪边排。
 * 列下标 → 字段的映射由调用方给出，映射返回 null 的列（例如「审核意见」这类自由文本）点不动。
 *
 * @param &lt;T&gt; 排序字段类型（通常是枚举）
 */
public final class TableSortBinder<T> {

    /** 列下标 → 排序字段。 */
    public interface ColumnMap<T> {

        /**
         * 取该列对应的排序字段。
         *
         * @param columnIndex 视图列下标
         * @return 排序字段；该列不参与排序时返回 null
         */
        T fieldOf(int columnIndex);
    }

    /** 升序记号。 */
    static final String ASC_MARK = " ▲";

    /** 降序记号。 */
    static final String DESC_MARK = " ▼";

    /** 被绑定的表格。 */
    private final JTable m_table;

    /** 列 → 字段映射。 */
    private final ColumnMap<T> m_column_map;

    /** 排序变化回调。 */
    private Runnable m_on_sort_changed;

    /** 当前排序字段；null 表示还没点过表头。 */
    private T m_field;

    /** 当前是否降序。 */
    private boolean m_descending;

    /**
     * 构造绑定器。
     *
     * @param table 目标表格
     * @param columnMap 列下标 → 排序字段
     * @throws IllegalArgumentException 任一参数为 null
     */
    public TableSortBinder(JTable table, ColumnMap<T> columnMap) {
        if (table == null || columnMap == null) {
            throw new IllegalArgumentException("table and columnMap must not be null");
        }
        this.m_table = table;
        this.m_column_map = columnMap;
    }

    /** 挂上表头点击监听。 */
    public void bind() {
        JTableHeader header = m_table.getTableHeader();
        if (header == null) {
            return;
        }
        // 禁止拖动列：列的次序与「哪一列在排序」是绑在一起的，允许拖动会让记号与数据错位
        header.setReorderingAllowed(false);
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                int column = m_table.columnAtPoint(event.getPoint());
                if (column >= 0) {
                    selectColumn(column);
                }
            }
        });
    }

    /**
     * 处理「点了某一列」：同一列再点一次翻转方向，换一列则从升序开始。
     *
     * <p>
     * 拆成公开方法而不是只写在鼠标监听里，是为了这段判断能不建鼠标事件就单测（ADR-0005）。
     *
     * @param column 视图列下标
     * @return 是否处理了（该列不可排序时返回 false，调用方无需重查）
     */
    public boolean selectColumn(int column) {
        T field = m_column_map.fieldOf(column);
        if (field == null) {
            return false;
        }
        if (field.equals(m_field)) {
            m_descending = !m_descending;
        } else {
            m_field = field;
            m_descending = false;
        }
        paintMarks();
        if (m_on_sort_changed != null) {
            m_on_sort_changed.run();
        }
        return true;
    }

    /**
     * 设置排序变化回调（由页面重新查询）。
     *
     * @param onSortChanged 回调
     */
    public void setOnSortChanged(Runnable onSortChanged) {
        m_on_sort_changed = onSortChanged;
    }

    /** @return 当前排序字段；null 表示沿用服务端默认排序 */
    public T getField() {
        return m_field;
    }

    /** @return 当前是否降序 */
    public boolean isDescending() {
        return m_descending;
    }

    /**
     * 按当前状态重写各列表头：排序列加记号，其余列恢复原始文字。
     *
     * <p>
     * 原始文字从表格模型取（{@code getColumnName}），而不是在第一处保留一份副本——模型里的列名
     * 才是唯一真相，另存一份迟早在改列时会不同步。
     */
    private void paintMarks() {
        int columnCount = m_table.getColumnCount();
        int index = 0;
        while (index < columnCount) {
            TableColumn column = m_table.getColumnModel().getColumn(index);
            String title = m_table.getModel()
                    .getColumnName(m_table.convertColumnIndexToModel(index));
            T field = m_column_map.fieldOf(index);
            if (field != null && field.equals(m_field)) {
                column.setHeaderValue(title + (m_descending ? DESC_MARK : ASC_MARK));
            } else {
                column.setHeaderValue(title);
            }
            index = index + 1;
        }
        JTableHeader header = m_table.getTableHeader();
        if (header != null) {
            header.repaint();
        }
    }
}
