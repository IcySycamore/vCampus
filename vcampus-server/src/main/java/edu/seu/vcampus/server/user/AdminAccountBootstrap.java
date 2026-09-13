package edu.seu.vcampus.server.user;

import edu.seu.vcampus.common.user.entity.Role;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 管理员账号引导：启动时从服务器本地文件读取管理员账号并建号。
 *
 * <p>
 * 这样做的目的（对应「管理员账号保存在服务器本地文件」的要求）：
 * <ul>
 * <li>不再把演示账号硬编码在代码里——换管理员、改口令只需编辑文件；</li>
 * <li>解决空库引导死锁：「注册需要管理员会话」而库里还没有管理员；</li>
 * <li>文件缺失时自动生成一份带注释的模板，首次启动即可用。</li>
 * </ul>
 *
 * <p>
 * 文件格式（UTF-8，Tab 分隔，{@code #} 开头为注释）：
 *
 * <pre>
 * 登录名 \t 姓名 \t 初始口令
 * </pre>
 *
 * <p>
 * 口令只在引导文件里出现一次：导入时立即转成随机盐 + {@code sha256(salt + 口令)} 存进账户库
 * （{@link FileUserRepository}），系统其它地方不再保存明文。已存在的账号会被跳过（幂等）， 因此重复启动不会覆盖已改过的口令。
 */
public final class AdminAccountBootstrap {

    /** 默认管理员引导文件（相对服务端工作目录）。 */
    public static final String DEFAULT_FILE = "data/admins.tsv";

    /** 模板内容（含默认账号与说明）。 */
    private static final String TEMPLATE = "# vCampus 管理员账号引导文件（Tab 分隔：登录名\\t姓名\\t初始口令）\n"
            + "# 首次启动会导入下列账号；导入后口令只以加盐哈希形式保存在 data/users.tsv 中。\n"
            + "# 已存在的账号会被跳过，所以改这里不会覆盖已有口令——要重置请删掉 data/users.tsv 后重启。\n"
            + "# 默认账号：admin / admin123（请登录后立即修改口令）\n" + "admin\t系统管理员\tadmin123\n";

    /** 私有构造器，禁止实例化引导工具。 */
    private AdminAccountBootstrap() {
    }

    /**
     * 导入管理员账号（幂等）。
     *
     * @param auth 认证服务
     * @param file 管理员引导文件
     * @return 本次新建的账号数量
     * @throws IOException 文件读写失败
     * @throws IllegalArgumentException 参数为 null
     */
    public static int seed(AuthService auth, File file) throws IOException {
        if (auth == null || file == null) {
            throw new IllegalArgumentException("auth and file must not be null");
        }
        if (!file.exists()) {
            writeTemplate(file);
            System.out.println("未找到管理员账号文件，已生成模板 " + file.getPath() + "（默认账号 admin / admin123）");
            return 0;
        }
        int created = 0;
        List<String> failures = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8")));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.length() == 0 || trimmed.startsWith("#")) {
                    continue;
                }
                String[] fields = line.split("\t", -1);
                String username = fields[0].trim();
                if (username.length() == 0) {
                    continue;
                }
                String displayName = fields.length > 1 && fields[1].trim().length() > 0
                        ? fields[1].trim()
                        : username;
                String password = fields.length > 2 ? fields[2] : "";
                if (password.length() == 0) {
                    System.err.println("管理员 " + username + " 未配置口令，已跳过");
                    continue;
                }
                if (auth.exists(username)) {
                    continue;
                }
                String role = resolveRole(fields.length > 3 ? fields[3].trim() : null);
                try {
                    auth.register(username, displayName, password,
                            role == null ? Role.ADMIN.getDisplayName() : role);
                    created++;
                } catch (RuntimeException e) {
                    failures.add(username + "(" + e.getMessage() + ")");
                }
            }
        } finally {
            reader.close();
        }
        if (!failures.isEmpty()) {
            System.err.println("以下管理员账号导入失败: " + failures);
        }
        if (created > 0) {
            System.out.println("已从 " + file.getPath() + " 导入 " + created + " 个管理员账号");
        }
        return created;
    }

    private static String resolveRole(String text) {
        if (text == null || text.length() == 0) {
            return null;
        }
        Role role = Role.fromDisplayName(text);
        if (role != null) {
            return role.getDisplayName();
        }
        try {
            return Role.valueOf(text.toUpperCase(Locale.ENGLISH)).getDisplayName();
        } catch (IllegalArgumentException e) {
            System.err.println("引导文件中的角色无法识别，已按管理员处理: " + text);
            return null;
        }
    }

    private static void writeTemplate(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("无法创建目录: " + parent);
        }
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8")));
        try {
            writer.write(TEMPLATE);
        } finally {
            writer.close();
        }
    }
}
