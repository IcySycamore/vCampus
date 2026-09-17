package edu.seu.vcampus.common.shop.dto;

import edu.seu.vcampus.common.shop.entity.ShopItem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 商品列表分页查询响应。
 */
public final class ShopItemListResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<ShopItem> items;
    private final int pageNumber;
    private final int pageSize;
    private final long totalCount;

    /**
     * 创建商品分页响应。
     *
     * @param items 当前页商品，不能为空且不能包含空元素
     * @param pageNumber 当前页码，从 1 开始
     * @param pageSize 每页记录数，范围为 1 至 100
     * @param totalCount 满足条件的商品总数，不能为负
     */
    public ShopItemListResponse(List<ShopItem> items,
            int pageNumber, int pageSize, long totalCount) {
        if (items == null) {
            throw new IllegalArgumentException("items must not be null");
        }
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be at least one");
        }
        if (pageSize < 1 || pageSize > ShopItemQuery.MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize must be between one and 100");
        }
        if (items.size() > pageSize) {
            throw new IllegalArgumentException("items must not exceed pageSize");
        }
        if (totalCount < items.size()) {
            throw new IllegalArgumentException("totalCount must cover current page");
        }
        this.items = immutableCopy(items);
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
    }

    /** @return 不可修改的当前页商品列表 */
    public List<ShopItem> getItems() {
        return items;
    }

    /** @return 当前页码 */
    public int getPageNumber() {
        return pageNumber;
    }

    /** @return 每页记录数 */
    public int getPageSize() {
        return pageSize;
    }

    /** @return 满足条件的商品总数 */
    public long getTotalCount() {
        return totalCount;
    }

    /** @return 总页数；无记录时返回零 */
    public long getTotalPages() {
        if (totalCount == 0) {
            return 0;
        }
        return (totalCount - 1) / pageSize + 1;
    }

    private static List<ShopItem> immutableCopy(List<ShopItem> items) {
        List<ShopItem> copy = new ArrayList<ShopItem>(items.size());
        for (ShopItem item : items) {
            if (item == null) {
                throw new IllegalArgumentException("items must not contain null");
            }
            copy.add(item);
        }
        return Collections.unmodifiableList(copy);
    }
}
