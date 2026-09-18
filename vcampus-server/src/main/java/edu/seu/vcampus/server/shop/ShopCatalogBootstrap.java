package edu.seu.vcampus.server.shop;

import edu.seu.vcampus.common.shop.entity.ShopItem;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 把内存版演示目录幂等迁移到 Shop JDBC 存储。 */
final class ShopCatalogBootstrap {

    private static final String[][] ITEMS = {
        { "S001", "校园文化衫", "59.90", "100", "纯棉短袖，多尺码可选" },
        { "S002", "A5 线圈笔记本", "12.50", "30", "横线内页，适合课堂记录" },
        { "S003", "不锈钢保温杯", "88.00", "20", "500ml 容量，便携防漏" },
        { "S004", "校徽钥匙扣", "9.90", "80", "金属校徽造型，轻巧耐用" },
        { "S005", "校园帆布袋", "36.00", "45", "加厚棉布，可容纳教材和电脑" },
        { "S006", "校园明信片套装", "18.00", "60", "六张校园建筑主题明信片" },
        { "S007", "校名证件挂绳", "8.90", "120", "可拆卸卡扣，适配校园卡" },
        { "S008", "晴雨折叠伞", "42.00", "28", "轻量伞骨，晴雨两用" },
        { "S009", "校园建筑书签", "6.90", "75", "金属镂空书签，附流苏" },
        { "S010", "珐琅纪念徽章", "15.90", "50", "校园主题珐琅工艺徽章" },
        { "S011", "中性笔三支装", "9.90", "90", "0.5mm 黑色速干笔芯" },
        { "S012", "荧光标记笔", "12.80", "55", "四色组合，柔和不透纸" },
        { "S013", "便签组合", "6.50", "100", "索引贴与方形便签组合装" },
        { "S014", "资料收纳袋", "5.90", "70", "A4 透明按扣文件袋" },
        { "S015", "32GB U盘", "49.90", "24", "USB 3.0，高速便携存储" },
        { "S016", "Type-C 数据线", "19.90", "65", "1.5 米编织线，支持快充" },
        { "S017", "10000mAh 移动电源", "89.00", "18", "双接口输出，带电量显示" },
        { "S018", "有线耳机", "39.90", "26", "3.5mm 接口，带线控麦克风" },
        { "S019", "USB 护眼台灯", "69.00", "16", "三档色温，亮度可调" },
        { "S020", "科学计算器", "32.00", "35", "适合基础课程与日常计算" },
        { "S021", "速干运动毛巾", "24.90", "40", "轻薄吸汗，附收纳袋" },
        { "S022", "防滑瑜伽垫", "49.00", "14", "加厚防滑，适合宿舍锻炼" },
        { "S023", "羽毛球拍套装", "58.00", "12", "双拍组合，附三只训练球" },
        { "S024", "运动水壶", "29.90", "32", "700ml 大容量，单手开盖" },
        { "S025", "洗衣液", "16.90", "48", "低泡易漂洗，1kg 瓶装" },
        { "S026", "抽纸三包装", "8.50", "85", "原生木浆，宿舍日常装" },
        { "S027", "宿舍门锁", "12.00", "38", "黄铜锁芯，附两把钥匙" },
        { "S028", "便携雨衣", "15.00", "42", "加厚可重复使用，带帽设计" },
        { "S029", "能量零食组合", "19.80", "36", "坚果与谷物棒组合装" },
        { "S030", "挂耳咖啡", "24.80", "25", "五包装，中度烘焙" }
    };

    private ShopCatalogBootstrap() {
    }

    static synchronized boolean ensure(ShopDao dao) {
        List<ShopItem> stored = dao.findAllItems();
        Set<String> itemIds = new HashSet<String>();
        if (stored != null) {
            for (ShopItem item : stored) {
                itemIds.add(item.getSiId());
            }
        }
        boolean complete = true;
        for (String[] row : ITEMS) {
            if (!itemIds.contains(row[0])) {
                complete &= dao.addItem(new ShopItem(row[0], row[1],
                        new BigDecimal(row[2]), Integer.valueOf(row[3]), row[4], null));
            }
        }
        return complete;
    }
}
