package edu.seu.vcampus.client.course;

import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.client.user.UserService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 选课客户端服务装配测试。
 */
class CourseServiceTest {

    /** 装配入口在参数为 null 时拒绝。 */
    @Test
    void rejectsNullArguments() {
        final ClientMessageDispatcher dispatcher = new ClientMessageDispatcher();
        final UserService user = new UserService(dispatcher);

        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                CourseModule.register(null, user);
            }
        });
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                CourseModule.register(dispatcher, null);
            }
        });
    }

    /** 装配入口返回非空服务。 */
    @Test
    void assemblesService() {
        ClientMessageDispatcher dispatcher = new ClientMessageDispatcher();
        UserService user = new UserService(dispatcher);

        assertNotNull(CourseModule.register(dispatcher, user));
    }
}
