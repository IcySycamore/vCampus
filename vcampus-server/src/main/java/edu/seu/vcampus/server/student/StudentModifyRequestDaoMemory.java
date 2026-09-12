package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.dto.ModifyRequestQuery;
import edu.seu.vcampus.common.student.entity.StudentModifyRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 学籍修改申请单的内存实现（骨架期占位，不依赖数据库）。
 *
 * <p>
 * 语义与 {@link StudentDaoMemory} 保持一致：主键由自增计数器模拟数据库分配并回填到传入对象；
 * 列表查询只按条件过滤，分页由调用方给出 offset/limit，本层不做页码换算。
 *
 * <p>
 * 排序：结果按提交时间倒序（最新的在前），便于教务先看到新提交的申请。
 */
public class StudentModifyRequestDaoMemory implements StudentModifyRequestDao {

    /** 主键 → 申请单 的存储。 */
    private final Map<Long, StudentModifyRequest> m_store =
            new HashMap<Long, StudentModifyRequest>();

    /** 自增主键计数器（模拟数据库自增分配）。 */
    private final AtomicLong m_next_id = new AtomicLong(1L);

    /**
     * 构造一个空的内存实现。
     */
    public StudentModifyRequestDaoMemory() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean insert(StudentModifyRequest request) {
        if (request == null || request.getProfileId() == null) {
            return false;
        }
        Long id = request.getRequestId();
        if (id == null) {
            id = Long.valueOf(m_next_id.getAndIncrement());
            request.setRequestId(id);
        }
        m_store.put(id, request);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StudentModifyRequest findById(Long requestId) {
        if (requestId == null) {
            return null;
        }
        return m_store.get(requestId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean update(StudentModifyRequest request) {
        if (request == null || request.getRequestId() == null) {
            return false;
        }
        if (!m_store.containsKey(request.getRequestId())) {
            return false;
        }
        m_store.put(request.getRequestId(), request);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<StudentModifyRequest> find(ModifyRequestQuery query, int offset,
            int limit) {
        List<StudentModifyRequest> matched = collect(query);
        Collections.sort(matched, new Comparator<StudentModifyRequest>() {
            @Override
            public int compare(StudentModifyRequest left,
                    StudentModifyRequest right) {
                return Long.compare(right.getAppliedAt(), left.getAppliedAt());
            }
        });
        return slice(matched, offset, limit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long count(ModifyRequestQuery query) {
        return collect(query).size();
    }

    /**
     * 收集满足条件的申请单。
     *
     * @param query 过滤条件
     * @return 匹配的申请单
     */
    private List<StudentModifyRequest> collect(ModifyRequestQuery query) {
        List<StudentModifyRequest> matched =
                new ArrayList<StudentModifyRequest>();
        Iterator<StudentModifyRequest> iterator = m_store.values().iterator();
        while (iterator.hasNext()) {
            StudentModifyRequest request = iterator.next();
            if (matches(request, query)) {
                matched.add(request);
            }
        }
        return matched;
    }

    /**
     * 判断一条申请单是否满足查询条件。
     *
     * @param request 申请单
     * @param query   过滤条件；null 表示不过滤
     * @return 是否匹配
     */
    private boolean matches(StudentModifyRequest request,
            ModifyRequestQuery query) {
        if (query == null) {
            return true;
        }
        if (query.getStatus() != null
                && query.getStatus() != request.getStatus()) {
            return false;
        }
        Long profileId = query.getProfileId();
        if (profileId != null && !profileId.equals(request.getProfileId())) {
            return false;
        }
        return true;
    }

    /**
     * 按 offset/limit 做分页切片。
     *
     * @param all    已排序的全量结果
     * @param offset 起始下标（小于 0 按 0 处理）
     * @param limit  最多返回条数（小于等于 0 时返回空列表）
     * @return 切片结果
     */
    private List<StudentModifyRequest> slice(List<StudentModifyRequest> all,
            int offset, int limit) {
        if (limit <= 0 || offset >= all.size()) {
            return new ArrayList<StudentModifyRequest>();
        }
        int from = offset < 0 ? 0 : offset;
        int to = Math.min(from + limit, all.size());
        return new ArrayList<StudentModifyRequest>(all.subList(from, to));
    }
}
