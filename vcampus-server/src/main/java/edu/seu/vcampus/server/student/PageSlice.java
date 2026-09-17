package edu.seu.vcampus.server.student;

import java.util.ArrayList;
import java.util.List;

/**
 * 分页切片工具：按 offset/limit 取一段，负责边界（空集合、offset 越界、limit 非正）。
 *
 * <p>
 * 学籍查询是「先把全量取回来、在内存里过滤排序」，所以切片只能在这里做 —— 查询条件里含 联查用户模块算出来的姓名，没条件整段下推成 SQL。抽到一处是为了避免两套边界处理出现分歧。
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
