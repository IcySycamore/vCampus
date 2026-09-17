package edu.seu.vcampus.common.shop.dto;

import edu.seu.vcampus.common.shop.entity.ShopOrder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 订单列表分页查询响应。
 */
public final class OrderListResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<ShopOrder> orders;
    private final int pageNumber;
    private final int pageSize;
    private final long totalCount;

    /**
     * 创建订单分页响应。
     *
     * @param orders 当前页订单，不能为空且不能包含空元素
     * @param pageNumber 当前页码，从 1 开始
     * @param pageSize 每页记录数，范围为 1 至 100
     * @param totalCount 满足条件的订单总数，不能为负
     */
    public OrderListResponse(List<ShopOrder> orders,
            int pageNumber, int pageSize, long totalCount) {
        if (orders == null) {
            throw new IllegalArgumentException("orders must not be null");
        }
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be at least one");
        }
        if (pageSize < 1 || pageSize > OrderQuery.MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize must be between one and 100");
        }
        if (orders.size() > pageSize) {
            throw new IllegalArgumentException("orders must not exceed pageSize");
        }
        if (totalCount < orders.size()) {
            throw new IllegalArgumentException("totalCount must cover current page");
        }
        this.orders = immutableCopy(orders);
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
    }

    /** @return 不可修改的当前页订单列表 */
    public List<ShopOrder> getOrders() {
        return orders;
    }

    /** @return 当前页码 */
    public int getPageNumber() {
        return pageNumber;
    }

    /** @return 每页记录数 */
    public int getPageSize() {
        return pageSize;
    }

    /** @return 满足条件的订单总数 */
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

    private static List<ShopOrder> immutableCopy(List<ShopOrder> orders) {
        List<ShopOrder> copy = new ArrayList<ShopOrder>(orders.size());
        for (ShopOrder order : orders) {
            if (order == null) {
                throw new IllegalArgumentException("orders must not contain null");
            }
            copy.add(order);
        }
        return Collections.unmodifiableList(copy);
    }
}
