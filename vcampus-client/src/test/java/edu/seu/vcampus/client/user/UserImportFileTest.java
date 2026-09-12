package edu.seu.vcampus.client.user;

import edu.seu.vcampus.common.user.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UserImportFile 解析测试：逗号/Tab、注释与空行、角色中英文、错误定位。
 */
class UserImportFileTest {

    /** 逗号分隔、忽略注释与空行。 */
    @Test
    void parsesCommaSeparatedLines() {
        List<String> lines = Arrays.asList("# 批量注册示例", "", "2025001,张三,学生,init1234",
                "2025002,李四,教师,init5678");

        List<RegisterRequest> requests = UserImportFile.parseLines(lines);

        assertEquals(2, requests.size());
        assertEquals("2025001", requests.get(0).m_user_name);
        assertEquals("张三", requests.get(0).m_display_name);
        assertEquals("学生", requests.get(0).m_role);
        assertEquals("init1234", requests.get(0).m_password);
        assertEquals("教师", requests.get(1).m_role);
    }

    /** 支持 Tab 分隔与英文角色名。 */
    @Test
    void parsesTabSeparatedAndEnglishRole() {
        List<String> lines = Arrays.asList("2025003\t王五\tADMIN\tpw");

        List<RegisterRequest> requests = UserImportFile.parseLines(lines);

        assertEquals("管理员", requests.get(0).m_role);
        assertEquals("王五", requests.get(0).m_display_name);
    }

    /** 姓名为空时允许，由服务器回落为登录名。 */
    @Test
    void allowsEmptyDisplayName() {
        List<RegisterRequest> requests = UserImportFile.parseLines(Arrays.asList("2025004,,学生,pw"));
        assertEquals("", requests.get(0).m_display_name);
    }

    /** 空文件返回空列表（不需要报错）。 */
    @Test
    void returnsEmptyForBlankInput() {
        assertTrue(UserImportFile.parseLines(null).isEmpty());
        assertTrue(UserImportFile.parseLines(new ArrayList<String>()).isEmpty());
        assertTrue(UserImportFile.parseLines(Arrays.asList("   ", "#注释")).isEmpty());
    }

    /** 字段不足：异常信息带行号，方便直接定位。 */
    @Test
    void rejectsShortLineWithLineNumber() {
        final List<String> lines = Arrays.asList("ok,张三,学生,pw", "2025009,缺角色");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        UserImportFile.parseLines(lines);
                    }
                });
        assertTrue(error.getMessage().contains("第 2 行"), error.getMessage());
    }

    /** 角色无法识别时明确报错，而不是默认成某个角色。 */
    @Test
    void rejectsUnknownRole() {
        final List<String> lines = Arrays.asList("2025010,赵六,校长,pw");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                new Executable() {
                    @Override
                    public void execute() {
                        UserImportFile.parseLines(lines);
                    }
                });
        assertTrue(error.getMessage().contains("角色无法识别"), error.getMessage());
    }

    /** 登录名或口令为空即报错。 */
    @Test
    void rejectsMissingUserNameOrPassword() {
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                UserImportFile.parseLines(Arrays.asList(",张三,学生,pw"));
            }
        });
        assertThrows(IllegalArgumentException.class, new Executable() {
            @Override
            public void execute() {
                UserImportFile.parseLines(Arrays.asList("2025011,张三,学生,"));
            }
        });
    }
}
