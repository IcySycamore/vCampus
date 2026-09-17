package edu.seu.vcampus.client.view.bank;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.bank.BankService;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.common.bank.dto.BankAdminAccountView;
import edu.seu.vcampus.common.constant.StatusCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JPasswordField;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/** 管理员重置密码任务的敏感数据生命周期测试。 */
class ResetBankPasswordDialogTest {

    @Test
    void resetFormProvidesTwoEditablePasswordInputs() {
        ResetBankPasswordForm form = new ResetBankPasswordForm();

        assertEquals(2, form.getComponentCount());
        assertEditableInput(form.newPasswordInput(),
                ResetBankPasswordForm.NEW_PASSWORD_INPUT);
        assertEditableInput(form.confirmationInput(),
                ResetBankPasswordForm.CONFIRM_PASSWORD_INPUT);
    }

    @Test
    void keepsPasswordUntilRequestCompletesThenClearsIt() {
        final BankService api = mock(BankService.class);
        final char[] password = "new-pass!!".toCharArray();
        final char[] expected = password.clone();
        final AtomicBoolean received = new AtomicBoolean();
        doAnswer(new Answer<BankAdminAccountView>() {
            @Override
            public BankAdminAccountView answer(InvocationOnMock invocation) {
                assertArrayEquals(expected, (char[]) invocation.getArguments()[1]);
                received.set(true);
                return null;
            }
        }).when(api).resetPassword("student", password);

        UiTasks.Task<BankAdminAccountView> task =
                ResetBankPasswordDialog.createResetTask(api, "student", password);
        task.run();

        assertTrue(received.get());
        assertArrayEquals(cleared(password.length), password);
    }

    @Test
    void clearsPasswordWhenRequestFails() {
        final BankService api = mock(BankService.class);
        final char[] password = "new-pass!!".toCharArray();
        doThrow(new ApiException(StatusCode.INTERNAL_ERROR))
                .when(api).resetPassword("student", password);
        final UiTasks.Task<BankAdminAccountView> task =
                ResetBankPasswordDialog.createResetTask(api, "student", password);

        assertThrows(ApiException.class, new Executable() {
            @Override
            public void execute() {
                task.run();
            }
        });
        assertArrayEquals(cleared(password.length), password);
    }

    private static char[] cleared(int length) {
        return new char[length];
    }

    private static void assertEditableInput(JPasswordField input, String expectedName) {
        assertEquals(expectedName, input.getName());
        assertTrue(input.isEditable());
        assertTrue(input.isEnabled());
        assertTrue(input.getPreferredSize().height >= 36);
    }
}
