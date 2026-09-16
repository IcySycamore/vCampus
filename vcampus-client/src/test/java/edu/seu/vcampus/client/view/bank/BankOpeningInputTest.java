package edu.seu.vcampus.client.view.bank;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 只测试表单输入规则，不调用银行业务或认证服务。 */
class BankOpeningInputTest {
    @Test
    void checksRequiredFieldsPasswordLengthAndConfirmation() {
        char[] login = "login".toCharArray();
        char[] bank = "bank-password".toCharArray();
        assertNotNull(BankOpeningInput.validationMessage("", login, bank, bank));
        assertNotNull(BankOpeningInput.validationMessage("admin", new char[0], bank, bank));
        assertNotNull(BankOpeningInput.validationMessage("admin", login,
                "short".toCharArray(), "short".toCharArray()));
        assertNotNull(BankOpeningInput.validationMessage("admin", login, bank,
                "different".toCharArray()));
        assertNull(BankOpeningInput.validationMessage("admin", login, bank, bank));
    }
}
