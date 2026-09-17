package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.course.College;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * 学院池引导：启动时从服务器本地文件读取学院清单并入库，得到「新建师生档案可以挂靠的学院池」。
 *
 * <p>
 * 为什么需要它：{@code tblCourseStudent.cstCollegeUuid} 与 {@code tblTeacher.tcCollegeUuid} 都是非空外键，
 * 而协议里没有创建学院的命令、界面也没有入口。库里一行学院都没有时，给学生或教师开户会被数据库以 1452 拒掉，而开户钩子失败会连带回滚整个注册 ——
 * 账号刚建好就被删掉。学院因此必须由引导文件预置， 不能指望部署方记得手工跑 SQL。
 *
 * <p>
 * 文件格式（UTF-8，Tab 分隔，{@code #} 开头为注释行）：
 *
 * <pre>
 * 学院 uuid \t 学院名称 [\t 官网 [\t 简介]]
 * </pre>
 *
 * <p>
 * 第一列的 uuid <b>必须稳定</b>：档案、课程都按它引用学院，改动等于换了一个学院。文件缺失时自动生成带
 * 注释的模板（里面有一个缺省学院），并把模板里的学院一并入库，因此全新部署无需任何手工 SQL 就能注册师生账号。
 *
 * <p>
 * 重复启动是幂等的：按 uuid 覆盖写（改名、改官网直接在文件里改），不会堆出第二套。反过来说，从文件里 删掉一行<b>不会</b>删除库里的学院 ——
 * 学院可能正被档案引用，删除只能由人显式操作。
 */
public final class CollegePoolBootstrap {

    /** 默认学院引导文件（相对服务端工作目录）。 */
    public static final String DEFAULT_FILE = "data/colleges.tsv";

    /** 覆盖引导文件路径的系统属性。 */
    public static final String FILE_PROPERTY = "vcampus.colleges.file";

    /** 模板内容（含默认学院与说明）。 */
    private static final String TEMPLATE = "# vCampus 学院池引导文件（Tab 分隔：学院 uuid \\t 学院名称 \\t 官网 \\t 简介）\n"
            + "# 服务端启动时把这里列出的学院写入库，并把它们作为新建学生/教师档案可挂靠的学院池。\n"
            + "# 学院是档案的非空外键目标，而系统没有别的创建入口：这里没有学院，师生账号就注册不了。\n"
            + "# 第一列的 uuid 必须稳定（档案与课程都按它引用学院），改动等于换了一个学院。\n"
            + "# 增删学院只需编辑本文件后重启；删掉一行不会删除库里已有的学院。\n"
            + "# 官网与简介可以省略，省略时该列留空即可。\n"
            + "# 下面的缺省学院 uuid 与服务端缺省值一致，请按需改名或加行。\n"
            + "00000000-0000-0000-0000-000000000c01\t计算机学院\n";

    /** 私有构造器，禁止实例化引导工具。 */
    private CollegePoolBootstrap() {
    }

    /**
     * 导入学院池（幂等）。
     *
     * <p>
     * 文件不存在时先生成模板，并把模板里的学院一并导入 —— 否则「全新部署能直接用」这个目的就落空了。 单行导入失败只记录并继续，不让一个手滑的 uuid 把服务端起不来。
     *
     * @param dao  课程目录数据访问
     * @param file 学院引导文件
     * @return 文件中列出的学院 uuid，按文件顺序；一个都没有时返回空列表
     * @throws IOException              文件读写失败
     * @throws IllegalArgumentException 参数为 null
     */
    public static List<String> seed(CourseDao dao, File file) throws IOException {
        if (dao == null || file == null) {
            throw new IllegalArgumentException("dao and file must not be null");
        }
        if (!file.exists()) {
            writeTemplate(file);
            System.out.println("未找到学院引导文件，已生成模板 " + file.getPath());
        }
        List<String> pool = new ArrayList<String>();
        List<String> failures = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8")));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.length() == 0 || trimmed.startsWith("#")) {
                    continue;
                }
                String[] fields = line.split("\t", -1);
                String uuid = fields[0].trim();
                String name = fields.length > 1 ? fields[1].trim() : "";
                if (uuid.length() == 0 || name.length() == 0) {
                    System.err.println("学院引导行缺少 uuid 或名称，已跳过: " + trimmed);
                    continue;
                }
                try {
                    dao.saveCollege(collegeOf(uuid, name, fields));
                    pool.add(uuid);
                } catch (RuntimeException e) {
                    failures.add(name + "(" + e.getMessage() + ")");
                }
            }
        } finally {
            reader.close();
        }
        if (!failures.isEmpty()) {
            System.err.println("以下学院导入失败: " + failures);
        }
        System.out.println("学院池已就绪: " + pool.size() + " 所学院（来源 " + file.getPath() + "）");
        return pool;
    }

    /**
     * 由引导行拼一个学院（官网、简介可省略）。
     *
     * @param uuid   学院 uuid
     * @param name   学院名称
     * @param fields 该行的全部字段
     * @return 学院
     */
    private static College collegeOf(String uuid, String name, String[] fields) {
        College college = new College(uuid, name);
        if (fields.length > 2 && fields[2].trim().length() > 0) {
            college.setWebsite(fields[2].trim());
        }
        if (fields.length > 3 && fields[3].trim().length() > 0) {
            college.setDescription(fields[3].trim());
        }
        return college;
    }

    /**
     * 写下引导文件模板。
     *
     * @param file 目标文件
     * @throws IOException 写入失败
     */
    private static void writeTemplate(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("无法创建目录: " + parent);
        }
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), Charset.forName("UTF-8")));
        try {
            writer.write(TEMPLATE);
        } finally {
            writer.close();
        }
    }
}
