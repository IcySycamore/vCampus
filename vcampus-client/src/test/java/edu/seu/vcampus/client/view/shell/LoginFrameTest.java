package edu.seu.vcampus.client.view.shell;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import javax.swing.AbstractButton;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 登录页的行为约定：只有「账号 + 密码」一条进门路径。
 *
 * <p>
 * 这里刻意不测「能不能画出窗口」那种什么都不会失败的东西 —— 它测的是两条真实约束： 页面上不存在任何免登录入口，以及空表单不会被放行去建连。
 */
class LoginFrameTest {

    /** 免登录入口的文案特征；出现任意一个就说明匿名通道又回来了。 */
    private static final String[] ANONYMOUS_ENTRY_WORDS = { "预览", "匿名", "游客", "体验", "跳过" };

    private LoginFrame frame;

    /** 无显示环境（headless）时跳过：Swing 顶层窗口建不出来。 */
    @BeforeEach
    void requireDisplay() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
                "无显示环境（headless），跳过需要真实窗口的用例");
        frame = new LoginFrame();
    }

    @Test
    void loginPageOffersNoAnonymousEntry() {
        assertNotNull(frame.loginButton(), "登录页必须有登录按钮");
        assertEquals("登  录", frame.loginButton().getText());
        assertNoAnonymousEntry(frame);
    }

    @Test
    void blankCredentialsAreRejectedWithoutConnecting() {
        frame.loginButton().doClick();
        assertEquals("请输入账号和密码", frame.message(),
                "空表单应停在提示上，不能进入建连流程");
    }

    private static void assertNoAnonymousEntry(Container container) {
        for (Component child : container.getComponents()) {
            if (child instanceof AbstractButton) {
                String text = ((AbstractButton) child).getText();
                if (text != null) {
                    for (String word : ANONYMOUS_ENTRY_WORDS) {
                        assertFalse(text.contains(word),
                                "登录页不应出现免登录入口，却发现按钮：" + text);
                    }
                }
            }
            if (child instanceof Container) {
                assertNoAnonymousEntry((Container) child);
            }
        }
    }
}
