-- =====================================================================
-- vCampus 虚拟校园系统 · 建库脚本（初始骨架，由组员 D 数据库设计负责扩充）
-- 库名 vCampus；CI 会执行本脚本初始化测试库。
-- 命名约定：表名 tblXxx；主键/字段约定见设计说明书。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS vCampus DEFAULT CHARACTER SET utf8mb4;
USE vCampus;

-- 用户表（登录账户：学生/教师/管理员）
DROP TABLE IF EXISTS tblUser;
CREATE TABLE tblUser (
  uId     VARCHAR(8)  NOT NULL                COMMENT '登录ID',
  uName   VARCHAR(20) NOT NULL                COMMENT '姓名',
  uAge    INT         NULL                    COMMENT '年龄(0-100)',
  uSex    VARCHAR(4)  NULL                    COMMENT '性别：男/女',
  uPwd    VARCHAR(32) NOT NULL                COMMENT '密码',
  uRole   VARCHAR(10) NOT NULL DEFAULT '学生' COMMENT '角色：学生/教师/管理员',
  PRIMARY KEY (uId)
) COMMENT='用户表';

-- 测试数据（供演示与 CI 使用；演示账号 001/1 等）
INSERT INTO tblUser (uId, uName, uAge, uSex, uPwd, uRole) VALUES
('001', '演示学生',   20, '男', '1', '学生'),
('002', '演示教师',   35, '女', '1', '教师'),
('003', '管理员',     30, '男', '1', '管理员');
