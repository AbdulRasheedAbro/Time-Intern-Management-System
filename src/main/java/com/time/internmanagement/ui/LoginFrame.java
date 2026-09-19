package com.time.internmanagement.ui;

import com.time.internmanagement.entity.Administrator;
import com.time.internmanagement.entity.Intern;
import com.time.internmanagement.entity.Supervisor;
import com.time.internmanagement.service.AuthService;
import com.time.internmanagement.service.InternService;
import com.time.internmanagement.service.SupervisorService;
import com.time.internmanagement.service.TaskService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginFrame extends JFrame {

    private final AuthService authService;
    private final InternService internService;
    private final TaskService taskService;
    private final SupervisorService supervisorService;

    private JTextField idField;
    private JPasswordField passwordField;
    private JLabel errorLabel;

    private static final Color BRAND_DARK = new Color(0x1E293B);
    private static final Color BRAND_ACCENT = new Color(0x3B82F6);
    private static final Color TEXT_MUTED = new Color(0x6B7280);
    private static final Color BORDER_GRAY = new Color(0xE5E7EB);
    private static final Color ERROR_RED = new Color(0xDC2626);

    public LoginFrame(AuthService authService,
                      InternService internService,
                      TaskService taskService,
                      SupervisorService supervisorService) {
        this.authService = authService;
        this.internService = internService;
        this.taskService = taskService;
        this.supervisorService = supervisorService;
        initUI();
    }

    private void initUI() {
        setTitle("TIME Intern Management System — Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(860, 560);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.add(buildBrandPanel(), BorderLayout.WEST);
        root.add(buildFormPanel(), BorderLayout.CENTER);

        add(root);
    }

    // ---------- Left brand panel ----------

    private JPanel buildBrandPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gradient = new GradientPaint(
                        0, 0, BRAND_DARK,
                        0, getHeight(), new Color(0x0F172A)
                );
                g2.setPaint(gradient);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panel.setPreferredSize(new Dimension(340, 0));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(0, 48, 0, 40));

        JLabel badge = new JLabel("T");
        badge.setOpaque(true);
        badge.setBackground(BRAND_ACCENT);
        badge.setForeground(Color.WHITE);
        badge.setFont(new Font("SansSerif", Font.BOLD, 22));
        badge.setHorizontalAlignment(SwingConstants.CENTER);
        badge.setMaximumSize(new Dimension(48, 48));
        badge.setPreferredSize(new Dimension(48, 48));
        badge.setAlignmentX(Component.LEFT_ALIGNMENT);
        badge.setBorder(BorderFactory.createEmptyBorder());

        JLabel brandTitle = new JLabel("<html>TIME Intern<br>Management System</html>");
        brandTitle.setFont(new Font("SansSerif", Font.BOLD, 24));
        brandTitle.setForeground(Color.WHITE);
        brandTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandTitle.setBorder(new EmptyBorder(20, 0, 12, 0));

        JLabel brandSubtitle = new JLabel(
                "<html><div style='width:220px'>Manage interns, assign tasks, and track progress — all in one place.</div></html>");
        brandSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        brandSubtitle.setForeground(new Color(0x94A3B8));
        brandSubtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(Box.createVerticalGlue());
        panel.add(badge);
        panel.add(brandTitle);
        panel.add(brandSubtitle);
        panel.add(Box.createVerticalGlue());

        JLabel footer = new JLabel("© TIME Institute");
        footer.setFont(new Font("SansSerif", Font.PLAIN, 11));
        footer.setForeground(new Color(0x64748B));
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(footer);
        panel.add(Box.createVerticalStrut(24));

        return panel;
    }

    // ---------- Right form panel ----------

    private JPanel buildFormPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(Color.WHITE);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);
        form.setMaximumSize(new Dimension(320, 360));
        form.setPreferredSize(new Dimension(320, 340));

        JLabel welcome = new JLabel("Welcome back");
        welcome.setFont(new Font("SansSerif", Font.BOLD, 24));
        welcome.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Sign in to continue");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(new EmptyBorder(4, 0, 28, 0));

        JLabel idLabel = new JLabel("ID / Username");
        idLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        idLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        idLabel.setBorder(new EmptyBorder(0, 0, 6, 0));

        idField = new JTextField();
        styleField(idField);

        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        passwordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordLabel.setBorder(new EmptyBorder(16, 0, 6, 0));

        passwordField = new JPasswordField();
        styleField(passwordField);
        passwordField.addActionListener(e -> handleLogin());

        errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        errorLabel.setForeground(ERROR_RED);
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        errorLabel.setBorder(new EmptyBorder(10, 0, 0, 0));

        JButton loginButton = new JButton("Sign In");
        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(320, 42));
        loginButton.setPreferredSize(new Dimension(320, 42));
        loginButton.setBackground(BRAND_ACCENT);
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        loginButton.setBorder(new EmptyBorder(0, 0, 0, 0));
        loginButton.setBorderPainted(false);
        loginButton.setOpaque(true);
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginButton.addActionListener(e -> handleLogin());

        form.add(welcome);
        form.add(subtitle);
        form.add(idLabel);
        form.add(idField);
        form.add(passwordLabel);
        form.add(passwordField);
        form.add(errorLabel);
        form.add(Box.createVerticalStrut(14));
        form.add(loginButton);

        outer.add(form);
        return outer;
    }

    private void styleField(JTextField field) {
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(320, 38));
        field.setPreferredSize(new Dimension(320, 38));
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY, 1, true),
                new EmptyBorder(4, 10, 4, 10)
        ));
    }

    // ---------- Login ----------

    private void handleLogin() {
        String idOrUsername = idField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (idOrUsername.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please fill in all fields.");
            return;
        }

        // Try intern first
        Intern intern = authService.loginIntern(idOrUsername, password);
        if (intern != null) {
            InternDashboardFrame internDashboard =
                    new InternDashboardFrame(intern, authService, internService, taskService);
            internDashboard.setVisible(true);
            dispose();
            return;
        }

        // Then supervisor
        Supervisor supervisor = authService.loginSupervisor(idOrUsername, password);
        if (supervisor != null) {
            SupervisorFrame supervisorFrame =
                    new SupervisorFrame(supervisor, authService, internService, taskService);
            supervisorFrame.setVisible(true);
            dispose();
            return;
        }

        // Then admin
        Administrator admin = authService.loginAdmin(idOrUsername, password);
        if (admin != null) {
            MainFrame mainFrame = new MainFrame(internService, taskService, supervisorService);
            mainFrame.setVisible(true);
            dispose();
            return;
        }

        // None matched
        errorLabel.setText("Invalid ID / username or password.");
    }
}