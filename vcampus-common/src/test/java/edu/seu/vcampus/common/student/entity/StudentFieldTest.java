package edu.seu.vcampus.common.student.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 学籍字段枚举测试：谁能搜、谁能排、下拉的顺序。
 *
 * <p>
 * 这些判定直接决定界面上会出现哪些选项，而「选项里少了某一列」是很难在使用中察觉的（用户只会
 * 觉得「这软件没法按姓名搜」），所以把可搜 / 可排的边界钉在这里。
 */
class StudentFieldTest {

    /**
     * 不能当搜索目标的字段：主键是内部编号，「状态 / 类别」已有专门的下拉框。
     */
    @Test
    void searchableExcludesProfileIdStatusAndCategory() {
        assertTrue(StudentField.ALL.isSearchable());
        assertTrue(StudentField.STUDENT_NO.isSearchable());
        assertTrue(StudentField.REAL_NAME.isSearchable());
        assertTrue(StudentField.USER_UUID.isSearchable());
        assertTrue(StudentField.FIELD.isSearchable());
        assertTrue(StudentField.JOIN_YEAR.isSearchable());

        assertFalse(StudentField.PROFILE_ID.isSearchable(), "主键不该让人在文本框里手打");
        assertFalse(StudentField.STATUS.isSearchable());
        assertFalse(StudentField.CATEGORY.isSearchable());
    }

    /**
     * 「全部字段」不能当排序字段——它是「多列一起比」的意思，没有对应的排序方式；其余都能排。
     */
    @Test
    void sortableExcludesAllOnly() {
        assertFalse(StudentField.ALL.isSortable());

        StudentField[] all = StudentField.values();
        int index = 1;
        while (index < all.length) {
            assertTrue(all[index].isSortable(), all[index] + " 应可排序");
            index = index + 1;
        }
    }

    /**
     * 两个子集都与 {@code values()} 同序：下拉里选项的先后应当是稳定的。
     */
    @Test
    void subsetsKeepDeclarationOrder() {
        StudentField[] searchable = StudentField.searchable();
        assertEquals(StudentField.ALL, searchable[0], "默认项应排第一，用户不改也能用");
        assertEquals(StudentField.STUDENT_NO, searchable[1]);
        assertEquals(6, searchable.length);

        StudentField[] sortable = StudentField.sortable();
        assertEquals(StudentField.values().length - 1, sortable.length);
        assertEquals(StudentField.PROFILE_ID, sortable[0], "客户端默认排序是主键，对应列表第一列");
    }

    /**
     * 显示名能来回翻译：界面显示什么、协议里传什么，用的是同一份文字。
     */
    @Test
    void displayNamesRoundTrip() {
        StudentField[] all = StudentField.values();
        int index = 0;
        while (index < all.length) {
            assertEquals(all[index], StudentField.fromDisplayName(all[index].getDisplayName()));
            index = index + 1;
        }
        assertNull(StudentField.fromDisplayName("不存在的字段"));
        assertNull(StudentField.fromDisplayName(null));
    }
}
