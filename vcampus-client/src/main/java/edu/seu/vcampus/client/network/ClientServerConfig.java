package edu.seu.vcampus.client.network;

import edu.seu.vcampus.common.constant.NetworkConstant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * 客户端连接目标配置：服务器地址与端口。
 *
 * <p>
 * 演示时服务器可能开在公网（通过端口映射），地址不能写死在常量里：登录页右上角的小齿轮可以改它，
 * 改完落到工作目录下的 {@code data/client.properties}，下次启动继续生效。
 *
 * <p>
 * 配置文件路径可用系统属性 {@code vcampus.client.config} 覆盖（集成测试用临时文件）。
 */
public final class ClientServerConfig {

    /** 默认配置文件路径。 */
    public static final String DEFAULT_FILE = "data/client.properties";

    /** 覆盖配置文件路径的系统属性名。 */
    public static final String FILE_PROPERTY = "vcampus.client.config";

    /** 键：服务器地址。 */
    private static final String KEY_HOST = "server.host";

    /** 键：服务器端口。 */
    private static final String KEY_PORT = "server.port";

    /** 服务器地址。 */
    private final String m_host;

    /** 服务器端口。 */
    private final int m_port;

    private ClientServerConfig(String host, int port) {
        this.m_host = host;
        this.m_port = port;
    }

    /**
     * 读取配置：文件缺失或字段非法时回落到默认值（{@link NetworkConstant}）。
     *
     * @return 连接配置
     */
    public static ClientServerConfig load() {
        Properties properties = new Properties();
        File file = file();
        if (file.isFile()) {
            InputStream input = null;
            try {
                input = new FileInputStream(file);
                properties.load(input);
            } catch (IOException e) {
                // 读不到就用默认值，不为一个配置文件阻断登录
                properties.clear();
            } finally {
                closeQuietly(input);
            }
        }
        String host = properties.getProperty(KEY_HOST, NetworkConstant.DEFAULT_HOST).trim();
        return new ClientServerConfig(isBlank(host) ? NetworkConstant.DEFAULT_HOST : host,
                parsePort(properties.getProperty(KEY_PORT)));
    }

    /**
     * 按显式地址构造（不落盘）。
     *
     * @param host 服务器地址
     * @param port 服务器端口
     * @return 连接配置
     * @throws IllegalArgumentException 地址为空或端口越界
     */
    public static ClientServerConfig of(String host, int port) {
        if (isBlank(host)) {
            throw new IllegalArgumentException("host must not be blank");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("port out of range: " + port);
        }
        return new ClientServerConfig(host.trim(), port);
    }

    /**
     * 保存到默认配置文件（父目录不存在时自动创建）。
     *
     * @throws IOException 写入失败
     */
    public void save() throws IOException {
        Properties properties = new Properties();
        properties.setProperty(KEY_HOST, m_host);
        properties.setProperty(KEY_PORT, String.valueOf(m_port));
        File target = file();
        File parent = target.getParentFile();
        if (parent != null && !parent.isDirectory()) {
            parent.mkdirs();
        }
        OutputStream output = null;
        try {
            output = new FileOutputStream(target);
            properties.store(output, "vCampus client connection");
        } finally {
            closeQuietly(output);
        }
    }

    /**
     * 校验地址与端口（供对话框即时提示，纯函数）。
     *
     * @param host 服务器地址
     * @param port 端口文本
     * @return 错误文案；通过返回 null
     */
    public static String validate(String host, String port) {
        if (isBlank(host)) {
            return "请填写服务器地址";
        }
        int value;
        try {
            value = Integer.parseInt(port == null ? "" : port.trim());
        } catch (NumberFormatException e) {
            return "端口必须是数字";
        }
        if (value <= 0 || value > 65535) {
            return "端口需在 1 - 65535 之间";
        }
        return null;
    }

    /** @return 服务器地址 */
    public String host() {
        return m_host;
    }

    /** @return 服务器端口 */
    public int port() {
        return m_port;
    }

    /** @return 形如 {@code host:port} 的展示文本 */
    public String address() {
        return m_host + ":" + m_port;
    }

    /** 配置文件（支持系统属性覆盖）。 */
    private static File file() {
        return new File(System.getProperty(FILE_PROPERTY, DEFAULT_FILE));
    }

    /** 端口文本解析：非法时回落默认端口。 */
    private static int parsePort(String text) {
        if (isBlank(text)) {
            return NetworkConstant.DEFAULT_PORT;
        }
        try {
            int value = Integer.parseInt(text.trim());
            return value <= 0 || value > 65535 ? NetworkConstant.DEFAULT_PORT : value;
        } catch (NumberFormatException e) {
            return NetworkConstant.DEFAULT_PORT;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private static void closeQuietly(java.io.Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) {
            return;
        }
    }
}
