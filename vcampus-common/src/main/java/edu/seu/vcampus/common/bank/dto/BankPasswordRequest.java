package edu.seu.vcampus.common.bank.dto;

import java.io.Serializable;

/** 银行密码操作请求。当前协议在受信任的内部链路上传递字符数组。 */
public final class BankPasswordRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    private final char[] password;
    public BankPasswordRequest(char[] password) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("银行密码不能为空");
        }
        this.password = password.clone();
    }
    public char[] getPassword() { return password.clone(); }
}
