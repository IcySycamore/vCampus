-- =====================================================================
-- vCampus 虚拟校园系统 · 建库脚本（初始骨架，由组员 D 数据库设计负责扩充）
-- 库名 vCampus；目标版本 MySQL 8.0（兼容 5.7）；CI 会执行本脚本初始化测试库。
-- 命名约定：表名 tblXxx；主键/字段约定见设计说明书。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS vCampus DEFAULT CHARACTER SET utf8mb4;
USE vCampus;

-- 先删子表再删父表，避免外键依赖导致脚本重复执行时失败
DROP TABLE IF EXISTS tblOrder;
DROP TABLE IF EXISTS tblShopItem;

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

-- =====================================================================
-- 商店模块（魏雨霏 维护）
-- =====================================================================

-- 商品表
CREATE TABLE tblShopItem (
  siId    VARCHAR(16)   NOT NULL           COMMENT '商品ID',
  siName  VARCHAR(50)   NOT NULL           COMMENT '商品名称',
  siPrice DECIMAL(10,2) NOT NULL           COMMENT '单价(元)',
  siStock INT           NOT NULL DEFAULT 0 COMMENT '库存数量',
  siDesc  VARCHAR(200)  NULL               COMMENT '商品描述',
  PRIMARY KEY (siId)
) COMMENT='商品表';

-- 订单表：总价由服务器端按"单价 × 数量"计算后落库，客户端不参与金额计算
CREATE TABLE tblOrder (
  oId       VARCHAR(32)   NOT NULL                  COMMENT '订单ID',
  oUserId   VARCHAR(8)    NOT NULL                  COMMENT '下单用户ID',
  oItemId   VARCHAR(16)   NOT NULL                  COMMENT '商品ID',
  oQuantity INT           NOT NULL                  COMMENT '购买数量',
  oTotal    DECIMAL(10,2) NOT NULL                  COMMENT '订单总价(元)',
  oTime     DATETIME      NOT NULL                  COMMENT '下单时间',
  oStatus   VARCHAR(10)   NOT NULL DEFAULT '待支付' COMMENT '状态：待支付/已支付/已取消',
  PRIMARY KEY (oId),
  KEY idxOrderUser (oUserId),
  CONSTRAINT fkOrderUser FOREIGN KEY (oUserId) REFERENCES tblUser (uId),
  CONSTRAINT fkOrderItem FOREIGN KEY (oItemId) REFERENCES tblShopItem (siId)
) COMMENT='订单表';

-- 商品测试数据（供演示与 CI 使用）
INSERT INTO tblShopItem (siId, siName, siPrice, siStock, siDesc) VALUES
('S001', '校园文化衫', 59.90, 100, '纯棉短袖，M/L/XL'),
('S002', '笔记本',     12.50,  30, 'A5 横线本'),
('S003', '保温杯',     88.00,  20, '500ml 不锈钢');
