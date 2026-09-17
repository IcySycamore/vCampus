package edu.seu.vcampus.common.course;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 排课撤销/重做栈测试。
 */
class ScheduleHistoryTest {

    private static List<ScheduleEntry> state(int scheduledCount) {
        List<ScheduleEntry> list = new ArrayList<ScheduleEntry>();
        for (int i = 0; i < scheduledCount; i++) {
            ScheduleEntry entry = new ScheduleEntry("CS" + i, "课程" + i, "t", 40, 0);
            entry.setTimeslot(CourseScheduler.periodTimeslot(1, i));
            entry.setClassroomUuid("r");
            list.add(entry);
        }
        return list;
    }

    @Test
    void undoReturnsPreviousState() {
        ScheduleHistory history = new ScheduleHistory();
        List<ScheduleEntry> s0 = state(0);
        List<ScheduleEntry> s1 = state(1);
        List<ScheduleEntry> s2 = state(2);
        history.record(s0);
        history.record(s1);

        List<ScheduleEntry> undone = history.undo(s2);

        assertEquals(1, undone.size());
        assertTrue(history.canRedo());
    }

    @Test
    void redoReturnsForwardState() {
        ScheduleHistory history = new ScheduleHistory();
        List<ScheduleEntry> s0 = state(0);
        List<ScheduleEntry> s1 = state(1);
        List<ScheduleEntry> s2 = state(2);
        history.record(s0);
        history.record(s1);

        List<ScheduleEntry> undone = history.undo(s2);
        List<ScheduleEntry> redone = history.redo(undone);

        assertEquals(2, redone.size());
    }

    @Test
    void undoWithoutHistoryReturnsCurrent() {
        ScheduleHistory history = new ScheduleHistory();
        List<ScheduleEntry> current = state(1);
        assertSame(current, history.undo(current));
        assertFalse(history.canUndo());
        assertFalse(history.canRedo());
    }

    @Test
    void recordClearsRedoStack() {
        ScheduleHistory history = new ScheduleHistory();
        history.record(state(0));
        history.record(state(1));
        history.undo(state(2));

        history.record(state(3));

        assertFalse(history.canRedo());
    }

    @Test
    void undoIsDeepCopy() {
        ScheduleHistory history = new ScheduleHistory();
        List<ScheduleEntry> before = state(1);
        history.record(before);

        // 记录后修改原状态，不应影响已保存的历史
        before.get(0).setTimeslot(null);
        List<ScheduleEntry> undone = history.undo(state(2));

        assertEquals(1, undone.size());
        assertNull(before.get(0).getTimeslot());
        assertNotNull(undone.get(0).getTimeslot());
    }
}
