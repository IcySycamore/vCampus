package edu.seu.vcampus.server.shopmodule.user;

import edu.seu.vcampus.common.user.User;
import edu.seu.vcampus.common.user.HumanInfo;
import java.util.List;

public interface UserDao {
    // 1. 根据登录 ID 查询用户（常用于登录）
    User findByUserId(String userId);
    
    // 2. 新增用户（注册）
    boolean addUser(User user);
    
    // 3. 查询所有用户
    List<User> findAll();

    /**
     * 根据用户 UUID 查询其关联的人基本信息档案。
     *
     * @param userUuid 用户 UUID
     * @return 关联档案；不存在时返回 null
     */
    HumanInfo findHumanInfoByUserUuid(String userUuid);
}