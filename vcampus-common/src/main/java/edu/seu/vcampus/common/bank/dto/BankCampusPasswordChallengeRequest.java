package edu.seu.vcampus.common.bank.dto;

import java.io.Serializable;

/** 修改银行密码专用的校园密码挑战请求。 */
public final class BankCampusPasswordChallengeRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String username;

    public BankCampusPasswordChallengeRequest(String username) {
        if (username == null || username.trim().length() == 0) {
            throw new IllegalArgumentException("校园账号不能为空");
        }
        this.username = username.trim();
    }

    public String getUsername() {
        return username;
    }
}
