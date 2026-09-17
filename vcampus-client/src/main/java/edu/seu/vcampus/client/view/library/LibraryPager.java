package edu.seu.vcampus.client.view.library;

import edu.seu.vcampus.client.view.component.RoundedButton;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.message.PageResponse;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** 图书列表共用的分页条，只维护页码并触发页面重新查询。 */
final class LibraryPager extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JButton previous = navigationButton("上一页");
    private final JButton next = navigationButton("下一页");
    private final JLabel label = new JLabel("第 1/0 页");
    private final Runnable reload;
    private int pageNumber = PageResponse.DEFAULT_PAGE_NUMBER;
    private int totalPages;
    private boolean busy;

    LibraryPager(String namePrefix, Runnable reload) {
        this.reload = reload;
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        previous.setName(namePrefix + "Previous");
        next.setName(namePrefix + "Next");
        label.setName(namePrefix + "Page");
        previous.addActionListener(move(-1));
        next.addActionListener(move(1));
        add(previous);
        add(label);
        add(next);
        updateControls();
    }

    int getPageNumber() {
        return pageNumber;
    }

    int getPageSize() {
        return PageResponse.DEFAULT_PAGE_SIZE;
    }

    void firstPage() {
        pageNumber = PageResponse.DEFAULT_PAGE_NUMBER;
    }

    void loading() {
        busy = true;
        updateControls();
    }

    void failed() {
        busy = false;
        updateControls();
    }

    void show(PageResponse<?> page) {
        pageNumber = page.getPageNumber();
        totalPages = page.getTotalPages();
        busy = false;
        label.setText("第 " + pageNumber + "/" + totalPages + " 页 · 共 "
                + page.getTotal() + " 项");
        updateControls();
    }

    private ActionListener move(final int change) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                int target = pageNumber + change;
                if (!busy && target >= 1 && target <= totalPages) {
                    pageNumber = target;
                    reload.run();
                }
            }
        };
    }

    private void updateControls() {
        previous.setEnabled(!busy && pageNumber > 1);
        next.setEnabled(!busy && pageNumber < totalPages);
    }

    private static JButton navigationButton(String text) {
        return new RoundedButton(text, new Color(225, 241, 248),
                UiTheme.NAVY, UiTheme.NAVY_LIGHT, 14);
    }
}
