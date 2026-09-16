-- ================================================
-- 多商店系统迁移脚本
-- 将单商店单商品订单升级为多商店多商品订单
-- ================================================

-- 步骤 1: 创建商店表
CREATE TABLE IF NOT EXISTS tblShop (
    shopId VARCHAR(20) PRIMARY KEY COMMENT '商店ID',
    shopName VARCHAR(100) NOT NULL COMMENT '商店名称',
    shopDesc TEXT COMMENT '商店描述',
    shopStatus VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '商店状态'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商店表';

-- 插入商店1、商店2、商店3
INSERT INTO tblShop (shopId, shopName, shopDesc) VALUES
('SHOP1', '商店1', '第一个商店'),
('SHOP2', '商店2', '第二个商店'),
('SHOP3', '商店3', '第三个商店');

-- 步骤 2: 备份现有订单表
CREATE TABLE IF NOT EXISTS tblOrder_backup AS SELECT * FROM tblOrder;

-- 步骤 3: 修改商品表，添加商店ID
ALTER TABLE tblShopItem ADD COLUMN siShopId VARCHAR(20) NOT NULL DEFAULT 'SHOP1';
ALTER TABLE tblShopItem ADD FOREIGN KEY (siShopId) REFERENCES tblShop(shopId);

-- 步骤 4: 创建订单明细表
CREATE TABLE IF NOT EXISTS tblOrderItem (
    oiId VARCHAR(50) PRIMARY KEY COMMENT '订单明细ID',
    oiOrderId VARCHAR(50) NOT NULL COMMENT '订单ID',
    oiItemId VARCHAR(20) NOT NULL COMMENT '商品ID',
    oiQuantity INT NOT NULL COMMENT '购买数量',
    oiPrice DECIMAL(10, 2) NOT NULL COMMENT '下单时单价',
    oiSubtotal DECIMAL(10, 2) NOT NULL COMMENT '小计',
    FOREIGN KEY (oiOrderId) REFERENCES tblOrder(oId) ON DELETE CASCADE,
    FOREIGN KEY (oiItemId) REFERENCES tblShopItem(siId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- 步骤 5: 修改订单主表，添加商店ID
ALTER TABLE tblOrder ADD COLUMN oShopId VARCHAR(20);
UPDATE tblOrder o
INNER JOIN tblShopItem si ON o.oItemId = si.siId
SET o.oShopId = si.siShopId;
UPDATE tblOrder SET oShopId = 'SHOP1' WHERE oShopId IS NULL;
ALTER TABLE tblOrder MODIFY oShopId VARCHAR(20) NOT NULL;
ALTER TABLE tblOrder ADD FOREIGN KEY (oShopId) REFERENCES tblShop(shopId);

-- 步骤 6: 迁移现有订单到订单明细表
INSERT INTO tblOrderItem (oiId, oiOrderId, oiItemId, oiQuantity, oiPrice, oiSubtotal)
SELECT
    CONCAT(oId, '-1') AS oiId,
    oId,
    oItemId,
    oQuantity,
    oTotal / oQuantity AS oiPrice,
    oTotal
FROM tblOrder;
