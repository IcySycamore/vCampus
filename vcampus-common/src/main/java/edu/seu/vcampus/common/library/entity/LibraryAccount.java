package edu.seu.vcampus.common.library.entity;

import edu.seu.vcampus.common.library.LibraryPolicy;
import java.io.Serializable;
import java.util.Date;

/**
 * 校园用户的图书馆读者账户。
 *
 * <p>账户保存稳定的读者身份、借阅上限和账户状态。借阅、预约及罚款明细由各自
 * 的记录实体保存，避免在账户中维护容易失真的汇总副本。</p>
 */
public class LibraryAccount implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String userUuid;
    private LibraryAccountStatus status = LibraryAccountStatus.NORMAL;
    private int borrowLimit;
    private Date createdAt;
    private Date updatedAt;
    private boolean deleted;

    /** 创建供序列化框架或 DAO 填充的空账户。 */
    public LibraryAccount() {
    }

    /**
     * 创建正常状态的读者账户。
     * @param userUuid 所属用户 UUID
     * @param borrowLimit 同时借阅上限
     * @param createdAt 建档时间
     */
    public LibraryAccount(String userUuid, int borrowLimit, Date createdAt) {
        setUserUuid(userUuid);
        setBorrowLimit(borrowLimit);
        setCreatedAt(createdAt);
        setUpdatedAt(createdAt);
    }

    /** @return 图书馆账户主键 */
    public Long getId() {
        return id;
    }

    /** @param id 图书馆账户主键 */
    public void setId(Long id) {
        if (id != null && id.longValue() <= 0L) {
            throw new IllegalArgumentException("id must be positive");
        }
        this.id = id;
    }

    /** @return 所属用户 UUID */
    public String getUserUuid() {
        return userUuid;
    }

    /** @param userUuid 所属用户 UUID；账户建立后不能更换归属 */
    public void setUserUuid(String userUuid) {
        if (userUuid == null || userUuid.trim().length() == 0) {
            throw new IllegalArgumentException("userUuid must not be blank");
        }
        if (this.userUuid != null && !this.userUuid.equals(userUuid)) {
            throw new IllegalStateException("library account owner cannot be changed");
        }
        this.userUuid = userUuid;
    }

    /** @return 账户状态 */
    public LibraryAccountStatus getStatus() {
        return status;
    }

    /** @param status 账户状态 */
    public void setStatus(LibraryAccountStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        this.status = status;
        touch();
    }

    /** @return 同时借阅上限 */
    public int getBorrowLimit() {
        return borrowLimit;
    }

    /** @param borrowLimit 同时借阅上限，范围为 0 到系统上限 */
    public void setBorrowLimit(int borrowLimit) {
        if (borrowLimit < 0 || borrowLimit > LibraryPolicy.BORROW_LIMIT) {
            throw new IllegalArgumentException("borrowLimit is outside the allowed range");
        }
        this.borrowLimit = borrowLimit;
        touch();
    }

    /** @return 建档时间的副本 */
    public Date getCreatedAt() {
        return copy(createdAt);
    }

    /** @param createdAt 建档时间 */
    public void setCreatedAt(Date createdAt) {
        this.createdAt = copy(createdAt);
    }

    /** @return 最后更新时间的副本 */
    public Date getUpdatedAt() {
        return copy(updatedAt);
    }

    /** @param updatedAt 最后更新时间 */
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = copy(updatedAt);
    }

    /** @return 是否已随用户账户软删除 */
    public boolean isDeleted() {
        return deleted;
    }

    /** @return 当前账户是否允许新增借阅、续借和预约 */
    public boolean isOperational() {
        return !deleted && status == LibraryAccountStatus.NORMAL;
    }

    /** @param at 软删除时间 */
    public void markDeleted(Date at) {
        deleted = true;
        updatedAt = copy(at);
    }

    private void touch() {
        if (createdAt != null) {
            updatedAt = new Date();
        }
    }

    private static Date copy(Date value) {
        return value == null ? null : new Date(value.getTime());
    }
}
