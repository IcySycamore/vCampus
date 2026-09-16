-- =====================================================================
-- vCampus 虚拟校园系统 · 建库脚本（初始骨架，由组员 D 数据库设计负责扩充）
-- 库名 vCampus；目标版本 MySQL 8.0（兼容 5.7）；CI 会执行本脚本初始化测试库。
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

-- 图书馆馆藏表
DROP TABLE IF EXISTS tblBook;
CREATE TABLE tblBook (
  isbn            VARCHAR(20)  NOT NULL COMMENT 'ISBN',
  title           VARCHAR(200) NOT NULL COMMENT '书名',
  author          VARCHAR(100) NOT NULL COMMENT '作者',
  category        VARCHAR(100) NOT NULL COMMENT '分类',
  totalCopies     INT          NOT NULL COMMENT '馆藏总数',
  availableCopies INT          NOT NULL COMMENT '当前可借数量',
  withdrawn       BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '是否已下架',
  PRIMARY KEY (isbn),
  KEY idxBookTitle (title),
  KEY idxBookAuthor (author),
  KEY idxBookCategory (category)
) COMMENT='图书馆馆藏表';

-- 首页馆藏速览与借阅演示所用的初始图书
INSERT INTO tblBook
  (isbn, title, author, category, totalCopies, availableCopies, withdrawn)
VALUES
('9787111213826', 'Java编程思想', 'Bruce Eckel', '计算机', 8, 8, FALSE),
('9787111547426', 'Effective Java', 'Joshua Bloch', '计算机', 6, 6, FALSE),
('9787302423287', 'Java语言程序设计', '梁勇', '计算机', 10, 10, FALSE),
('9787111641247', '深入理解Java虚拟机', '周志明', '计算机', 7, 7, FALSE),
('9787111612728', '算法（第4版）', 'Robert Sedgewick', '计算机', 5, 5, FALSE),
('9787111407010', '代码整洁之道', 'Robert C. Martin', '软件工程', 4, 4, FALSE),
('9787111558422', '数据库系统概念', 'Abraham Silberschatz', '数据库', 6, 6, FALSE);
