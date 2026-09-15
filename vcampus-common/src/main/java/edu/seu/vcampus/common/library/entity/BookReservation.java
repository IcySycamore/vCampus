package edu.seu.vcampus.common.library.entity;

import java.io.Serializable;
import java.util.Date;

/** 用户对暂无库存图书提交的一条预约记录。 */
public class BookReservation implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String userId;
    private String isbn;
    private String bookTitle;
    private Date requestedAt;
    private Date readyAt;
    private Date expiresAt;
    private ReservationStatus status = ReservationStatus.WAITING;

    /** 创建空预约记录。 */
    public BookReservation() {
    }

    /**
     * 创建等待中的预约。
     * @param userId 用户 UUID
     * @param isbn ISBN
     * @param bookTitle 书名快照
     * @param requestedAt 申请时间
     */
    public BookReservation(String userId, String isbn, String bookTitle,
            Date requestedAt) {
        this.userId = userId;
        this.isbn = isbn;
        this.bookTitle = bookTitle;
        this.requestedAt = copy(requestedAt);
    }

    /** @return 预约号 */
    public Long getId() {
        return id;
    }

    /** @param id 预约号 */
    public void setId(Long id) {
        this.id = id;
    }

    /** @return 用户 UUID */
    public String getUserId() {
        return userId;
    }

    /** @param userId 用户 UUID */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /** @return ISBN */
    public String getIsbn() {
        return isbn;
    }

    /** @param isbn ISBN */
    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    /** @return 书名快照 */
    public String getBookTitle() {
        return bookTitle;
    }

    /** @param bookTitle 书名快照 */
    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    /** @return 申请时间副本 */
    public Date getRequestedAt() {
        return copy(requestedAt);
    }

    /** @param requestedAt 申请时间 */
    public void setRequestedAt(Date requestedAt) {
        this.requestedAt = copy(requestedAt);
    }

    /** @return 到馆时间副本 */
    public Date getReadyAt() {
        return copy(readyAt);
    }

    /** @param readyAt 到馆时间 */
    public void setReadyAt(Date readyAt) {
        this.readyAt = copy(readyAt);
    }

    /** @return 保留截止时间副本 */
    public Date getExpiresAt() {
        return copy(expiresAt);
    }

    /** @param expiresAt 保留截止时间 */
    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = copy(expiresAt);
    }

    /** @return 当前状态 */
    public ReservationStatus getStatus() {
        return status;
    }

    /** @param status 当前状态 */
    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    /** @return 是否仍占据队列或保留馆藏 */
    public boolean isActive() {
        return status == ReservationStatus.WAITING || status == ReservationStatus.READY;
    }

    private static Date copy(Date value) {
        return value == null ? null : new Date(value.getTime());
    }
}
