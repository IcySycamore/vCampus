package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.dto.ModifyRequestQuery;
import edu.seu.vcampus.common.student.entity.ModifyRequestStatus;
import edu.seu.vcampus.common.student.entity.StudentModifyRequest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 【文件版】修改申请单存储：申请单落在服务器本地文件，重启后「我提过什么、批没批」都还在。
 *
 * <p>
 * 与 {@link StudentModifyRequestDaoMemory} 的语义一致（按提交时间倒序、分页交给调用方），
 * 只多了持久化。这一条对学生尤其要紧：申请是与教务的往来凭据，服务端一重启就消失的话，
 * 学生会以为「提交失败了」，教务那边的待办也会凭空蒸发。
 *
 * <p>
 * 文件格式：每行一条，Tab 分隔，UTF-8，无表头：
 *
 * <pre>
 * 单号 \t 学籍主键 \t 申请人 uuid \t 变更内容 \t 理由 \t 状态 \t 审核意见 \t 提交时间 \t 审核人 \t 审核时间
 * </pre>
 *
 * <p>
 * 写入、读取的容错策略与 {@link StudentDaoFile}（以及 {@code FileUserRepository}）一致：
 * 临时文件 + 原子替换；坏行跳过并告警，不拖垮启动。
 */
public class StudentModifyRequestDaoFile implements StudentModifyRequestDao {

    /** 字段分隔符（Tab）。 */
    private static final String SEPARATOR = "\t";

    /** 每行字段数。 */
    private static final int FIELD_COUNT = 10;

    /** 存储文件。 */
    private final File m_file;

    /** 单号 → 申请单 的索引。 */
    private final Map<Long, StudentModifyRequest> m_store = new ConcurrentHashMap<Long, StudentModifyRequest>();

    /** 自增单号计数器。 */
    private final AtomicLong m_next_id = new AtomicLong(1L);

    /**
     * 打开（或创建）申请单文件并加载全部申请。
     *
     * @param file 申请单文件；不存在时视为空库，首次写入时创建
     * @throws IOException 文件存在但读取失败
     * @throws IllegalArgumentException file 为 null
     */
    public StudentModifyRequestDaoFile(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }
        this.m_file = file;
        load();
    }

    /** @return 申请单文件 */
    public File getFile() {
        return m_file;
    }

    /** @return 已加载的申请单数量 */
    public int size() {
        return m_store.size();
    }

    /** {@inheritDoc} */
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
        persist();
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public StudentModifyRequest findById(Long requestId) {
        return requestId == null ? null : m_store.get(requestId);
    }

    /** {@inheritDoc} */
    @Override
    public boolean update(StudentModifyRequest request) {
        if (request == null || request.getRequestId() == null
                || !m_store.containsKey(request.getRequestId())) {
            return false;
        }
        m_store.put(request.getRequestId(), request);
        persist();
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public List<StudentModifyRequest> find(ModifyRequestQuery query, int offset, int limit) {
        List<StudentModifyRequest> matched = collect(query);
        Collections.sort(matched, new Comparator<StudentModifyRequest>() {
            @Override
            public int compare(StudentModifyRequest left, StudentModifyRequest right) {
                return Long.compare(right.getAppliedAt(), left.getAppliedAt());
            }
        });
        return PageSlice.of(matched, offset, limit);
    }

    /** {@inheritDoc} */
    @Override
    public long count(ModifyRequestQuery query) {
        return collect(query).size();
    }

    /**
     * 从文件加载全部申请单。
     *
     * @throws IOException 读取失败
     */
    private void load() throws IOException {
        if (!m_file.exists()) {
            return;
        }
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(m_file), Charset.forName("UTF-8")));
        try {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().length() == 0 || line.startsWith("#")) {
                    continue;
                }
                StudentModifyRequest request = parse(line, lineNumber);
                if (request != null) {
                    m_store.put(request.getRequestId(), request);
                }
            }
        } finally {
            reader.close();
        }
        m_next_id.set(1L);
        Iterator<Long> it = m_store.keySet().iterator();
        while (it.hasNext()) {
            long id = it.next().longValue();
            if (id >= m_next_id.get()) {
                m_next_id.set(id + 1L);
            }
        }
    }

    /**
     * 解析一行；字段数不足或枚举非法时告警并跳过该行。
     *
     * @param line 行文本
     * @param lineNumber 行号（仅用于告警）
     * @return 申请单；无法解析返回 null
     */
    private StudentModifyRequest parse(String line, int lineNumber) {
        String[] fields = line.split(SEPARATOR, -1);
        if (fields.length < FIELD_COUNT) {
            System.err.println("申请单文件第 " + lineNumber + " 行字段数不足，已跳过");
            return null;
        }
        try {
            StudentModifyRequest request = new StudentModifyRequest();
            request.setRequestId(Long.valueOf(fields[0]));
            request.setProfileId(StudentDaoFile.emptyToNull(fields[1]) == null
                    ? null
                    : Long.valueOf(fields[1]));
            request.setApplicantUuid(StudentDaoFile.emptyToNull(fields[2]));
            request.setChangesJson(fields[3]);
            request.setReason(StudentDaoFile.emptyToNull(fields[4]));
            request.setStatus(ModifyRequestStatus.valueOf(fields[5]));
            request.setComment(StudentDaoFile.emptyToNull(fields[6]));
            request.setAppliedAt(Long.parseLong(fields[7]));
            request.setAuditedBy(StudentDaoFile.emptyToNull(fields[8]));
            request.setAuditedAt(Long.parseLong(fields[9]));
            return request;
        } catch (RuntimeException e) {
            System.err.println("申请单文件第 " + lineNumber + " 行无法解析，已跳过：" + e.getMessage());
            return null;
        }
    }

    /**
     * 把全部申请单写回文件（临时文件 + 原子替换）。
     */
    private synchronized void persist() {
        File parent = m_file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            System.err.println("无法创建申请单文件目录: " + parent);
            return;
        }
        File temp = new File(m_file.getPath() + ".tmp");
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temp),
                    Charset.forName("UTF-8")));
            Iterator<StudentModifyRequest> it = m_store.values().iterator();
            while (it.hasNext()) {
                writer.write(lineOf(it.next()));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("申请单文件写入失败: " + e.getMessage());
            return;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {
                    // 写入阶段的失败已经提示过，这里不重复刷屏
                }
            }
        }
        try {
            Files.move(temp.toPath(), m_file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("申请单文件替换失败: " + e.getMessage());
        }
    }

    /**
     * 把一条申请单转成一行文本。
     *
     * @param request 申请单
     * @return 行文本（不含换行）
     */
    private static String lineOf(StudentModifyRequest request) {
        return request.getRequestId() + SEPARATOR + text(request.getProfileId()) + SEPARATOR
                + StudentDaoFile.sanitize(request.getApplicantUuid()) + SEPARATOR
                + StudentDaoFile.sanitize(request.getChangesJson()) + SEPARATOR
                + StudentDaoFile.sanitize(request.getReason()) + SEPARATOR
                + (request.getStatus() == null ? "" : request.getStatus().name()) + SEPARATOR
                + StudentDaoFile.sanitize(request.getComment()) + SEPARATOR
                + request.getAppliedAt() + SEPARATOR
                + StudentDaoFile.sanitize(request.getAuditedBy()) + SEPARATOR
                + request.getAuditedAt();
    }

    /**
     * 收集满足条件的申请单。
     *
     * @param query 过滤条件；null 表示全部
     * @return 匹配的申请单
     */
    private List<StudentModifyRequest> collect(ModifyRequestQuery query) {
        List<StudentModifyRequest> matched = new ArrayList<StudentModifyRequest>();
        Iterator<StudentModifyRequest> it = m_store.values().iterator();
        while (it.hasNext()) {
            StudentModifyRequest request = it.next();
            if (matches(request, query)) {
                matched.add(request);
            }
        }
        return matched;
    }

    /**
     * 判断一条申请单是否满足查询条件（状态 / 学籍主键 / 申请人）。
     *
     * @param request 申请单
     * @param query 过滤条件；null 表示不过滤
     * @return 是否匹配
     */
    private static boolean matches(StudentModifyRequest request, ModifyRequestQuery query) {
        if (query == null) {
            return true;
        }
        if (query.getStatus() != null && query.getStatus() != request.getStatus()) {
            return false;
        }
        Long profileId = query.getProfileId();
        if (profileId != null && !profileId.equals(request.getProfileId())) {
            return false;
        }
        String applicant = query.getApplicantUuid();
        return applicant == null || applicant.equals(request.getApplicantUuid());
    }

    /**
     * 主键文本；null 写成空串（读回时再还原成 null）。
     *
     * @param id 主键；可为 null
     * @return 文本
     */
    private static String text(Long id) {
        return id == null ? "" : id.toString();
    }
}
