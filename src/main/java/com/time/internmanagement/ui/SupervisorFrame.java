package com.time.internmanagement.ui;

import com.time.internmanagement.entity.Supervisor;
import com.time.internmanagement.service.AuthService;
import com.time.internmanagement.service.InternService;
import com.time.internmanagement.service.SupervisorService;
import com.time.internmanagement.service.TaskService;
import com.time.internmanagement.util.DataBaseManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class SupervisorFrame extends JFrame {

    private final Supervisor supervisor;
    private final AuthService authService;
    private final InternService internService;
    private final TaskService taskService;

    private static final Color BRAND_DARK   = new Color(0x1E293B);
    private static final Color BRAND_ACCENT = new Color(0x3B82F6);
    private static final Color BG_LIGHT     = new Color(0xF8FAFC);
    private static final Color TEXT_MUTED   = new Color(0x94A3B8);

    public SupervisorFrame(Supervisor supervisor,
                           AuthService authService,
                           InternService internService,
                           TaskService taskService) {
        this.supervisor = supervisor;
        this.authService = authService;
        this.internService = internService;
        this.taskService = taskService;
        initUI();
    }

    private void initUI() {
        setTitle("TIME Intern Management System — Supervisor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1120, 720);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_LIGHT);
        root.add(buildBanner(), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);

        getContentPane().setBackground(BG_LIGHT);
        add(root);
    }

    private JPanel buildBanner() {
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
        banner.setBorder(new EmptyBorder(22, 40, 22, 40));
        banner.setPreferredSize(new Dimension(0, 96));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        left.setOpaque(false);

        JLabel badge = new JLabel("T");
        badge.setOpaque(true);
        badge.setBackground(BRAND_ACCENT);
        badge.setForeground(Color.WHITE);
        badge.setFont(new Font("SansSerif", Font.BOLD, 20));
        badge.setHorizontalAlignment(SwingConstants.CENTER);
        badge.setPreferredSize(new Dimension(44, 44));

        JPanel textBlock = new JPanel();
        textBlock.setOpaque(false);
        textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Supervisor Dashboard");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        String dept = supervisor.getDepartment() != null ? supervisor.getDepartment() : "—";
        JLabel subtitle = new JLabel(String.format(
                "<html>%s &nbsp;•&nbsp; Username: %s &nbsp;•&nbsp; Department: %s</html>",
                supervisor.getFullName(), supervisor.getUsername(), dept));
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(new EmptyBorder(3, 0, 0, 0));

        textBlock.add(title);
        textBlock.add(subtitle);

        left.add(badge);
        left.add(textBlock);

        JButton logout = new JButton("Log out") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x334155));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        logout.setFont(new Font("SansSerif", Font.PLAIN, 13));
        logout.setForeground(Color.WHITE);
        logout.setContentAreaFilled(false);
        logout.setBorderPainted(false);
        logout.setFocusPainted(false);
        logout.setOpaque(false);
        logout.setPreferredSize(new Dimension(96, 34));
        logout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logout.addActionListener(e -> handleLogout());

        JPanel rightWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightWrap.setOpaque(false);
        rightWrap.add(logout);

        banner.add(left, BorderLayout.WEST);
        banner.add(rightWrap, BorderLayout.EAST);
        return banner;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(BG_LIGHT);
        body.setBorder(new EmptyBorder(16, 24, 20, 24));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabs.setBackground(BG_LIGHT);

        try {
            SupervisorPanel internsPanel =
                    new SupervisorPanel(supervisor, internService);
            tabs.addTab("My Interns", internsPanel);
        } catch (Exception ex) {
            tabs.addTab("My Interns", errorPanel("Could not load interns panel", ex));
        }

        try {
            SupervisorTaskPanel tasksPanel =
                    new SupervisorTaskPanel(supervisor, internService, taskService);
            tabs.addTab("Tasks", tasksPanel);
        } catch (Exception ex) {
            tabs.addTab("Tasks", errorPanel("Could not load tasks panel", ex));
        }

        body.add(tabs, BorderLayout.CENTER);
        return body;
    }

    private JPanel errorPanel(String message, Exception ex) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_LIGHT);
        panel.setBorder(new EmptyBorder(40, 40, 40, 40));

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area.setBackground(new Color(0xFEF2F2));
        area.setForeground(new Color(0x991B1B));

        java.io.StringWriter sw = new java.io.StringWriter();
        ex.printStackTrace(new java.io.PrintWriter(sw));
        area.setText(message + "\n\n" + ex.toString() + "\n\n" + sw.toString());

        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        return panel;
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Log out and return to the login screen?",
                "Confirm Logout", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        dispose();

        SwingUtilities.invokeLater(() -> {
            try {
                DataBaseManager db = new DataBaseManager();
                AuthService auth = new AuthService(db);
                InternService interns = new InternService(db);
                TaskService tasks = new TaskService(interns, db);
                SupervisorService supervisors = new SupervisorService(db);

                new LoginFrame(auth, interns, tasks, supervisors).setVisible(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null,
                        "Could not return to login: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}