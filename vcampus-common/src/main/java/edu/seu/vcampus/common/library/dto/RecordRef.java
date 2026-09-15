package edu.seu.vcampus.common.library.dto;

import java.io.Serializable;

/** 通过记录号引用一条借阅记录。 */
public final class RecordRef implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long recordId;

    /**
     * 创建借阅记录引用。
     * @param recordId 借阅记录号
     */
    public RecordRef(long recordId) {
        this.recordId = recordId;
    }

    /** @return 借阅记录号 */
    public long getRecordId() {
        return recordId;
    }
}
