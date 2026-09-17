package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.dto.StudentQuery;
import edu.seu.vcampus.common.student.entity.StudentField;
import edu.seu.vcampus.common.student.entity.StudentProfile;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 学籍排序：按查询条件指定的字段与方向，就地排好一批档案。
 *
 * <p>
 * 与 {@link StudentMatcher} 配对：先过滤、再排序、最后才切片（见 {@link StudentService#listStudents}）。
 * 排序必须在切片之前，否则排的只是「当前这一页」——页内有序、页间乱序，翻页时看得最清楚。
 *
 * <p>
 * <b>为什么必须有默认排序</b>：存储层是 {@code HashMap}，迭代顺序既不稳定也不可预期，同样一批
 * 数据两次查询可能给出不同顺序。不排序的结果就是「刷新一下行就换位置」，看起来像丢了数据。
 * 所以未指定排序字段时一律按主键升序兜底。
 *
 * <p>
 * 空值（例如没有学号、没采到姓名）统一排在后面：把空值排在最前会让第一页全是空白行，比不加排序
 * 还难用。
 *
 * <p>
 * <b>文本比较用 {@link Collator} 而不是 {@code String.compareTo}</b>：后者按 Unicode 码位排，
 * 中文姓名会排成「张、李、陈」这种谁看都不像有规律的样子（码位顺序：张 5F20 &lt; 李 674E &lt;
 * 陈 9648，与拼音完全无关）。Collator 按中文拼音排，得到的是「陈、李、张」。
 */
final class StudentSorter {

    /** 私有构造器，禁止实例化工具类。 */
    private StudentSorter() {
    }

    /**
     * 按 {@link StudentQuery} 指定的字段与方向排序。
     *
     * @param profiles 待排序列表（就地排序）
     * @param query 查询条件；null 或未指定可排序字段时按主键升序
     */
    static void sort(List<StudentProfile> profiles, StudentQuery query) {
        if (profiles == null || profiles.size() < 2) {
            return;
        }
        final StudentField key = resolveKey(query);
        // Collator 不是线程安全的，所以每次排序新建一个、只在本次比较中使用，不共享。
        final Collator collator = Collator.getInstance(Locale.CHINA);
        Comparator<StudentProfile> comparator = new Comparator<StudentProfile>() {
            @Override
            public int compare(StudentProfile left, StudentProfile right) {
                return compareBy(left, right, key, collator);
            }
        };
        if (query != null && query.isDescending()) {
            comparator = Collections.reverseOrder(comparator);
        }
        Collections.sort(profiles, comparator);
    }

    /**
     * 取生效的排序字段：没指定、或指定了不能排序的值（ALL）时回退到主键。
     *
     * @param query 查询条件（可为 null）
     * @return 排序字段，永不为 null
     */
    private static StudentField resolveKey(StudentQuery query) {
        if (query == null || query.getSortBy() == null
                || !query.getSortBy().isSortable()) {
            return StudentField.PROFILE_ID;
        }
        return query.getSortBy();
    }

    /**
     * 按字段比较两条档案。
     *
     * @param left 左
     * @param right 右
     * @param key 排序字段
     * @param collator 文本比较器（按中文拼音）
     * @return 比较结果
     */
    private static int compareBy(StudentProfile left, StudentProfile right, StudentField key,
            Collator collator) {
        if (key == StudentField.JOIN_YEAR) {
            return Integer.compare(left.getJoinYear(), right.getJoinYear());
        }
        if (key == StudentField.STATUS) {
            return compareOrdinals(left.getStatus(), right.getStatus());
        }
        if (key == StudentField.CATEGORY) {
            return compareOrdinals(left.getPersonCategory(), right.getPersonCategory());
        }
        if (key == StudentField.STUDENT_NO) {
            return compareText(left.getStudentNo(), right.getStudentNo(), collator);
        }
        if (key == StudentField.REAL_NAME) {
            return compareText(left.getRealName(), right.getRealName(), collator);
        }
        if (key == StudentField.USER_UUID) {
            return compareText(left.getUserUuid(), right.getUserUuid(), collator);
        }
        if (key == StudentField.FIELD) {
            return compareText(left.getField(), right.getField(), collator);
        }
        return compareNumbers(left.getId(), right.getId());
    }

    /**
     * 比较两个可空数值，空值排在后面。
     *
     * @param left 左
     * @param right 右
     * @return 比较结果
     */
    private static int compareNumbers(Long left, Long right) {
        if (left == null) {
            return right == null ? 0 : 1;
        }
        if (right == null) {
            return -1;
        }
        return left.compareTo(right);
    }

    /**
     * 比较两个可空文本（按中文拼音），空值排在后面。
     *
     * @param left 左
     * @param right 右
     * @param collator 文本比较器
     * @return 比较结果
     */
    private static int compareText(String left, String right, Collator collator) {
        if (left == null) {
            return right == null ? 0 : 1;
        }
        if (right == null) {
            return -1;
        }
        return collator.compare(left, right);
    }

    /**
     * 比较两个可空枚举，按声明序排列（在校 → 暂离 → 离校 → 毕业 → 退休），空值排在后面。
     *
     * <p>
     * 用枚举声明序而不是显示名字符串比较：后者会按拼音/笔画乱排，而声明序本身就是业务上
     * 「从在校到离校」的自然顺序，正好是人看状态时想看到的排法。
     *
     * @param left 左
     * @param right 右
     * @param &lt;T&gt; 枚举类型
     * @return 比较结果
     */
    private static <T extends Enum<T>> int compareOrdinals(T left, T right) {
        if (left == null) {
            return right == null ? 0 : 1;
        }
        if (right == null) {
            return -1;
        }
        return Integer.compare(left.ordinal(), right.ordinal());
    }
}
