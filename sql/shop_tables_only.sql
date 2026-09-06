-- 仅建 shop 相关表，不影响现有 tbluser 数据

CREATE TABLE IF NOT EXISTS tblShopItem (
    siId VARCHAR(20) PRIMARY KEY COMMENT '商品ID',
    siName VARCHAR(100) NOT NULL COMMENT '商品名称',
    siPrice DECIMAL(10, 2) NOT NULL COMMENT '单价',
    siStock INT NOT NULL DEFAULT 0 COMMENT '库存',
    siDesc TEXT COMMENT '商品描述'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

CREATE TABLE IF NOT EXISTS tblOrder (
    oId VARCHAR(50) PRIMARY KEY COMMENT '订单ID',
    oUserId VARCHAR(8) NOT NULL COMMENT '用户ID',
    oItemId VARCHAR(20) NOT NULL COMMENT '商品ID',
    oQuantity INT NOT NULL COMMENT '购买数量',
    oTotal DECIMAL(10, 2) NOT NULL COMMENT '订单总额',
    oTime DATETIME NOT NULL COMMENT '下单时间',
    oStatus VARCHAR(20) NOT NULL COMMENT '订单状态',
    FOREIGN KEY (oUserId) REFERENCES tbluser(uId) ON DELETE CASCADE,
    FOREIGN KEY (oItemId) REFERENCES tblShopItem(siId) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 插入演示商品（测试用）
INSERT INTO tblShopItem (siId, siName, siPrice, siStock, siDesc) VALUES
('S001', 'T-Shirt', 59.90, 100, 'Campus style'),
('S002', 'Notebook', 29.90, 200, 'A5 set'),
('S003', 'Bottle', 79.00, 50, '500ml');
