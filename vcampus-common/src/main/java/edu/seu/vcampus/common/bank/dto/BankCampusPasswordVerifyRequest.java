package edu.seu.vcampus.common.bank.dto;

import java.io.Serializable;

/** 修改银行密码专用的校园密码 proof 请求。 */
public final class BankCampusPasswordVerifyRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String username;
    private final String proof;

    public BankCampusPasswordVerifyRequest(String username, String proof) {
        if (username == null || username.trim().length() == 0
                || proof == null || proof.trim().length() == 0) {
            throw new IllegalArgumentException("校园密码验证参数不能为空");
        }
        this.username = username.trim();
        this.proof = proof.trim();
    }

    public String getUsername() {
        return username;
    }

    public String getProof() {
        return proof;
    }
}
