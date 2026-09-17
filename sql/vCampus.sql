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

-- =====================================================================
-- 学籍模块（负责：邬致远）
-- 字段映射与语义约定见 docs/学籍模块数据库对接说明.md
-- =====================================================================

-- 在校人员档案表（学生与教师共用；管理员没有档案，故 personCategory 不含 ADMIN）
DROP TABLE IF EXISTS tblCampusProfile;
CREATE TABLE tblCampusProfile (
  srId            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '档案主键',
  userUuid        VARCHAR(36) NOT NULL                COMMENT '所属用户 uuid',
  personCategory  VARCHAR(16) NOT NULL                COMMENT '人员类别：STUDENT/TEACHER（不含管理员）',
  joinYear        INT         NOT NULL                COMMENT '入校年份：学生=入学年份，教师=入职年份',
  status          VARCHAR(16) NOT NULL                COMMENT '在校状态：ENROLLED/SUSPENDED/WITHDRAWN/GRADUATED/RETIRED',
  field           VARCHAR(64)     NULL                COMMENT '学术方向：学生=专业，教师=研究方向',
  studentNo       VARCHAR(16)     NULL                COMMENT '学号（纯展示字段，不参与定位）',
  deleted         TINYINT(1)  NOT NULL DEFAULT 0      COMMENT '软删除标记：0 未删除，1 已删除',
  PRIMARY KEY (srId),
  KEY idx_profile_user (userUuid),
  KEY idx_profile_category (personCategory),
  KEY idx_profile_field (field)
) COMMENT='在校人员档案表（学生与教师）';

-- 档案修改申请单表（学生提交、教务审核）
DROP TABLE IF EXISTS tblModifyRequest;
CREATE TABLE tblModifyRequest (
  mrId          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '申请单主键',
  profileId     BIGINT       NOT NULL                COMMENT '目标档案主键',
  applicantUuid VARCHAR(36)  NOT NULL                COMMENT '申请人账户 uuid',
  changesJson   VARCHAR(512) NOT NULL                COMMENT '变更内容，键值文本，如 status=SUSPENDED;field=计算机视觉',
  reason        VARCHAR(255)     NULL                COMMENT '申请理由',
  status        VARCHAR(16)  NOT NULL                COMMENT '待审状态：PENDING/APPROVED/REJECTED',
  comment       VARCHAR(255)     NULL                COMMENT '审核意见',
  appliedAt     BIGINT       NOT NULL                COMMENT '提交时间（毫秒时间戳）',
  auditedBy     VARCHAR(36)      NULL                COMMENT '审核人账户 uuid',
  auditedAt     BIGINT           NULL                COMMENT '审核时间（毫秒时间戳）',
  PRIMARY KEY (mrId),
  KEY idx_request_profile (profileId),
  KEY idx_request_status (status),
  KEY idx_request_applied (appliedAt)
) COMMENT='档案修改申请单表';

