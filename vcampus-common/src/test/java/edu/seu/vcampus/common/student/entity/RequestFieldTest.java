package edu.seu.vcampus.common.student.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 申请单字段枚举测试：可搜 / 可排的边界。
 */
class RequestFieldTest {

    /**
     * 「状态 / 申请时间」不能当搜索目标：状态已有专门下拉框，时间没人会手打一个时间戳。
     */
    @Test
    void searchableExcludesStatusAndAppliedAt() {
        assertTrue(RequestField.ALL.isSearchable());
        assertTrue(RequestField.REQUEST_ID.isSearchable());
        assertTrue(RequestField.PROFILE_ID.isSearchable());
        assertTrue(RequestField.APPLICANT_UUID.isSearchable());
        assertTrue(RequestField.CHANGES.isSearchable());
        assertTrue(RequestField.REASON.isSearchable());

        assertFalse(RequestField.STATUS.isSearchable());
        assertFalse(RequestField.APPLIED_AT.isSearchable());
    }

    /**
     * 自由文本（变更内容、理由）能搜不能排，反过来状态与时间能排不能搜。
     */
    @Test
    void sortableExcludesFreeTextFields() {
        assertTrue(RequestField.REQUEST_ID.isSortable());
        assertTrue(RequestField.PROFILE_ID.isSortable());
        assertTrue(RequestField.APPLICANT_UUID.isSortable());
        assertTrue(RequestField.STATUS.isSortable());
        assertTrue(RequestField.APPLIED_AT.isSortable());

        assertFalse(RequestField.ALL.isSortable());
        assertFalse(RequestField.CHANGES.isSortable(), "长自由文本排序没有意义");
        assertFalse(RequestField.REASON.isSortable());
        assertFalse(RequestField.APPLIED_AT.isSearchable());
    }

    /**
     * 显示名能来回翻译；默认项排第一。
     */
    @Test
    void displayNamesRoundTrip() {
        assertEquals(RequestField.ALL, RequestField.searchable()[0]);
        RequestField[] all = RequestField.values();
        int index = 0;
        while (index < all.length) {
            assertEquals(all[index], RequestField.fromDisplayName(all[index].getDisplayName()));
            index = index + 1;
        }
        assertNull(RequestField.fromDisplayName("不存在的字段"));
    }
}
