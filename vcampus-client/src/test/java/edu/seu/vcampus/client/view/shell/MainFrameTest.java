package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.client.api.ClientApis;
import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 主窗口的进门条件：没有已认证的会话就不给开。
 *
 * <p>
 * 这条约束曾经被两个「不接 API」的构造器绕过 —— 它们凭空造一个会话记录就能把主界面开出来， 于是「离线预览」那类免登录入口能进主界面，里面还顶着一个假身份。本测试盯住两点：
 * 没有会话必须拒绝，以及那两个构造器不许再回来。
 */
class MainFrameTest {

    @Test
    void refusesToOpenWithoutApis() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        MainFrame.requireSession(null);
                    }
                });
        assertTrue(error.getMessage().contains("未登录"), "异常要说明原因，不能只说参数为 null");
    }

    @Test
    void refusesToOpenBeforeLogin() {
        final ClientApis apis = ClientApis.create(new ClientMessageDispatcher());
        assertNull(apis.user().currentSession(), "未登录时不该有会话记录");
        assertThrows(IllegalStateException.class, new Executable() {
            @Override
            public void execute() {
                MainFrame.requireSession(apis);
            }
        });
    }

    @Test
    void onlyAcceptsAnAuthenticatedApiContainer() {
        Constructor<?>[] constructors = MainFrame.class.getConstructors();
        assertEquals(1, constructors.length,
                "主窗口只应有一个入口；多出来的入口就等于多一条免登录路径");
        assertEquals(ClientApis.class, constructors[0].getParameterTypes()[0]);
    }
}
