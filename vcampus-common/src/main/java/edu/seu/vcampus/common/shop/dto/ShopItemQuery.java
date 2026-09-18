package edu.seu.vcampus.common.shop.dto;

import java.io.Serializable;

/**
 * 商品查询请求。
 *
 * <p>用于分页查询商品列表,支持按关键词、分类等条件筛选。
 */
public final class ShopItemQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 默认页码。 */
    public static final int DEFAULT_PAGE_NUMBER = 1;

    /** 默认每页记录数。 */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** 单页允许的最大记录数。 */
    public static final int MAX_PAGE_SIZE = 100;

    private final int pageNumber;
    private final int pageSize;
    private final String keyword;

    /** 创建使用默认分页参数的请求。 */
    public ShopItemQuery() {
        this(DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE, null);
    }

    /**
     * 创建商品查询请求。
     *
     * @param pageNumber 页码，从 1 开始
     * @param pageSize 每页记录数，范围为 1 至 100
     */
    public ShopItemQuery(int pageNumber, int pageSize) {
        this(pageNumber, pageSize, null);
    }

    /**
     * 创建商品查询请求。
     *
     * @param pageNumber 页码，从 1 开始
     * @param pageSize 每页记录数，范围为 1 至 100
     * @param keyword 搜索关键词；为空表示查询全部
     */
    public ShopItemQuery(int pageNumber, int pageSize, String keyword) {
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be at least one");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize must be between one and 100");
        }
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.keyword = keyword;
    }

    /** @return 页码，从 1 开始 */
    public int getPageNumber() {
        return pageNumber;
    }

    /** @return 每页记录数 */
    public int getPageSize() {
        return pageSize;
    }

    /** @return 搜索关键词；为空表示查询全部 */
    public String getKeyword() {
        return keyword;
    }
}
