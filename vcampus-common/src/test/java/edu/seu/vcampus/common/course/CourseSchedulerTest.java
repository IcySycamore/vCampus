package edu.seu.vcampus.common.course;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 排课冲突检测测试。
 */
class CourseSchedulerTest {

    private static Classroom room(String uuid, int capacity) {
        Classroom room = new Classroom();
        room.setUuid(uuid);
        room.setCapacity(capacity);
        room.setLocation("教一");
        room.setName("101");
        for (int day = 1; day <= 7; day++) {
            room.getAvailableTimeslots().add(new Timeslot(day, 480, 1355));
        }
        return room;
    }

    private static ScheduleEntry course(String code, String name, String teacher, int capacity) {
        return new ScheduleEntry(code, name, teacher, capacity, 0);
    }

    @Test
    void periodTimeslotCoversSevenDaysThirteenPeriods() {
        assertEquals("周一", CourseScheduler.weekdayName(1));
        assertEquals("周日", CourseScheduler.weekdayName(7));
        assertEquals("第13节", CourseScheduler.periodName(12));
        assertEquals(new Timeslot(1, 480, 525), CourseScheduler.periodTimeslot(1, 0));
        assertEquals(new Timeslot(7, 1310, 1355), CourseScheduler.periodTimeslot(7, 12));
    }

    @Test
    void conflictsWhenCapacityInsufficient() {
        ScheduleEntry entry = course("CS101", "数据结构", "t1", 60);
        entry.setTimeslot(CourseScheduler.periodTimeslot(1, 0));
        entry.setClassroomUuid("r1");
        entry.setClassroomLocation("教一101");

        List<String> reasons = CourseScheduler.conflicts(entry,
                new ArrayList<ScheduleEntry>(), room("r1", 40), null);

        assertTrue(reasons.contains("教室容量不足"));
    }

    @Test
    void conflictsWhenTeacherDoubleBooked() {
        ScheduleEntry existing = course("CS101", "数据结构", "t1", 40);
        existing.setTimeslot(CourseScheduler.periodTimeslot(1, 0));
        existing.setClassroomUuid("r1");

        ScheduleEntry candidate = course("CS102", "计算机网络", "t1", 40);
        candidate.setTimeslot(CourseScheduler.periodTimeslot(1, 0));
        candidate.setClassroomUuid("r2");

        List<ScheduleEntry> schedule = new ArrayList<ScheduleEntry>();
        schedule.add(existing);

        assertFalse(CourseScheduler.conflicts(candidate, schedule, room("r2", 40), null).isEmpty());
    }

    @Test
    void conflictsWhenClassroomDoubleBooked() {
        ScheduleEntry existing = course("CS101", "数据结构", "t1", 40);
        existing.setTimeslot(CourseScheduler.periodTimeslot(1, 0));
        existing.setClassroomUuid("r1");

        ScheduleEntry candidate = course("CS102", "计算机网络", "t2", 40);
        candidate.setTimeslot(CourseScheduler.periodTimeslot(1, 0));
        candidate.setClassroomUuid("r1");

        List<ScheduleEntry> schedule = new ArrayList<ScheduleEntry>();
        schedule.add(existing);

        assertFalse(CourseScheduler.conflicts(candidate, schedule, room("r1", 40), null).isEmpty());
    }

    @Test
    void noConflictWhenAllGood() {
        ScheduleEntry existing = course("CS101", "数据结构", "t1", 40);
        existing.setTimeslot(CourseScheduler.periodTimeslot(1, 0));
        existing.setClassroomUuid("r1");

        ScheduleEntry candidate = course("CS102", "计算机网络", "t2", 40);
        candidate.setTimeslot(CourseScheduler.periodTimeslot(1, 1));
        candidate.setClassroomUuid("r1");

        List<ScheduleEntry> schedule = new ArrayList<ScheduleEntry>();
        schedule.add(existing);

        assertTrue(CourseScheduler.conflicts(candidate, schedule, room("r1", 40), null).isEmpty());
    }

    @Test
    void respectsTeacherAvailableTimeslots() {
        Teacher teacher = new Teacher();
        teacher.setUuid("t1");
        teacher.getAvailableTimeslots().add(new Timeslot(1, 480, 700));

        ScheduleEntry entry = course("CS101", "数据结构", "t1", 40);
        entry.setTimeslot(CourseScheduler.periodTimeslot(1, 5)); // 第6节 14:00-14:45
        entry.setClassroomUuid("r1");
        entry.setClassroomLocation("教一101");

        List<String> reasons = CourseScheduler.conflicts(entry,
                new ArrayList<ScheduleEntry>(), room("r1", 40), teacher);

        assertTrue(reasons.contains("上课时间不在教师可用时间槽内"));
    }
}
