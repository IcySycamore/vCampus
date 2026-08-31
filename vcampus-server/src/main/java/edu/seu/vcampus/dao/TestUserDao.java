package edu.seu.vcampus.dao;

import edu.seu.vcampus.model.User;
import java.util.List;

public class TestUserDao {
    public static void main(String[] args) {
        UserDao userDao = new UserDaoImpl();

        System.out.println("=== 1. 测试根据 uId 查找用户 ===");
        // 查询 uId 为 "001" 的演示学生
        User user = userDao.findByUsername("001"); 
        if (user != null) {
            System.out.println("查找成功 -> 账号:" + user.getuId() + " | 姓名:" + user.getuName() + " | 密码:" + user.getuPwd() + " | 角色:" + user.getuRole());
        } else {
            System.out.println("未找到该用户！");
        }

        System.out.println("\n=== 2. 测试查询所有用户 ===");
        List<User> list = userDao.findAll();
        for (User u : list) {
            System.out.println("账号: " + u.getuId() + " | 姓名: " + u.getuName() + " | 角色: " + u.getuRole());
        }
    }
}