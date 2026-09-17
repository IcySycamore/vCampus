package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.dto.StudentQuery;
import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.PersonCategory;
import edu.seu.vcampus.common.student.entity.StudentField;
import edu.seu.vcampus.common.student.entity.StudentProfile;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 学籍排序测试：默认序、按各字段排序、方向、空值位置。
 *
 * <p>
 * 最要紧的一条是 {@link #defaultsToProfileIdAscending()}：存储层是 {@code HashMap}，迭代顺序既不
 * 稳定也不可预期。若不加兜底排序，同一批数据两次查询可能给出不同顺序，界面上就是「刷新一下行就
 * 换位置」——看起来像丢了数据。
 */
class StudentSorterTest {

    /**
     * 不给条件时按主键升序。
     */
    @Test
    void defaultsToProfileIdAscending() {
        List<StudentProfile> list = threeProfiles();

        StudentSorter.sort(list, new StudentQuery());

        assertEquals(1L, list.get(0).getId().longValue());
        assertEquals(2L, list.get(1).getId().longValue());
        assertEquals(3L, list.get(2).getId().longValue());
    }

    /**
     * 条件为 null 也要排（服务端允许「不传条件查全部」），不能把顺序丢给 HashMap。
     */
    @Test
    void nullQueryStillSorts() {
        List<StudentProfile> list = threeProfiles();

        StudentSorter.sort(list, null);

        assertEquals(1L, list.get(0).getId().longValue());
    }

    /**
     * 按姓名升序 / 降序。
     *
     * <p>
     * 期望「陈七」排在「张三」前面，是因为中文姓名按<b>拼音</b>排（chen &lt; zhang）。这条断言就是
     * 用来钉住这个实现的：若哪天有人把比较换回 {@code String.compareTo}，它会立刻红——码位顺序下
     * 张(5F20) &lt; 陈(9648)，排出来正好相反。
     *
     * <p>
     * 「ALL」不是可排序字段，传了应回退到主键而不是不排。
     */
    @Test
    void sortsByNameBothDirections() {
        List<StudentProfile> list = threeProfiles();

        StudentSorter.sort(list, queryOf(StudentField.REAL_NAME, false));
        assertEquals("陈七", list.get(0).getRealName(), "中文姓名应按拼音排序");
        assertEquals("张三", list.get(2).getRealName());

        StudentSorter.sort(list, queryOf(StudentField.REAL_NAME, true));
        assertEquals("张三", list.get(0).getRealName());

        StudentSorter.sort(list, queryOf(StudentField.ALL, false));
        assertEquals(1L, list.get(0).getId().longValue(), "ALL 不是可排序字段，应回退到主键");
    }

    /**
     * 年份按数值比较，不是按字符串——按字符串排会把 2026 排到 202 前面去（这里用本用例锁住）。
     */
    @Test
    void sortsByJoinYearNumerically() {
        List<StudentProfile> list = threeProfiles();
        list.get(0).setJoinYear(2026);
        list.get(1).setJoinYear(2025);
        list.get(2).setJoinYear(2020);

        StudentSorter.sort(list, queryOf(StudentField.JOIN_YEAR, false));

        assertEquals(2020, list.get(0).getJoinYear());
        assertEquals(2026, list.get(2).getJoinYear());
    }

    /**
     * 状态与类别按枚举声明序排（在校 → 暂离 → 离校 → 毕业 → 退休），而不是按显示名的字符串。
     */
    @Test
    void sortsEnumsByDeclarationOrder() {
        List<StudentProfile> list = threeProfiles();
        list.get(0).setStatus(CampusStatus.GRADUATED);
        list.get(1).setStatus(CampusStatus.ENROLLED);
        list.get(2).setStatus(CampusStatus.SUSPENDED);

        StudentSorter.sort(list, queryOf(StudentField.STATUS, false));

        assertEquals(CampusStatus.ENROLLED, list.get(0).getStatus());
        assertEquals(CampusStatus.SUSPENDED, list.get(1).getStatus());
        assertEquals(CampusStatus.GRADUATED, list.get(2).getStatus());
    }

    /**
     * 空值排在后面：把空值排在最前会让第一页全是空白行，比不加排序还难用。
     */
    @Test
    void nullValuesSortLast() {
        List<StudentProfile> list = threeProfiles();
        list.get(0).setStudentNo(null);
        list.get(1).setStudentNo("202618002");
        list.get(2).setStudentNo("202618001");

        StudentSorter.sort(list, queryOf(StudentField.STUDENT_NO, false));

        assertEquals("202618001", list.get(0).getStudentNo());
        assertEquals("202618002", list.get(1).getStudentNo());
        assertNull(list.get(2).getStudentNo(), "没有学号的应排在最后");
    }

    /**
     * 单元素与空列表不应出错（也不该抛越界）。
     */
    @Test
    void handlesTinyLists() {
        StudentSorter.sort(new ArrayList<StudentProfile>(), new StudentQuery());
        List<StudentProfile> one = new ArrayList<StudentProfile>();
        one.add(new StudentProfile("uuid-1", 2026, CampusStatus.ENROLLED));
        StudentSorter.sort(one, new StudentQuery());
        assertEquals(1, one.size());
    }

    /**
     * 造三条档案：主键 1 张三、2 李四、3 陈七。故意打乱放入顺序，以证明确实排过。
     *
     * @return 档案列表
     */
    private static List<StudentProfile> threeProfiles() {
        List<StudentProfile> list = new ArrayList<StudentProfile>();
        list.add(profile(3L, "陈七", 2026));
        list.add(profile(1L, "张三", 2025));
        list.add(profile(2L, "李四", 2024));
        return list;
    }

    /**
     * 造一条带主键与姓名的档案。
     *
     * @param id 主键
     * @param name 姓名
     * @param joinYear 入校年份
     * @return 档案
     */
    private static StudentProfile profile(long id, String name, int joinYear) {
        StudentProfile profile = new StudentProfile("uuid-" + id, joinYear,
                CampusStatus.ENROLLED);
        profile.setId(Long.valueOf(id));
        profile.setRealName(name);
        profile.setStudentNo("2026" + id);
        profile.setPersonCategory(PersonCategory.STUDENT);
        return profile;
    }

    /**
     * 造一份「按某字段、某方向」排序的查询条件。
     *
     * @param field 排序字段
     * @param descending 是否降序
     * @return 查询条件
     */
    private static StudentQuery queryOf(StudentField field, boolean descending) {
        StudentQuery query = new StudentQuery();
        query.setSortBy(field);
        query.setDescending(descending);
        return query;
    }
}
