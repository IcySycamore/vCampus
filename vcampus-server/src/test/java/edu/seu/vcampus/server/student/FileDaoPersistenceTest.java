package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.ModifyRequestStatus;
import edu.seu.vcampus.common.student.entity.StudentModifyRequest;
import edu.seu.vcampus.common.student.entity.StudentProfile;

import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 落盘测试：服务端重启后，档案与申请单必须还在。
 *
 * <p>
 * 每条用例都是「写 → 换一个实例重新打开 → 读」：只 new 一次看不出问题，重新打开才等价于重启。
 * 这正是原先内存实现的症状——重开之后档案退回空白、申请单直接没影。
 */
class FileDaoPersistenceTest {

    /**
     * 档案与它改过的字段都要活过重启，主键也必须留住（申请单是指着主键找学籍的）。
     */
    @Test
    void profilesSurviveReopen() throws IOException {
        File file = temp("students");
        StudentDaoFile first = new StudentDaoFile(file);
        StudentProfile profile = new StudentProfile("uuid-stu", 2026, CampusStatus.ENROLLED);
        profile.setField("软件工程");
        profile.setStudentNo("20260007");
        assertTrue(first.insert(profile));

        StudentDaoFile reopened = new StudentDaoFile(file);
        StudentProfile back = reopened.findByUserUuid("uuid-stu");

        assertNotNull(back, "重启后档案必须还在");
        assertEquals("软件工程", back.getField());
        assertEquals("20260007", back.getStudentNo(), "学号也要落盘");
        assertEquals(2026, back.getJoinYear());
        assertEquals(CampusStatus.ENROLLED, back.getStatus());
        assertEquals(profile.getId(), back.getId(), "主键也得留住，否则申请单会指错学籍");

        // 主键从磁盘上的最大值之后继续分配，不会与已有记录撞号
        StudentProfile another = new StudentProfile("uuid-2", 2025, CampusStatus.ENROLLED);
        reopened.insert(another);
        assertFalse(another.getId().equals(back.getId()));
    }

    /**
     * 注销过的档案重启后不该复活——删除位也是数据，同样要落盘。
     */
    @Test
    void softDeletedProfileStaysHiddenAfterReopen() throws IOException {
        File file = temp("students-deleted");
        StudentDaoFile first = new StudentDaoFile(file);
        StudentProfile profile = new StudentProfile("uuid-stu", 2026, CampusStatus.ENROLLED);
        first.insert(profile);
        assertTrue(first.softDelete(profile.getId()));

        StudentDaoFile reopened = new StudentDaoFile(file);

        assertNull(reopened.findById(profile.getId()), "注销过的档案不该重启后复活");
        assertNull(reopened.findByUserUuid("uuid-stu"));
        assertEquals(0, reopened.findAll().size(), "注销过的档案不该留在未删除列表里");
    }

    /**
     * 申请单连同审核结果一起活过重启：学生要知道自己提的那条批没批。
     */
    @Test
    void modifyRequestsSurviveReopen() throws IOException {
        File file = temp("requests");
        StudentModifyRequestDaoFile first = new StudentModifyRequestDaoFile(file);
        StudentModifyRequest request = new StudentModifyRequest(Long.valueOf(7L), "uuid-stu",
                "field=软件工程", "专业录错了");
        assertTrue(first.insert(request));
        request.setStatus(ModifyRequestStatus.APPROVED);
        request.setComment("情况属实");
        request.setAuditedBy("uuid-tea");
        request.setAuditedAt(1700000000000L);
        assertTrue(first.update(request));

        StudentModifyRequestDaoFile reopened = new StudentModifyRequestDaoFile(file);
        StudentModifyRequest back = reopened.findById(request.getRequestId());

        assertNotNull(back, "重启后申请单必须还在");
        assertEquals(Long.valueOf(7L), back.getProfileId());
        assertEquals("uuid-stu", back.getApplicantUuid());
        assertEquals("field=软件工程", back.getChangesJson());
        assertEquals(ModifyRequestStatus.APPROVED, back.getStatus());
        assertEquals("情况属实", back.getComment());
        assertEquals("uuid-tea", back.getAuditedBy());
        assertEquals(1700000000000L, back.getAuditedAt());
        assertEquals(1L, reopened.count(null));
    }

    /**
     * 自由文本里混进 Tab 也不能把行结构搞错位——错位的后果是后面的行全部读串。
     */
    @Test
    void freeTextWithTabDoesNotCorruptLaterRows() throws IOException {
        File file = temp("requests-tab");
        StudentModifyRequestDaoFile first = new StudentModifyRequestDaoFile(file);
        StudentModifyRequest dirty = new StudentModifyRequest(Long.valueOf(1L), "uuid-a",
                "field=x", "理由里混进了\t制表符");
        first.insert(dirty);
        StudentModifyRequest clean = new StudentModifyRequest(Long.valueOf(2L), "uuid-b",
                "field=y", "正常的一条");
        first.insert(clean);

        StudentModifyRequestDaoFile reopened = new StudentModifyRequestDaoFile(file);

        assertEquals(2L, reopened.count(null), "一条脏数据不该把后面的行也带坏");
        assertEquals("理由里混进了 制表符", reopened.findById(dirty.getRequestId()).getReason());
        assertEquals("正常的一条", reopened.findById(clean.getRequestId()).getReason());
    }

    /**
     * 建一个临时文件并立刻删掉，让被测实现自己创建（避免拿空文件当初始状态）。
     *
     * @param prefix 文件名前缀
     * @return 临时文件路径
     * @throws IOException 创建失败
     */
    private static File temp(String prefix) throws IOException {
        File file = File.createTempFile("vcampus-" + prefix, ".tsv");
        file.deleteOnExit();
        assertTrue(file.delete(), "先删掉，让被测实现自己建文件");
        return file;
    }
}
