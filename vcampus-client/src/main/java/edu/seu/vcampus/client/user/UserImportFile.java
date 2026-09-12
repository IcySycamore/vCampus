package edu.seu.vcampus.client.user;

import edu.seu.vcampus.common.user.dto.RegisterRequest;
import edu.seu.vcampus.common.user.entity.Role;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量注册文件解析：把本地文本文件解析成 {@link RegisterRequest} 列表（命令 103 的载荷）。
 *
 * <p>
 * 放在客户端模块里而不是界面里：这是可测的纯逻辑（解析、角色归一、错误定位）， 界面只负责选文件与展示结果（见 ADR-0009 D11「判断逻辑下沉」）。
 *
 * <p>
 * 文件格式：UTF-8，{@code #} 开头为注释、空行忽略，每行四个字段，用逗号或 Tab 分隔：
 *
 * <pre>
 * 登录名, 姓名, 角色, 初始口令
 * 2025001,张三,学生,init1234
 * </pre>
 *
 * <p>
 * 角色可写中文（学生/教师/管理员）或英文枚举名（STUDENT/TEACHER/ADMIN），大小写不敏感。
 * 任一行的字段数不对、登录名/口令为空、角色无法识别，都会抛出带行号的 {@link IllegalArgumentException}，
 * 让管理员立刻知道改哪一行——而不是静默跳过后发现少了几个账号。
 */
public final class UserImportFile {

    /** 字段分隔符：逗号或 Tab。 */
    private static final String FIELD_SEPARATOR = "[,\\t]";

    /** 字段数量。 */
    private static final int FIELD_COUNT = 4;

    /** 私有构造器，禁止实例化工具类。 */
    private UserImportFile() {
    }

    /**
     * 解析批量注册文件。
     *
     * @param file 文件
     * @return 注册请求列表
     * @throws IOException 读取失败
     * @throws IllegalArgumentException 文件格式不合法（异常信息含行号）
     */
    public static List<RegisterRequest> parse(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }
        List<String> lines = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8")));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } finally {
            reader.close();
        }
        return parseLines(lines);
    }

    /**
     * 解析行列表（纯函数，便于单测）。
     *
     * @param lines 行列表
     * @return 注册请求列表
     * @throws IllegalArgumentException 某行格式不合法
     */
    public static List<RegisterRequest> parseLines(List<String> lines) {
        List<RegisterRequest> requests = new ArrayList<RegisterRequest>();
        if (lines == null) {
            return requests;
        }
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line == null || line.trim().length() == 0 || line.trim().startsWith("#")) {
                continue;
            }
            String[] fields = line.split(FIELD_SEPARATOR, -1);
            if (fields.length < FIELD_COUNT) {
                throw new IllegalArgumentException(
                        "第 " + (index + 1) + " 行字段不足（应为 登录名,姓名,角色,口令）：" + line);
            }
            String userName = fields[0].trim();
            String displayName = fields[1].trim();
            String roleName = resolveRole(fields[2].trim(), index + 1);
            String password = fields[3].trim();
            if (userName.length() == 0 || password.length() == 0) {
                throw new IllegalArgumentException("第 " + (index + 1) + " 行缺少登录名或口令：" + line);
            }
            RegisterRequest request = new RegisterRequest();
            request.m_user_name = userName;
            request.m_display_name = displayName;
            request.m_role = roleName;
            request.m_password = password;
            requests.add(request);
        }
        return requests;
    }

    private static String resolveRole(String text, int lineNumber) {
        Role role = Role.fromDisplayName(text);
        if (role != null) {
            return role.getDisplayName();
        }
        try {
            return Role.valueOf(text.toUpperCase(java.util.Locale.ENGLISH)).getDisplayName();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "第 " + lineNumber + " 行角色无法识别（学生/教师/管理员 或 " + "STUDENT/TEACHER/ADMIN）：" + text);
        }
    }
}
