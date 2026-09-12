package edu.seu.vcampus.server.user;

import edu.seu.vcampus.common.user.dto.LoginChallenge;
import edu.seu.vcampus.common.util.Sha256Util;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AdminAccountBootstrap 测试：模板生成、导入管理员、幂等跳过、异常行容错。
 */
class AdminAccountBootstrapTest {

    private File directory;
    private File adminsFile;
    private File usersFile;
    private AuthService auth;

    /**
     * 每个用例使用独立临时目录与独立账户库。
     *
     * @throws IOException 初始化失败
     */
    @BeforeEach
    void setUp() throws IOException {
        directory = Files.createTempDirectory("vcampus-admins").toFile();
        adminsFile = new File(directory, "admins.tsv");
        usersFile = new File(directory, "users.tsv");
        auth = new AuthService(new FileUserRepository(usersFile), new NonceManager(),
                new SessionManager());
    }

    /**
     * 清理临时文件。
     */
    @AfterEach
    void tearDown() {
        deleteQuietly(adminsFile);
        deleteQuietly(usersFile);
        deleteQuietly(directory);
    }

    /** 引导文件缺失时生成模板（含默认管理员），不阻塞启动。 */
    @Test
    void writesTemplateWhenMissing() throws IOException {
        int created = AdminAccountBootstrap.seed(auth, adminsFile);

        assertEquals(0, created);
        assertTrue(adminsFile.exists());
        assertTrue(readAll(adminsFile).contains("admin"));
    }

    /** 从文件导入管理员，且导入后即可用文件里的口令登录。 */
    @Test
    void importsAdminsFromFile() throws IOException {
        writeLines(adminsFile, "# 管理员账号", "root\t超级管理员\trootPw", "ops\t运维\topsPw");

        int created = AdminAccountBootstrap.seed(auth, adminsFile);

        assertEquals(2, created);
        assertNotNull(auth.repository().findByUsername("root"));
        assertEquals("管理员", auth.repository().findByUsername("root").getRole());
        assertEquals("超级管理员", auth.repository().findByUsername("root").getDisplayName());

        LoginChallenge challenge = auth.loginChallenge("root");
        assertNotNull(auth.loginVerify("root", proof(challenge, "rootPw")));
    }

    /** 幂等：已存在的账号跳过，不会覆盖改过的口令。 */
    @Test
    void skipsExistingAccounts() throws IOException {
        writeLines(adminsFile, "root\t超级管理员\trootPw");
        AdminAccountBootstrap.seed(auth, adminsFile);
        auth.changePassword("root", null, "new-salt", Sha256Util.sha256Hex("new-salt" + "changed"));

        int created = AdminAccountBootstrap.seed(auth, adminsFile);

        assertEquals(0, created);
        LoginChallenge challenge = auth.loginChallenge("root");
        assertNotNull(auth.loginVerify("root", proof(challenge, "changed")));
        assertNull(auth.loginVerify("root", proof(auth.loginChallenge("root"), "rootPw")));
    }

    /** 缺口令或字段不足的行跳过，其余行照常导入。 */
    @Test
    void toleratesBrokenLines() throws IOException {
        writeLines(adminsFile, "  ", "# 注释", "noPassword", "good\t好管理员\tpw123");

        int created = AdminAccountBootstrap.seed(auth, adminsFile);

        assertEquals(1, created);
        assertNotNull(auth.repository().findByUsername("good"));
        assertNull(auth.repository().findByUsername("noPassword"));
    }

    /** 参数为 null 时快速失败。 */
    @Test
    void rejectsNullArguments() {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                try {
                    AdminAccountBootstrap.seed(null, adminsFile);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }

    private String proof(LoginChallenge challenge, String password) {
        String inner = Sha256Util.sha256Hex(challenge.m_salt + password);
        return Sha256Util.sha256Hex(challenge.m_nonce + inner);
    }

    private String readAll(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        return new String(bytes, Charset.forName("UTF-8"));
    }

    private void writeLines(File file, String... lines) throws IOException {
        Writer writer = new OutputStreamWriter(new FileOutputStream(file),
                Charset.forName("UTF-8"));
        try {
            for (String line : lines) {
                writer.write(line);
                writer.write("\n");
            }
        } finally {
            writer.close();
        }
    }

    private void deleteQuietly(File target) {
        if (target.exists() && !target.delete()) {
            System.err.println("临时文件清理失败: " + target);
        }
    }
}
