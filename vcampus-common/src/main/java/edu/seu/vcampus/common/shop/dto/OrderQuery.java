package edu.seu.vcampus.common.shop.dto;

import edu.seu.vcampus.common.shop.entity.ShopOrderStatus;
import java.io.Serializable;

/**
 * 订单查询请求。
 *
 * <p>用于分页查询用户的订单列表。
 */
public final class OrderQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 默认页码。 */
    public static final int DEFAULT_PAGE_NUMBER = 1;

    /** 默认每页记录数。 */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** 单页允许的最大记录数。 */
    public static final int MAX_PAGE_SIZE = 100;

    private final int pageNumber;
    private final int pageSize;
    private final ShopOrderStatus status;
    private final String userUuid;

    /** 创建使用默认分页参数的请求。 */
    public OrderQuery() {
        this(DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE, null, null);
    }

    /**
     * 创建订单查询请求。
     *
     * @param pageNumber 页码，从 1 开始
     * @param pageSize 每页记录数，范围为 1 至 100
     */
    public OrderQuery(int pageNumber, int pageSize) {
        this(pageNumber, pageSize, null, null);
    }

    /**
     * 创建订单查询请求（包含筛选条件）。
     *
     * @param pageNumber 页码，从 1 开始
     * @param pageSize 每页记录数，范围为 1 至 100
     * @param status 订单状态筛选，null表示不筛选
     * @param userUuid 用户UUID筛选，null表示不筛选
     */
    public OrderQuery(int pageNumber, int pageSize, ShopOrderStatus status, String userUuid) {
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be at least one");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize must be between one and 100");
        }
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.status = status;
        this.userUuid = userUuid;
    }

    /** @return 页码，从 1 开始 */
    public int getPageNumber() {
        return pageNumber;
    }

    /** @return 每页记录数 */
    public int getPageSize() {
        return pageSize;
    }

    /** @return 订单状态筛选，null表示不筛选 */
    public ShopOrderStatus getStatus() {
        return status;
    }

    /** @return 用户UUID筛选，null表示不筛选 */
    public String getUserUuid() {
        return userUuid;
    }
}
