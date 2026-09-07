package edu.seu.vcampus.server.module.student;

import edu.seu.vcampus.common.entity.StudentProfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 学籍数据访问的内存实现（骨架期占位，不依赖数据库）。
 *
 * <p>
 * 用于在 DbHelper 合入前打通 Service 层测试；后续替换为 JDBC 实现
 * {@code StudentDaoJdbc} 时，{@link StudentService} 无需改动（依赖接口）。
 * 主键由本实现用自增计数器模拟数据库自增分配。
 *
 * <p>
 * 软删除语义：普通查询只返回未删除的记录，已软删除的记录查询不到，
 * 但记录仍保留在内存中。
 */
public class StudentDaoMemory implements StudentDao {

    /** 主键 → 学籍记录 的存储。 */
    private final Map<Long, StudentProfile> m_store =
            new HashMap<Long, StudentProfile>();

    /** 自增主键计数器（模拟数据库自增分配）。 */
    private final AtomicLong m_next_id = new AtomicLong(1L);

    /**
     * 构造一个空的内存实现。
     */
    public StudentDaoMemory() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StudentProfile findById(Long id) {
        StudentProfile profile = m_store.get(id);
        if (profile == null) {
            return null;
        }
        if (profile.isDeleted()) {
            return null;
        }
        return profile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StudentProfile findByUserId(Long userId) {
        Iterator<StudentProfile> it = m_store.values().iterator();
        while (it.hasNext()) {
            StudentProfile profile = it.next();
            if (profile.isDeleted()) {
                continue;
            }
            Long current = profile.getUserId();
            if (current == null) {
                if (userId == null) {
                    return profile;
                }
                continue;
            }
            if (current.equals(userId)) {
                return profile;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<StudentProfile> findAll() {
        List<StudentProfile> result = new ArrayList<StudentProfile>();
        Iterator<StudentProfile> it = m_store.values().iterator();
        while (it.hasNext()) {
            StudentProfile profile = it.next();
            if (!profile.isDeleted()) {
                result.add(profile);
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean insert(StudentProfile profile) {
        if (profile == null) {
            return false;
        }
        Long id = profile.getId();
        if (id == null) {
            id = m_next_id.getAndIncrement();
            profile.setId(id);
        }
        m_store.put(id, profile);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean update(StudentProfile profile) {
        if (profile == null) {
            return false;
        }
        Long id = profile.getId();
        if (id == null) {
            return false;
        }
        if (!m_store.containsKey(id)) {
            return false;
        }
        m_store.put(id, profile);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean softDelete(Long id) {
        if (id == null) {
            return false;
        }
        StudentProfile profile = m_store.get(id);
        if (profile == null) {
            return false;
        }
        profile.markDeleted();
        return true;
    }
}
