package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.constant.Command;
import edu.seu.vcampus.common.user.entity.Role;
import edu.seu.vcampus.server.user.AccountProvisioning;
import edu.seu.vcampus.server.user.AuthModule;
import edu.seu.vcampus.server.user.SessionManager;
import edu.seu.vcampus.server.user.UserRepository;
import edu.seu.vcampus.server.user.UserRepository.Credential;
import edu.seu.vcampus.server.network.ServerMessageDispatcher;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * 学籍模块装配入口：登记学籍命令码与处理器。
 *
 * <p>
 * 与 {@code UserModule} / {@code BankModule} 同构，应用组装层只需调用
 * {@link #register(ServerMessageDispatcher, SessionManager)}，不必了解模块内部构造。
 */
public final class StudentModule {

    /** 学籍档案默认文件（相对工作目录，与 {@code data/admins.tsv} 同一约定）。 */
    public static final String DEFAULT_STUDENT_FILE = "data/students.tsv";

    /** 修改申请单默认文件。 */
    public static final String DEFAULT_REQUEST_FILE = "data/student-requests.tsv";

    /** 学籍文件路径的系统属性名（便于测试与多实例部署时改路径）。 */
    public static final String STUDENT_FILE_PROPERTY = "vcampus.student.file";

    /** 申请单文件路径的系统属性名。 */
    public static final String REQUEST_FILE_PROPERTY = "vcampus.student.request.file";

    /**
     * 存储方式：{@code file}（默认，落本地文件）或 {@code jdbc}（落 MySQL）。
     *
     * <p>
     * 默认仍是文件：演示机与同学的本机未必装了 MySQL，默认走数据库会让「拉下代码就能跑」变成
     * 「先装库再建表」。要用数据库就显式切：
     * {@code -Dvcampus.student.storage=jdbc} 或环境变量 {@code VCAMPUS_STUDENT_STORAGE=jdbc}。
     */
    public static final String STORAGE_PROPERTY = "vcampus.student.storage";

    /** 数据库存储的取值。 */
    private static final String STORAGE_JDBC = "jdbc";

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
        register(dispatcher, sessions, null);
    }

    /**
     * 登记学籍模块全部命令，并把学籍开户钩子接入账户生命周期（建号即可查到自己的学籍）。
     *
     * @param dispatcher 应用共享的消息分发器
     * @param sessions 全服唯一的会话表（命令级鉴权复用）
     * @param provisioning 开户钩子登记表；null 表示不为新账号建档
     * @throws IllegalArgumentException 必要参数为 null
     */
    public static void register(ServerMessageDispatcher dispatcher, SessionManager sessions,
            AccountProvisioning provisioning) {
        if (dispatcher == null || sessions == null) {
            throw new IllegalArgumentException("dispatcher and sessions must not be null");
        }
        StudentDao dao;
        StudentModifyRequestDao requests;
        if (useJdbc()) {
            // 两个 DAO 共用同一个连接来源：它们必须落在同一个库，各自新建一个也无妨，但共用一个
            // 能保证配置只解析一次（日志里也只打一条 URL）。
            StudentDataSource source = new StudentDataSource();
            dao = new StudentDaoJdbc(source);
            requests = new StudentModifyRequestDaoJdbc(source);
            System.out.println("学籍：使用数据库存储 " + source.getUrl());
        } else {
            dao = openStudentDao();
            requests = openRequestDao();
        }
        // 账户库取用户模块装配的那一份（文件库/内存库不同实例，自建会查到空数据），
        // 学籍只存 uuid，列表里的姓名靠它反查。
        StudentService studentService = new StudentService(dao,
                requests, AuthModule.repository());
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
            StudentProvisioner provisioner = new StudentProvisioner(dao);
            provisioning.add(provisioner);
            int handled = provisionExisting(provisioner, existingAccounts());
            System.out.println("学籍：核对了 " + handled + " 个既有账号的在校档案");
        }
    }

    /**
     * 是否使用数据库存储：系统属性优先，其次环境变量，未配置时为文件存储。
     *
     * @return 使用数据库返回 true
     */
    private static boolean useJdbc() {
        String value = System.getProperty(STORAGE_PROPERTY);
        if (value == null) {
            value = System.getenv("VCAMPUS_STUDENT_STORAGE");
        }
        return value != null && STORAGE_JDBC.equalsIgnoreCase(value.trim());
    }

    /**
     * 打开学籍档案存储（落盘，重启后档案与专业不丢）。
     *
     * <p>
     * 路径约定与 {@code data/admins.tsv} 一致：相对工作目录。早先用内存实现，服务端一重启，
     * 开户钩子虽然会把档案补回来，但专业 / 入学年份以及学生做过的修改全被抹平，
     * 界面上就是「我刚填的东西又没了」。
     *
     * @return 学籍存储
     * @throws IllegalStateException 文件存在但打不开（宁可起不来，也不要静默退回内存）
     */
    private static StudentDao openStudentDao() {
        File file = new File(System.getProperty(STUDENT_FILE_PROPERTY, DEFAULT_STUDENT_FILE));
        try {
            return new StudentDaoFile(file);
        } catch (IOException e) {
            throw new IllegalStateException("学籍文件无法打开: " + file + " （" + e.getMessage() + "）");
        }
    }

    /**
     * 打开修改申请单存储（落盘，重启后「我提过什么、批没批」还在）。
     *
     * @return 申请单存储
     * @throws IllegalStateException 文件存在但打不开
     */
    private static StudentModifyRequestDao openRequestDao() {
        File file = new File(
                System.getProperty(REQUEST_FILE_PROPERTY, DEFAULT_REQUEST_FILE));
        try {
            return new StudentModifyRequestDaoFile(file);
        } catch (IOException e) {
            throw new IllegalStateException("申请单文件无法打开: " + file + " （" + e.getMessage()
                    + "）");
        }
    }

    /**
     * 取账户库里的全部账号。
     *
     * @return 账号列表；用户模块尚未装配时返回空表
     */
    private static List<Credential> existingAccounts() {
        UserRepository users = AuthModule.repository();
        return users == null ? Collections.<Credential>emptyList() : users.findAll();
    }

    /**
     * 给「装配完成前就已存在」的账号补一次建档。
     *
     * <p>
     * 开户钩子只在账号<b>创建</b>那一刻被调用，而 {@code data/admins.tsv} 的导入发生在
     * {@link AuthModule#bootstrap} 内部——那时各模块还没来得及登记钩子，导入进去的账号
     * 于是有姓名、却没有在校档案，界面上的表现就是「学籍记录不存在」。启动时回头过一遍，
     * 把这条缝补上。
     *
     * <p>
     * 不必自己判「有没有档案」：{@link StudentProvisioner#provision} 幂等，已有档案就直接
     * 返回，不会产生第二条；管理员这类不该建档的角色它内部也会跳过。
     *
     * @param provisioner 学籍开户钩子
     * @param accounts 账号列表；null 视为空
     * @return 过了一遍的账号数（不等于新建的档案数）
     */
    static int provisionExisting(StudentProvisioner provisioner, List<Credential> accounts) {
        if (provisioner == null || accounts == null) {
            return 0;
        }
        int handled = 0;
        int index = 0;
        while (index < accounts.size()) {
            Credential account = accounts.get(index);
            index = index + 1;
            if (account == null || account.getUuid() == null) {
                continue;
            }
            String roleText = account.getRole();
            provisioner.provision(account.getUuid(), account.getDisplayName(),
                    roleText == null ? null : Role.fromDisplayName(roleText));
            handled = handled + 1;
        }
        return handled;
    }
}
