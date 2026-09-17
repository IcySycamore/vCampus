package edu.seu.vcampus.client.view.library;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.table.TableCellRenderer;

/** 在列宽不足时循环滚动文本的表格单元格渲染器。 */
final class LibraryMarqueeCellRenderer extends JComponent implements TableCellRenderer {
    private static final long serialVersionUID = 1L;
    private static final int PADDING = 6;
    private static final int GAP = 24;
    private static final int STEP = 2;
    private JTable table;
    private String text = "";
    private boolean selected;
    private int offset;
    private final Timer timer;

    LibraryMarqueeCellRenderer() {
        timer = new Timer(70, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (table != null && table.isShowing()) {
                    advance();
                } else {
                    ((Timer) event.getSource()).stop();
                }
            }
        });
    }

    @Override
    public Component getTableCellRendererComponent(JTable source, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        table = source;
        text = value == null ? "" : String.valueOf(value);
        selected = isSelected;
        setFont(source.getFont());
        if (source.isShowing() && !timer.isRunning()) {
            timer.start();
        }
        return this;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Color background = selected ? table.getSelectionBackground() : table.getBackground();
        Color foreground = selected ? table.getSelectionForeground() : table.getForeground();
        graphics.setColor(background);
        graphics.fillRect(0, 0, getWidth(), getHeight());
        graphics.setColor(foreground);
        graphics.setFont(getFont());
        FontMetrics metrics = graphics.getFontMetrics();
        int baseline = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
        int textWidth = metrics.stringWidth(text);
        if (textWidth <= getWidth() - PADDING * 2) {
            graphics.drawString(text, PADDING, baseline);
            return;
        }
        int cycle = textWidth + GAP;
        int start = PADDING - offset % cycle;
        graphics.drawString(text, start, baseline);
        graphics.drawString(text, start + cycle, baseline);
    }

    void advance() {
        offset += STEP;
        if (table != null) {
            table.repaint();
        }
    }

    int offset() {
        return offset;
    }
}
