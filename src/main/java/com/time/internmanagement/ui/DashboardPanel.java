package com.time.internmanagement.ui;

import com.time.internmanagement.service.InternService;
import com.time.internmanagement.service.TaskService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class DashboardPanel extends JPanel {

    private final InternService internService;
    private final TaskService taskService;

    private JLabel totalInternsValue, activeInternsValue, completedInternsValue;
    private JLabel totalTasksValue, pendingTasksValue, inProgressTasksValue, completedTasksValue;

    // ---------- Shared palette (matches LoginFrame) ----------
    private static final Color BRAND_DARK    = new Color(0x1E293B);
    private static final Color BRAND_ACCENT  = new Color(0x3B82F6);
    private static final Color BG_LIGHT      = new Color(0xF8FAFC);
    private static final Color CARD_BG       = Color.WHITE;
    private static final Color BORDER_GRAY   = new Color(0xE5E7EB);
    private static final Color TEXT_DARK     = new Color(0x0F172A);
    private static final Color TEXT_MUTED    = new Color(0x6B7280);

    // Accent colors used per-stat
    private static final Color ACCENT_BLUE   = new Color(0x3B82F6);
    private static final Color ACCENT_GREEN  = new Color(0x16A34A);
    private static final Color ACCENT_ORANGE = new Color(0xEA580C);
    private static final Color ACCENT_PURPLE = new Color(0x7C3AED);
    private static final Color ACCENT_SLATE  = new Color(0x64748B);

    public DashboardPanel(InternService internService, TaskService taskService) {
        this.internService = internService;
        this.taskService = taskService;
        setBackground(BG_LIGHT);
        initUI();
        refreshStats();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(0, 0, 0, 0));

        add(buildTopBanner(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
    }

    // ---------- Top banner (matches the dark login brand panel) ----------

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
        banner.setBorder(new EmptyBorder(24, 40, 24, 40));
        banner.setPreferredSize(new Dimension(0, 110));

        JPanel textBlock = new JPanel();
        textBlock.setOpaque(false);
        textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Dashboard");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Overview of interns and tasks across the system");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subtitle.setForeground(new Color(0x94A3B8));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(new EmptyBorder(4, 0, 0, 0));

        textBlock.add(title);
        textBlock.add(subtitle);

        // Right-side refresh button styled for dark banner
        JButton refreshButton = new JButton("Refresh") {
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
        refreshButton.setFont(new Font("SansSerif", Font.PLAIN, 13));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setContentAreaFilled(false);
        refreshButton.setBorderPainted(false);
        refreshButton.setFocusPainted(false);
        refreshButton.setOpaque(false);
        refreshButton.setPreferredSize(new Dimension(100, 36));
        refreshButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshButton.addActionListener(e -> refreshStats());

        JPanel rightWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightWrap.setOpaque(false);
        rightWrap.add(refreshButton);

        banner.add(textBlock, BorderLayout.WEST);
        banner.add(rightWrap, BorderLayout.EAST);
        return banner;
    }

    // ---------- Content ----------

    private JPanel buildContent() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(BG_LIGHT);
        content.setBorder(new EmptyBorder(28, 40, 28, 40));

        // Interns section
        JLabel internSection = sectionLabel("Interns");
        JPanel internRow = new JPanel(new GridLayout(1, 3, 16, 0));
        internRow.setOpaque(false);

        totalInternsValue     = new JLabel();
        activeInternsValue    = new JLabel();
        completedInternsValue = new JLabel();

        internRow.add(statCard("Total Interns", totalInternsValue, ACCENT_BLUE));
        internRow.add(statCard("Active", activeInternsValue, ACCENT_GREEN));
        internRow.add(statCard("Completed", completedInternsValue, ACCENT_SLATE));

        // Tasks section
        JLabel taskSection = sectionLabel("Tasks");
        JPanel taskRow = new JPanel(new GridLayout(1, 4, 16, 0));
        taskRow.setOpaque(false);

        totalTasksValue      = new JLabel();
        pendingTasksValue    = new JLabel();
        inProgressTasksValue = new JLabel();
        completedTasksValue  = new JLabel();

        taskRow.add(statCard("Total Tasks", totalTasksValue, ACCENT_BLUE));
        taskRow.add(statCard("Pending", pendingTasksValue, ACCENT_ORANGE));
        taskRow.add(statCard("In Progress", inProgressTasksValue, ACCENT_PURPLE));
        taskRow.add(statCard("Completed", completedTasksValue, ACCENT_GREEN));

        content.add(internSection);
        content.add(Box.createVerticalStrut(10));
        content.add(internRow);
        content.add(Box.createVerticalStrut(32));
        content.add(taskSection);
        content.add(Box.createVerticalStrut(10));
        content.add(taskRow);

        // Left-align everything
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_LIGHT);
        wrapper.add(content, BorderLayout.NORTH);
        return wrapper;
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text.toUpperCase());
        label.setFont(new Font("SansSerif", Font.BOLD, 11));
        label.setForeground(TEXT_MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /**
     * A card with a soft shadow, rounded corners, an accent stripe on the
     * left edge, caption on top, and a large colored value below.
     */
    private JPanel statCard(String label, JLabel valueLabel, Color accentColor) {

        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                // Shadow
                g2.setColor(new Color(0, 0, 0, 12));
                g2.fillRoundRect(2, 3, getWidth() - 4, getHeight() - 4, 14, 14);

                // Card background
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth() - 3, getHeight() - 3, 14, 14);

                // Border
                g2.setColor(BORDER_GRAY);
                g2.drawRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 14, 14);

                // Left accent stripe
                g2.setColor(accentColor);
                g2.fillRoundRect(0, 0, 6, getHeight() - 3, 14, 14);
                // Square off the right side of the stripe so only the left is rounded
                g2.fillRect(3, 0, 3, getHeight() - 3);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(18, 22, 18, 18));
        card.setPreferredSize(new Dimension(0, 120));

        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        JLabel caption = new JLabel(label);
        caption.setFont(new Font("SansSerif", Font.PLAIN, 13));
        caption.setForeground(TEXT_MUTED);
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);

        valueLabel.setText("0");
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 34));
        valueLabel.setForeground(TEXT_DARK);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueLabel.setBorder(new EmptyBorder(8, 0, 0, 0));

        // Small accent dot before the caption
        JPanel captionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        captionRow.setOpaque(false);
        captionRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel dot = new JPanel();
        dot.setBackground(accentColor);
        dot.setPreferredSize(new Dimension(8, 8));
        dot.setMaximumSize(new Dimension(8, 8));
        dot.setBorder(BorderFactory.createLineBorder(accentColor));

        // Draw a circle by overriding paintComponent
        JPanel circle = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accentColor);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        circle.setOpaque(false);
        circle.setPreferredSize(new Dimension(8, 8));
        circle.setMaximumSize(new Dimension(8, 8));

        captionRow.add(circle);
        captionRow.add(caption);

        inner.add(captionRow);
        inner.add(valueLabel);
        card.add(inner, BorderLayout.CENTER);

        return card;
    }

    // ---------- Data refresh ----------

    public void refreshStats() {
        totalInternsValue.setText(String.valueOf(internService.getTotalInternCount()));
        activeInternsValue.setText(String.valueOf(internService.getActiveInternCount()));
        completedInternsValue.setText(String.valueOf(internService.getCompletedInternCount()));

        totalTasksValue.setText(String.valueOf(taskService.getTotalTaskCount()));
        pendingTasksValue.setText(String.valueOf(taskService.getPendingTaskCount()));
        inProgressTasksValue.setText(String.valueOf(taskService.getInProgressTaskCount()));
        completedTasksValue.setText(String.valueOf(taskService.getCompletedTaskCount()));
    }
}