package edu.seu.vcampus.client.student;

import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.client.user.ClientSession;

/**
 * 学籍客户端模块装配入口：与 server 的 {@code StudentModule.register} 对称。
 *
 * <p>
 * 学籍不带自己的处理器（查询结果全部由同步方法直接返回，不走向 UI 的异步回调），所以本方法
 * 只新建服务。会话对象由应用入口从用户模块取（{@code userService.getSession()}）后传入，
 * 保证登录后学籍请求自动带上同一个 token，不必复制会话状态。
 */
public final class StudentModule {

    /** 私有构造器，禁止实例化装配入口。 */
    private StudentModule() {
    }

    /**
     * 装配学籍客户端模块。
     *
     * @param dispatcher 客户端消息分发器
     * @param session 与用户模块共用的内存会话
     * @return 学籍客户端服务
     * @throws IllegalArgumentException 参数为 null
     */
    public static StudentService register(ClientMessageDispatcher dispatcher,
            ClientSession session) {
        if (dispatcher == null || session == null) {
            throw new IllegalArgumentException("dispatcher and session must not be null");
        }
        return new StudentService(dispatcher, session);
    }
}
