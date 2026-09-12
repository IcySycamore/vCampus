package edu.seu.vcampus.server.user;

import edu.seu.vcampus.server.user.UserRepository.Credential;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * InMemoryUserRepository 测试：凭证读写、分页顺序、启停与改密落点。
 */
class InMemoryUserRepositoryTest {

    private InMemoryUserRepository repository;

    /**
     * 每个用例使用独立仓库。
     */
    @BeforeEach
    void setUp() {
        repository = new InMemoryUserRepository();
    }

    /** 五参保存：姓名默认取登录名、默认启用。 */
    @Test
    void saveDefaultsDisplayNameAndEnabled() {
        repository.save("001", "uuid-1", "salt", "hash", "学生");

        Credential credential = repository.findByUsername("001");
        assertEquals("001", credential.getUsername());
        assertEquals("001", credential.getDisplayName());
        assertEquals("uuid-1", credential.getUuid());
        assertTrue(credential.isEnabled());
        assertTrue(repository.exists("001"));
        assertEquals(credential, repository.findByUuid("uuid-1"));
    }

    /** 完整保存覆盖同名列，且按登录名稳定排序。 */
    @Test
    void findAllIsSortedByUsername() {
        repository.save(new Credential("003", "uuid-3", "丙", "s", "h", "管理员", true));
        repository.save(new Credential("001", "uuid-1", "甲", "s", "h", "学生", true));
        repository.save(new Credential("002", "uuid-2", "乙", "s", "h", "教师", false));

        List<Credential> all = repository.findAll();

        assertEquals(3, all.size());
        assertEquals("001", all.get(0).getUsername());
        assertEquals("003", all.get(2).getUsername());
    }

    /** 姓名、启用位与凭证（盐/哈希）可分别更新。 */
    @Test
    void updatesDisplayNameEnabledAndCredential() {
        repository.save("001", "uuid-1", "salt", "hash", "学生");

        repository.update("001", "张三");
        repository.setEnabled("001", false);
        repository.updateCredential("001", "salt-2", "hash-2");

        Credential credential = repository.findByUsername("001");
        assertEquals("张三", credential.getDisplayName());
        assertFalse(credential.isEnabled());
        assertEquals("salt-2", credential.getSalt());
        assertEquals("hash-2", credential.getHash());
    }

    /** 删除后查不到；对不存在的账号操作不抛异常。 */
    @Test
    void deletesAndToleratesMissingUsers() {
        repository.save("001", "uuid-1", "salt", "hash", "学生");
        repository.delete("001");

        assertNull(repository.findByUsername("001"));
        assertNull(repository.findByUuid("uuid-1"));
        assertFalse(repository.exists("001"));

        repository.update("ghost", "无名");
        repository.setEnabled("ghost", false);
        repository.updateCredential("ghost", "s", "h");
        repository.delete("ghost");
        assertNull(repository.findByUsername(null));
        assertNull(repository.findByUuid(null));
        assertFalse(repository.exists(null));
    }

    /** null 凭证拒绝保存。 */
    @Test
    void rejectsNullCredential() {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                repository.save((Credential) null);
            }
        });
    }
}
