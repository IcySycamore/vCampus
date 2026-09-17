package edu.seu.vcampus.common.library.dto;

import java.io.Serializable;

/** 缴纳滞纳金请求：借阅记录号与银行账户密码。 */
public final class FinePaymentRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long recordId;
    private final char[] password;

    /**
     * 创建缴纳滞纳金请求。
     * @param recordId 借阅记录号
     * @param password 银行账户密码
     */
    public FinePaymentRequest(long recordId, char[] password) {
        if (recordId <= 0) {
            throw new IllegalArgumentException("recordId must be positive");
        }
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("银行密码不能为空");
        }
        this.recordId = recordId;
        this.password = password.clone();
    }

    /** @return 借阅记录号 */
    public long getRecordId() {
        return recordId;
    }

    /** @return 银行账户密码的副本 */
    public char[] getPassword() {
        return password.clone();
    }
}
