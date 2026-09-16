package edu.seu.vcampus.client.view.bank;

import java.util.Arrays;

/** 开户表单本地格式校验；不验证账户身份或保存密码。 */
final class BankOpeningInput {
    private BankOpeningInput() {
    }

    static String validationMessage(String name, char[] login, char[] bank, char[] confirmation) {
        if (name == null || name.trim().isEmpty() || login == null || login.length == 0) {
            return "请填写校园账号和登录密码";
        }
        if (bank == null || bank.length < 8 || bank.length > 64) {
            return "银行密码须为 8 至 64 个字符";
        }
        if (!Arrays.equals(bank, confirmation)) {
            return "两次输入的银行密码不一致";
        }
        return null;
    }
}
