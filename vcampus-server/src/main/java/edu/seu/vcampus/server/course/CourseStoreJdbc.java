package edu.seu.vcampus.server.course;

import edu.seu.vcampus.common.course.Building;

import edu.seu.vcampus.common.course.Classroom;
import edu.seu.vcampus.common.course.College;
import edu.seu.vcampus.common.course.CourseSection;
import edu.seu.vcampus.common.course.Student;
import edu.seu.vcampus.common.course.Teacher;
import edu.seu.vcampus.server.db.DatabaseAccessException;
import edu.seu.vcampus.server.db.DbHelper;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static edu.seu.vcampus.server.db.JdbcSupport.closeQuietly;

/**
 * 【MySQL 版】课程目录的持久化后端。
 *
 * <p>
 * 读写本身委托给 {@link CourseCatalogStoreJdbc}（学院 / 教师 / 学生 /
 * 教室）与 {@link CourseSectionStoreJdbc} （课程本体及子表），本类只管连接与事务边界 —— 一次 save 涉及主表加若干子表，必须落在同一个
 * 事务里，否则中途失败会留下「主表更新了、子表还是旧的」这种半截状态。
 *
 * <p>
 * 事务用法：进入时关掉自动提交，写完 commit，异常时 rollback；finally 里把自动提交恢复再交还 连接，免得后续复用这个连接的人拿到一个还开着事务的会话语境。
 */
public final class CourseStoreJdbc implements CourseStore {

    /** @return 全部学院 */
    @Override
    public List<College> loadColleges() {
        Connection connection = null;
        try {
            connection = DbHelper.getConnection();
            return CourseCatalogStoreJdbc.loadColleges(connection);
        } catch (SQLException e) {
            throw new DatabaseAccessException("加载学院失败", e);
        } finally {
            closeQuietly(connection);
        }
    }

    /** @return 全部教师 */
    @Override
    public List<Teacher> loadTeachers() {
        Connection connection = null;
        try {
            connection = DbHelper.getConnection();
            return CourseCatalogStoreJdbc.loadTeachers(connection);
        } catch (SQLException e) {
            throw new DatabaseAccessException("加载教师失败", e);
        } finally {
            closeQuietly(connection);
        }
    }

    /** @return 全部选课模块的学生档案 */
    @Override
    public List<Student> loadStudents() {
        Connection connection = null;
        try {
            connection = DbHelper.getConnection();
            return CourseCatalogStoreJdbc.loadStudents(connection);
        } catch (SQLException e) {
            throw new DatabaseAccessException("加载选课学生失败", e);
        } finally {
            closeQuietly(connection);
        }
    }

    /** @return 全部教室 */
    @Override
    public List<Classroom> loadClassrooms() {
        Connection connection = null;
        try {
            connection = DbHelper.getConnection();
            return CourseCatalogStoreJdbc.loadClassrooms(connection);
        } catch (SQLException e) {
            throw new DatabaseAccessException("加载教室失败", e);
        } finally {
            closeQuietly(connection);
        }
    }

    /** @return 全部课程 */
    @Override
    public List<CourseSection> loadCourses() {
        Connection connection = null;
        try {
            connection = DbHelper.getConnection();
            return CourseSectionStoreJdbc.loadAll(connection);
        } catch (SQLException e) {
            throw new DatabaseAccessException("加载课程失败", e);
        } finally {
            closeQuietly(connection);
        }
    }

    /** @param college 学院 @return 写入成功为 true */
    @Override
    public boolean saveCollege(final College college) {
        return inTransaction(new Work() {
            @Override
            public void run(Connection connection) throws SQLException {
                CourseCatalogStoreJdbc.saveCollege(connection, college);
            }
        }, "保存学院失败", college == null ? null : college.getUuid());
    }

    /** @param teacher 教师 @return 写入成功为 true */
    @Override
    public boolean saveTeacher(final Teacher teacher) {
        return inTransaction(new Work() {
            @Override
            public void run(Connection connection) throws SQLException {
                CourseCatalogStoreJdbc.saveTeacher(connection, teacher);
            }
        }, "保存教师失败", teacher == null ? null : teacher.getUuid());
    }

    /** @param student 学生 @return 写入成功为 true */
    @Override
    public boolean saveStudent(final Student student) {
        return inTransaction(new Work() {
            @Override
            public void run(Connection connection) throws SQLException {
                CourseCatalogStoreJdbc.saveStudent(connection, student);
            }
        }, "保存选课学生失败", student == null ? null : student.getUuid());
    }

    /** @param classroom 教室 @return 写入成功为 true */
    @Override
    public boolean saveClassroom(final Classroom classroom) {
        return inTransaction(new Work() {
            @Override
            public void run(Connection connection) throws SQLException {
                CourseCatalogStoreJdbc.saveClassroom(connection, classroom);
            }
        }, "保存教室失败", classroom == null ? null : classroom.getUuid());
    }

    /** @param course 课程 @return 写入成功为 true */
    @Override
    public boolean saveCourse(final CourseSection course) {
        return inTransaction(new Work() {
            @Override
            public void run(Connection connection) throws SQLException {
                CourseSectionStoreJdbc.save(connection, course);
            }
        }, "保存课程失败", course == null ? null : course.getUuid());
    }

    /** @param uuid 教师 uuid @return 命中记录为 true */
    @Override
    public boolean deleteTeacher(final String uuid) {
        Connection connection = null;
        try {
            connection = DbHelper.getConnection();
            connection.setAutoCommit(false);
            boolean removed = CourseCatalogStoreJdbc.deleteTeacher(connection, uuid);
            connection.commit();
            connection.setAutoCommit(true);
            return removed;
        } catch (SQLException e) {
            rollbackQuietly(connection);
            throw new DatabaseAccessException("删除教师失败: " + uuid, e);
        } finally {
            closeQuietly(connection);
        }
    }

    /**
     * 加载全部教学楼。
     *
     * <p>
     * 教学楼只是教室的归属标签（名字 + 所属学院），没有反向索引，因此不进内存 DAO 的任何索引结构。
     *
     * @return 教学楼列表，不返回 null
     */
    @Override
    public List<Building> loadBuildings() {
        return BuildingStoreJdbc.load();
    }

    /** @param building 教学楼 @return 写入成功为 true */
    @Override
    public boolean saveBuilding(final Building building) {
        return inTransaction(new Work() {
            @Override
            public void run(Connection connection) throws SQLException {
                BuildingStoreJdbc.save(connection, building);
            }
        }, "保存教学楼失败", building == null ? null : building.getUuid());
    }

    /**
     * 删除一门课程：先清掉所有指向它的子行，再删主表。
     *
     * <p>
     * 子行分四处：课程领域、选课关系、时间槽、成绩。其中成绩表有指向课程的外键，顺序不能颠倒。
     *
     * @param uuid 课程 uuid
     * @return 命中记录为 true
     */
    @Override
    public boolean deleteCourse(final String uuid) {
        return inTransaction(new Work() {
            @Override
            public void run(Connection connection) throws SQLException {
                CourseSectionStoreJdbc.deleteCourse(connection, uuid);
            }
        }, "删除课程失败", uuid);
    }

    /**
     * 在一个事务里跑一段写操作。
     *
     * @param work    写操作
     * @param message 出错时的提示前缀
     * @param uuid    出错时附带的实体 uuid；可为 null
     * @return 恒为 true；失败会抛异常
     */
    private static boolean inTransaction(Work work, String message, String uuid) {
        Connection connection = null;
        try {
            connection = DbHelper.getConnection();
            connection.setAutoCommit(false);
            work.run(connection);
            connection.commit();
            connection.setAutoCommit(true);
            return true;
        } catch (SQLException e) {
            rollbackQuietly(connection);
            throw new DatabaseAccessException(uuid == null ? message : message + ": " + uuid, e);
        } finally {
            closeQuietly(connection);
        }
    }

    /**
     * 安静回滚。
     *
     * @param connection 连接；可为 null
     */
    private static void rollbackQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.rollback();
            connection.setAutoCommit(true);
        } catch (SQLException ignored) {
            // 回滚失败时连接即将关闭，不需要再上报
        }
    }

    /** 一段用同一个连接完成的写操作。 */
    private interface Work {

        /**
         * 执行写操作。
         *
         * @param connection 事务连接
         * @throws SQLException 写入失败
         */
        void run(Connection connection) throws SQLException;
    }
}
