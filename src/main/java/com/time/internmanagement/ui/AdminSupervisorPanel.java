package com.time.internmanagement.ui;

import com.time.internmanagement.entity.Supervisor;
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

public class AdminSupervisorPanel extends JPanel {

    private final SupervisorService supervisorService;
    private final MainFrame mainFrame;

    private DefaultTableModel tableModel;
    private JTable table;

    private JTextField idField, usernameField, fullNameField, departmentField;
    private JTextField emailField, contactField, joiningDateField;
    private JPasswordField passwordField;

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
            "Supervisor ID", "Username", "Full Name", "Department",
            "Email", "Contact", "Joining Date"
    };

    public AdminSupervisorPanel(SupervisorService supervisorService, MainFrame mainFrame) {
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

        JLabel title = new JLabel("Supervisors");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Manage supervisor accounts, contact details, and joining dates");
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

        JPanel headRow = new JPanel(new BorderLayout());
        headRow.setOpaque(false);

        JButton refresh = styledSecondaryButton("Refresh");
        refresh.addActionListener(e -> refreshTable());

        JButton toggleFormButton = styledSecondaryButton("Add Supervisor");
        toggleFormButton.addActionListener(e -> toggleForm());

        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightButtons.setOpaque(false);
        rightButtons.add(refresh);
        rightButtons.add(toggleFormButton);

        headRow.add(rightButtons, BorderLayout.EAST);
        content.add(headRow, BorderLayout.NORTH);
        content.add(buildTableCard(), BorderLayout.CENTER);

        formScroll = new JScrollPane(buildFormCard());
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        formScroll.getViewport().setBackground(BG_LIGHT);
        formScroll.setPreferredSize(new Dimension(0, 190));
        formScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);

        content.add(formScroll, BorderLayout.SOUTH);
        return content;
    }

    private void toggleForm() {
        if (formScroll == null) return;
        formScroll.setVisible(!formScroll.isVisible());
        revalidate();
        repaint();
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
            if (i != 2 && i != 4) {
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

        idField.setText(safeString(tableModel.getValueAt(row, 0)));
        usernameField.setText(safeString(tableModel.getValueAt(row, 1)));
        fullNameField.setText(safeString(tableModel.getValueAt(row, 2)));
        departmentField.setText(safeString(tableModel.getValueAt(row, 3)));
        emailField.setText(safeString(tableModel.getValueAt(row, 4)));
        contactField.setText(safeString(tableModel.getValueAt(row, 5)));
        joiningDateField.setText(safeString(tableModel.getValueAt(row, 6)));
        passwordField.setText("");

        idField.setEditable(false);
    }

    private static String safeString(Object v) {
        if (v == null) return "";
        String s = v.toString();
        return "—".equals(s) ? "" : s;
    }

    private JPanel buildFormCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY, 1, true),
                new EmptyBorder(12, 20, 12, 20)
        ));

        JLabel heading = new JLabel("Supervisor Details");
        heading.setFont(new Font("SansSerif", Font.BOLD, 14));
        heading.setForeground(TEXT_DARK);
        heading.setBorder(new EmptyBorder(0, 0, 10, 0));

        idField = new JTextField();
        usernameField = new JTextField();
        fullNameField = new JTextField();
        departmentField = new JTextField();
        emailField = new JTextField();
        contactField = new JTextField();
        joiningDateField = new JTextField("YYYY-MM-DD");
        passwordField = new JPasswordField();

        styleField(idField);
        styleField(usernameField);
        styleField(fullNameField);
        styleField(departmentField);
        styleField(emailField);
        styleField(contactField);
        styleField(joiningDateField);
        styleField(passwordField);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);

        addFieldPair(formPanel, 0, 0, "Supervisor ID:", idField, 1);
        addFieldPair(formPanel, 0, 2, "Username:", usernameField, 1);
        addFieldPair(formPanel, 0, 4, "Full Name:", fullNameField, 1);

        addFieldPair(formPanel, 1, 0, "Department:", departmentField, 1);
        addFieldPair(formPanel, 1, 2, "Email:", emailField, 1);
        addFieldPair(formPanel, 1, 4, "Contact:", contactField, 1);

        addFieldPair(formPanel, 2, 0, "Joining Date:", joiningDateField, 1);
        addFieldPair(formPanel, 2, 2, "Password:", passwordField, 1);

        JLabel hint = new JLabel("(leave blank on edit to keep existing)");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
        hint.setForeground(TEXT_MUTED);

        GridBagConstraints hintGc = new GridBagConstraints();
        hintGc.gridx = 4;
        hintGc.gridy = 2;
        hintGc.anchor = GridBagConstraints.LINE_START;
        hintGc.insets = new Insets(4, 6, 4, 0);
        formPanel.add(hint, hintGc);

        card.add(heading, BorderLayout.NORTH);
        card.add(formPanel, BorderLayout.CENTER);
        card.add(buildButtonPanel(), BorderLayout.SOUTH);
        return card;
    }

    private void addFieldPair(JPanel panel, int row, int labelCol,
                              String text, JComponent field, int span) {
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
        fieldGc.weightx = 0.3;
        fieldGc.insets = new Insets(4, 0, 4, 20);
        panel.add(field, fieldGc);
    }

    private JPanel buildButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        panel.setOpaque(false);

        JButton addButton = styledPrimaryButton("Add Supervisor");
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

    private void handleAdd() {
        try {
            String id = idField.getText().trim();
            String username = usernameField.getText().trim();
            String fullName = fullNameField.getText().trim();
            String department = departmentField.getText().trim();
            String email = emailField.getText().trim();
            String contact = contactField.getText().trim();
            String joiningStr = joiningDateField.getText().trim();
            String rawPassword = new String(passwordField.getPassword()).trim();

            if (ValidationUtils.isNullOrEmpty(id)
                    || ValidationUtils.isNullOrEmpty(username)
                    || ValidationUtils.isNullOrEmpty(fullName)) {
                showError("Supervisor ID, Username, and Full Name are required.");
                return;
            }
            if (ValidationUtils.isNullOrEmpty(rawPassword)) {
                showError("Please set a login password for this supervisor.");
                return;
            }

            LocalDate joiningDate = parseJoiningDate(joiningStr);
            if (joiningDate == null && !joiningStr.isEmpty() && !"YYYY-MM-DD".equals(joiningStr)) {
                showError("Joining Date must be in YYYY-MM-DD format.");
                return;
            }

            Supervisor s = new Supervisor(
                    id, username, fullName,
                    department.isEmpty() ? null : department,
                    PasswordUtils.hash(rawPassword)
            );
            s.setEmail(email.isEmpty() ? null : email);
            s.setContact(contact.isEmpty() ? null : contact);
            s.setJoiningDate(joiningDate);

            supervisorService.addSupervisor(s);
            mainFrame.refreshAll();
            clearForm();
            JOptionPane.showMessageDialog(this, "Supervisor added successfully.");
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void handleEdit() {
        try {
            String id = idField.getText().trim();
            if (id.isEmpty()) {
                showError("Select a supervisor from the table first.");
                return;
            }

            Supervisor existing = supervisorService.searchById(id);
            if (existing == null) {
                showError("Supervisor not found: " + id);
                return;
            }

            String username = usernameField.getText().trim();
            String fullName = fullNameField.getText().trim();
            String department = departmentField.getText().trim();
            String email = emailField.getText().trim();
            String contact = contactField.getText().trim();
            String joiningStr = joiningDateField.getText().trim();
            String rawPassword = new String(passwordField.getPassword()).trim();

            if (ValidationUtils.isNullOrEmpty(username) || ValidationUtils.isNullOrEmpty(fullName)) {
                showError("Username and Full Name cannot be empty.");
                return;
            }

            LocalDate joiningDate = parseJoiningDate(joiningStr);
            if (joiningDate == null && !joiningStr.isEmpty() && !"YYYY-MM-DD".equals(joiningStr)) {
                showError("Joining Date must be in YYYY-MM-DD format.");
                return;
            }

            existing.setUsername(username);
            existing.setFullName(fullName);
            existing.setDepartment(department.isEmpty() ? null : department);
            existing.setEmail(email.isEmpty() ? null : email);
            existing.setContact(contact.isEmpty() ? null : contact);
            existing.setJoiningDate(joiningDate);
            if (!ValidationUtils.isNullOrEmpty(rawPassword)) {
                existing.setPassword(PasswordUtils.hash(rawPassword));
            }

            com.time.internmanagement.util.DataBaseManager db =
                    new com.time.internmanagement.util.DataBaseManager();
            db.saveSupervisor(existing);

            mainFrame.refreshAll();
            clearForm();
            JOptionPane.showMessageDialog(this, "Supervisor updated successfully.");
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private LocalDate parseJoiningDate(String s) {
        if (s == null || s.isEmpty() || "YYYY-MM-DD".equals(s)) return null;
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private void handleDelete() {
        String id = idField.getText().trim();
        if (id.isEmpty()) {
            showError("Select a supervisor from the table first.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete supervisor " + id + "?\n\n" +
                        "Any interns currently assigned to this supervisor will be unassigned.\n" +
                        "This cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            supervisorService.deleteSupervisor(id);
            mainFrame.refreshAll();
            clearForm();
            JOptionPane.showMessageDialog(this,
                    "Supervisor " + id + " deleted.",
                    "Deleted", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void clearForm() {
        idField.setText("");
        idField.setEditable(true);
        usernameField.setText("");
        fullNameField.setText("");
        departmentField.setText("");
        emailField.setText("");
        contactField.setText("");
        joiningDateField.setText("YYYY-MM-DD");
        passwordField.setText("");
        table.clearSelection();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        List<Supervisor> supervisors = supervisorService.getAllSupervisors();
        for (Supervisor s : supervisors) {
            tableModel.addRow(new Object[]{
                    s.getSupervisorId(),
                    s.getUsername(),
                    s.getFullName(),
                    s.getDepartment() != null ? s.getDepartment() : "—",
                    s.getEmail() != null ? s.getEmail() : "—",
                    s.getContact() != null ? s.getContact() : "—",
                    s.getJoiningDate() != null ? s.getJoiningDate().toString() : "—"
            });
        }
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

    private JButton styledPrimaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setBackground(BRAND_ACCENT);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setPreferredSize(new Dimension(140, 34));
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