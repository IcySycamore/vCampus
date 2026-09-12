package edu.seu.vcampus.common.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 分页响应：所有列表类接口的统一返回形态。
 *
 * <p>
 * 页码从 1 开始；每页条数默认 {@value #DEFAULT_PAGE_SIZE}，服务端会把超过
 * {@value #MAX_PAGE_SIZE} 的请求值截断到上限，避免一次拉爆数据。
 *
 * <p>
 * 放在 common 是因为请求与响应双端共用：界面读页信息渲染分页条，服务端负责填。
 *
 * @param <T> 列表元素类型
 */
public class PageResponse<T> implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 每页默认条数。 */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** 每页最大条数（超出按此上限截断）。 */
    public static final int MAX_PAGE_SIZE = 100;

    /** 当前页数据（可能为空列表，但不会是 null）。 */
    private final List<T> m_items;

    /** 满足条件的总条数（用于计算总页数）。 */
    private final long m_total;

    /** 当前页码（从 1 开始）。 */
    private final int m_page_number;

    /** 每页条数。 */
    private final int m_page_size;

    /**
     * 构造分页响应。
     *
     * @param items      当前页数据；null 视为空列表
     * @param total      满足条件的总条数
     * @param pageNumber 当前页码（小于 1 时按 1 处理）
     * @param pageSize   每页条数（小于 1 时按默认值处理）
     */
    public PageResponse(List<T> items, long total, int pageNumber, int pageSize) {
        this.m_items = items == null
                ? new ArrayList<T>()
                : Collections.unmodifiableList(new ArrayList<T>(items));
        this.m_total = total < 0L ? 0L : total;
        this.m_page_number = pageNumber < 1 ? 1 : pageNumber;
        this.m_page_size = pageSize < 1 ? DEFAULT_PAGE_SIZE : pageSize;
    }

    /**
     * 构造一个空页（无数据时使用，避免调用方判空）。
     *
     * @param pageNumber 当前页码
     * @param pageSize   每页条数
     * @param <T>        元素类型
     * @return 空页
     */
    public static <T> PageResponse<T> empty(int pageNumber, int pageSize) {
        return new PageResponse<T>(new ArrayList<T>(), 0L, pageNumber, pageSize);
    }

    /** @return 当前页数据（只读，非 null） */
    public List<T> getItems() {
        return m_items;
    }

    /** @return 满足条件的总条数 */
    public long getTotal() {
        return m_total;
    }

    /** @return 当前页码（从 1 开始） */
    public int getPageNumber() {
        return m_page_number;
    }

    /** @return 每页条数 */
    public int getPageSize() {
        return m_page_size;
    }

    /**
     * @return 总页数；至少为 1，便于界面直接显示「第 1/1 页」而不是「第 1/0 页」
     */
    public int getTotalPages() {
        if (m_page_size < 1) {
            return 1;
        }
        final long pages = (m_total + m_page_size - 1L) / m_page_size;
        return pages < 1L ? 1 : (int) pages;
    }

    /** @return 是否存在下一页 */
    public boolean hasNext() {
        return m_page_number < getTotalPages();
    }

    /** @return 是否存在上一页 */
    public boolean hasPrevious() {
        return m_page_number > 1;
    }

    /**
     * 把请求页参数规范化为合法值（页码至少 1、页大小落在 [1, MAX_PAGE_SIZE]）。
     *
     * @param pageNumber 请求页码
     * @param pageSize   请求页大小
     * @return 长度为 2 的数组：[页码, 页大小]
     */
    public static int[] normalize(int pageNumber, int pageSize) {
        int size = pageSize;
        if (size < 1) {
            size = DEFAULT_PAGE_SIZE;
        }
        if (size > MAX_PAGE_SIZE) {
            size = MAX_PAGE_SIZE;
        }
        int number = pageNumber < 1 ? 1 : pageNumber;
        return new int[] {number, size};
    }
}
