package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.dto.StudentQuery;
import edu.seu.vcampus.common.student.entity.StudentProfile;

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
 * 用于在 DbHelper 合入前打通 Service 层测试；后续替换为 JDBC 实现 {@code StudentDaoJdbc}
 * 时，{@link StudentService} 无需改动（依赖接口）。 主键由本实现用自增计数器模拟数据库自增分配。
 *
 * <p>
 * 软删除语义：普通查询只返回未删除的记录，已软删除的记录查询不到， 但记录仍保留在内存中。
 */
public class StudentDaoMemory implements StudentDao {

    /** 主键 → 学籍记录 的存储。 */
    private final Map<Long, StudentProfile> m_store = new HashMap<Long, StudentProfile>();

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
    public StudentProfile findByUserUuid(String userUuid) {
        Iterator<StudentProfile> it = m_store.values().iterator();
        while (it.hasNext()) {
            StudentProfile profile = it.next();
            if (profile.isDeleted()) {
                continue;
            }
            String current = profile.getUserUuid();
            if (current == null) {
                if (userUuid == null) {
                    return profile;
                }
                continue;
            }
            if (current.equals(userUuid)) {
                return profile;
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public List<StudentProfile> findAll() {
        return collect(null);
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

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    public List<StudentProfile> find(StudentQuery query, int offset, int limit) {
        return PageSlice.of(collect(query), offset, limit);
    }

    /** {@inheritDoc} */
    @Override
    public long count(StudentQuery query) {
        return collect(query).size();
    }

    /**
     * 收集满足条件的未删除记录。
     *
     * @param query 过滤条件；null 表示全部
     * @return 匹配的记录
     */
    private List<StudentProfile> collect(StudentQuery query) {
        List<StudentProfile> matched = new ArrayList<StudentProfile>();
        Iterator<StudentProfile> it = m_store.values().iterator();
        while (it.hasNext()) {
            StudentProfile profile = it.next();
            if (!profile.isDeleted() && matches(profile, query)) {
                matched.add(profile);
            }
        }
        return matched;
    }

    /**
     * 判断一条记录是否满足查询条件（各条件之间是「与」的关系）。
     *
     * @param profile 学籍记录
     * @param query 过滤条件；null 表示不过滤
     * @return 是否匹配
     */
    private boolean matches(StudentProfile profile, StudentQuery query) {
        if (query == null) {
            return true;
        }
        if (query.getProfileId() != null
                && !query.getProfileId().equals(profile.getId())) {
            return false;
        }
        if (query.getUserUuid() != null
                && !query.getUserUuid().equals(profile.getUserUuid())) {
            return false;
        }
        if (query.getStatus() != null && query.getStatus() != profile.getStatus()) {
            return false;
        }
        String keyword = query.getKeyword();
        if (keyword == null || keyword.trim().length() == 0) {
            return true;
        }
        String trimmed = keyword.trim();
        String uuid = profile.getUserUuid();
        if (uuid != null && uuid.contains(trimmed)) {
            return true;
        }
        return String.valueOf(profile.getEnrollYear()).contains(trimmed);
    }
}
