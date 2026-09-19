package com.time.internmanagement.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import com.time.internmanagement.service.AuthService;
import com.time.internmanagement.service.InternService;
import com.time.internmanagement.service.SupervisorService;
import com.time.internmanagement.service.TaskService;
import com.time.internmanagement.util.DataBaseManager;

public class MainFrame extends JFrame {

    private final InternService internService;
    private final TaskService taskService;
    private final SupervisorService supervisorService;

    private DashboardPanel dashboardPanel;
    private InternPanel internPanel;
    private TaskPanel taskPanel;
    private AdminSupervisorPanel supervisorPanel;

    private JTabbedPane tabbedPane;

    public MainFrame(InternService internService,
                     TaskService taskService,
                     SupervisorService supervisorService) {
        this.internService = internService;
        this.taskService = taskService;
        this.supervisorService = supervisorService;

        initUI();
        setupWindowCloseHandler();
    }

    private void initUI() {
        setTitle("TIME Intern Management System — Administrator");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1050, 680);
        setLocationRelativeTo(null);

        // ---------- Top bar with title + logout button ----------
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBorder(new EmptyBorder(8, 12, 8, 12));

        JLabel appTitle = new JLabel("TIME Intern Management — Administrator");
        appTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        topBar.add(appTitle, BorderLayout.WEST);

        JButton logoutButton = new JButton("Log out");
        logoutButton.addActionListener(e -> handleLogout());
        topBar.add(logoutButton, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // ---------- Tabs ----------
        tabbedPane = new JTabbedPane();

        dashboardPanel = new DashboardPanel(internService, taskService);
        internPanel = new InternPanel(internService, supervisorService, this);
        taskPanel = new TaskPanel(taskService, internService, this);
        supervisorPanel = new AdminSupervisorPanel(supervisorService, this);

        tabbedPane.addTab("Dashboard", dashboardPanel);
        tabbedPane.addTab("Interns", internPanel);
        tabbedPane.addTab("Tasks", taskPanel);
        tabbedPane.addTab("Supervisors", supervisorPanel);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private void setupWindowCloseHandler() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int confirm = JOptionPane.showConfirmDialog(MainFrame.this,
                        "Exit the application?",
                        "Confirm Exit", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    dispose();
                    System.exit(0);
                }
            }
        });
    }

    // ---------- Logout ----------

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Log out and return to the login screen?",
                "Confirm Logout", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        dispose();

        // Rebuild the entire service graph for a clean session — no stale caches.
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

    public void refreshAll() {
        dashboardPanel.refreshStats();
        internPanel.refreshTable();
        taskPanel.refreshTable();
        supervisorPanel.refreshTable();
    }
}