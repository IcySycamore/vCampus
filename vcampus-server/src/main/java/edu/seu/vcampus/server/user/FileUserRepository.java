package edu.seu.vcampus.server.user;

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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 【文件版】用户账户存储：账户落在服务器本地文件，重启后账号仍然存在。
 *
 * <p>
 * 为什么不直接上 MySQL（ADR-0002）：账户是所有功能的前置依赖，而数据库接入尚未完成；
 * 用本地文件先把「账号可持久化、可由运维直接维护」这条链路跑通，后续把本类替换为 {@code UserDaoJdbc} 即可（上层只依赖
 * {@link UserRepository} 接口）。
 *
 * <p>
 * 文件格式：每行一条，Tab 分隔，UTF-8，无表头：
 *
 * <pre>
 * 登录名 \t uuid \t 姓名 \t 盐 \t 加盐哈希 \t 角色显示名 \t 启用位(1/0)
 * </pre>
 *
 * <p>
 * 写入采用「临时文件 + 原子替换」，避免进程中断留下半个文件；读取时损坏行会被跳过并打印告警， 不会因为一行坏数据导致整个服务起不来。存储的始终是
 * {@code sha256(salt + 口令)}，<b>不落明文口令</b>。
 */
public class FileUserRepository implements UserRepository {

    /** 字段分隔符（Tab）。 */
    private static final String SEPARATOR = "\t";

    /** 每行字段数。 */
    private static final int FIELD_COUNT = 7;

    /** 存储文件。 */
    private final File m_file;

    /** 登录名 → 凭证。 */
    private final Map<String, Credential> m_users = new ConcurrentHashMap<String, Credential>();

    /**
     * 打开（或创建）账户文件并加载全部账户。
     *
     * @param file 账户文件；不存在时视为空库，首次写入时创建
     * @throws IOException 文件存在但读取失败
     * @throws IllegalArgumentException file 为 null
     */
    public FileUserRepository(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file must not be null");
        }
        this.m_file = file;
        load();
    }

    /** @return 账户文件 */
    public File getFile() {
        return m_file;
    }

    /** @return 已加载的账户数量 */
    public int size() {
        return m_users.size();
    }

    @Override
    public void save(String username, String uuid, String salt, String hash, String role) {
        save(new Credential(username, uuid, username, salt, hash, role, true));
    }

    @Override
    public void save(Credential credential) {
        if (credential == null || credential.getUsername() == null) {
            throw new IllegalArgumentException("credential and its username must not be null");
        }
        m_users.put(credential.getUsername(), credential);
        persist();
    }

    @Override
    public Credential findByUsername(String username) {
        return username == null ? null : m_users.get(username);
    }

    @Override
    public Credential findByUuid(String uuid) {
        if (uuid == null) {
            return null;
        }
        for (Credential credential : m_users.values()) {
            if (uuid.equals(credential.getUuid())) {
                return credential;
            }
        }
        return null;
    }

    @Override
    public boolean exists(String username) {
        return username != null && m_users.containsKey(username);
    }

    @Override
    public List<Credential> findAll() {
        List<Credential> all = new ArrayList<Credential>(m_users.values());
        Collections.sort(all, new Comparator<Credential>() {
            @Override
            public int compare(Credential left, Credential right) {
                String first = left.getUsername() == null ? "" : left.getUsername();
                String second = right.getUsername() == null ? "" : right.getUsername();
                return first.compareTo(second);
            }
        });
        return all;
    }

    @Override
    public void update(String username, String displayName) {
        Credential credential = findByUsername(username);
        if (credential != null) {
            credential.setDisplayName(displayName);
            persist();
        }
    }

    @Override
    public void setEnabled(String username, boolean enabled) {
        Credential credential = findByUsername(username);
        if (credential != null) {
            credential.setEnabled(enabled);
            persist();
        }
    }

    @Override
    public void updateCredential(String username, String salt, String hash) {
        Credential credential = findByUsername(username);
        if (credential != null) {
            credential.setSalt(salt);
            credential.setHash(hash);
            persist();
        }
    }

    @Override
    public void delete(String username) {
        if (username != null && m_users.remove(username) != null) {
            persist();
        }
    }

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
                String[] fields = line.split(SEPARATOR, -1);
                if (fields.length < FIELD_COUNT) {
                    System.err.println("账户文件第 " + lineNumber + " 行字段数不足，已跳过");
                    continue;
                }
                Credential credential = new Credential(fields[0], fields[1], fields[2], fields[3],
                        fields[4], fields[5], "1".equals(fields[6]));
                m_users.put(credential.getUsername(), credential);
            }
        } finally {
            reader.close();
        }
    }

    private synchronized void persist() {
        File parent = m_file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            System.err.println("无法创建账户文件目录: " + parent);
            return;
        }
        File temp = new File(m_file.getPath() + ".tmp");
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(temp), Charset.forName("UTF-8")));
            for (Credential credential : findAll()) {
                writer.write(toLine(credential));
                writer.newLine();
            }
            writer.flush();
            writer.close();
            writer = null;
            Files.move(temp.toPath(), m_file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("账户文件写入失败: " + e.getMessage());
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {
                    // 关闭失败无需处理：已记录主流程错误
                }
            }
            if (temp.exists() && !temp.delete()) {
                System.err.println("临时账户文件清理失败: " + temp);
            }
        }
    }

    private String toLine(Credential credential) {
        StringBuilder builder = new StringBuilder();
        builder.append(safe(credential.getUsername())).append(SEPARATOR);
        builder.append(safe(credential.getUuid())).append(SEPARATOR);
        builder.append(safe(credential.getDisplayName())).append(SEPARATOR);
        builder.append(safe(credential.getSalt())).append(SEPARATOR);
        builder.append(safe(credential.getHash())).append(SEPARATOR);
        builder.append(safe(credential.getRole())).append(SEPARATOR);
        builder.append(credential.isEnabled() ? "1" : "0");
        return builder.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(SEPARATOR, " ");
    }
}
