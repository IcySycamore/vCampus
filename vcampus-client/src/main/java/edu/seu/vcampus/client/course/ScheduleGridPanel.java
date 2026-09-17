package edu.seu.vcampus.client.course;

import edu.seu.vcampus.client.api.ApiException;
import edu.seu.vcampus.client.view.UiTasks;
import edu.seu.vcampus.client.view.theme.UiFactory;
import edu.seu.vcampus.client.view.theme.UiTheme;
import edu.seu.vcampus.common.course.Classroom;
import edu.seu.vcampus.common.course.Course;
import edu.seu.vcampus.common.course.CourseScheduler;
import edu.seu.vcampus.common.course.ScheduleEntry;
import edu.seu.vcampus.common.course.ScheduleHistory;
import edu.seu.vcampus.common.course.Teacher;
import edu.seu.vcampus.common.course.Timeslot;
import edu.seu.vcampus.common.course.dto.CourseSaveRequest;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * 管理员「排课」界面：三栏布局 + 每间教室一张独立课表。
 *
 * <p>顶部工具栏（自动排课/撤销/重做/应用到服务器）+ 左侧待排课程列表 + 中间「每教室一个页签」的
 * 「节次×星期」网格 + 右侧详情与冲突提示。点击待排课程后点击某教室网格的空格即可排课；冲突格子标红。
 */
public class ScheduleGridPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {"时间", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    private final CourseService api;
    private static final String[] COURSE_COLUMNS = {"课程编号", "课程名称", "授课教师", "容量"};
    private static final int PAGE_SIZE = 8;

    private final DefaultTableModel courseTableModel = new DefaultTableModel(COURSE_COLUMNS, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable courseTable = new JTable(courseTableModel);
    private final JTextField searchField = new JTextField(20);
    private final JLabel pageLabel = new JLabel("第 1 / 1 页");
    private final List<ScheduleEntry> pageEntries = new ArrayList<ScheduleEntry>();
    private int coursePage = 1;
    private final Map<String, DefaultTableModel> gridModels = new HashMap<String, DefaultTableModel>();
    private final JTabbedPane classroomTabs = new JTabbedPane();
    private final JLabel statsLabel = new JLabel("  已排 0 / 未排 0");
    private final JTextArea detailsArea = new JTextArea(12, 18);
    private final JLabel statusLabel = new JLabel("  请登录后使用排课功能");

    private final List<ScheduleEntry> schedule = new ArrayList<ScheduleEntry>();
    private final List<Classroom> classrooms = new ArrayList<Classroom>();
    private final List<Teacher> teachers = new ArrayList<Teacher>();
    private final ScheduleHistory history = new ScheduleHistory();
    private String selectedCourseCode;
    private Set<String> conflictCells = new HashSet<String>();

    /** 创建离线预览界面。 */
    public ScheduleGridPanel() {
        this(null);
    }

    /**
     * 创建接入排课服务的界面；{@code api} 为 null 时仅离线预览。
     *
     * @param api 选课 API
     */
    public ScheduleGridPanel(CourseService api) {
        this.api = api;
        setLayout(new BorderLayout(0, 14));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 22, 28));
        add(toolbar(), BorderLayout.NORTH);
        add(center(), BorderLayout.CENTER);
        styleStatus();
        add(statusLabel, BorderLayout.SOUTH);

        courseTable.getTableHeader().setReorderingAllowed(false);
        courseTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                int row = courseTable.rowAtPoint(event.getPoint());
                if (row < 0 || row >= pageEntries.size()) {
                    return;
                }
                ScheduleEntry entry = pageEntries.get(row);
                selectedCourseCode = entry.getCourseCode();
                updateDetails();
                openCourseDialog(entry);
            }
        });

        detailsArea.setEditable(false);
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        detailsArea.setFont(UiTheme.font(Font.PLAIN, 13F));
        detailsArea.setForeground(UiTheme.TEXT);
        detailsArea.setBackground(UiTheme.SURFACE);

        refresh();
    }

    private JPanel toolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        toolbar.setOpaque(false);
        JButton addButton = UiFactory.primaryButton("添加课程", "user");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                showAddDialog();
            }
        });
        toolbar.add(addButton);
        JButton deleteButton = UiFactory.secondaryButton("删除课程", "return");
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                confirmDelete();
            }
        });
        toolbar.add(deleteButton);
        JButton undoButton = UiFactory.secondaryButton("撤销", "return");
        undoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                undo();
            }
        });
        toolbar.add(undoButton);
        JButton redoButton = UiFactory.secondaryButton("重做", "edit");
        redoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                redo();
            }
        });
        toolbar.add(redoButton);
        JButton applyButton = UiFactory.secondaryButton("应用到服务器", "refresh");
        applyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                applyToServer();
            }
        });
        toolbar.add(applyButton);
        JButton refreshButton = UiFactory.secondaryButton("刷新", "refresh");
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                refresh();
            }
        });
        toolbar.add(refreshButton);
        toolbar.add(statsLabel);
        return toolbar;
    }

    private JPanel center() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        JPanel rightSide = new JPanel(new BorderLayout(10, 0));
        rightSide.setOpaque(false);
        rightSide.add(classroomTabs, BorderLayout.CENTER);
        rightSide.add(rightPanel(), BorderLayout.EAST);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel(), rightSide);
        split.setDividerLocation(420);
        split.setResizeWeight(0);
        split.setContinuousLayout(true);
        split.setBorder(null);
        wrapper.add(split, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel leftPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        searchField.setPreferredSize(new java.awt.Dimension(380, 36));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent event) {
                coursePage = 1;
                renderUnscheduled();
            }
        });
        panel.add(searchField, BorderLayout.NORTH);

        JPanel listPanel = new JPanel(new BorderLayout(0, 6));
        listPanel.setOpaque(false);
        JLabel title = label("待排课程");
        title.setFont(UiTheme.font(Font.BOLD, 16F));
        listPanel.add(title, BorderLayout.NORTH);
        UiFactory.styleTable(courseTable);
        JScrollPane listScroll = new JScrollPane(courseTable);
        listScroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        listScroll.getViewport().setBackground(UiTheme.SURFACE);
        listPanel.add(listScroll, BorderLayout.CENTER);
        listPanel.add(pageBar(), BorderLayout.SOUTH);

        panel.add(listPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel pageBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        bar.setOpaque(false);
        JButton prev = UiFactory.secondaryButton("上一页", "return");
        prev.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (coursePage > 1) {
                    coursePage--;
                    renderUnscheduled();
                }
            }
        });
        bar.add(prev);
        bar.add(pageLabel);
        JButton next = UiFactory.secondaryButton("下一页", "edit");
        next.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                coursePage++;
                renderUnscheduled();
            }
        });
        bar.add(next);
        return bar;
    }

    private JPanel rightPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        JLabel title = label("详情与冲突");
        title.setFont(UiTheme.font(Font.BOLD, 16F));
        panel.add(title, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(detailsArea);
        scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        scroll.setPreferredSize(new java.awt.Dimension(260, 320));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(UiTheme.MUTED);
        label.setFont(UiTheme.font(Font.BOLD, 13F));
        return label;
    }

    private void styleStatus() {
        statusLabel.setOpaque(true);
        statusLabel.setForeground(UiTheme.MUTED);
        statusLabel.setBackground(new Color(234, 241, 245));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(9, 10, 9, 10));
    }

    private class CellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        private final String roomUuid;

        CellRenderer(String roomUuid) {
            this.roomUuid = roomUuid;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
                    row, column);
            setHorizontalAlignment(CENTER);
            if (column == 0) {
                c.setBackground(UiTheme.SURFACE);
                c.setForeground(UiTheme.MUTED);
                c.setFont(UiTheme.font(Font.BOLD, 13F));
                return c;
            }
            c.setFont(UiTheme.font(Font.PLAIN, 12F));
            if (conflictCells.contains(cellKey(roomUuid, column, row))) {
                c.setBackground(new Color(255, 214, 214));
                c.setForeground(new Color(180, 0, 0));
            } else {
                c.setBackground(isSelected ? table.getSelectionBackground() : UiTheme.SURFACE);
                c.setForeground(UiTheme.TEXT);
            }
            return c;
        }
    }

    private void refresh() {
        if (api == null) {
            statusLabel.setText("  请登录后使用排课功能");
            return;
        }
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                loadCourses(api.listCourses());
                loadClassrooms(api.listClassrooms());
                loadTeachers(api.listTeachers());
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void result) {
                render();
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                statusLabel.setText("  " + error.getMessage());
            }
        });
    }

    private void loadCourses(List<Course> courses) {
        schedule.clear();
        if (courses != null) {
            for (Course course : courses) {
                ScheduleEntry entry = new ScheduleEntry(course.getCode(), course.getName(),
                        course.getTeacherUuid(), course.getCapacity(), course.getEnrolled());
                entry.setUuid(course.getUuid());
                entry.setClassroomUuid(course.getClassroomUuid());
                entry.setTimeslot(course.getTimeslot());
                entry.setRequiredDirections(course.getRequiredDirections());
                entry.setEligibleMajors(course.getEligibleMajors());
                entry.setCollegeUuid(course.getCollegeUuid());
                schedule.add(entry);
            }
        }
    }

    private void loadClassrooms(List<Classroom> list) {
        classrooms.clear();
        if (list != null) {
            classrooms.addAll(list);
        }
        rebuildClassroomTabs();
    }

    private void loadTeachers(List<Teacher> list) {
        teachers.clear();
        if (list != null) {
            teachers.addAll(list);
        }
    }

    private void rebuildClassroomTabs() {
        classroomTabs.removeAll();
        gridModels.clear();
        for (final Classroom room : classrooms) {
            DefaultTableModel model = new DefaultTableModel(COLUMNS, CourseScheduler.PERIODS) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            final JTable table = new JTable(model);
            table.getTableHeader().setReorderingAllowed(false);
            table.setRowHeight(52);
            table.setDefaultRenderer(Object.class, new CellRenderer(room.getUuid()));
            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    int row = table.rowAtPoint(event.getPoint());
                    int column = table.columnAtPoint(event.getPoint());
                    if (row >= 0 && column >= 1) {
                        onGridCellClicked(room.getUuid(), column, row);
                    }
                }
            });
            gridModels.put(room.getUuid(), model);
            JScrollPane scroll = new JScrollPane(table);
            scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
            scroll.getViewport().setBackground(UiTheme.SURFACE);
            classroomTabs.addTab(room.getLocation() + room.getName() + "（容量"
                    + room.getCapacity() + "）", scroll);
        }
    }

    private void render() {
        conflictCells = computeConflicts();
        renderGrid();
        renderUnscheduled();
        updateStats();
        updateDetails();
    }

    private void renderGrid() {
        for (Classroom room : classrooms) {
            DefaultTableModel model = gridModels.get(room.getUuid());
            if (model == null) {
                continue;
            }
            for (int row = 0; row < CourseScheduler.PERIODS; row++) {
                model.setValueAt(CourseScheduler.periodName(row), row, 0);
                for (int col = 1; col <= CourseScheduler.WEEKDAYS; col++) {
                    model.setValueAt("", row, col);
                }
            }
        }
        for (ScheduleEntry entry : schedule) {
            if (entry.getTimeslot() == null) {
                continue;
            }
            DefaultTableModel model = gridModels.get(entry.getClassroomUuid());
            if (model == null) {
                continue;
            }
            for (int[] cell : cellsOf(entry)) {
                model.setValueAt(entry.getCourseName(), cell[1], cell[0]);
            }
        }
    }

    private void renderUnscheduled() {
        courseTableModel.setRowCount(0);
        pageEntries.clear();
        String key = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        List<ScheduleEntry> filtered = new ArrayList<ScheduleEntry>();
        for (ScheduleEntry entry : schedule) {
            if (entry.getTimeslot() != null) {
                continue;
            }
            String label = entry.getCourseCode() + " " + entry.getCourseName();
            if (key.length() == 0 || label.toLowerCase().contains(key)) {
                filtered.add(entry);
            }
        }
        int totalPages = (filtered.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        if (totalPages == 0) {
            totalPages = 1;
        }
        if (coursePage > totalPages) {
            coursePage = totalPages;
        }
        int from = (coursePage - 1) * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, filtered.size());
        for (int i = from; i < to; i++) {
            ScheduleEntry entry = filtered.get(i);
            pageEntries.add(entry);
            courseTableModel.addRow(new Object[] {
                entry.getCourseCode(), entry.getCourseName(),
                entry.getTeacherUuid() == null ? "未认领" : entry.getTeacherUuid(),
                Integer.valueOf(entry.getCapacity())
            });
        }
        pageLabel.setText("第 " + coursePage + " / " + totalPages + " 页");
    }

    private void updateStats() {
        int scheduled = scheduledEntries().size();
        statsLabel.setText("  已排 " + scheduled + " / 未排 " + (schedule.size() - scheduled));
    }

    private Set<String> computeConflicts() {
        Set<String> cells = new HashSet<String>();
        List<ScheduleEntry> scheduled = scheduledEntries();
        for (ScheduleEntry entry : scheduled) {
            List<ScheduleEntry> others = new ArrayList<ScheduleEntry>();
            for (ScheduleEntry other : scheduled) {
                if (other != entry) {
                    others.add(other);
                }
            }
            List<String> reasons = CourseScheduler.conflicts(entry, others,
                    classroomOf(entry.getClassroomUuid()), teacherOf(entry.getTeacherUuid()));
            if (!reasons.isEmpty()) {
                for (int[] cell : cellsOf(entry)) {
                    cells.add(cellKey(entry.getClassroomUuid(), cell[0], cell[1]));
                }
            }
        }
        return cells;
    }

    private List<ScheduleEntry> scheduledEntries() {
        List<ScheduleEntry> result = new ArrayList<ScheduleEntry>();
        for (ScheduleEntry entry : schedule) {
            if (entry.getTimeslot() != null) {
                result.add(entry);
            }
        }
        return result;
    }

    private List<int[]> cellsOf(ScheduleEntry entry) {
        List<int[]> result = new ArrayList<int[]>();
        Timeslot t = entry.getTimeslot();
        if (t == null) {
            return result;
        }
        for (int period = 0; period < CourseScheduler.PERIODS; period++) {
            if (CourseScheduler.periodTimeslot(t.getWeekday(), period).overlaps(t)) {
                result.add(new int[] {t.getWeekday(), period});
            }
        }
        return result;
    }

    private String cellKey(String roomUuid, int weekday, int period) {
        return roomUuid + "-" + weekday + "-" + period;
    }

    private Classroom classroomOf(String uuid) {
        if (uuid == null) {
            return null;
        }
        for (Classroom room : classrooms) {
            if (uuid.equals(room.getUuid())) {
                return room;
            }
        }
        return null;
    }

    private Teacher teacherOf(String uuid) {
        if (uuid == null) {
            return null;
        }
        for (Teacher teacher : teachers) {
            if (uuid.equals(teacher.getUuid())) {
                return teacher;
            }
        }
        return null;
    }

    private String locationOf(ScheduleEntry entry) {
        if (entry.getClassroomLocation() != null) {
            return entry.getClassroomLocation();
        }
        Classroom room = classroomOf(entry.getClassroomUuid());
        return room == null ? "" : room.getLocation();
    }

    private ScheduleEntry entryAtClassroom(String roomUuid, int weekday, int period) {
        Timeslot target = CourseScheduler.periodTimeslot(weekday, period);
        for (ScheduleEntry entry : schedule) {
            if (entry.getTimeslot() != null && target.overlaps(entry.getTimeslot())
                    && roomUuid.equals(entry.getClassroomUuid())) {
                return entry;
            }
        }
        return null;
    }

    private void onGridCellClicked(String roomUuid, int weekday, int period) {
        ScheduleEntry existing = entryAtClassroom(roomUuid, weekday, period);
        if (existing != null) {
            selectedCourseCode = existing.getCourseCode();
            updateDetails();
            openCourseDialog(existing);
            return;
        }
        if (selectedCourseCode == null) {
            statusLabel.setText("  请先在左侧选择一门待排课程");
            return;
        }
        assignCourse(roomUuid, weekday, period);
    }

    private ScheduleEntry findEntry(String code) {
        for (ScheduleEntry entry : schedule) {
            if (entry.getCourseCode().equals(code)) {
                return entry;
            }
        }
        return null;
    }

    private void assignCourse(String roomUuid, int weekday, int period) {
        ScheduleEntry entry = findEntry(selectedCourseCode);
        if (entry == null) {
            statusLabel.setText("  课程不存在");
            return;
        }
        Classroom room = classroomOf(roomUuid);
        if (room == null) {
            statusLabel.setText("  教室不存在");
            return;
        }
        ScheduleEntry attempt = entry.copy();
        attempt.setTimeslot(resolveTimeslotFor(entry, weekday, period));
        attempt.setClassroomUuid(room.getUuid());
        attempt.setClassroomLocation(room.getLocation());

        List<ScheduleEntry> others = new ArrayList<ScheduleEntry>();
        for (ScheduleEntry other : scheduledEntries()) {
            if (!other.getCourseCode().equals(entry.getCourseCode())) {
                others.add(other);
            }
        }
        List<String> reasons = CourseScheduler.conflicts(attempt, others, room,
                teacherOf(attempt.getTeacherUuid()));
        if (!reasons.isEmpty()) {
            statusLabel.setText("  排课冲突：" + join(reasons));
            return;
        }
        applyNewState(replaceEntry(attempt));
        statusLabel.setText("  已排：" + entry.getCourseName() + " → " + room.getLocation());
    }

    private List<ScheduleEntry> replaceEntry(ScheduleEntry updated) {
        List<ScheduleEntry> result = new ArrayList<ScheduleEntry>();
        for (ScheduleEntry entry : schedule) {
            if (entry.getCourseCode().equals(updated.getCourseCode())) {
                result.add(updated.copy());
            } else {
                result.add(entry.copy());
            }
        }
        return result;
    }

    private void applyNewState(List<ScheduleEntry> newState) {
        history.record(schedule);
        schedule.clear();
        schedule.addAll(newState);
        render();
    }

    private void undo() {
        List<ScheduleEntry> prev = history.undo(schedule);
        schedule.clear();
        schedule.addAll(prev);
        render();
        statusLabel.setText("  已撤销");
    }

    private void redo() {
        List<ScheduleEntry> next = history.redo(schedule);
        schedule.clear();
        schedule.addAll(next);
        render();
        statusLabel.setText("  已重做");
    }

    private void applyToServer() {
        if (api == null) {
            statusLabel.setText("  服务器未连接，当前仅可预览界面");
            return;
        }
        final List<CourseSaveRequest> requests = new ArrayList<CourseSaveRequest>();
        for (ScheduleEntry entry : schedule) {
            requests.add(toSaveRequest(entry));
        }
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                for (CourseSaveRequest request : requests) {
                    api.updateCourse(request);
                }
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void result) {
                statusLabel.setText("  已应用 " + requests.size() + " 门课程到服务器");
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                statusLabel.setText("  " + error.getMessage());
            }
        });
    }

    private CourseSaveRequest toSaveRequest(ScheduleEntry entry) {
        CourseSaveRequest request = new CourseSaveRequest();
        request.setUuid(entry.getUuid());
        request.setCode(entry.getCourseCode());
        request.setName(entry.getCourseName());
        request.setCapacity(Integer.valueOf(entry.getCapacity()));
        request.setTeacherUuid(entry.getTeacherUuid() == null ? "" : entry.getTeacherUuid());
        request.setClassroomUuid(entry.getClassroomUuid() == null ? "" : entry.getClassroomUuid());
        request.setTimeslot(entry.getTimeslot());
        request.setRequiredDirections(entry.getRequiredDirections());
        request.setEligibleMajors(entry.getEligibleMajors());
        request.setCollegeUuid(entry.getCollegeUuid() == null ? "" : entry.getCollegeUuid());
        return request;
    }

    private void applyEdit(CourseSaveRequest request) {
        history.record(schedule);
        List<ScheduleEntry> newSchedule = new ArrayList<ScheduleEntry>();
        for (ScheduleEntry entry : schedule) {
            if (entry.getUuid() != null && entry.getUuid().equals(request.getUuid())) {
                newSchedule.add(entryFromRequest(entry, request));
            } else {
                newSchedule.add(entry.copy());
            }
        }
        schedule.clear();
        schedule.addAll(newSchedule);
        render();
        statusLabel.setText("  课程信息已保存（待应用到服务器）");
    }

    private ScheduleEntry entryFromRequest(ScheduleEntry old, CourseSaveRequest request) {
        ScheduleEntry entry = old.copy();
        if (request.getCode() != null && request.getCode().trim().length() > 0) {
            entry.setCourseCode(request.getCode().trim());
        }
        if (request.getName() != null && request.getName().trim().length() > 0) {
            entry.setCourseName(request.getName().trim());
        }
        if (request.getCapacity() != null) {
            entry.setCapacity(request.getCapacity().intValue());
        }
        if (request.getTeacherUuid() != null) {
            String teacher = request.getTeacherUuid().trim();
            entry.setTeacherUuid(teacher.length() == 0 ? null : teacher);
        }
        if (request.getClassroomUuid() != null) {
            String room = request.getClassroomUuid().trim();
            entry.setClassroomUuid(room.length() == 0 ? null : room);
            entry.setClassroomLocation(null);
        }
        if (request.getTimeslot() != null) {
            entry.setTimeslot(request.getTimeslot());
        }
        if (request.getRequiredDirections() != null) {
            entry.setRequiredDirections(request.getRequiredDirections());
        }
        if (request.getEligibleMajors() != null) {
            entry.setEligibleMajors(request.getEligibleMajors());
        }
        if (request.getCollegeUuid() != null) {
            String college = request.getCollegeUuid().trim();
            entry.setCollegeUuid(college.length() == 0 ? null : college);
        }
        return entry;
    }

    private Timeslot resolveTimeslotFor(ScheduleEntry entry, int weekday, int period) {
        if (entry.getTimeslot() != null) {
            return new Timeslot(weekday, entry.getTimeslot().getStartMinute(),
                    entry.getTimeslot().getEndMinute());
        }
        return CourseScheduler.periodTimeslot(weekday, period);
    }

    private void openCourseDialog(ScheduleEntry entry) {
        if (entry == null) {
            return;
        }
        final JDialog dialog = new JDialog();
        dialog.setTitle("课程信息：" + entry.getCourseName());
        dialog.setModal(true);
        CourseEditFormPanel form = new CourseEditFormPanel(api);
        form.setSaveListener(new CourseEditFormPanel.SaveListener() {
            @Override
            public void onSave(CourseSaveRequest request) {
                applyEdit(request);
                dialog.dispose();
            }
        });
        form.render(entry, teachers, classrooms);
        dialog.setContentPane(form);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void showAddDialog() {
        final JDialog dialog = new JDialog();
        dialog.setTitle("添加课程");
        dialog.setModal(true);
        CourseEditFormPanel form = new CourseEditFormPanel(api);
        form.setSaveListener(new CourseEditFormPanel.SaveListener() {
            @Override
            public void onSave(CourseSaveRequest request) {
                submitAdd(request);
                dialog.dispose();
            }
        });
        form.renderNew(teachers, classrooms);
        dialog.setContentPane(form);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void submitAdd(final CourseSaveRequest request) {
        if (api == null) {
            statusLabel.setText("  服务器未连接，当前仅可预览界面");
            return;
        }
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                api.addCourse(request);
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void result) {
                statusLabel.setText("  课程已添加");
                refresh();
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                statusLabel.setText("  " + error.getMessage());
            }
        });
    }

    private void confirmDelete() {
        if (selectedCourseCode == null) {
            statusLabel.setText("  请先在左侧选择一门课程");
            return;
        }
        ScheduleEntry entry = findEntry(selectedCourseCode);
        int result = JOptionPane.showConfirmDialog(this,
                "确认删除课程「" + entry.getCourseCode() + " " + entry.getCourseName() + "」？",
                "删除课程", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        submitDelete(entry.getCourseCode());
    }

    private void submitDelete(final String courseCode) {
        if (api == null) {
            statusLabel.setText("  服务器未连接，当前仅可预览界面");
            return;
        }
        UiTasks.run(new UiTasks.Task<Void>() {
            @Override
            public Void run() {
                api.deleteCourse(courseCode);
                return null;
            }
        }, new UiTasks.Success<Void>() {
            @Override
            public void accept(Void result) {
                statusLabel.setText("  课程已删除");
                refresh();
            }
        }, new UiTasks.Failure() {
            @Override
            public void accept(ApiException error) {
                statusLabel.setText("  " + error.getMessage());
            }
        });
    }

    private void updateDetails() {
        ScheduleEntry entry = selectedCourseCode == null ? null : findEntry(selectedCourseCode);
        if (entry == null) {
            detailsArea.setText("请选择左侧待排课程，或点击某教室网格中的已排课程查看详情。");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("课程：").append(entry.getCourseCode()).append(" ")
                .append(entry.getCourseName()).append("\n");
        sb.append("教师：").append(entry.getTeacherUuid() == null ? "未认领" : entry.getTeacherUuid())
                .append("\n");
        sb.append("容量/已选：").append(entry.getCapacity()).append(" / ")
                .append(entry.getEnrolled()).append("\n");
        if (entry.getTimeslot() != null) {
            sb.append("时间：").append(entry.getTimeslot()).append("\n");
            sb.append("教室：").append(locationOf(entry)).append("\n");
            List<ScheduleEntry> others = new ArrayList<ScheduleEntry>();
            for (ScheduleEntry other : scheduledEntries()) {
                if (!other.getCourseCode().equals(entry.getCourseCode())) {
                    others.add(other);
                }
            }
            List<String> reasons = CourseScheduler.conflicts(entry, others,
                    classroomOf(entry.getClassroomUuid()), teacherOf(entry.getTeacherUuid()));
            sb.append("冲突：").append(reasons.isEmpty() ? "无" : join(reasons)).append("\n");
        } else {
            sb.append("状态：待排课\n");
        }
        detailsArea.setText(sb.toString());
    }

    private static String join(List<String> reasons) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < reasons.size(); i++) {
            if (i > 0) {
                sb.append("；");
            }
            sb.append(reasons.get(i));
        }
        return sb.toString();
    }
}
