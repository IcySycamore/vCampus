package edu.seu.vcampus.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 服务器端冒烟测试：验证测试框架与本模块可正常编译、加载、执行。
 */
class VCampusServerAppTest {

    /**
     * 占位冒烟断言，确保 surefire + JUnit5 工作正常。
     */
    @Test
    void contextLoads() {
        assertTrue(true);
    }

    /** 显式图书馆启动入口拒绝不完整注入。 */
    @Test
    void rejectsMissingLibraryService() {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                VCampusServerApp.startServer(0, null);
            }
        });
    }
}
