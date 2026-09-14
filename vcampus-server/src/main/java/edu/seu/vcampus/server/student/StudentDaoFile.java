package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.dto.StudentQuery;
import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.PersonCategory;
import edu.seu.vcampus.common.student.entity.StudentProfile;

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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 【文件版】学籍存储：档案落在服务器本地文件，重启后档案还在。
 *
 * <p>
 * 与 {@link StudentDaoMemory} 的区别只有「持久化」：查询、分页、软删除语义逐条照抄，同样复用
 * {@link StudentMatcher} 与 {@link PageSlice}，所以两者可以互换（{@link StudentService} 只依赖
 * {@link StudentDao}）。用文件而不是 MySQL，理由与 {@code FileUserRepository} 相同：数据库接入
 * 尚未完成，先用本地文件把「数据能留住」这条链路跑通，日后换成 {@code StudentDaoJdbc} 上层不用动。
 *
 * <p>
 * <b>为什么非落盘不可</b>：内存实现下，服务端一重启，开户钩子虽然会把档案补回来，但专业 / 入学年份
 * 以及学生自己做过的修改全都被抹平，界面上就是「我刚填的东西又没了」。
 *
 * <p>
 * 文件格式：每行一条，Tab 分隔，UTF-8，无表头：
 *
 * <pre>
 * 主键 \t 账户 uuid \t 人员类别 \t 入校年份 \t 在校状态 \t 学术方向 \t 删除位(1/0)
 * </pre>
 *
 * <p>
 * 写入用「临时文件 + 原子替换」，进程中断不会留下半个文件；读取时字段数不对的行跳过并告警，
 * 不因一行坏数据让整个服务起不来。与 {@code FileUserRepository} 保持一致。
 */
public class StudentDaoFile implements StudentDao {

    /** 字段分隔符（Tab）。 */
    private static final String SEPARATOR = "\t";

    /** 每行字段数。 */
    private static final int FIELD_COUNT = 7;

    /** 存储文件。 */
    private final File m_file;

    /** 主键 → 学籍记录 的索引（与文件同源，查询走它）。 */
    private final Map<Long, StudentProfile> m_store = new ConcurrentHashMap<Long, StudentProfile>();

    /** 自增主键计数器。 */
    private final AtomicLong m_next_id = new AtomicLong(1L);

    /**
     * 打开（或创建）学籍文件并加载全部档案。
     *
     * @param file 学籍文件；不存在时视为空库，首次写入时创建
     * @throws IOException 文件存在但读取失败
     * @throws IllegalArgumentException file 为 null
     */
    public StudentDaoFile(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }
        this.m_file = file;
        load();
    }

    /** @return 学籍文件 */
    public File getFile() {
        return m_file;
    }

    /** @return 已加载的档案数量（含已软删除的） */
    public int size() {
        return m_store.size();
    }

    /** {@inheritDoc} */
    @Override
    public StudentProfile findById(Long id) {
        StudentProfile profile = m_store.get(id);
        return profile == null || profile.isDeleted() ? null : profile;
    }

    /** {@inheritDoc} */
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

    /** {@inheritDoc} */
    @Override
    public boolean insert(StudentProfile profile) {
        if (profile == null) {
            return false;
        }
        Long id = profile.getId();
        if (id == null) {
            id = Long.valueOf(m_next_id.getAndIncrement());
            profile.setId(id);
        }
        m_store.put(id, profile);
        persist();
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean update(StudentProfile profile) {
        if (profile == null || profile.getId() == null
                || !m_store.containsKey(profile.getId())) {
            return false;
        }
        m_store.put(profile.getId(), profile);
        persist();
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean softDelete(Long id) {
        StudentProfile profile = m_store.get(id);
        if (profile == null) {
            return false;
        }
        profile.markDeleted();
        persist();
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
     * 从文件加载全部档案。
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
                StudentProfile profile = parse(line, lineNumber);
                if (profile != null) {
                    m_store.put(profile.getId(), profile);
                }
            }
        } finally {
            reader.close();
        }
        m_next_id.set(1L);
        Iterator<Long> keys = m_store.keySet().iterator();
        while (keys.hasNext()) {
            long id = keys.next().longValue();
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
     * @return 档案；无法解析返回 null
     */
    private StudentProfile parse(String line, int lineNumber) {
        String[] fields = line.split(SEPARATOR, -1);
        if (fields.length < FIELD_COUNT) {
            System.err.println("学籍文件第 " + lineNumber + " 行字段数不足，已跳过");
            return null;
        }
        try {
            StudentProfile profile = new StudentProfile();
            profile.setId(Long.valueOf(fields[0]));
            profile.setUserUuid(emptyToNull(fields[1]));
            profile.setPersonCategory(PersonCategory.valueOf(fields[2]));
            profile.setJoinYear(Integer.parseInt(fields[3]));
            profile.setStatus(emptyToNull(fields[4]) == null
                    ? null
                    : CampusStatus.valueOf(fields[4]));
            profile.setField(emptyToNull(fields[5]));
            if ("1".equals(fields[6])) {
                profile.markDeleted();
            }
            return profile;
        } catch (RuntimeException e) {
            System.err.println("学籍文件第 " + lineNumber + " 行无法解析，已跳过：" + e.getMessage());
            return null;
        }
    }

    /**
     * 把全部档案写回文件（临时文件 + 原子替换）。
     */
    private synchronized void persist() {
        File parent = m_file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            System.err.println("无法创建学籍文件目录: " + parent);
            return;
        }
        File temp = new File(m_file.getPath() + ".tmp");
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temp),
                    Charset.forName("UTF-8")));
            Iterator<StudentProfile> it = m_store.values().iterator();
            while (it.hasNext()) {
                writer.write(lineOf(it.next()));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("学籍文件写入失败: " + e.getMessage());
            return;
        } finally {
            closeQuietly(writer);
        }
        try {
            Files.move(temp.toPath(), m_file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("学籍文件替换失败: " + e.getMessage());
        }
    }

    /**
     * 把一条档案转成一行文本。
     *
     * @param profile 档案
     * @return 行文本（不含换行）
     */
    private static String lineOf(StudentProfile profile) {
        return profile.getId() + SEPARATOR + sanitize(profile.getUserUuid()) + SEPARATOR
                + profile.getPersonCategory().name() + SEPARATOR + profile.getJoinYear()
                + SEPARATOR + (profile.getStatus() == null ? "" : profile.getStatus().name())
                + SEPARATOR + sanitize(profile.getField()) + SEPARATOR
                + (profile.isDeleted() ? "1" : "0");
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
            if (!profile.isDeleted() && StudentMatcher.matches(profile, query)) {
                matched.add(profile);
            }
        }
        return matched;
    }

    /**
     * 去掉会破坏行结构的字符：Tab 与换行一律换成空格。
     *
     * <p>
     * 不做转义而是替换，是因为这些字段来自单行输入框，本来就不可能含换行；真混进 Tab 时，
     * 宁可少一个字符，也不能让整行字段错位、把后面的数据读串。
     *
     * @param value 原值
     * @return 可安全写入的值；null 返回空串
     */
    static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
    }

    /**
     * 空串一律当 null（写入时 null 与空串不可区分，读回时统一成 null）。
     *
     * @param value 字段原文
     * @return 原值；空串返回 null
     */
    static String emptyToNull(String value) {
        return value == null || value.length() == 0 ? null : value;
    }

    /**
     * 静默关闭写入器。
     *
     * @param writer 写入器；可为 null
     */
    private static void closeQuietly(BufferedWriter writer) {
        if (writer == null) {
            return;
        }
        try {
            writer.close();
        } catch (IOException ignored) {
            // 关闭失败已在写入阶段提示过，这里不再重复刷屏
        }
    }
}
