package edu.seu.vcampus.client.view.shop;

import edu.seu.vcampus.common.shop.entity.ShopItem;
import java.awt.Color;

/** 单店铺商品目录使用的分类名称、筛选项和视觉色。 */
final class ShopCategoryCatalog {

    private static final String[] IDS = {
        "", "CULTURE", "STUDY", "DIGITAL", "SPORT", "LIFE"
    };
    private static final String[] NAMES = {
        "全部分类", "校园文创", "学习文具", "数码配件", "运动健康", "生活补给"
    };

    private ShopCategoryCatalog() {
    }

    static String[] filterNames() {
        return NAMES.clone();
    }

    static String idAt(int index) {
        return index >= 0 && index < IDS.length ? IDS[index] : "";
    }

    static boolean matches(ShopItem item, String categoryId) {
        return categoryId == null || categoryId.length() == 0
                || categoryId.equals(idFor(item));
    }

    static String nameFor(ShopItem item) {
        String id = idFor(item);
        for (int index = 1; index < IDS.length; index++) {
            if (IDS[index].equals(id)) {
                return NAMES[index];
            }
        }
        return "校园好物";
    }

    static Color colorFor(ShopItem item) {
        String id = idFor(item);
        if ("CULTURE".equals(id)) {
            return new Color(178, 55, 62);
        }
        if ("DIGITAL".equals(id)) {
            return new Color(43, 112, 102);
        }
        if ("SPORT".equals(id)) {
            return new Color(68, 91, 145);
        }
        if ("LIFE".equals(id)) {
            return new Color(137, 92, 46);
        }
        return new Color(31, 73, 101);
    }

    private static String idFor(ShopItem item) {
        String id = item == null ? null : item.getSiId();
        if (isOneOf(id, "S001", "S004", "S005", "S006", "S007", "S009", "S010")) {
            return "CULTURE";
        }
        if (isOneOf(id, "S002", "S011", "S012", "S013", "S014", "S020")) {
            return "STUDY";
        }
        if (isOneOf(id, "S015", "S016", "S017", "S018", "S019")) {
            return "DIGITAL";
        }
        if (isOneOf(id, "S021", "S022", "S023", "S024")) {
            return "SPORT";
        }
        return "LIFE";
    }

    private static boolean isOneOf(String value, String... candidates) {
        for (String candidate : candidates) {
            if (candidate.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
