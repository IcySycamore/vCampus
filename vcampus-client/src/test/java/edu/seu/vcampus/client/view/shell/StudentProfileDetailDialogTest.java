package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.PersonCategory;
import edu.seu.vcampus.common.student.entity.StudentProfile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 人员档案详情测试：只测文本拼装这段纯函数（ADR-0005 不写 GUI 自动化）。
 *
 * <p>
 * 不构造对话框——{@code linesOf} / {@code fieldLabel} / {@code titleOf} 都是静态纯函数，直接调即可。
 */
class StudentProfileDetailDialogTest {

    /**
     * 展示的每一项都真的落到文本里：漏一项用户在详情里就看不到，而那正是他点开详情的原因。
     */
    @Test
    void linesCoverEveryShownField() {
        List<String> lines = StudentProfileDetailDialog.linesOf(student());

        assertContains(lines, "主键：6");
        assertContains(lines, "学号：20260001");
        assertContains(lines, "姓名：演示学生");
        assertContains(lines, "人员类别：学生");
        assertContains(lines, "在校状态：在校");
        assertContains(lines, "入校年份：2026");
        assertContains(lines, "专业：软件工程");
        assertContains(lines, "账户标识：uuid-6");
    }

    /**
     * 学术方向的标签跟着人员类别走：学生读「专业」，教师读「研究方向」。
     *
     * <p>
     * 两边共用同一个 {@code field} 字段，标签写错会让看的人以为档案填错了地方。
     */
    @Test
    void fieldLabelFollowsCategory() {
        StudentProfile teacher = new StudentProfile("uuid-2", PersonCategory.TEACHER, 2026,
                CampusStatus.ENROLLED);
        teacher.setField("计算机视觉");
        teacher.setRealName("演示教师");

        assertEquals("专业", StudentProfileDetailDialog.fieldLabel(student()));
        assertEquals("研究方向", StudentProfileDetailDialog.fieldLabel(teacher));
        assertContains(StudentProfileDetailDialog.linesOf(teacher), "研究方向：计算机视觉");
    }

    /**
     * 学术方向空着时给一句说明而不是短横线：新生自助填写的档案本来就是空的，
     * 「还没填」和「填了但显示不出来」必须区分得开。
     */
    @Test
    void blankFieldSaysNotFilledYet() {
        StudentProfile blank = new StudentProfile("uuid-9", 2026, CampusStatus.ENROLLED);
        blank.setId(Long.valueOf(9L));

        List<String> lines = StudentProfileDetailDialog.linesOf(blank);

        assertContains(lines, "专业：（尚未填写）");
        assertContains(lines, "学号：-");
    }

    /**
     * 标题带上主键与姓名，一眼知道看的是谁的档案。
     */
    @Test
    void titleCarriesIdAndName() {
        assertEquals("人员档案详情 · #6 演示学生",
                StudentProfileDetailDialog.titleOf(student()));
    }

    /**
     * 造一条学生档案。
     *
     * @return 档案
     */
    private static StudentProfile student() {
        StudentProfile profile = new StudentProfile("uuid-6", 2026, CampusStatus.ENROLLED);
        profile.setId(Long.valueOf(6L));
        profile.setStudentNo("20260001");
        profile.setRealName("演示学生");
        profile.setField("软件工程");
        return profile;
    }

    /**
     * 断言某一行文本存在。
     *
     * @param lines 全部行
     * @param expected 期望的整行文本
     */
    private static void assertContains(List<String> lines, String expected) {
        assertTrue(lines.contains(expected), "详情里应有「" + expected + "」，实得 " + lines);
    }
}
