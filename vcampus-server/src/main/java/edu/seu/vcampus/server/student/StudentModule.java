package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.server.user.InMemoryUserRepository;
import edu.seu.vcampus.server.user.SessionManager;
import edu.seu.vcampus.server.network.ServerMessageDispatcher;

/**
 * 学籍模块装配入口：登记学籍命令码与处理器。
 *
 * <p>
 * 与 {@code UserModule} / {@code BankModule} 同构，应用组装层只需调用
 * {@link #register(ServerMessageDispatcher, SessionManager)}，不必了解模块内部构造。
 */
public final class StudentModule {

    /** 私有构造器，禁止实例化装配入口。 */
    private StudentModule() {
    }

    /**
     * 登记学籍模块全部命令。
     *
     * @param dispatcher 应用共享的消息分发器
     * @param sessions 全服唯一的会话表（命令级鉴权复用）
     * @throws IllegalArgumentException 参数为 null
     */
    public static void register(ServerMessageDispatcher dispatcher, SessionManager sessions) {
        if (dispatcher == null || sessions == null) {
            throw new IllegalArgumentException("dispatcher and sessions must not be null");
        }
        StudentService studentService = new StudentService(new StudentDaoMemory(),
                new StudentModifyRequestDaoMemory(), InMemoryUserRepository.getInstance());
        StudentMessageHandler handler = new StudentMessageHandler(studentService, sessions);
        dispatcher.register(Command.STUDENT_QUERY, handler);
        dispatcher.register(Command.STUDENT_MODIFY_APPLY, handler);
        dispatcher.register(Command.STUDENT_MODIFY_AUDIT, handler);
        dispatcher.register(Command.STUDENT_MODIFY_LIST, handler);
        dispatcher.register(Command.STUDENT_LIST, handler);
        dispatcher.register(Command.STUDENT_REGISTER, handler);
        dispatcher.register(Command.STUDENT_DELETE, handler);
        dispatcher.register(Command.STUDENT_CHANGE_STATUS, handler);
    }
}
