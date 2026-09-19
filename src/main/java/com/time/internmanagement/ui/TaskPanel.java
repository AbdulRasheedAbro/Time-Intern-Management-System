package com.time.internmanagement.ui;

import com.time.internmanagement.exception.DataBaseException;
import com.time.internmanagement.exception.DuplicateIdException;
import com.time.internmanagement.exception.InternNotFoundException;
import com.time.internmanagement.exception.InvalidInputException;
import com.time.internmanagement.entity.Task;
import com.time.internmanagement.entity.TaskPriority;
import com.time.internmanagement.entity.TaskStatus;
import com.time.internmanagement.service.InternService;
import com.time.internmanagement.service.TaskService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

public class TaskPanel extends JPanel {

    private static final long MAX_PDF_BYTES = 5L * 1024 * 1024; // 5 MB

    private final TaskService taskService;
    private final InternService internService;
    private final MainFrame mainFrame;

    private DefaultTableModel tableModel;
    private JTable table;

    private JTextField idField, titleField, descriptionField, internIdField;
    private JTextField assignedDateField, deadlineField;
    private JComboBox<TaskPriority> priorityCombo;
    private JComboBox<TaskStatus> statusCombo;

    private JTextField filterInternField;
    private JComboBox<String> filterStatusCombo;
    private JComboBox<String> filterPriorityCombo;

    private JScrollPane formScroll;

    private JButton attachPdfButton;
    private JButton viewTaskPdfButton;
    private JButton viewSubmissionButton;

    // ---------- Palette ----------
    private static final Color BRAND_DARK    = new Color(0x1E293B);
    private static final Color BRAND_ACCENT  = new Color(0x3B82F6);
    private static final Color BG_LIGHT      = new Color(0xF8FAFC);
    private static final Color CARD_BG       = Color.WHITE;
    private static final Color BORDER_GRAY   = new Color(0xE5E7EB);
    private static final Color TEXT_DARK     = new Color(0x0F172A);
    private static final Color TEXT_MUTED    = new Color(0x6B7280);
    private static final Color FIELD_BORDER  = new Color(0xD1D5DB);

    private static final String[] COLUMNS = {
            "Task ID", "Title", "Description", "Assigned Intern",
            "Assigned Date", "Deadline", "Priority", "Status",
            "Task PDF", "Submission"
    };

    public TaskPanel(TaskService taskService, InternService internService, MainFrame mainFrame) {
        this.taskService = taskService;
        this.internService = internService;
        this.mainFrame = mainFrame;
        setBackground(BG_LIGHT);
        initUI();
        refreshTable();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(BG_LIGHT);
        add(buildTopBanner(), BorderLayout.NORTH);
        add(buildMainContent(), BorderLayout.CENTER);
    }

    // ---------- Banner ----------

    private JPanel buildTopBanner() {
        JPanel banner = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gradient = new GradientPaint(
                        0, 0, BRAND_DARK,
                        getWidth(), getHeight(), new Color(0x0F172A)
                );
                g2.setPaint(gradient);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        banner.setLayout(new BorderLayout());
        banner.setBorder(new EmptyBorder(16, 40, 16, 40));
        banner.setPreferredSize(new Dimension(0, 82));

        JPanel textBlock = new JPanel();
        textBlock.setOpaque(false);
        textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Tasks");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Create, assign, attach briefs, and track tasks for interns");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(new Color(0x94A3B8));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(new EmptyBorder(2, 0, 0, 0));

        textBlock.add(title);
        textBlock.add(subtitle);

        banner.add(textBlock, BorderLayout.WEST);
        return banner;
    }

    // ---------- Main content ----------

    private JPanel buildMainContent() {
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBackground(BG_LIGHT);
        content.setBorder(new EmptyBorder(16, 28, 16, 28));

        content.add(buildFilterCard(), BorderLayout.NORTH);
        content.add(buildTableCard(), BorderLayout.CENTER);

        formScroll = new JScrollPane(buildFormCard());
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        formScroll.getViewport().setBackground(BG_LIGHT);
        formScroll.setPreferredSize(new Dimension(0, 220));
        formScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);

        content.add(formScroll, BorderLayout.SOUTH);
        return content;
    }

    // ---------- Filter bar ----------

    private JPanel buildFilterCard() {
        JPanel card = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY, 1, true),
                new EmptyBorder(4, 10, 4, 10)
        ));

        filterInternField = new JTextField(12);
        styleField(filterInternField);

        filterStatusCombo = new JComboBox<>();
        filterStatusCombo.addItem("All Statuses");
        for (TaskStatus s : TaskStatus.values()) {
            filterStatusCombo.addItem(s.name());
        }
        styleCombo(filterStatusCombo);

        filterPriorityCombo = new JComboBox<>();
        filterPriorityCombo.addItem("All Priorities");
        for (TaskPriority p : TaskPriority.values()) {
            filterPriorityCombo.addItem(p.name());
        }
        styleCombo(filterPriorityCombo);

        JButton applyFilterButton = styledSecondaryButton("Apply Filters");
        applyFilterButton.addActionListener(e -> applyFilters());

        JButton clearButton = styledSecondaryButton("Clear");
        clearButton.addActionListener(e -> {
            filterInternField.setText("");
            filterStatusCombo.setSelectedIndex(0);
            filterPriorityCombo.setSelectedIndex(0);
            refreshTable();
        });

        JButton toggleFormButton = styledSecondaryButton("Add / Edit Task");
        toggleFormButton.addActionListener(e -> toggleForm());

        card.add(label("Intern ID:"));
        card.add(filterInternField);
        card.add(label("Status:"));
        card.add(filterStatusCombo);
        card.add(label("Priority:"));
        card.add(filterPriorityCombo);
        card.add(applyFilterButton);
        card.add(clearButton);
        card.add(toggleFormButton);

        return card;
    }

    private void toggleForm() {
        if (formScroll == null) return;
        formScroll.setVisible(!formScroll.isVisible());
        revalidate();
        repaint();
    }

    private void applyFilters() {
        List<Task> results = taskService.getAllTasks();

        String internId = filterInternField.getText().trim();
        if (!internId.isEmpty()) {
            results = taskService.filterByIntern(internId);
        }

        String statusChoice = (String) filterStatusCombo.getSelectedItem();
        if (statusChoice != null && !statusChoice.equals("All Statuses")) {
            TaskStatus status = TaskStatus.valueOf(statusChoice);
            results.removeIf(t -> t.getStatus() != status);
        }

        String priorityChoice = (String) filterPriorityCombo.getSelectedItem();
        if (priorityChoice != null && !priorityChoice.equals("All Priorities")) {
            TaskPriority priority = TaskPriority.valueOf(priorityChoice);
            results.removeIf(t -> t.getPriority() != priority);
        }

        populateTable(results);
    }

    // ---------- Table ----------

    private JPanel buildTableCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY, 1, true),
                new EmptyBorder(4, 4, 4, 4)
        ));

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        styleTable();
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedRowIntoForm();
                updateActionButtonsState();
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        card.add(scroll, BorderLayout.CENTER);
        card.add(buildActionBar(), BorderLayout.SOUTH);
        return card;
    }

    private void styleTable() {
        table.setRowHeight(32);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(BORDER_GRAY);
        table.setSelectionBackground(new Color(0xDBEAFE));
        table.setSelectionForeground(Color.BLACK);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(0xF1F5F9));
        table.getTableHeader().setForeground(TEXT_DARK);
        table.getTableHeader().setPreferredSize(new Dimension(0, 34));

        DefaultTableCellRenderer centered = new DefaultTableCellRenderer();
        centered.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            if (i != 1 && i != 2) {
                table.getColumnModel().getColumn(i).setCellRenderer(centered);
            }
        }
    }

    private JPanel buildActionBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        bar.setBackground(CARD_BG);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_GRAY));

        viewTaskPdfButton = styledSecondaryButton("View Task PDF");
        viewTaskPdfButton.setEnabled(false);
        viewTaskPdfButton.addActionListener(e -> handleViewTaskPdf());

        viewSubmissionButton = styledSecondaryButton("View Submission");
        viewSubmissionButton.setEnabled(false);
        viewSubmissionButton.addActionListener(e -> handleViewSubmission());

        bar.add(viewTaskPdfButton);
        bar.add(viewSubmissionButton);
        return bar;
    }

    private void updateActionButtonsState() {
        int row = table.getSelectedRow();
        if (row < 0) {
            if (viewTaskPdfButton != null) viewTaskPdfButton.setEnabled(false);
            if (viewSubmissionButton != null) viewSubmissionButton.setEnabled(false);
            if (attachPdfButton != null) attachPdfButton.setEnabled(false);
            return;
        }
        Object taskPdf = tableModel.getValueAt(row, 8);
        Object submission = tableModel.getValueAt(row, 9);

        if (viewTaskPdfButton != null) {
            viewTaskPdfButton.setEnabled(taskPdf != null && !"—".equals(taskPdf.toString()));
        }
        if (viewSubmissionButton != null) {
            viewSubmissionButton.setEnabled(submission != null && !"—".equals(submission.toString()));
        }
        if (attachPdfButton != null) {
            attachPdfButton.setEnabled(true);
        }
    }

    private void loadSelectedRowIntoForm() {
        int row = table.getSelectedRow();
        if (row < 0) return;

        if (formScroll != null && !formScroll.isVisible()) {
            formScroll.setVisible(true);
            revalidate();
            repaint();
        }

        idField.setText(tableModel.getValueAt(row, 0).toString());
        titleField.setText(tableModel.getValueAt(row, 1).toString());
        descriptionField.setText(tableModel.getValueAt(row, 2).toString());
        internIdField.setText(tableModel.getValueAt(row, 3).toString());
        assignedDateField.setText(tableModel.getValueAt(row, 4).toString());
        deadlineField.setText(tableModel.getValueAt(row, 5).toString());
        priorityCombo.setSelectedItem(TaskPriority.valueOf(tableModel.getValueAt(row, 6).toString()));
        statusCombo.setSelectedItem(TaskStatus.valueOf(tableModel.getValueAt(row, 7).toString()));

        idField.setEditable(false);
    }

    // ---------- Form ----------

    private JPanel buildFormCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY, 1, true),
                new EmptyBorder(12, 20, 12, 20)
        ));

        JLabel heading = new JLabel("Task Details");
        heading.setFont(new Font("SansSerif", Font.BOLD, 14));
        heading.setForeground(TEXT_DARK);
        heading.setBorder(new EmptyBorder(0, 0, 10, 0));

        idField = new JTextField();
        titleField = new JTextField();
        descriptionField = new JTextField();
        internIdField = new JTextField();
        assignedDateField = new JTextField("YYYY-MM-DD");
        deadlineField = new JTextField("YYYY-MM-DD");
        priorityCombo = new JComboBox<>(TaskPriority.values());
        statusCombo = new JComboBox<>(TaskStatus.values());

        styleField(idField);
        styleField(titleField);
        styleField(descriptionField);
        styleField(internIdField);
        styleField(assignedDateField);
        styleField(deadlineField);
        styleCombo(priorityCombo);
        styleCombo(statusCombo);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);

        addFieldPair(formPanel, 0, 0, "Task ID:", idField, 1, false);
        addFieldPair(formPanel, 0, 2, "Title:", titleField, 3, true);

        addFieldPair(formPanel, 1, 0, "Intern ID:", internIdField, 1, false);
        addFieldPair(formPanel, 1, 2, "Assigned Date:", assignedDateField, 1, false);
        addFieldPair(formPanel, 1, 4, "Deadline:", deadlineField, 1, false);

        addFieldPair(formPanel, 2, 0, "Description:", descriptionField, 3, true);
        addFieldPair(formPanel, 2, 4, "Priority:", priorityCombo, 1, false);

        addFieldPair(formPanel, 3, 4, "Status:", statusCombo, 1, false);

        card.add(heading, BorderLayout.NORTH);
        card.add(formPanel, BorderLayout.CENTER);
        card.add(buildButtonPanel(), BorderLayout.SOUTH);
        return card;
    }

    private void addFieldPair(JPanel panel, int row, int labelCol,
                              String text, JComponent field,
                              int span, boolean wide) {
        GridBagConstraints labelGc = new GridBagConstraints();
        labelGc.gridx = labelCol;
        labelGc.gridy = row;
        labelGc.anchor = GridBagConstraints.LINE_END;
        labelGc.insets = new Insets(4, 6, 4, 6);
        labelGc.weightx = 0;
        panel.add(formLabel(text), labelGc);

        GridBagConstraints fieldGc = new GridBagConstraints();
        fieldGc.gridx = labelCol + 1;
        fieldGc.gridy = row;
        fieldGc.gridwidth = span;
        fieldGc.fill = GridBagConstraints.HORIZONTAL;
        fieldGc.weightx = wide ? 1 : 0.3;
        fieldGc.insets = new Insets(4, 0, 4, 20);
        panel.add(field, fieldGc);
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        panel.setOpaque(false);

        JButton addButton = styledPrimaryButton("Create Task");
        addButton.addActionListener(e -> handleAdd());

        JButton editButton = styledSecondaryButton("Update");
        editButton.addActionListener(e -> handleEdit());

        JButton deleteButton = styledSecondaryButton("Delete");
        deleteButton.addActionListener(e -> handleDelete());

        attachPdfButton = styledSecondaryButton("Attach Task PDF");
        attachPdfButton.setEnabled(false);
        attachPdfButton.addActionListener(e -> handleAttachPdf());

        JButton clearFormButton = styledSecondaryButton("Clear Form");
        clearFormButton.addActionListener(e -> clearForm());

        panel.add(clearFormButton);
        panel.add(deleteButton);
        panel.add(editButton);
        panel.add(attachPdfButton);
        panel.add(addButton);
        return panel;
    }

    // ---------- Handlers ----------

    private void handleAdd() {
        try {
            Task task = buildTaskFromForm();
            taskService.createTask(task);
            mainFrame.refreshAll();
            clearForm();
            JOptionPane.showMessageDialog(this,
                    "Task created successfully.\n\n"
                            + "To attach a brief PDF, select the task in the table and click 'Attach Task PDF'.");
        } catch (DuplicateIdException | InvalidInputException | InternNotFoundException
                 | DataBaseException | DateTimeParseException ex) {
            showError(ex.getMessage());
        }
    }

    private void handleEdit() {
        try {
            String id = idField.getText().trim();
            if (id.isEmpty()) {
                showError("Select a task from the table first.");
                return;
            }
            Task updated = buildTaskFromForm();
            taskService.editTask(id, updated);
            mainFrame.refreshAll();
            clearForm();
            JOptionPane.showMessageDialog(this, "Task updated successfully.");
        } catch (InternNotFoundException | InvalidInputException
                 | DataBaseException | DateTimeParseException ex) {
            showError(ex.getMessage());
        }
    }

    private void handleDelete() {
        String id = idField.getText().trim();
        if (id.isEmpty()) {
            showError("Select a task from the table first.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete task " + id + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            taskService.deleteTask(id);
            mainFrame.refreshAll();
            clearForm();
        } catch (InternNotFoundException | DataBaseException ex) {
            showError(ex.getMessage());
        }
    }

    // ---------- Task PDF ----------

    private void handleAttachPdf() {
        int row = table.getSelectedRow();
        if (row < 0) {
            showError("Select a task from the table first.");
            return;
        }
        String taskId = tableModel.getValueAt(row, 0).toString();

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select task PDF");
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileNameExtensionFilter("PDF files", "pdf"));

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();

        if (!file.getName().toLowerCase().endsWith(".pdf")) {
            showError("Only PDF files are accepted.");
            return;
        }
        if (file.length() > MAX_PDF_BYTES) {
            showError("PDF exceeds 5 MB limit.");
            return;
        }

        byte[] bytes;
        try {
            bytes = Files.readAllBytes(file.toPath());
        } catch (IOException ex) {
            showError("Could not read file: " + ex.getMessage());
            return;
        }

        if (!isPdf(bytes)) {
            showError("That file isn't a valid PDF.");
            return;
        }

        try {
            taskService.attachAssignmentPdf(taskId, bytes, file.getName());
            refreshTable();
            JOptionPane.showMessageDialog(this,
                    "Task PDF attached to " + taskId + ".",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void handleViewTaskPdf() {
        int row = table.getSelectedRow();
        if (row < 0) {
            showError("Select a task first.");
            return;
        }
        String taskId = tableModel.getValueAt(row, 0).toString();

        Task full;
        try {
            full = taskService.loadTaskWithFile(taskId);
        } catch (Exception ex) {
            showError(ex.getMessage());
            return;
        }

        if (full.getAssignmentFile() == null || full.getAssignmentFile().length == 0) {
            JOptionPane.showMessageDialog(this,
                    "This task has no attached PDF.",
                    "No file", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        saveAndOpenPdf(full.getAssignmentFile(),
                full.getAssignmentFileName() != null
                        ? full.getAssignmentFileName()
                        : taskId + "-task.pdf",
                null);
    }

    // ---------- Submission ----------

    private void handleViewSubmission() {
        int row = table.getSelectedRow();
        if (row < 0) {
            showError("Select a task first.");
            return;
        }
        String taskId = tableModel.getValueAt(row, 0).toString();

        Task full;
        try {
            full = taskService.loadTaskWithFile(taskId);
        } catch (Exception ex) {
            showError(ex.getMessage());
            return;
        }

        boolean hasFile = full.getSubmissionFile() != null && full.getSubmissionFile().length > 0;
        boolean hasText = full.getSubmissionText() != null && !full.getSubmissionText().isBlank();

        if (!hasFile && !hasText) {
            JOptionPane.showMessageDialog(this,
                    "This task has no submission yet.",
                    "No submission", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (hasFile) {
            saveAndOpenPdf(full.getSubmissionFile(),
                    full.getSubmissionFileName() != null
                            ? full.getSubmissionFileName()
                            : taskId + ".pdf",
                    buildSubmissionDetails(full));
        } else {
            JOptionPane.showMessageDialog(this,
                    buildSubmissionDetails(full),
                    "Submission", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private String buildSubmissionDetails(Task full) {
        StringBuilder info = new StringBuilder();
        if (full.getSubmissionText() != null && !full.getSubmissionText().isBlank()) {
            info.append("Comment:\n").append(full.getSubmissionText());
        }
        if (full.getSubmittedDate() != null) {
            if (info.length() > 0) info.append("\n\n");
            info.append("Submitted on: ").append(full.getSubmittedDate());
        }
        return info.toString();
    }

    // ---------- Helpers ----------

    private Task buildTaskFromForm() throws DateTimeParseException {
        String id = idField.getText().trim();
        String title = titleField.getText().trim();
        String description = descriptionField.getText().trim();
        String internId = internIdField.getText().trim();
        LocalDate assignedDate = LocalDate.parse(assignedDateField.getText().trim());
        LocalDate deadline = LocalDate.parse(deadlineField.getText().trim());
        TaskPriority priority = (TaskPriority) priorityCombo.getSelectedItem();
        TaskStatus status = (TaskStatus) statusCombo.getSelectedItem();

        return new Task(id, title, description, internId, assignedDate, deadline,
                priority, status, null, null);
    }

    private void clearForm() {
        idField.setText("");
        idField.setEditable(true);
        titleField.setText("");
        descriptionField.setText("");
        internIdField.setText("");
        assignedDateField.setText("YYYY-MM-DD");
        deadlineField.setText("YYYY-MM-DD");
        priorityCombo.setSelectedIndex(0);
        statusCombo.setSelectedIndex(0);
        table.clearSelection();
        if (viewTaskPdfButton != null) viewTaskPdfButton.setEnabled(false);
        if (viewSubmissionButton != null) viewSubmissionButton.setEnabled(false);
        if (attachPdfButton != null) attachPdfButton.setEnabled(false);
    }

    private void saveAndOpenPdf(byte[] bytes, String suggestedName, String extraInfo) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save PDF as…");
        chooser.setSelectedFile(new File(suggestedName));
        chooser.setFileFilter(new FileNameExtensionFilter("PDF files", "pdf"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File target = chooser.getSelectedFile();
        if (!target.getName().toLowerCase().endsWith(".pdf")) {
            target = new File(target.getAbsolutePath() + ".pdf");
        }

        try {
            Files.write(target.toPath(), bytes);
        } catch (IOException ex) {
            showError("Could not save file: " + ex.getMessage());
            return;
        }

        if (extraInfo != null && !extraInfo.isBlank()) {
            JOptionPane.showMessageDialog(this,
                    "Saved to: " + target.getAbsolutePath() + "\n\n" + extraInfo,
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
        }

        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(target);
            }
        } catch (IOException ignored) { }
    }

    private static boolean isPdf(byte[] bytes) {
        return bytes.length >= 5
                && bytes[0] == '%'
                && bytes[1] == 'P'
                && bytes[2] == 'D'
                && bytes[3] == 'F'
                && bytes[4] == '-';
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void refreshTable() {
        populateTable(taskService.getAllTasks());
    }

    private void populateTable(List<Task> tasks) {
        tableModel.setRowCount(0);
        for (Task t : tasks) {
            String taskPdf = t.getAssignmentFileName() != null ? t.getAssignmentFileName() : "—";

            String submission = t.getSubmissionFileName() != null
                    ? t.getSubmissionFileName()
                    : (t.getSubmissionText() != null ? "(text)" : "—");

            tableModel.addRow(new Object[]{
                    t.getTaskId(), t.getTitle(), t.getDescription(), t.getAssignedInternId(),
                    t.getAssignedDate(), t.getDeadline(), t.getPriority(), t.getStatus(),
                    taskPdf, submission
            });
        }
        if (viewTaskPdfButton != null) viewTaskPdfButton.setEnabled(false);
        if (viewSubmissionButton != null) viewSubmissionButton.setEnabled(false);
        if (attachPdfButton != null) attachPdfButton.setEnabled(false);
    }

    // ---------- Styling helpers ----------

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.PLAIN, 13));
        l.setForeground(TEXT_MUTED);
        return l;
    }

    private JLabel formLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.PLAIN, 13));
        l.setForeground(TEXT_DARK);
        return l;
    }

    private void styleField(JTextField field) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FIELD_BORDER, 1, true),
                new EmptyBorder(4, 10, 4, 10)
        ));
    }

    private void styleCombo(JComboBox<?> combo) {
        combo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        combo.setBackground(Color.WHITE);
        combo.setBorder(BorderFactory.createLineBorder(FIELD_BORDER, 1, true));
    }

    private JButton styledPrimaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setBackground(BRAND_ACCENT);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setPreferredSize(new Dimension(120, 34));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton styledSecondaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.PLAIN, 13));
        b.setForeground(TEXT_DARK);
        b.setBackground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FIELD_BORDER, 1, true),
                new EmptyBorder(4, 14, 4, 14)
        ));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}