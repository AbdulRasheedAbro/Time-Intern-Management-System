package com.time.internmanagement.ui;

import com.time.internmanagement.entity.Intern;
import com.time.internmanagement.entity.Supervisor;
import com.time.internmanagement.entity.Task;
import com.time.internmanagement.entity.TaskPriority;
import com.time.internmanagement.entity.TaskStatus;
import com.time.internmanagement.exception.DataBaseException;
import com.time.internmanagement.exception.DuplicateIdException;
import com.time.internmanagement.exception.InternNotFoundException;
import com.time.internmanagement.exception.InvalidInputException;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SupervisorTaskPanel extends JPanel {

    private static final long MAX_PDF_BYTES = 5L * 1024 * 1024;

    private final Supervisor supervisor;
    private final InternService internService;
    private final TaskService taskService;

    private DefaultTableModel tableModel;
    private JTable table;

    private JTextField idField, titleField, descriptionField;
    private JTextField assignedDateField, deadlineField;
    private JComboBox<Intern> internCombo;
    private JComboBox<TaskPriority> priorityCombo;
    private JComboBox<TaskStatus> statusCombo;

    private JButton viewSubmissionButton;
    private JButton attachPdfButton;
    private JButton viewTaskPdfButton;

    private static final Color BRAND_ACCENT = new Color(0x3B82F6);
    private static final Color BG_LIGHT     = new Color(0xF8FAFC);
    private static final Color CARD_BG      = Color.WHITE;
    private static final Color BORDER_GRAY  = new Color(0xE5E7EB);
    private static final Color TEXT_DARK    = new Color(0x0F172A);
    private static final Color TEXT_MUTED   = new Color(0x6B7280);
    private static final Color FIELD_BORDER = new Color(0xD1D5DB);

    private static final String[] COLUMNS = {
            "Task ID", "Title", "Assigned Intern", "Assigned Date",
            "Deadline", "Priority", "Status", "Task PDF", "Submission"
    };

    public SupervisorTaskPanel(Supervisor supervisor,
                               InternService internService,
                               TaskService taskService) {
        this.supervisor = supervisor;
        this.internService = internService;
        this.taskService = taskService;
        setBackground(BG_LIGHT);
        initUI();
        refreshAll();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 12));
        setBackground(BG_LIGHT);
        setBorder(new EmptyBorder(16, 28, 16, 28));

        JPanel headRow = new JPanel(new BorderLayout());
        headRow.setOpaque(false);

        JPanel textBlock = new JPanel();
        textBlock.setOpaque(false);
        textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Tasks for My Interns");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(TEXT_DARK);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel(
                "Create tasks for the interns you supervise, attach briefs, and view their submissions.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(new EmptyBorder(4, 0, 0, 0));

        textBlock.add(title);
        textBlock.add(subtitle);

        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightButtons.setOpaque(false);

        viewSubmissionButton = styledSecondaryButton("View Submission");
        viewSubmissionButton.setEnabled(false);
        viewSubmissionButton.addActionListener(e -> handleViewSubmission());

        viewTaskPdfButton = styledSecondaryButton("View Task PDF");
        viewTaskPdfButton.setEnabled(false);
        viewTaskPdfButton.addActionListener(e -> handleViewTaskPdf());

        JButton refresh = styledSecondaryButton("Refresh");
        refresh.addActionListener(e -> refreshAll());

        rightButtons.add(viewTaskPdfButton);
        rightButtons.add(viewSubmissionButton);
        rightButtons.add(refresh);

        headRow.add(textBlock, BorderLayout.WEST);
        headRow.add(rightButtons, BorderLayout.EAST);
        add(headRow, BorderLayout.NORTH);

        JPanel centerWrapper = new JPanel(new BorderLayout(0, 12));
        centerWrapper.setOpaque(false);

        centerWrapper.add(buildTableCard(), BorderLayout.CENTER);
        centerWrapper.add(buildFormCard(), BorderLayout.SOUTH);

        add(centerWrapper, BorderLayout.CENTER);
    }

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

    private JPanel buildFormCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY, 1, true),
                new EmptyBorder(12, 20, 12, 20)
        ));

        JLabel heading = new JLabel("Create Task");
        heading.setFont(new Font("SansSerif", Font.BOLD, 14));
        heading.setForeground(TEXT_DARK);
        heading.setBorder(new EmptyBorder(0, 0, 10, 0));

        idField = new JTextField();
        titleField = new JTextField();
        descriptionField = new JTextField();
        internCombo = new JComboBox<>();
        assignedDateField = new JTextField(LocalDate.now().toString());
        deadlineField = new JTextField(LocalDate.now().plusWeeks(2).toString());
        priorityCombo = new JComboBox<>(TaskPriority.values());
        statusCombo = new JComboBox<>(TaskStatus.values());

        styleField(idField);
        styleField(titleField);
        styleField(descriptionField);
        styleField(assignedDateField);
        styleField(deadlineField);
        styleCombo(internCombo);
        styleCombo(priorityCombo);
        styleCombo(statusCombo);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);

        addFieldPair(formPanel, 0, 0, "Task ID:", idField, 1, false);
        addFieldPair(formPanel, 0, 2, "Title:", titleField, 3, true);

        addFieldPair(formPanel, 1, 0, "Assigned Intern:", internCombo, 1, false);
        addFieldPair(formPanel, 1, 2, "Assigned Date:", assignedDateField, 1, false);
        addFieldPair(formPanel, 1, 4, "Deadline:", deadlineField, 1, false);

        addFieldPair(formPanel, 2, 0, "Description:", descriptionField, 3, true);
        addFieldPair(formPanel, 2, 4, "Priority:", priorityCombo, 1, false);

        addFieldPair(formPanel, 3, 4, "Status:", statusCombo, 1, false);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttons.setOpaque(false);

        attachPdfButton = styledSecondaryButton("Attach Task PDF");
        attachPdfButton.addActionListener(e -> handleAttachPdf());

        JButton create = styledPrimaryButton("Create Task");
        create.addActionListener(e -> handleAdd());

        JButton clear = styledSecondaryButton("Clear Form");
        clear.addActionListener(e -> clearForm());

        buttons.add(clear);
        buttons.add(attachPdfButton);
        buttons.add(create);

        card.add(heading, BorderLayout.NORTH);
        card.add(formPanel, BorderLayout.CENTER);
        card.add(buttons, BorderLayout.SOUTH);
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

    private void loadSelectedRowIntoForm() {
        int row = table.getSelectedRow();
        if (row < 0) return;

        idField.setText(tableModel.getValueAt(row, 0).toString());
        titleField.setText(tableModel.getValueAt(row, 1).toString());

        String internLabel = tableModel.getValueAt(row, 2).toString();
        String internId = internLabel.split(" ")[0];
        for (int i = 0; i < internCombo.getItemCount(); i++) {
            Intern candidate = internCombo.getItemAt(i);
            if (candidate.getInternId().equals(internId)) {
                internCombo.setSelectedIndex(i);
                break;
            }
        }

        assignedDateField.setText(tableModel.getValueAt(row, 3).toString());
        deadlineField.setText(tableModel.getValueAt(row, 4).toString());
        priorityCombo.setSelectedItem(TaskPriority.valueOf(tableModel.getValueAt(row, 5).toString()));
        statusCombo.setSelectedItem(TaskStatus.valueOf(tableModel.getValueAt(row, 6).toString()));

        idField.setEditable(false);
    }

    private void updateActionButtonsState() {
        int row = table.getSelectedRow();
        if (row < 0) {
            if (viewSubmissionButton != null) viewSubmissionButton.setEnabled(false);
            if (viewTaskPdfButton != null) viewTaskPdfButton.setEnabled(false);
            if (attachPdfButton != null) attachPdfButton.setEnabled(false);
            return;
        }
        Object taskPdf = tableModel.getValueAt(row, 7);
        Object submission = tableModel.getValueAt(row, 8);

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

    public void refreshAll() {
        refreshInternCombo();
        refreshTable();
    }

    private void refreshInternCombo() {
        internCombo.removeAllItems();
        List<Intern> myInterns = internService.getInternsForSupervisor(
                supervisor.getSupervisorId());
        for (Intern i : myInterns) {
            internCombo.addItem(i);
        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0);

        Set<String> myInternIds = new HashSet<>();
        List<Intern> myInterns = internService.getInternsForSupervisor(
                supervisor.getSupervisorId());
        for (Intern i : myInterns) {
            myInternIds.add(i.getInternId());
        }

        List<Task> tasks = taskService.getTasksForInterns(myInternIds);
        for (Task t : tasks) {
            String taskPdf = t.getAssignmentFileName() != null ? t.getAssignmentFileName() : "—";

            String submission = t.getSubmissionFileName() != null
                    ? t.getSubmissionFileName()
                    : (t.getSubmissionText() != null ? "(text)" : "—");

            String internLabel = t.getAssignedInternId();
            for (Intern i : myInterns) {
                if (i.getInternId().equals(t.getAssignedInternId())) {
                    internLabel = i.getInternId() + " — " + i.getFullName();
                    break;
                }
            }

            tableModel.addRow(new Object[]{
                    t.getTaskId(), t.getTitle(), internLabel,
                    t.getAssignedDate(), t.getDeadline(),
                    t.getPriority(), t.getStatus(),
                    taskPdf, submission
            });
        }

        if (viewSubmissionButton != null) viewSubmissionButton.setEnabled(false);
        if (viewTaskPdfButton != null) viewTaskPdfButton.setEnabled(false);
    }

    private void handleAdd() {
        Intern selectedIntern = (Intern) internCombo.getSelectedItem();
        if (selectedIntern == null) {
            showError("You have no interns assigned to you yet.");
            return;
        }

        try {
            String id = idField.getText().trim();
            String title = titleField.getText().trim();
            String description = descriptionField.getText().trim();
            LocalDate assignedDate = LocalDate.parse(assignedDateField.getText().trim());
            LocalDate deadline = LocalDate.parse(deadlineField.getText().trim());
            TaskPriority priority = (TaskPriority) priorityCombo.getSelectedItem();
            TaskStatus status = (TaskStatus) statusCombo.getSelectedItem();

            Task task = new Task(id, title, description,
                    selectedIntern.getInternId(),
                    assignedDate, deadline, priority, status, null, null);

            taskService.createTaskAsSupervisor(task, supervisor);
            refreshTable();
            clearForm();
            JOptionPane.showMessageDialog(this, "Task created successfully.\n\n"
                    + "To attach a brief PDF, select the task in the table and click 'Attach Task PDF'.");
        } catch (DuplicateIdException | InvalidInputException | InternNotFoundException
                 | DataBaseException | DateTimeParseException ex) {
            showError(ex.getMessage());
        }
    }

    // ---------- Task PDF attach / view ----------

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

    private void clearForm() {
        idField.setText("");
        idField.setEditable(true);
        titleField.setText("");
        descriptionField.setText("");
        assignedDateField.setText(LocalDate.now().toString());
        deadlineField.setText(LocalDate.now().plusWeeks(2).toString());
        priorityCombo.setSelectedIndex(0);
        statusCombo.setSelectedIndex(0);
        if (internCombo.getItemCount() > 0) internCombo.setSelectedIndex(0);
        table.clearSelection();
        if (viewSubmissionButton != null) viewSubmissionButton.setEnabled(false);
        if (viewTaskPdfButton != null) viewTaskPdfButton.setEnabled(false);
    }

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

    // ---------- Styling helpers ----------

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
        b.setPreferredSize(new Dimension(130, 34));
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