package edu.seu.vcampus.client.api;

import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * ClientApis 装配测试：容器只读、模块自装配、会话随连接失效。
 */
class ClientApisTest {

    /** 容器暴露用户管理 API，且每个分发器装配出独立实例。 */
    @Test
    void assemblesUserApiPerDispatcher() {
        ClientApis first = ClientApis.create(new ClientMessageDispatcher());
        ClientApis second = ClientApis.create(new ClientMessageDispatcher());

        assertNotNull(first.user());
        assertNotNull(second.user());
        assertNotSame(first.user(), second.user());
    }

    /** 连接断开后容器内的 API 仍可安全查询，且没有悬挂登录态。 */
    @Test
    void connectionClosedLeavesApiUsable() {
        ClientMessageDispatcher dispatcher = new ClientMessageDispatcher();
        ClientApis apis = ClientApis.create(dispatcher);

        dispatcher.connectionClosed(new IOException("peer reset"));

        assertFalse(apis.user().isLoggedIn());
    }

    /** 分发器为 null 时拒绝装配。 */
    @Test
    void rejectsNullDispatcher() {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                ClientApis.create(null);
            }
        });
    }
}
