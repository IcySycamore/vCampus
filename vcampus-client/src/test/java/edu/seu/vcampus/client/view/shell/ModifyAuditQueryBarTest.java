package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.common.student.dto.ModifyRequestQuery;
import edu.seu.vcampus.common.student.entity.ModifyRequestStatus;
import edu.seu.vcampus.common.student.entity.RequestField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 修改审核筛选条测试：只测「控件 → 查询条件」这段映射。
 *
 * <p>
 * 不驱动按钮（ADR-0005）。这里要钉住的是三件事：默认只看待审（这个页面的日常用法是清待办）、
 * 搜索字段默认是「全部字段」、页码与每页条数如实带出去。
 */
class ModifyAuditQueryBarTest {

    /**
     * 默认条件：只看待审 + 全部字段，不填关键词。
     */
    @Test
    void defaultsToPendingAndAllFields() {
        ModifyRequestQuery query = new ModifyAuditQueryBar().toQuery(1, 5);

        assertEquals(ModifyRequestStatus.PENDING, query.getStatus());
        assertEquals(RequestField.ALL, query.getSearchField());
        assertEquals("", query.getKeyword());
        assertEquals(1, query.getPageNumber());
        assertEquals(5, query.getPageSize());
    }

    /**
     * 关键词去首尾空白后带出——尾随空格会让服务端比对失败（用户看不出来）。
     */
    @Test
    void keywordIsTrimmed() {
        ModifyAuditQueryBar bar = new ModifyAuditQueryBar();
        bar.setKeyword("  休学  ");

        assertEquals("休学", bar.toQuery(2, 5).getKeyword());
        assertEquals(2, bar.toQuery(2, 5).getPageNumber());
    }

    /**
     * 选了搜索字段就按那一列搜；选了「全部状态」表示不过滤（必须是 null，不能是某个具体状态）。
     */
    @Test
    void fieldAndStatusFollowSelection() {
        ModifyAuditQueryBar bar = new ModifyAuditQueryBar();
        bar.selectField(RequestField.REASON.getDisplayName());
        bar.selectStatus(ModifyAuditQueryBar.ALL_STATUSES);

        ModifyRequestQuery query = bar.toQuery(1, 5);

        assertEquals(RequestField.REASON, query.getSearchField());
        assertNull(query.getStatus(), "「全部状态」必须翻译成 null");
    }

    /**
     * 重置把三个控件复位：关键词清空、字段回全部、状态回待审核。
     */
    @Test
    void clearRestoresDefaults() {
        ModifyAuditQueryBar bar = new ModifyAuditQueryBar();
        bar.setKeyword("理由");
        bar.selectField(RequestField.REASON.getDisplayName());
        bar.selectStatus(ModifyAuditQueryBar.ALL_STATUSES);

        bar.clear();
        ModifyRequestQuery query = bar.toQuery(1, 5);

        assertEquals("", bar.getKeyword());
        assertEquals(RequestField.ALL, query.getSearchField());
        assertEquals(ModifyRequestStatus.PENDING, query.getStatus());
    }
}
