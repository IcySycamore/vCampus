package edu.seu.vcampus.server.student;

import java.util.ArrayList;
import java.util.List;

/**
 * 内存实现共用的分页切片工具。
 *
 * <p>
 * 学籍记录与修改申请单两个内存 DAO 都需要「按 offset/limit 取一段」，抽到一处是为了避免
 * 两套边界处理出现分歧（空集合、offset 越界、limit 非正、offset 为负这些情况很容易只在一处
 * 写对）。将来的 JDBC 实现用 {@code LIMIT ? OFFSET ?} 承担同一职责，无需本工具。
 */
final class PageSlice {

    /**
     * 私有构造器，禁止实例化工具类。
     */
    private PageSlice() {
    }

    /**
     * 取指定区间。
     *
     * @param all    已排序的全量结果
     * @param offset 起始下标（小于 0 按 0 处理）
     * @param limit  最多返回条数（非正时返回空列表）
     * @param <T>    元素类型
     * @return 切片结果（新列表，可安全修改）
     */
    static <T> List<T> of(List<T> all, int offset, int limit) {
        if (all == null || limit <= 0 || offset >= all.size()) {
            return new ArrayList<T>();
        }
        int from = offset < 0 ? 0 : offset;
        int to = Math.min(from + limit, all.size());
        return new ArrayList<T>(all.subList(from, to));
    }
}
