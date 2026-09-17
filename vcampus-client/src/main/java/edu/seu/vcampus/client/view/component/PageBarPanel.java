package edu.seu.vcampus.client.view.component;

import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.message.PageResponse;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 分页栏：上一页 / 页码信息 / 下一页。学籍管理与修改审核共用。
 *
 * <p>
 * 只负责「显示第几页」与「翻页时通知页面重查」，不认识任何业务 API：页面把回调传进来，
 * 在回调里按 {@link #getPageNumber()} / {@link #getPageSize()} 重新查询即可
 * （见 ADR-0009 D1，页面不写线程逻辑）。
 *
 * <p>
 * 页码与每页条数都以服务端返回的 {@link PageResponse} 为准（由 {@link #sync} 回填）——服务端会把
 * 越界页码夹到合法区间，以它为准才不会出现「本地以为在第 5 页、服务端其实回到第 1 页」。
 * 「下一页」按钮只在未超过服务端告知的总页数时可点。
 */
public class PageBarPanel extends JPanel {

    /** 序列化版本号。 */
    private static final long serialVersionUID = 1L;

    /** 页码信息标签。 */
    private final JLabel m_label = new JLabel(" ");

    /** 上一页按钮。 */
    private final JButton m_prev = new JButton("上一页");

    /** 下一页按钮。 */
    private final JButton m_next = new JButton("下一页");

    /** 当前页码（从 1 开始）。 */
    private int m_page_number = 1;

    /** 每页条数。 */
    private int m_page_size = PageResponse.DEFAULT_PAGE_SIZE;

    /** 总页数；至少为 1，避免出现「第 1 / 0 页」。 */
    private int m_total_pages = 1;

    /**
     * 创建分页栏，每页条数取默认值。
     *
     * @param onPageChanged 翻页后的重查回调
     * @throws IllegalArgumentException 回调为 null
     */
    public PageBarPanel(final Runnable onPageChanged) {
        this(onPageChanged, PageResponse.DEFAULT_PAGE_SIZE);
    }

    /**
     * 创建分页栏并指定每页条数。
     *
     * <p>
     * 列表页大多希望一页只放几条（学籍管理取 5）：行数少时整表一屏看得完，也不必滚动，
     * 而分页条本身把「一共多少条、在第几页」写清楚了。条数由调用方决定而不是写死在这里，
     * 是因为不同列表合适的粒度不一样。
     *
     * @param onPageChanged 翻页后的重查回调
     * @param initialPageSize 每页条数（越界时夹到 [1, 100]）
     * @throws IllegalArgumentException 回调为 null
     */
    public PageBarPanel(final Runnable onPageChanged, int initialPageSize) {
        if (onPageChanged == null) {
            throw new IllegalArgumentException("onPageChanged must not be null");
        }
        m_page_size = PageResponse.normalizePageSize(initialPageSize);
        setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        setOpaque(false);
        m_prev.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (canGoPrevious()) {
                    m_page_number = m_page_number - 1;
                    onPageChanged.run();
                }
            }
        });
        m_next.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (canGoNext()) {
                    m_page_number = m_page_number + 1;
                    onPageChanged.run();
                }
            }
        });
        m_label.setForeground(UiTheme.MUTED);
        m_label.setFont(UiTheme.font(Font.PLAIN, 12F));
        add(m_prev);
        add(m_label);
        add(m_next);
        updateButtons();
    }

    /**
     * 用服务端返回的分页结果同步页码与按钮状态。
     *
     * @param page 分页结果；null 视为空结果
     */
    public void sync(PageResponse<?> page) {
        if (page == null) {
            // 空结果只有一页，页码也要跟着回到 1；否则标签写着「第 1 / 1 页」而内部还停在第 2 页。
            m_page_number = 1;
            m_total_pages = 1;
            m_label.setText("第 1 / 1 页    共 0 条");
        } else {
            m_page_number = page.getPageNumber();
            m_page_size = page.getPageSize();
            m_total_pages = Math.max(1, page.getTotalPages());
            m_label.setText("第 " + m_page_number + " / " + m_total_pages + " 页    共 "
                    + page.getTotal() + " 条");
        }
        updateButtons();
    }

    /** 回到第一页：改了查询条件之后调用，避免停留在旧结果的页码上。 */
    public void resetToFirstPage() {
        m_page_number = 1;
        updateButtons();
    }

    /** @return 当前页码（从 1 开始） */
    public int getPageNumber() {
        return m_page_number;
    }

    /** @return 每页条数 */
    public int getPageSize() {
        return m_page_size;
    }

    /** @return 总页数；无结果时为 1，不会出现 0 */
    public int getTotalPages() {
        return m_total_pages;
    }

    /**
     * 是否还能往前翻。
     *
     * <p>
     * 把翻页判定公开出来，一是两个按钮的可点状态由它统一推导（不会出现「按钮亮着但点了没反应」），
     * 二是这段与页码有关的判断可以不建界面就能单测（ADR-0009 D10：判断逻辑下沉）。
     *
     * @return 当前页不是第一页
     */
    public boolean canGoPrevious() {
        return m_page_number > 1;
    }

    /**
     * 是否还能往后翻。
     *
     * <p>
     * 总页数一律以服务端返回的为准：本地多翻一页不会报错，只会拿到空页，所以这里必须夹住。
     *
     * @return 当前页未达总页数
     */
    public boolean canGoNext() {
        return m_page_number < m_total_pages;
    }

    /** 按当前页码与总页数刷新两个按钮的可点状态。 */
    private void updateButtons() {
        m_prev.setEnabled(canGoPrevious());
        m_next.setEnabled(canGoNext());
    }
}
