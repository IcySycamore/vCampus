package edu.seu.vcampus.common.course;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 排课撤销/重做栈（纯逻辑）。
 *
 * <p>每次排课变更<b>前</b>调用 {@link #record(List)} 保存变更前的状态（恢复点）；
 * {@link #undo(List)} / {@link #redo(List)} 传入当前状态、返回目标状态。状态以深拷贝方式保存，
 * 避免外部修改污染历史。
 */
public final class ScheduleHistory {

    /** 撤销栈（栈顶为最近一次记录）。 */
    private final Deque<List<ScheduleEntry>> m_undo = new ArrayDeque<List<ScheduleEntry>>();

    /** 重做栈。 */
    private final Deque<List<ScheduleEntry>> m_redo = new ArrayDeque<List<ScheduleEntry>>();

    /** 记录一个新状态，并清空重做栈。 */
    public void record(List<ScheduleEntry> state) {
        m_undo.push(copy(state));
        m_redo.clear();
    }

    /** 撤销：返回上一个状态；无可撤销时原样返回当前状态。 */
    public List<ScheduleEntry> undo(List<ScheduleEntry> current) {
        if (m_undo.isEmpty()) {
            return current;
        }
        m_redo.push(copy(current));
        return m_undo.pop();
    }

    /** 重做：返回下一个状态；无可重做时原样返回当前状态。 */
    public List<ScheduleEntry> redo(List<ScheduleEntry> current) {
        if (m_redo.isEmpty()) {
            return current;
        }
        m_undo.push(copy(current));
        return m_redo.pop();
    }

    /** @return 是否可撤销。 */
    public boolean canUndo() {
        return !m_undo.isEmpty();
    }

    /** @return 是否可重做。 */
    public boolean canRedo() {
        return !m_redo.isEmpty();
    }

    private static List<ScheduleEntry> copy(List<ScheduleEntry> state) {
        List<ScheduleEntry> result = new ArrayList<ScheduleEntry>();
        if (state != null) {
            for (ScheduleEntry entry : state) {
                result.add(entry.copy());
            }
        }
        return result;
    }
}
