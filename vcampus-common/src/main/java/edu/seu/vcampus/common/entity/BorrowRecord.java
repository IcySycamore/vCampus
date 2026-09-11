package edu.seu.vcampus.common.entity;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户的一条图书借阅记录。
 */
public class BorrowRecord implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long id;
    private String userId;
    private String isbn;
    private String bookTitle;
    private Date borrowedAt;
    private Date dueAt;
    private Date returnedAt;

    /** 创建空借阅记录。 */
    public BorrowRecord() {
    }

    /**
     * 创建尚未归还的借阅记录。
     *
     * @param userId 用户 ID
     * @param isbn ISBN
     * @param bookTitle 书名
     * @param borrowedAt 借出时间
     * @param dueAt 应还时间
     */
    public BorrowRecord(String userId, String isbn, String bookTitle,
            Date borrowedAt, Date dueAt) {
        this.userId = userId;
        this.isbn = isbn;
        this.bookTitle = bookTitle;
        this.borrowedAt = copy(borrowedAt);
        this.dueAt = copy(dueAt);
    }

    /** @return 记录号 */
    public Long getId() {
        return id;
    }

    /** @param id 记录号 */
    public void setId(Long id) {
        this.id = id;
    }

    /** @return 用户 ID */
    public String getUserId() {
        return userId;
    }

    /** @param userId 用户 ID */
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

    /** @return 书名 */
    public String getBookTitle() {
        return bookTitle;
    }

    /** @param bookTitle 书名 */
    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    /** @return 借出时间的副本 */
    public Date getBorrowedAt() {
        return copy(borrowedAt);
    }

    /** @param borrowedAt 借出时间 */
    public void setBorrowedAt(Date borrowedAt) {
        this.borrowedAt = copy(borrowedAt);
    }

    /** @return 应还时间的副本 */
    public Date getDueAt() {
        return copy(dueAt);
    }

    /** @param dueAt 应还时间 */
    public void setDueAt(Date dueAt) {
        this.dueAt = copy(dueAt);
    }

    /** @return 实际归还时间的副本，未归还时为 null */
    public Date getReturnedAt() {
        return copy(returnedAt);
    }

    /** @param returnedAt 实际归还时间 */
    public void setReturnedAt(Date returnedAt) {
        this.returnedAt = copy(returnedAt);
    }

    /** @return 是否已经归还 */
    public boolean isReturned() {
        return returnedAt != null;
    }

    private static Date copy(Date value) {
        return value == null ? null : new Date(value.getTime());
    }
}
