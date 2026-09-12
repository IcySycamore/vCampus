package edu.seu.vcampus.client.student;

import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.client.user.UserService;

/**
 * 学籍客户端模块装配入口：与 server 的 {@code StudentModule.register} 对称。
 *
 * <p>
 * 学籍不带自己的处理器（查询结果全部由同步方法直接返回，不走向 UI 的异步回调），所以本方法
 * 只新建服务。token 由用户模块 API 现取（{@code userService.currentToken()}）——
 * 登录/登出会换 token，现取才能保证学籍请求始终带着当前有效的那个。
 */
public final class StudentModule {

    /** 私有构造器，禁止实例化装配入口。 */
    private StudentModule() {
    }

    /**
     * 装配学籍客户端模块。
     *
     * @param dispatcher 客户端消息分发器
     * @param user 用户模块 API（提供当前 token）
     * @return 学籍客户端服务
     * @throws IllegalArgumentException 参数为 null
     */
    public static StudentService register(ClientMessageDispatcher dispatcher,
            UserService user) {
        if (dispatcher == null || user == null) {
            throw new IllegalArgumentException("dispatcher and user must not be null");
        }
        return new StudentService(dispatcher, user);
    }
}
