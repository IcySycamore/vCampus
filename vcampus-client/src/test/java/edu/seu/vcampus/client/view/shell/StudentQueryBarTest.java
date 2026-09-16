package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.common.message.PageResponse;
import edu.seu.vcampus.common.student.dto.StudentQuery;
import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.PersonCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 学籍筛选条测试：只测「控件 → 查询条件」这段映射。
 *
 * <p>
 * 不驱动按钮（ADR-0005：不写 GUI 自动化）。真正值得测的是三件事：不填条件时不能凭空加上过滤
 * （否则会少查一半记录）、选了条件要翻译成对应枚举、页码与每页条数要如实带出去。
 */
class StudentQueryBarTest {

    /**
     * 不填任何条件时，只有分页参数被带上。
     */
    @Test
    void blankBarPassesPaginationOnly() {
        StudentQuery query = new StudentQueryBar().toQuery(1, 5);

        assertEquals(1, query.getPageNumber());
        assertEquals(5, query.getPageSize());
        assertEquals("", query.getKeyword());
        assertNull(query.getPersonCategory(), "「全部类别」必须翻译成 null 而不是某个具体类别");
        assertNull(query.getStatus(), "「全部状态」必须翻译成 null");
    }

    /**
     * 关键词原样带出，且去掉首尾空白（尾随空格会让服务端匹配不到人）。
     */
    @Test
    void keywordIsTrimmed() {
        StudentQueryBar bar = new StudentQueryBar();
        bar.setKeyword("  202618001  ");

        StudentQuery query = bar.toQuery(2, 5);

        assertEquals("202618001", query.getKeyword());
        assertEquals(2, query.getPageNumber());
    }

    /**
     * 选择了类别与状态时翻译成对应枚举。
     */
    @Test
    void selectionsBecomeEnums() {
        StudentQueryBar bar = new StudentQueryBar();
        bar.selectCategory(PersonCategory.TEACHER.getDisplayName());
        bar.selectStatus(CampusStatus.GRADUATED.getDisplayName());

        StudentQuery query = bar.toQuery(1, PageResponse.DEFAULT_PAGE_SIZE);

        assertEquals(PersonCategory.TEACHER, query.getPersonCategory());
        assertEquals(CampusStatus.GRADUATED, query.getStatus());
    }

    /**
     * 重置把三个控件复位到「不过滤」。
     */
    @Test
    void clearRestoresNoFilter() {
        StudentQueryBar bar = new StudentQueryBar();
        bar.setKeyword("张三");
        bar.selectCategory(PersonCategory.TEACHER.getDisplayName());
        bar.selectStatus(CampusStatus.GRADUATED.getDisplayName());

        bar.clear();
        StudentQuery query = bar.toQuery(1, 5);

        assertEquals("", bar.getKeyword());
        assertNull(query.getPersonCategory());
        assertNull(query.getStatus());
    }
}
