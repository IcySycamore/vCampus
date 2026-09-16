package edu.seu.vcampus.client.library;

import edu.seu.vcampus.client.network.ClientMessageDispatcher;
import edu.seu.vcampus.client.user.UserService;

/** 图书馆客户端模块装配，借用已存在的用户 API 和连接。 */
public final class LibraryModule {
    private LibraryModule() {
    }

    /**
     * 装配图书馆 API。
     * @param dispatcher 共享分发器
     * @param users 同一连接上的用户 API
     * @return 图书馆 API
     */
    public static LibraryService register(ClientMessageDispatcher dispatcher, UserService users) {
        return new LibraryService(dispatcher, users);
    }
}
