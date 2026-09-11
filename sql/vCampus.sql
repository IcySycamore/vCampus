-- =====================================================================
-- vCampus 虚拟校园系统 · 建库脚本（初始骨架，由组员 D 数据库设计负责扩充）
-- 库名 vCampus；目标版本 MySQL 8.0（兼容 5.7）；CI 会执行本脚本初始化测试库。
-- 命名约定：表名 tblXxx；主键/字段约定见设计说明书。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS vCampus DEFAULT CHARACTER SET utf8mb4;
USE vCampus;

-- 先删子表再删父表，避免外键依赖导致脚本重复执行时失败
DROP TABLE IF EXISTS tblUserPurchaseRecord;
DROP TABLE IF EXISTS tblOrder;
DROP TABLE IF EXISTS tblShopItem;
DROP TABLE IF EXISTS tblBankAccount;
DROP TABLE IF EXISTS tblLibraryBook;
DROP TABLE IF EXISTS tblCourse;
DROP TABLE IF EXISTS tblStudent;
DROP TABLE IF EXISTS tblUserHumanInfo;
DROP TABLE IF EXISTS tblHumanInfo;
DROP TABLE IF EXISTS tblGlobalSequence;

-- 全局序列表（由服务端 DAO 维护各业务序列）
CREATE TABLE tblGlobalSequence (
  gsName  VARCHAR(64) NOT NULL COMMENT '序列名',
  gsValue INT         NOT NULL COMMENT '当前值',
  PRIMARY KEY (gsName)
) COMMENT='全局序列表';

-- 用户表（登录账户：学生/教师/管理员）
DROP TABLE IF EXISTS tblUser;
CREATE TABLE tblUser (
  uUuid   CHAR(36)    NOT NULL                COMMENT '用户UUID',
  uId     VARCHAR(8)  NOT NULL                COMMENT '登录ID',
  uName   VARCHAR(20) NOT NULL                COMMENT '姓名',
  uAge    INT         NULL                    COMMENT '年龄(0-100)',
  uSex    VARCHAR(4)  NULL                    COMMENT '性别：男/女',
  uPwd    VARCHAR(32) NOT NULL                COMMENT '密码',
  uRole   VARCHAR(10) NOT NULL DEFAULT '学生' COMMENT '角色：学生/教师/管理员',
  PRIMARY KEY (uUuid),
  UNIQUE KEY ukUserId (uId)
) COMMENT='用户表';

-- 人基本信息档案表，与登录用户通过 tblUserHumanInfo 关联
CREATE TABLE tblHumanInfo (
  hiUuid        CHAR(36)     NOT NULL COMMENT '档案UUID',
  hiId          VARCHAR(8)   NOT NULL COMMENT '档案业务ID',
  hiName        VARCHAR(20)  NOT NULL COMMENT '姓名',
  hiTel         VARCHAR(20)  NULL COMMENT '电话',
  hiHomeAddress VARCHAR(200) NULL COMMENT '家庭住址',
  hiWorkAddress VARCHAR(200) NULL COMMENT '学校或工作地址',
  hiAge         INT          NULL COMMENT '年龄',
  hiGender      VARCHAR(16)  NULL COMMENT '性别',
  hiDepartment  VARCHAR(40)  NULL COMMENT '院系',
  hiMajor       VARCHAR(40)  NULL COMMENT '专业',
  hiTitle       VARCHAR(40)  NULL COMMENT '职称',
  PRIMARY KEY (hiUuid),
  UNIQUE KEY ukHumanInfoId (hiId)
) COMMENT='人基本信息档案表';

-- 用户与人基本信息档案映射表
CREATE TABLE tblUserHumanInfo (
  uUuid  CHAR(36) NOT NULL COMMENT '用户UUID',
  hiUuid CHAR(36) NOT NULL COMMENT '档案UUID',
  PRIMARY KEY (uUuid),
  UNIQUE KEY ukUserHumanInfoHuman (hiUuid),
  CONSTRAINT fkUserHumanInfoUser FOREIGN KEY (uUuid) REFERENCES tblUser (uUuid),
  CONSTRAINT fkUserHumanInfoHuman FOREIGN KEY (hiUuid) REFERENCES tblHumanInfo (hiUuid)
) COMMENT='用户与人基本信息映射表';

-- 测试数据（供演示与 CI 使用；演示账号 001/1 等）
INSERT INTO tblUser (uUuid, uId, uName, uAge, uSex, uPwd, uRole) VALUES
('00000000-0000-0000-0000-000000000001', '001', '演示学生', 20, '男', '1', '学生'),
('00000000-0000-0000-0000-000000000002', '002', '演示教师', 35, '女', '1', '教师'),
('00000000-0000-0000-0000-000000000003', '003', '管理员', 30, '男', '1', '管理员');

INSERT INTO tblHumanInfo (hiUuid, hiId, hiName, hiAge, hiGender) VALUES
('20000000-0000-0000-0000-000000000001', '001', '演示学生', 20, 'MALE'),
('20000000-0000-0000-0000-000000000002', '002', '演示教师', 35, 'FEMALE'),
('20000000-0000-0000-0000-000000000003', '003', '管理员', 30, 'MALE');

INSERT INTO tblUserHumanInfo (uUuid, hiUuid) VALUES
('00000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001'),
('00000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002'),
('00000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000003');

-- =====================================================================
-- 六个业务模块的核心表：用户、学籍、选课、图书馆、商店、银行各一张
-- 其他表（HumanInfo、用户档案映射、订单、全局序列）属于基础或辅助数据。
-- =====================================================================

-- 学生学籍模块核心表
CREATE TABLE tblStudent (
  stUuid  CHAR(36)    NOT NULL COMMENT '学籍UUID',
  stId    VARCHAR(16) NOT NULL COMMENT '学籍业务ID',
  uUuid   CHAR(36)    NOT NULL COMMENT '用户UUID',
  stGrade VARCHAR(16) NULL COMMENT '年级',
  stState VARCHAR(16) NOT NULL DEFAULT '在读' COMMENT '学籍状态',
  PRIMARY KEY (stUuid),
  UNIQUE KEY ukStudentId (stId),
  UNIQUE KEY ukStudentUser (uUuid),
  CONSTRAINT fkStudentUser FOREIGN KEY (uUuid) REFERENCES tblUser (uUuid)
) COMMENT='学生学籍核心表';

-- 选课模块核心表
CREATE TABLE tblCourse (
  coUuid  CHAR(36)    NOT NULL COMMENT '课程UUID',
  coId    VARCHAR(16) NOT NULL COMMENT '课程业务ID',
  coName  VARCHAR(80) NOT NULL COMMENT '课程名称',
  coCredit DECIMAL(3,1) NOT NULL DEFAULT 0 COMMENT '课程学分',
  coState VARCHAR(16) NOT NULL DEFAULT '开放' COMMENT '课程状态',
  PRIMARY KEY (coUuid),
  UNIQUE KEY ukCourseId (coId)
) COMMENT='选课核心表';

-- 图书馆模块核心表
CREATE TABLE tblLibraryBook (
  lbUuid   CHAR(36)     NOT NULL COMMENT '图书UUID',
  lbId     VARCHAR(16)  NOT NULL COMMENT '图书业务ID',
  lbTitle  VARCHAR(120) NOT NULL COMMENT '书名',
  lbAuthor VARCHAR(80)  NULL COMMENT '作者',
  lbStock  INT          NOT NULL DEFAULT 0 COMMENT '可借数量',
  PRIMARY KEY (lbUuid),
  UNIQUE KEY ukLibraryBookId (lbId)
) COMMENT='图书馆核心表';

-- 银行模块核心表
CREATE TABLE tblBankAccount (
  baUuid   CHAR(36)    NOT NULL COMMENT '银行账户UUID',
  baId     VARCHAR(20) NOT NULL COMMENT '银行账户业务ID',
  uUuid    CHAR(36)    NOT NULL COMMENT '用户UUID',
  baBalance DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '账户余额',
  baState  VARCHAR(16) NOT NULL DEFAULT '正常' COMMENT '账户状态',
  PRIMARY KEY (baUuid),
  UNIQUE KEY ukBankAccountId (baId),
  UNIQUE KEY ukBankAccountUser (uUuid),
  CONSTRAINT fkBankAccountUser FOREIGN KEY (uUuid) REFERENCES tblUser (uUuid)
) COMMENT='银行核心表';

-- =====================================================================
-- 商店模块（魏雨霏 维护）
-- =====================================================================

-- 商品表
CREATE TABLE tblShopItem (
  siUuid  CHAR(36)      NOT NULL           COMMENT '商品UUID',
  siId    VARCHAR(16)   NOT NULL           COMMENT '商品业务ID',
  siName  VARCHAR(50)   NOT NULL           COMMENT '商品名称',
  siPrice DECIMAL(10,2) NOT NULL           COMMENT '单价(元)',
  siStock INT           NOT NULL DEFAULT 0 COMMENT '库存数量',
  siDesc  VARCHAR(200)  NULL               COMMENT '商品描述',
  PRIMARY KEY (siUuid),
  UNIQUE KEY ukShopItemId (siId)
) COMMENT='商品表';

-- 订单表：总价由服务器端按"单价 × 数量"计算后落库，客户端不参与金额计算
CREATE TABLE tblOrder (
  oId       VARCHAR(32)   NOT NULL                  COMMENT '订单ID',
  oUserUuid CHAR(36)      NOT NULL                  COMMENT '下单用户UUID',
  oItemId   VARCHAR(16)   NOT NULL                  COMMENT '商品ID',
  oQuantity INT           NOT NULL                  COMMENT '购买数量',
  oTotal    DECIMAL(10,2) NOT NULL                  COMMENT '订单总价(元)',
  oTime     DATETIME      NOT NULL                  COMMENT '下单时间',
  oStatus   VARCHAR(10)   NOT NULL DEFAULT '待支付' COMMENT '状态：待支付/已支付/已取消',
  PRIMARY KEY (oId),
  KEY idxOrderUser (oUserUuid),
  CONSTRAINT fkOrderUser FOREIGN KEY (oUserUuid) REFERENCES tblUser (uUuid),
  CONSTRAINT fkOrderItem FOREIGN KEY (oItemId) REFERENCES tblShopItem (siId)
) COMMENT='订单表';

-- 用户购买记录表：保存每个用户对商品的具体购买/退货明细。
CREATE TABLE tblUserPurchaseRecord (
  sprUuid      CHAR(36)      NOT NULL COMMENT '购买记录UUID',
  sprId        VARCHAR(32)   NOT NULL COMMENT '购买记录业务ID',
  userUuid     CHAR(36)      NOT NULL COMMENT '购买用户UUID',
  orderId      VARCHAR(32)   NOT NULL COMMENT '关联订单ID',
  itemUuid     CHAR(36)      NOT NULL COMMENT '商品UUID',
  itemId       VARCHAR(16)   NOT NULL COMMENT '商品业务ID',
  quantity     INT           NOT NULL COMMENT '购买数量',
  unitPrice    DECIMAL(10,2) NOT NULL COMMENT '单价(元)',
  totalPrice   DECIMAL(10,2) NOT NULL COMMENT '总价(元)',
  actionType   VARCHAR(16)   NOT NULL DEFAULT 'BUY' COMMENT '动作类型：BUY/RETURN',
  status       VARCHAR(16)   NOT NULL DEFAULT 'COMPLETED' COMMENT '状态：PENDING/COMPLETED/CANCELLED/REFUNDED',
  createdAt    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  updatedAt    DATETIME      NULL COMMENT '更新时间',
  PRIMARY KEY (sprUuid),
  UNIQUE KEY ukUserPurchaseRecordId (sprId),
  KEY idxUserPurchaseUser (userUuid),
  KEY idxUserPurchaseOrder (orderId),
  KEY idxUserPurchaseItem (itemUuid),
  KEY idxUserPurchaseTime (createdAt),
  CONSTRAINT fkUserPurchaseUser FOREIGN KEY (userUuid) REFERENCES tblUser (uUuid),
  CONSTRAINT fkUserPurchaseOrder FOREIGN KEY (orderId) REFERENCES tblOrder (oId),
  CONSTRAINT fkUserPurchaseItem FOREIGN KEY (itemUuid) REFERENCES tblShopItem (siUuid)
) COMMENT='用户购买记录表';

-- 商品测试数据（供演示与 CI 使用）
INSERT INTO tblShopItem (siUuid, siId, siName, siPrice, siStock, siDesc) VALUES
('10000000-0000-0000-0000-000000000001', 'S001', '校园文化衫', 59.90, 100, '纯棉短袖，M/L/XL'),
('10000000-0000-0000-0000-000000000002', 'S002', '笔记本', 12.50, 30, 'A5 横线本'),
('10000000-0000-0000-0000-000000000003', 'S003', '保温杯', 88.00, 20, '500ml 不锈钢');
