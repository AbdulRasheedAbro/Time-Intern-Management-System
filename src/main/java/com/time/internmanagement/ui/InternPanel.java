package com.time.internmanagement.ui;

import com.time.internmanagement.exception.DataBaseException;
import com.time.internmanagement.exception.DuplicateIdException;
import com.time.internmanagement.exception.InternNotFoundException;
import com.time.internmanagement.exception.InvalidInputException;
import com.time.internmanagement.entity.Intern;
import com.time.internmanagement.entity.InternStatus;
import com.time.internmanagement.entity.Supervisor;
import com.time.internmanagement.service.InternService;
import com.time.internmanagement.service.SupervisorService;
import com.time.internmanagement.util.PasswordUtils;
import com.time.internmanagement.util.ValidationUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

public class InternPanel extends JPanel {

    private final InternService internService;
    private final SupervisorService supervisorService;
    private final MainFrame mainFrame;

    private DefaultTableModel tableModel;
    private JTable table;

    private JTextField idField, nameField, emailField, phoneField, deptField;
    private JTextField startDateField, endDateField;
    private JPasswordField passwordField;
    private JComboBox<InternStatus> statusCombo;
    private JComboBox<Supervisor> supervisorCombo;
    private JTextField supervisorIdField;   // NEW

    private JTextField searchField;
    private JComboBox<String> filterDeptCombo;
    private JComboBox<String> filterStatusCombo;

    private JScrollPane formScroll;

    private static final Color BRAND_DARK    = new Color(0x1E293B);
    private static final Color BRAND_ACCENT  = new Color(0x3B82F6);
    private static final Color BG_LIGHT      = new Color(0xF8FAFC);
    private static final Color CARD_BG       = Color.WHITE;
    private static final Color BORDER_GRAY   = new Color(0xE5E7EB);
    private static final Color TEXT_DARK     = new Color(0x0F172A);
    private static final Color TEXT_MUTED    = new Color(0x6B7280);
    private static final Color FIELD_BORDER  = new Color(0xD1D5DB);

    private static final String[] COLUMNS = {
            "ID", "Name", "Email", "Phone", "Department",
            "Start Date", "End Date", "Supervisor", "Status"
    };

    public InternPanel(InternService internService,
                       SupervisorService supervisorService,
                       MainFrame mainFrame) {
        this.internService = internService;
        this.supervisorService = supervisorService;
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

        JLabel title = new JLabel("Interns");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Manage intern profiles, assignments, and statuses");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(new Color(0x94A3B8));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(new EmptyBorder(2, 0, 0, 0));

        textBlock.add(title);
        textBlock.add(subtitle);

        banner.add(textBlock, BorderLayout.WEST);
        return banner;
    }

    private JPanel buildMainContent() {
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBackground(BG_LIGHT);
        content.setBorder(new EmptyBorder(16, 28, 16, 28));

        content.add(buildFilterCard(), BorderLayout.NORTH);
        content.add(buildTableCard(), BorderLayout.CENTER);

        formScroll = new JScrollPane(buildFormCard());
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        formScroll.getViewport().setBackground(BG_LIGHT);
        formScroll.setPreferredSize(new Dimension(0, 210));
        formScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);

        content.add(formScroll, BorderLayout.SOUTH);
        return content;
    }

    private JPanel buildFilterCard() {
        JPanel card = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY, 1, true),
                new EmptyBorder(4, 10, 4, 10)
        ));

        searchField = new JTextField(14);
        styleField(searchField);

        JButton searchButton = styledSecondaryButton("Search by Name");
        searchButton.addActionListener(e -> searchByName());

        filterDeptCombo = new JComboBox<>();
        filterDeptCombo.addItem("All Departments");
        styleCombo(filterDeptCombo);

        filterStatusCombo = new JComboBox<>();
        filterStatusCombo.addItem("All Statuses");
        for (InternStatus s : InternStatus.values()) {
            filterStatusCombo.addItem(s.name());
        }
        styleCombo(filterStatusCombo);

        JButton applyFilterButton = styledSecondaryButton("Apply Filters");
        applyFilterButton.addActionListener(e -> applyFilters());

        JButton clearButton = styledSecondaryButton("Clear");
        clearButton.addActionListener(e -> {
            searchField.setText("");
            filterDeptCombo.setSelectedIndex(0);
            filterStatusCombo.setSelectedIndex(0);
            refreshTable();
        });

        JButton toggleFormButton = styledSecondaryButton("Add / Edit Intern");
        toggleFormButton.addActionListener(e -> toggleForm());

        card.add(label("Search:"));
        card.add(searchField);
        card.add(searchButton);
        card.add(label("Department:"));
        card.add(filterDeptCombo);
        card.add(label("Status:"));
        card.add(filterStatusCombo);
        card.add(applyFilterButton);
        card.add(clearButton);
        card.add(toggleFormButton);

        return card;
    }

    private void toggleForm() {
        if (formScroll == null) return;
        formScroll.setVisible(!formScroll.isVisible());
        if (formScroll.isVisible()) {
            refreshSupervisorCombo();
        }
        revalidate();
        repaint();
    }

    private void searchByName() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            refreshTable();
            return;
        }
        List<Intern> results = internService.searchInternsByName(query);
        populateTable(results);
    }

    private void applyFilters() {
        List<Intern> results = internService.getAllInterns();

        String deptChoice = (String) filterDeptCombo.getSelectedItem();
        if (deptChoice != null && !deptChoice.equals("All Departments")) {
            results = internService.filterByDepartment(deptChoice);
        }

        String statusChoice = (String) filterStatusCombo.getSelectedItem();
        if (statusChoice != null && !statusChoice.equals("All Statuses")) {
            InternStatus status = InternStatus.valueOf(statusChoice);
            results.removeIf(i -> i.getStatus() != status);
        }

        populateTable(results);
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
        table.getSelectionModel().addListSelectionListener(e -> loadSelectedRowIntoForm());

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

    private void loadSelectedRowIntoForm() {
        int row = table.getSelectedRow();
        if (row < 0) return;

        if (formScroll != null && !formScroll.isVisible()) {
            formScroll.setVisible(true);
            revalidate();
            repaint();
        }

        String id = tableModel.getValueAt(row, 0).toString();
        idField.setText(id);
        nameField.setText(safeString(tableModel.getValueAt(row, 1)));
        emailField.setText(safeString(tableModel.getValueAt(row, 2)));
        phoneField.setText(safeString(tableModel.getValueAt(row, 3)));
        deptField.setText(safeString(tableModel.getValueAt(row, 4)));
        startDateField.setText(safeString(tableModel.getValueAt(row, 5)));
        endDateField.setText(safeString(tableModel.getValueAt(row, 6)));
        statusCombo.setSelectedItem(InternStatus.valueOf(tableModel.getValueAt(row, 8).toString()));

        passwordField.setText("");
        idField.setEditable(false);

        // Select the matching supervisor in the combo by looking up the intern's supervisorId
        Intern intern = internService.getAllInterns().stream()
                .filter(i -> i.getInternId().equals(id))
                .findFirst()
                .orElse(null);

        if (intern != null && intern.getSupervisorId() != null) {
            supervisorIdField.setText(intern.getSupervisorId());
            for (int i = 0; i < supervisorCombo.getItemCount(); i++) {
                Supervisor s = supervisorCombo.getItemAt(i);
                if (s != null && s.getSupervisorId().equals(intern.getSupervisorId())) {
                    supervisorCombo.setSelectedIndex(i);
                    return;
                }
            }
        } else {
            supervisorIdField.setText("");
        }
        if (supervisorCombo.getItemCount() > 0) {
            supervisorCombo.setSelectedIndex(0);
        }
    }

    private static String safeString(Object v) {
        if (v == null) return "";
        return v.toString();
    }

    // ---------- Form ----------

    private JPanel buildFormCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY, 1, true),
                new EmptyBorder(12, 20, 12, 20)
        ));

        JLabel heading = new JLabel("Intern Details");
        heading.setFont(new Font("SansSerif", Font.BOLD, 14));
        heading.setForeground(TEXT_DARK);
        heading.setBorder(new EmptyBorder(0, 0, 10, 0));

        idField = new JTextField();
        nameField = new JTextField();
        emailField = new JTextField();
        phoneField = new JTextField();
        deptField = new JTextField();
        startDateField = new JTextField("YYYY-MM-DD");
        endDateField = new JTextField("YYYY-MM-DD");
        passwordField = new JPasswordField();
        statusCombo = new JComboBox<>(InternStatus.values());
        supervisorCombo = new JComboBox<>();
        supervisorCombo.setRenderer(new SupervisorRenderer());
        supervisorIdField = new JTextField();

        styleField(idField);
        styleField(nameField);
        styleField(emailField);
        styleField(phoneField);
        styleField(deptField);
        styleField(startDateField);
        styleField(endDateField);
        styleField(passwordField);
        styleField(supervisorIdField);
        styleCombo(statusCombo);
        styleCombo(supervisorCombo);

        // Dropdown selection auto-fills the supervisor ID text field
        supervisorCombo.addActionListener(e -> {
            Supervisor selected = (Supervisor) supervisorCombo.getSelectedItem();
            if (selected != null) {
                supervisorIdField.setText(selected.getSupervisorId());
            } else {
                supervisorIdField.setText("");
            }
        });

        // Typing a supervisor ID auto-selects the matching supervisor in the dropdown
        supervisorIdField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                String typedId = supervisorIdField.getText().trim();
                if (typedId.isEmpty()) return;
                for (int i = 0; i < supervisorCombo.getItemCount(); i++) {
                    Supervisor s = supervisorCombo.getItemAt(i);
                    if (s != null && s.getSupervisorId().equalsIgnoreCase(typedId)) {
                        supervisorCombo.setSelectedIndex(i);
                        return;
                    }
                }
            }
        });

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);

        addFieldPair(formPanel, 0, 0, "Intern ID:", idField);
        addFieldPair(formPanel, 0, 2, "Full Name:", nameField);
        addFieldPair(formPanel, 0, 4, "Email:", emailField);

        addFieldPair(formPanel, 1, 0, "Phone:", phoneField);
        addFieldPair(formPanel, 1, 2, "Department:", deptField);
        addFieldPair(formPanel, 1, 4, "Supervisor:", supervisorCombo);

        addFieldPair(formPanel, 2, 0, "Start Date:", startDateField);
        addFieldPair(formPanel, 2, 2, "End Date:", endDateField);
        addFieldPair(formPanel, 2, 4, "Status:", statusCombo);

        addFieldPair(formPanel, 3, 0, "Supervisor ID:", supervisorIdField);

        JPanel passwordRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        passwordRow.setOpaque(false);
        passwordRow.add(formLabel("Login Password:"));
        passwordField.setPreferredSize(new Dimension(200, 28));
        passwordRow.add(passwordField);

        JLabel hint = new JLabel("(blank = keep existing on edit)");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hint.setForeground(TEXT_MUTED);
        passwordRow.add(hint);

        JPanel formWrap = new JPanel(new BorderLayout());
        formWrap.setOpaque(false);
        formWrap.add(formPanel, BorderLayout.NORTH);
        formWrap.add(passwordRow, BorderLayout.CENTER);

        card.add(heading, BorderLayout.NORTH);
        card.add(formWrap, BorderLayout.CENTER);
        card.add(buildButtonPanel(), BorderLayout.SOUTH);
        return card;
    }

    private void addFieldPair(JPanel panel, int row, int labelCol,
                              String text, JComponent field) {
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
        fieldGc.fill = GridBagConstraints.HORIZONTAL;
        fieldGc.weightx = 1;
        fieldGc.insets = new Insets(4, 0, 4, 20);
        panel.add(field, fieldGc);
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        panel.setOpaque(false);

        JButton addButton = styledPrimaryButton("Add Intern");
        addButton.addActionListener(e -> handleAdd());

        JButton editButton = styledSecondaryButton("Update");
        editButton.addActionListener(e -> handleEdit());

        JButton deleteButton = styledSecondaryButton("Delete");
        deleteButton.addActionListener(e -> handleDelete());

        JButton clearFormButton = styledSecondaryButton("Clear Form");
        clearFormButton.addActionListener(e -> clearForm());

        panel.add(clearFormButton);
        panel.add(deleteButton);
        panel.add(editButton);
        panel.add(addButton);
        return panel;
    }

    // ---------- Handlers ----------

    private void handleAdd() {
        try {
            String rawPassword = new String(passwordField.getPassword()).trim();
            if (ValidationUtils.isNullOrEmpty(rawPassword)) {
                showError("Please set an initial login password for this intern.");
                return;
            }
            Intern intern = buildInternFromForm(PasswordUtils.hash(rawPassword));
            internService.addIntern(intern);
            mainFrame.refreshAll();
            clearForm();
            JOptionPane.showMessageDialog(this, "Intern added successfully.");
        } catch (DuplicateIdException | InvalidInputException | DataBaseException
                 | DateTimeParseException ex) {
            showError(ex.getMessage());
        }
    }

    private void handleEdit() {
        try {
            String id = idField.getText().trim();
            if (id.isEmpty()) {
                showError("Select an intern from the table first.");
                return;
            }

            String rawPassword = new String(passwordField.getPassword()).trim();
            String passwordToUse;
            if (ValidationUtils.isNullOrEmpty(rawPassword)) {
                passwordToUse = internService.searchInternById(id).getPassword();
            } else {
                passwordToUse = PasswordUtils.hash(rawPassword);
            }

            Intern updated = buildInternFromForm(passwordToUse);
            internService.editIntern(id, updated);
            mainFrame.refreshAll();
            clearForm();
            JOptionPane.showMessageDialog(this, "Intern updated successfully.");
        } catch (InternNotFoundException | InvalidInputException
                 | DataBaseException | DateTimeParseException ex) {
            showError(ex.getMessage());
        }
    }

    private void handleDelete() {
        String id = idField.getText().trim();
        if (id.isEmpty()) {
            showError("Select an intern from the table first.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete intern " + id + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            internService.deleteIntern(id);
            mainFrame.refreshAll();
            clearForm();
        } catch (InternNotFoundException | DataBaseException ex) {
            showError(ex.getMessage());
        }
    }

    private Intern buildInternFromForm(String passwordHash) throws DateTimeParseException {
        String id = idField.getText().trim();
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String dept = deptField.getText().trim();
        LocalDate start = LocalDate.parse(startDateField.getText().trim());
        LocalDate end = LocalDate.parse(endDateField.getText().trim());
        InternStatus status = (InternStatus) statusCombo.getSelectedItem();

        Supervisor selectedSupervisor = (Supervisor) supervisorCombo.getSelectedItem();

        Intern intern = new Intern(
                id, name, email, phone, dept,
                start, end,
                selectedSupervisor != null ? selectedSupervisor.getFullName() : null,
                status,
                passwordHash
        );
        intern.setSupervisorId(
                selectedSupervisor != null ? selectedSupervisor.getSupervisorId() : null);
        return intern;
    }

    private void clearForm() {
        idField.setText("");
        idField.setEditable(true);
        nameField.setText("");
        emailField.setText("");
        phoneField.setText("");
        deptField.setText("");
        startDateField.setText("YYYY-MM-DD");
        endDateField.setText("YYYY-MM-DD");
        passwordField.setText("");
        supervisorIdField.setText("");
        statusCombo.setSelectedIndex(0);
        if (supervisorCombo.getItemCount() > 0) {
            supervisorCombo.setSelectedIndex(0);
        }
        table.clearSelection();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void refreshTable() {
        populateTable(internService.getAllInterns());
        refreshDepartmentFilterOptions();
        refreshSupervisorCombo();
    }

    private void refreshSupervisorCombo() {
        if (supervisorCombo == null) return;
        Supervisor previous = (Supervisor) supervisorCombo.getSelectedItem();
        supervisorCombo.removeAllItems();
        supervisorCombo.addItem(null);   // "— None —"
        for (Supervisor s : supervisorService.getAllSupervisors()) {
            supervisorCombo.addItem(s);
        }
        if (previous != null) {
            supervisorCombo.setSelectedItem(previous);
        }
    }

    private void populateTable(List<Intern> interns) {
        tableModel.setRowCount(0);
        for (Intern i : interns) {
            tableModel.addRow(new Object[]{
                    i.getInternId(), i.getFullName(), i.getEmail(), i.getPhoneNumber(),
                    i.getDepartment(), i.getStartDate(), i.getEndDate(),
                    i.getSupervisorName() != null ? i.getSupervisorName() : "—",
                    i.getStatus()
            });
        }
    }

    private void refreshDepartmentFilterOptions() {
        String current = (String) filterDeptCombo.getSelectedItem();
        filterDeptCombo.removeAllItems();
        filterDeptCombo.addItem("All Departments");
        internService.getAllInterns().stream()
                .map(Intern::getDepartment)
                .distinct()
                .sorted()
                .forEach(filterDeptCombo::addItem);
        if (current != null) {
            filterDeptCombo.setSelectedItem(current);
        }
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

    /** Renders the supervisor combo: null → "— None —", otherwise "Full Name (Department)". */
    private static class SupervisorRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value == null) {
                setText("— None —");
            } else if (value instanceof Supervisor) {
                Supervisor s = (Supervisor) value;
                String dept = s.getDepartment() != null ? s.getDepartment() : "—";
                setText(s.getFullName() + "  (" + dept + ")");
            }
            return this;
        }
    }
}