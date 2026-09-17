package edu.seu.vcampus.client.course;

import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.client.user.UserService;

/**
 * 选课客户端模块装配入口：与 server 的 {@code CourseModule.register} 对称。
 */
public final class CourseModule {

    /** 私有构造器，禁止实例化装配入口。 */
    private CourseModule() {
    }

    /**
     * 装配选课客户端模块。
     *
     * @param dispatcher 客户端消息分发器
     * @param user 用户模块 API（提供当前 token）
     * @return 选课客户端服务
     * @throws IllegalArgumentException 参数为 null
     */
    public static CourseService register(ClientMessageDispatcher dispatcher,
            UserService user) {
        if (dispatcher == null || user == null) {
            throw new IllegalArgumentException("dispatcher and user must not be null");
        }
        return new CourseService(dispatcher, user);
    }
}
