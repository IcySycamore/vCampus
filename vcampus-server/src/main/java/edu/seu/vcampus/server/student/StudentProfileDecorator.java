package edu.seu.vcampus.server.student;

import edu.seu.vcampus.common.student.entity.StudentProfile;
import edu.seu.vcampus.server.user.UserRepository;
import edu.seu.vcampus.server.user.UserRepository.Credential;

import java.util.List;

/**
 * 给学籍档案补上「展示用姓名」。
 *
 * <p>
 * 学籍表只存账户 uuid，姓名归用户模块维护。界面要在学籍列表里显示姓名，就必须由服务端联查
 * 一次：本类把这层联查收在一处，避免每个查询方法各写一遍——散着写最容易在新增查询方法时漏掉，
 * 表现就是「列表里有的名字有、有的没有」。
 *
 * <p>
 * <b>容错</b>：用户仓储为 null（单模块测试没有用户模块）或查不到账户（账户已注销）时，姓名保持
 * 为 null 而不抛异常——列表显示不该因为某一行没有名字就整页失败。调用方读
 * {@link StudentProfile#getDisplayName()} 会回退成 uuid，不会出现空字符串。
 */
final class StudentProfileDecorator {

    /** 用户凭证存储；可为 null（此时不联查）。 */
    private final UserRepository m_users;

    /**
     * 构造联查器。
     *
     * @param users 用户凭证存储；可为 null
     */
    StudentProfileDecorator(UserRepository users) {
        this.m_users = users;
    }

    /**
     * 给单条档案补姓名。
     *
     * @param profile 档案（可为 null）
     * @return 同一个档案对象（便于链式返回）
     */
    StudentProfile decorate(StudentProfile profile) {
        if (profile == null || m_users == null || profile.getRealName() != null) {
            return profile;
        }
        Credential credential = m_users.findByUuid(profile.getUserUuid());
        if (credential != null) {
            profile.setRealName(credential.getRealName());
        }
        return profile;
    }

    /**
     * 给一批档案补姓名（列表查询用）。
     *
     * @param profiles 档案列表（可为 null）
     * @return 同一个列表对象
     */
    List<StudentProfile> decorate(List<StudentProfile> profiles) {
        if (profiles == null || m_users == null) {
            return profiles;
        }
        int index = 0;
        while (index < profiles.size()) {
            decorate(profiles.get(index));
            index = index + 1;
        }
        return profiles;
    }
}
