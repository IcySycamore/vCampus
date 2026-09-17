package edu.seu.vcampus.client.view.bank;

import javax.swing.JButton;
import javax.swing.JPasswordField;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** 银行密码输入框的显示/隐藏行为测试。 */
class BankPasswordFieldTest {
    @Test
    void togglesPasswordVisibilityWithoutChangingTheValue() {
        BankPasswordField password = new BankPasswordField();
        JPasswordField input = (JPasswordField) password.getComponent(0);
        JButton toggle = (JButton) password.getComponent(1);
        input.setText("bank-pass");
        char maskedEcho = input.getEchoChar();

        toggle.doClick();
        assertEquals((char) 0, input.getEchoChar());
        assertEquals("隐藏", toggle.getText());
        assertArrayEquals("bank-pass".toCharArray(), input.getPassword());

        toggle.doClick();
        assertNotEquals((char) 0, input.getEchoChar());
        assertEquals(maskedEcho, input.getEchoChar());
        assertEquals("显示", toggle.getText());
        assertArrayEquals("bank-pass".toCharArray(), input.getPassword());

        password.clear();
        assertArrayEquals(new char[0], input.getPassword());
    }
}
