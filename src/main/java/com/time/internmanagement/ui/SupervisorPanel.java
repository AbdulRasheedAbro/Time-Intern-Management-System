package com.time.internmanagement.ui;

import com.time.internmanagement.entity.Intern;
import com.time.internmanagement.entity.Supervisor;
import com.time.internmanagement.service.InternService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class SupervisorPanel extends JPanel {

    private final Supervisor supervisor;
    private final InternService internService;

    private DefaultTableModel tableModel;
    private JTable table;

    private static final Color BG_LIGHT     = new Color(0xF8FAFC);
    private static final Color CARD_BG      = Color.WHITE;
    private static final Color BORDER_GRAY  = new Color(0xE5E7EB);
    private static final Color TEXT_DARK    = new Color(0x0F172A);
    private static final Color TEXT_MUTED   = new Color(0x6B7280);
    private static final Color FIELD_BORDER = new Color(0xD1D5DB);

    private static final String[] COLUMNS = {
            "ID", "Name", "Email", "Phone", "Department",
            "Start Date", "End Date", "Status"
    };

    public SupervisorPanel(Supervisor supervisor, InternService internService) {
        this.supervisor = supervisor;
        this.internService = internService;
        setBackground(BG_LIGHT);
        initUI();
        refreshTable();
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

        JLabel title = new JLabel("My Interns");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(TEXT_DARK);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel(
                "Interns assigned to you. Only you and the administrator can see this list.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(new EmptyBorder(4, 0, 0, 0));

        textBlock.add(title);
        textBlock.add(subtitle);

        JButton refresh = styledSecondaryButton("Refresh");
        refresh.addActionListener(e -> refreshTable());

        headRow.add(textBlock, BorderLayout.WEST);
        headRow.add(refresh, BorderLayout.EAST);
        add(headRow, BorderLayout.NORTH);

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

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        card.add(scroll, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
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

    public void refreshTable() {
        tableModel.setRowCount(0);
        List<Intern> interns = internService.getInternsForSupervisor(
                supervisor.getSupervisorId());

        for (Intern i : interns) {
            tableModel.addRow(new Object[]{
                    i.getInternId(), i.getFullName(), i.getEmail(), i.getPhoneNumber(),
                    i.getDepartment(), i.getStartDate(), i.getEndDate(), i.getStatus()
            });
        }

        if (interns.isEmpty()) {
            tableModel.addRow(new Object[]{
                    "—", "No interns assigned to you yet.", "", "", "", "", "", ""
            });
        }
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