package com.time.internmanagement.ui;

import com.time.internmanagement.entity.Intern;
import com.time.internmanagement.entity.Task;
import com.time.internmanagement.entity.TaskStatus;
import com.time.internmanagement.service.AuthService;
import com.time.internmanagement.service.InternService;
import com.time.internmanagement.service.SupervisorService;
import com.time.internmanagement.service.TaskService;
import com.time.internmanagement.util.DataBaseManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;

public class InternDashboardFrame extends JFrame {

    private static final long MAX_PDF_BYTES = 5L * 1024 * 1024; // 5 MB

    private final Intern intern;
    private final AuthService authService;
    private final InternService internService;
    private final TaskService taskService;
    private final DefaultTableModel tableModel;
    private final JTable table;

    private static final Color BRAND_DARK = new Color(0x1E293B);
    private static final Color BRAND_ACCENT = new Color(0x3B82F6);
    private static final Color TEXT_MUTED = new Color(0x6B7280);
    private static final Color BORDER_GRAY = new Color(0xE5E7EB);
    private static final Color BG_LIGHT = new Color(0xF8FAFC);

    public InternDashboardFrame(Intern intern, AuthService authService,
                                InternService internService, TaskService taskService) {
        this.intern = intern;
        this.authService = authService;
        this.internService = internService;
        this.taskService = taskService;
        this.tableModel = buildTableModel();
        this.table = new JTable(tableModel);
        initUI();
        loadTasks();
    }

    private void initUI() {
        setTitle("TIME Intern Management System — My Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1020, 620);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG_LIGHT);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);

        getContentPane().setBackground(BG_LIGHT);
        add(root);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BRAND_DARK);
        header.setBorder(new EmptyBorder(20, 32, 20, 32));

        JPanel textBlock = new JPanel();
        textBlock.setOpaque(false);
        textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));

        JLabel welcome = new JLabel("Welcome, " + intern.getFullName());
        welcome.setFont(new Font("SansSerif", Font.BOLD, 20));
        welcome.setForeground(Color.WHITE);
        welcome.setAlignmentX(Component.LEFT_ALIGNMENT);

        String supervisorDisplay = intern.getSupervisorName() != null
                && !intern.getSupervisorName().isBlank()
                ? intern.getSupervisorName()
                : "—";

        JLabel details = new JLabel(String.format(
                "<html>ID: %s &nbsp;&nbsp;•&nbsp;&nbsp; Department: %s &nbsp;&nbsp;•&nbsp;&nbsp; "
                        + "Supervisor: %s &nbsp;&nbsp;•&nbsp;&nbsp; Status: %s</html>",
                intern.getInternId(),
                intern.getDepartment(),
                supervisorDisplay,
                intern.getStatus()));
        details.setFont(new Font("SansSerif", Font.PLAIN, 13));
        details.setForeground(new Color(0x94A3B8));
        details.setAlignmentX(Component.LEFT_ALIGNMENT);
        details.setBorder(new EmptyBorder(4, 0, 0, 0));

        textBlock.add(welcome);
        textBlock.add(details);

        JButton logout = new JButton("Log out");
        logout.setFont(new Font("SansSerif", Font.PLAIN, 13));
        logout.setForeground(Color.WHITE);
        logout.setBorderPainted(false);
        logout.setContentAreaFilled(false);
        logout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logout.addActionListener(e -> handleLogout());

        header.add(textBlock, BorderLayout.WEST);
        header.add(logout, BorderLayout.EAST);
        return header;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setBackground(BG_LIGHT);
        body.setBorder(new EmptyBorder(24, 32, 24, 32));

        JLabel sectionTitle = new JLabel("My Tasks");
        sectionTitle.setFont(new Font("SansSerif", Font.BOLD, 16));

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY, 1, true),
                new EmptyBorder(4, 4, 4, 4)
        ));

        styleTable();
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        card.add(scroll, BorderLayout.CENTER);
        card.add(buildActionBar(), BorderLayout.SOUTH);

        body.add(sectionTitle, BorderLayout.NORTH);
        body.add(card, BorderLayout.CENTER);
        return body;
    }

    private void styleTable() {
        table.setRowHeight(34);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(BORDER_GRAY);
        table.setSelectionBackground(new Color(0xDBEAFE));
        table.setSelectionForeground(Color.BLACK);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(0xF1F5F9));
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));

        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(280);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);
        table.getColumnModel().getColumn(5).setPreferredWidth(80);
        table.getColumnModel().getColumn(6).setPreferredWidth(140);

        table.getColumnModel().getColumn(0).setMaxWidth(70);
    }

    private JPanel buildActionBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_GRAY),
                new EmptyBorder(14, 16, 14, 16)
        ));
        bar.setBackground(Color.WHITE);

        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftButtons.setOpaque(false);

        JButton viewTaskPdf = secondaryButton("Download Task PDF");
        viewTaskPdf.addActionListener(e -> handleViewTaskPdf());

        JButton refresh = secondaryButton("Refresh");
        refresh.addActionListener(e -> loadTasks());

        leftButtons.add(viewTaskPdf);
        leftButtons.add(refresh);

        JButton submit = primaryButton("Submit Task");
        submit.addActionListener(e -> handleSubmit());
        JPanel rightButton = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightButton.setOpaque(false);
        rightButton.add(submit);

        bar.add(leftButtons, BorderLayout.WEST);
        bar.add(rightButton, BorderLayout.EAST);
        return bar;
    }

    private JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setBackground(BRAND_ACCENT);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setPreferredSize(new Dimension(140, 36));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.PLAIN, 13));
        button.setForeground(new Color(0x334155));
        button.setBackground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(BORDER_GRAY, 1, true));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private DefaultTableModel buildTableModel() {
        return new DefaultTableModel(
                new Object[]{"Task ID", "Title", "Priority", "Status", "Deadline", "Task PDF", "Submission"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private void loadTasks() {
        tableModel.setRowCount(0);
        List<Task> tasks = taskService.getTasksForIntern(intern.getInternId());
        for (Task t : tasks) {
            String taskPdf = t.getAssignmentFileName() != null ? "✓" : "—";
            String submission = t.getSubmissionFileName() != null
                    ? t.getSubmissionFileName()
                    : (t.getSubmissionText() != null ? "(text)" : "—");
            tableModel.addRow(new Object[]{
                    t.getTaskId(),
                    t.getTitle(),
                    t.getPriority(),
                    t.getStatus(),
                    t.getDeadline(),
                    taskPdf,
                    submission
            });
        }
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Log out and return to the login screen?",
                "Confirm Logout", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        dispose();

        SwingUtilities.invokeLater(() -> {
            try {
                DataBaseManager db = new DataBaseManager();
                AuthService auth = new AuthService(db);
                InternService interns = new InternService(db);
                TaskService tasks = new TaskService(interns, db);
                SupervisorService supervisors = new SupervisorService(db);

                LoginFrame login = new LoginFrame(auth, interns, tasks, supervisors);
                login.setVisible(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null,
                        "Could not return to login: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }

    private void handleViewTaskPdf() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                    "Select a task in the table first.",
                    "No task selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String taskId = (String) tableModel.getValueAt(row, 0);

        Task full;
        try {
            full = taskService.loadTaskWithFile(taskId);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not load task: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (full.getAssignmentFile() == null || full.getAssignmentFile().length == 0) {
            JOptionPane.showMessageDialog(this,
                    "No task PDF attached to this task.",
                    "No file", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save task PDF as…");
        chooser.setSelectedFile(new File(
                full.getAssignmentFileName() != null
                        ? full.getAssignmentFileName()
                        : taskId + "-task.pdf"));
        chooser.setFileFilter(new FileNameExtensionFilter("PDF files", "pdf"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File target = chooser.getSelectedFile();
        if (!target.getName().toLowerCase().endsWith(".pdf")) {
            target = new File(target.getAbsolutePath() + ".pdf");
        }

        try {
            Files.write(target.toPath(), full.getAssignmentFile());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not save file: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(target);
            }
        } catch (Exception ignored) {
        }
    }

    private void handleSubmit() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                    "Select a task in the table first.",
                    "No task selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String taskId = (String) tableModel.getValueAt(row, 0);
        TaskStatus status = (TaskStatus) tableModel.getValueAt(row, 3);

        if (status == TaskStatus.COMPLETED) {
            JOptionPane.showMessageDialog(this,
                    "This task has already been submitted.",
                    "Already submitted", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        LocalDate deadline = (LocalDate) tableModel.getValueAt(row, 4);
        if (deadline != null && LocalDate.now().isAfter(deadline)) {
            JOptionPane.showMessageDialog(this,
                    "The deadline has passed. You cannot submit this task now.\n"
                            + "Deadline was " + deadline + ".",
                    "Deadline passed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select your submission (PDF only)");
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileNameExtensionFilter("PDF files", "pdf"));

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();

        if (!file.getName().toLowerCase().endsWith(".pdf")) {
            JOptionPane.showMessageDialog(this,
                    "Only PDF files are accepted.",
                    "Invalid file type", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (file.length() > MAX_PDF_BYTES) {
            JOptionPane.showMessageDialog(this,
                    "PDF is too large. Maximum size is 5 MB.",
                    "File too large", JOptionPane.ERROR_MESSAGE);
            return;
        }

        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file.toPath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not read the file: " + ex.getMessage(),
                    "Submission failed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (bytes.length < 5
                || bytes[0] != '%'
                || bytes[1] != 'P'
                || bytes[2] != 'D'
                || bytes[3] != 'F'
                || bytes[4] != '-') {
            JOptionPane.showMessageDialog(this,
                    "That file isn't a valid PDF.",
                    "Invalid file", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String comment = JOptionPane.showInputDialog(this,
                "Add an optional comment for your submission:", "");
        if (comment == null) return;

        try {
            taskService.submitTask(taskId, comment, bytes, file.getName());
            JOptionPane.showMessageDialog(this,
                    "Submission uploaded for task " + taskId + ".",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            loadTasks();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not submit: " + ex.getMessage(),
                    "Submission failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}