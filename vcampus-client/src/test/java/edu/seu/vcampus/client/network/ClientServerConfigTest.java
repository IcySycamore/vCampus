package edu.seu.vcampus.client.network;

import edu.seu.vcampus.common.constant.NetworkConstant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 客户端服务器地址配置测试：默认值回落、保存后读回、非法输入校验。
 */
class ClientServerConfigTest {

    /** 临时配置文件。 */
    private File m_tempFile;

    /** 每个用例前把配置路径指向临时文件（避免动到工作目录的 data/）。 */
    @BeforeEach
    void setUp() throws IOException {
        m_tempFile = File.createTempFile("vcampus-client", ".properties");
        if (!m_tempFile.delete()) {
            throw new IOException("无法准备临时配置文件");
        }
        System.setProperty(ClientServerConfig.FILE_PROPERTY, m_tempFile.getPath());
    }

    /** 用例后清理系统属性与临时文件。 */
    @AfterEach
    void tearDown() {
        System.clearProperty(ClientServerConfig.FILE_PROPERTY);
        if (m_tempFile != null && m_tempFile.exists() && !m_tempFile.delete()) {
            m_tempFile.deleteOnExit();
        }
    }

    @Test
    void fallsBackToDefaultsWhenFileMissing() {
        ClientServerConfig config = ClientServerConfig.load();

        assertEquals(NetworkConstant.DEFAULT_HOST, config.host());
        assertEquals(NetworkConstant.DEFAULT_PORT, config.port());
        assertEquals(NetworkConstant.DEFAULT_HOST + ":" + NetworkConstant.DEFAULT_PORT,
                config.address());
    }

    @Test
    void savesAndLoadsPublicAddress() throws IOException {
        ClientServerConfig.of("203.0.113.10", 18888).save();

        ClientServerConfig config = ClientServerConfig.load();

        assertEquals("203.0.113.10", config.host());
        assertEquals(18888, config.port());
        assertEquals("203.0.113.10:18888", config.address());
    }

    @Test
    void ignoresIllegalValuesInFile() throws IOException {
        writeConfig("", "not-a-port");

        ClientServerConfig config = ClientServerConfig.load();

        assertEquals(NetworkConstant.DEFAULT_HOST, config.host());
        assertEquals(NetworkConstant.DEFAULT_PORT, config.port());
    }

    @Test
    void ignoresOutOfRangePortInFile() throws IOException {
        writeConfig("10.0.0.9", "70000");

        ClientServerConfig config = ClientServerConfig.load();

        assertEquals("10.0.0.9", config.host());
        assertEquals(NetworkConstant.DEFAULT_PORT, config.port());
    }

    @Test
    void validateReportsFirstProblem() {
        assertEquals("请填写服务器地址", ClientServerConfig.validate("  ", "8888"));
        assertEquals("端口必须是数字", ClientServerConfig.validate("127.0.0.1", "abc"));
        assertEquals("端口需在 1 - 65535 之间", ClientServerConfig.validate("127.0.0.1", "0"));
        assertEquals("端口需在 1 - 65535 之间", ClientServerConfig.validate("127.0.0.1", "65536"));
        assertNull(ClientServerConfig.validate("127.0.0.1", "8888"));
    }

    @Test
    void rejectsInvalidExplicitValues() {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                ClientServerConfig.of("", 8888);
            }
        });
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                ClientServerConfig.of("127.0.0.1", 0);
            }
        });
    }

    /** 直接写一个配置文件（模拟用户手改坏的情况）。 */
    private void writeConfig(String host, String port) throws IOException {
        OutputStream output = new FileOutputStream(m_tempFile);
        try {
            output.write(
                    ("server.host=" + host + "\nserver.port=" + port + "\n").getBytes("UTF-8"));
        } finally {
            output.close();
        }
    }
}
