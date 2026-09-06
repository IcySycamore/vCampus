package edu.seu.vcampus.server.module.user;

import edu.seu.vcampus.common.entity.User;
import java.util.List;

public interface UserDao {
    // 1. 根据用户名查询用户（常用于登录）
    User findByUsername(String username);
    
    // 2. 新增用户（注册）
    boolean addUser(User user);
    
    // 3. 查询所有用户
    List<User> findAll();
}