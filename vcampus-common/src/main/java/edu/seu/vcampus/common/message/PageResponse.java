package edu.seu.vcampus.common.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 分页响应载荷：列表类查询的统一返回形态（见 ADR-0009 D4）。
 *
 * <p>
 * 请求侧的分页参数默认 1/20、单页上限 100，与 {@code BankTransactionQueryRequest} 既有约定一致；
 * 服务端返回本对象时同时给出 {@code total}，客户端据此渲染分页条。
 *
 * @param <T> 记录类型，必须可序列化
 */
public class PageResponse<T> implements Serializable {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 默认页码（从 1 开始）。 */
    public static final int DEFAULT_PAGE_NUMBER = 1;

    /** 默认每页记录数。 */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** 单页允许的最大记录数。 */
    public static final int MAX_PAGE_SIZE = 100;

    /** 当前页记录。 */
    private final List<T> m_items;

    /** 满足条件的记录总数。 */
    private final long m_total;

    /** 当前页码，从 1 开始。 */
    private final int m_page_number;

    /** 每页记录数。 */
    private final int m_page_size;

    /**
     * 构造分页响应。
     *
     * @param items 当前页记录；null 视为空页
     * @param total 记录总数，负数视为 0
     * @param pageNumber 页码，从 1 开始
     * @param pageSize 每页记录数
     */
    public PageResponse(List<T> items, long total, int pageNumber, int pageSize) {
        this.m_items = items == null ? new ArrayList<T>() : new ArrayList<T>(items);
        this.m_total = total < 0L ? 0L : total;
        this.m_page_number = normalizePageNumber(pageNumber);
        this.m_page_size = normalizePageSize(pageSize);
    }

    /**
     * 构造空页。
     *
     * @param <T> 记录类型
     * @return 空页（总数 0，使用默认分页参数）
     */
    public static <T> PageResponse<T> empty() {
        return new PageResponse<T>(null, 0L, DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE);
    }

    /**
     * 归一化页码：小于 1 一律取 1。
     *
     * @param pageNumber 原始页码
     * @return 合法页码
     */
    public static int normalizePageNumber(int pageNumber) {
        return pageNumber < DEFAULT_PAGE_NUMBER ? DEFAULT_PAGE_NUMBER : pageNumber;
    }

    /**
     * 归一化每页记录数：小于 1 取默认值，超过上限取上限。
     *
     * @param pageSize 原始每页记录数
     * @return 合法每页记录数
     */
    public static int normalizePageSize(int pageSize) {
        if (pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return pageSize > MAX_PAGE_SIZE ? MAX_PAGE_SIZE : pageSize;
    }

    /**
     * 计算分页偏移量（供 DAO 的 limit/offset 使用）。
     *
     * @param pageNumber 页码，从 1 开始
     * @param pageSize 每页记录数
     * @return 偏移量，非负
     */
    public static int offsetOf(int pageNumber, int pageSize) {
        int number = normalizePageNumber(pageNumber);
        int size = normalizePageSize(pageSize);
        return (number - 1) * size;
    }

    /** @return 当前页记录（只读视图） */
    public List<T> getItems() {
        return Collections.unmodifiableList(m_items);
    }

    /** @return 记录总数 */
    public long getTotal() {
        return m_total;
    }

    /** @return 当前页码 */
    public int getPageNumber() {
        return m_page_number;
    }

    /** @return 每页记录数 */
    public int getPageSize() {
        return m_page_size;
    }

    /** @return 总页数（空结果返回 0） */
    public int getTotalPages() {
        if (m_total <= 0L) {
            return 0;
        }
        return (int) ((m_total + m_page_size - 1L) / m_page_size);
    }

    /** @return 是否还有下一页 */
    public boolean hasNext() {
        return m_page_number < getTotalPages();
    }

    /** @return 当前页是否为空 */
    public boolean isEmpty() {
        return m_items.isEmpty();
    }
}
