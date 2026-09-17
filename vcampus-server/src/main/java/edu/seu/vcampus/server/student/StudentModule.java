package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.server.user.AccountProvisioning;
import edu.seu.vcampus.server.user.AuthModule;
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
     * 登记学籍模块全部命令，并把学籍开户钩子接入账户生命周期（建号即可查到自己的学籍）。
     *
     * @param dispatcher   应用共享的消息分发器
     * @param sessions     全服唯一的会话表（命令级鉴权复用）
     * @param provisioning 开户钩子登记表；null 表示不为新账号建档
     * @throws IllegalArgumentException 必要参数为 null
     */
    public static void register(ServerMessageDispatcher dispatcher, SessionManager sessions,
            AccountProvisioning provisioning) {
        if (dispatcher == null || sessions == null) {
            throw new IllegalArgumentException("dispatcher and sessions must not be null");
        }
        StudentDao dao = openStudentDao();
        StudentService studentService = new StudentService(dao,
                openRequestDao(), AuthModule.repository());
        StudentMessageHandler handler = new StudentMessageHandler(studentService, sessions);
        dispatcher.register(Command.STUDENT_QUERY, handler);
        dispatcher.register(Command.STUDENT_MODIFY_APPLY, handler);
        dispatcher.register(Command.STUDENT_MODIFY_AUDIT, handler);
        dispatcher.register(Command.STUDENT_MODIFY_LIST, handler);
        dispatcher.register(Command.STUDENT_LIST, handler);
        dispatcher.register(Command.STUDENT_REGISTER, handler);
        dispatcher.register(Command.STUDENT_DELETE, handler);
        dispatcher.register(Command.STUDENT_CHANGE_STATUS, handler);
        if (provisioning != null) {
            provisioning.add(new StudentProvisioner(dao));
        }
    }

    /**
     * 打开学籍档案存储（落地 MySQL，重启后档案与专业不丢）。
     *
     * <p>
     * 早先用内存实现，服务端一重启，开户钩子虽然会把档案补回来，但专业 / 入学年份以及学生 做过的修改全被抹平，界面上就是「我刚填的东西又没了」。现在只走数据库，没有回退：
     * 缺库属于配置错误，取连接时就该失败，而不是静默退回内存。
     *
     * @return 学籍存储
     */
    private static StudentDao openStudentDao() {
        return new StudentDaoJdbc();
    }

    /**
     * 打开修改申请单存储
     *
     * @return 申请单存储
     */
    private static StudentModifyRequestDao openRequestDao() {
        return new StudentModifyRequestDaoJdbc();
    }
}
