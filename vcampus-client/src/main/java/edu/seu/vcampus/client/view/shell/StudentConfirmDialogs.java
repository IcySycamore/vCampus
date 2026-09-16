package edu.seu.vcampus.client.view.shell;

import edu.seu.vcampus.common.student.entity.CampusStatus;
import edu.seu.vcampus.common.student.entity.StudentProfile;

import java.awt.Component;
import javax.swing.JOptionPane;

/**
 * 学籍三个写操作的确认弹窗：文案与「点了取消就当没点」的判定。
 *
 * <p>
 * 从 {@link StudentActionBar} 抽出，一是让两者的代码各自待在能一眼读完的长度内，二是把面向用户的
 * 文案集中到一处：改一个措辞不必在动作代码里翻找。本类只问不改，返回 true 表示用户确认了。
 *
 * <p>
 * 每个动作都先确认，是因为这三件事都不可在界面上撤销：改状态会写库，注销虽然只是软删除，
 * 但记录随即从列表消失（要恢复只能手动改数据文件）。「误点即生效」在管理端是最贵的一类失误。
 */
final class StudentConfirmDialogs {

    /** 私有构造器，禁止实例化工具类。 */
    private StudentConfirmDialogs() {
    }

    /**
     * 确认修改在校状态。
     *
     * <p>
     * 文案只陈述两件事：改哪条记录、改成什么，以及确认后会发生什么。不预告「接下来还会问一句
     * 什么」——那是界面自己的流程，写在正文里就像软件在自言自语；用户需要知道的是改动本身。
     *
     * @param parent 父组件
     * @param target 目标学籍
     * @param status 要改成的状态
     * @return 用户是否确认
     */
    static boolean confirmStatusChange(Component parent, StudentProfile target,
            CampusStatus status) {
        String message = "记录：" + describe(target) + "\n"
                + "新状态：" + status.getDisplayName() + "\n\n"
                + "确认后将立即写入在校人员档案。";
        return confirm(parent, message, "修改在校状态");
    }

    /**
     * 问是否把学籍一并注销（软删除）。
     *
     * <p>
     * 用「是 / 否」而不是「确定 / 取消」：这里是两个都合理的选项，用户要明确二选一；拿「取消」
     * 当「不注销」用，会让人以为整个改状态的操作被取消了。
     *
     * @param parent 父组件
     * @param target 目标学籍
     * @param status 刚改成的状态
     * @return 用户是否选择一并注销
     */
    static boolean confirmCascadeDelete(Component parent, StudentProfile target,
            CampusStatus status) {
        String message = "记录：" + describe(target) + "\n"
                + "在校状态已改为：" + status.getDisplayName() + "\n\n"
                + "是否同时注销这条学籍？\n"
                + "注销为软删除：档案仍保留，但不再出现在学籍列表中。";
        return confirmYesNo(parent, message, "注销学籍");
    }

    /**
     * 确认注销学籍。
     *
     * @param parent 父组件
     * @param target 目标学籍
     * @return 用户是否确认
     */
    static boolean confirmDelete(Component parent, StudentProfile target) {
        String message = "记录：" + describe(target) + "\n\n"
                + "确认注销这条学籍？\n"
                + "注销为软删除：档案仍保留，但不再出现在学籍列表中。";
        return confirm(parent, message, "注销学籍");
    }

    /**
     * 弹一个「确定/取消」确认框。
     *
     * @param parent 父组件
     * @param message 正文
     * @param title 标题
     * @return 用户是否点了确定
     */
    private static boolean confirm(Component parent, String message, String title) {
        int choice = JOptionPane.showConfirmDialog(parent, message, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        return choice == JOptionPane.OK_OPTION;
    }

    /**
     * 弹一个「是/否」确认框。
     *
     * <p>
     * 与确定/取消分开，是因为「否」在这里是一个有效选择（不注销，只改状态），而不是「放弃操作」。
     *
     * @param parent 父组件
     * @param message 正文
     * @param title 标题
     * @return 用户是否点了「是」
     */
    private static boolean confirmYesNo(Component parent, String message, String title) {
        int choice = JOptionPane.showConfirmDialog(parent, message, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }

    /**
     * 目标的展示形式：主键 + 姓名。
     *
     * @param target 学籍
     * @return 展示文本
     */
    private static String describe(StudentProfile target) {
        return "#" + idText(target) + "（" + nameOf(target) + "）";
    }

    /**
     * 主键文本。
     *
     * @param target 学籍
     * @return 主键文本；无主键返回「?」
     */
    private static String idText(StudentProfile target) {
        return target.getId() == null ? "?" : target.getId().toString();
    }

    /**
     * 展示名（姓名优先，缺失时退到一句说明而不是留空）。
     *
     * @param target 学籍
     * @return 展示名
     */
    private static String nameOf(StudentProfile target) {
        String name = target.getRealName();
        return name == null || name.trim().length() == 0 ? "未登记姓名" : name;
    }
}
