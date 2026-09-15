package edu.seu.vcampus.server.user;

import edu.seu.vcampus.server.user.UserRepository.Credential;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FileUserRepository 测试：落盘与重载、损坏行容错、更新操作持久化。
 */
class FileUserRepositoryTest {

    private File directory;
    private File file;

    /**
     * 每个用例使用独立临时目录。
     *
     * @throws IOException 创建临时目录失败
     */
    @BeforeEach
    void setUp() throws IOException {
        directory = Files.createTempDirectory("vcampus-users").toFile();
        file = new File(directory, "users.tsv");
    }

    /**
     * 清理临时文件。
     */
    @AfterEach
    void tearDown() {
        deleteQuietly(file);
        deleteQuietly(new File(file.getPath() + ".tmp"));
        deleteQuietly(directory);
    }

    /** 账户写入文件后，重新打开仍在（这就是「服务器本地文件保存账号」的核心）。 */
    @Test
    void persistsAcrossReopen() throws IOException {
        FileUserRepository first = new FileUserRepository(file);
        first.save("admin", "uuid-1", "salt-1", "hash-1", "管理员");

        FileUserRepository reopened = new FileUserRepository(file);

        Credential credential = reopened.findByUsername("admin");
        assertNotNull(credential);
        assertEquals("uuid-1", credential.getUuid());
        assertEquals("hash-1", credential.getHash());
        assertEquals("管理员", credential.getRole());
        assertTrue(credential.isEnabled());
        assertEquals(1, reopened.size());
    }

    /** 启停、改名与改密都会立即落盘。 */
    @Test
    void updatesArePersisted() throws IOException {
        FileUserRepository repository = new FileUserRepository(file);
        repository.save(new Credential("001", "uuid-1", "张三", "salt", "hash", "学生", true));

        repository.setEnabled("001", false);
        repository.update("001", "张三丰");
        repository.updateCredential("001", "salt-2", "hash-2");

        Credential reloaded = new FileUserRepository(file).findByUsername("001");
        assertFalse(reloaded.isEnabled());
        assertEquals("张三丰", reloaded.getDisplayName());
        assertEquals("salt-2", reloaded.getSalt());
        assertEquals("hash-2", reloaded.getHash());
    }

    /** 注销后文件里不再有该账号。 */
    @Test
    void deleteRemovesFromFile() throws IOException {
        FileUserRepository repository = new FileUserRepository(file);
        repository.save("001", "uuid-1", "salt", "hash", "学生");
        repository.save("002", "uuid-2", "salt", "hash", "教师");

        repository.delete("001");

        FileUserRepository reloaded = new FileUserRepository(file);
        assertNull(reloaded.findByUsername("001"));
        assertNotNull(reloaded.findByUsername("002"));
        assertNotNull(reloaded.findByUuid("uuid-2"));
        assertEquals(1, reloaded.size());
    }

    /** 损坏行只跳过该行，不影响其它账号——一行坏数据不能让服务起不来。 */
    @Test
    void skipsBrokenLines() throws IOException {
        writeLines("# 注释行", "bad-line-without-fields", "002\tuuid-2\t李四\tsalt\thash\t教师\t1");

        FileUserRepository repository = new FileUserRepository(file);

        assertEquals(1, repository.size());
        assertNotNull(repository.findByUsername("002"));
        assertNull(repository.findByUsername("bad-line-without-fields"));
    }

    /** 文件不存在时视为空库，不抛异常。 */
    @Test
    void toleratesMissingFile() throws IOException {
        FileUserRepository repository = new FileUserRepository(
                new File(directory, "not-exists.tsv"));
        assertEquals(0, repository.size());
        assertTrue(repository.findAll().isEmpty());
    }

    /** 查询结果按登录名稳定排序，保证分页可预期。 */
    @Test
    void findAllIsSorted() throws IOException {
        FileUserRepository repository = new FileUserRepository(file);
        repository.save("003", "uuid-3", "s", "h", "管理员");
        repository.save("001", "uuid-1", "s", "h", "学生");
        repository.save("002", "uuid-2", "s", "h", "教师");

        assertEquals("001", repository.findAll().get(0).getUsername());
        assertEquals("003", repository.findAll().get(2).getUsername());
    }

    /** null 文件与 null 凭证都被拒绝。 */
    @Test
    void rejectsNullArguments() {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                new FileUserRepository(null);
            }
        });
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                new FileUserRepository(file).save((Credential) null);
            }
        });
    }

    private void writeLines(String... lines) throws IOException {
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
