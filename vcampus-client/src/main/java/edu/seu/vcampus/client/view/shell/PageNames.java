package edu.seu.vcampus.client.view.shell;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 客户端一级页面的统一标识与标题表。
 */
public final class PageNames {

    public static final String HOME = "home";

    /** 用户管理（仅管理员可注册，见 {@code Permissions.can(role, USER_MANAGE)}）。 */
    public static final String USER_ADMIN = "user-admin";

    /** 商店管理（仅管理员可注册）。 */
    public static final String SHOP_ADMIN = "shop-admin";

    public static final String STUDENT = "student";
    public static final String COURSE = "course";
    public static final String LIBRARY = "library";
    public static final String SHOP = "shop";
    public static final String BANK = "bank";

    /**
     * 全局搜索用的页面标题表（标题 → 页面标识）。
     *
     * <p>
     * 标题集中在这里，避免「侧栏、工作台磁贴、全局搜索」三处各写一份文案而逐渐走样； 调用方按自己已注册的页面过滤即可（见
     * {@code MainContentPanel#searchPages()}）。
     *
     * @return 可搜索的页面（按展示顺序）
     */
    public static Map<String, String> searchTitles() {
        Map<String, String> pages = new LinkedHashMap<String, String>();
        pages.put("校园工作台 · 待办、公告、日程", HOME);
        pages.put("个人信息 · 个人资料与在校状态", STUDENT);
        pages.put("选课与成绩 · 课程安排与学习成果", COURSE);
        pages.put("智慧图书馆 · 检索、借阅与归还", LIBRARY);
        pages.put("校园商店 · 商品与订单", SHOP);
        pages.put("校园银行 · 余额与消费流水", BANK);
        pages.put("用户管理 · 注册、启停与注销账号", USER_ADMIN);
        pages.put("商店管理 · 商品与订单管理", SHOP_ADMIN);
        return pages;
    }

    private PageNames() {
    }
}
