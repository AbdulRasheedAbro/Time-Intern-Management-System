package com.time.internmanagement;

import com.formdev.flatlaf.FlatLightLaf;
import com.time.internmanagement.service.AuthService;
import com.time.internmanagement.service.InternService;
import com.time.internmanagement.service.SupervisorService;
import com.time.internmanagement.service.TaskService;
import com.time.internmanagement.ui.LoginFrame;
import com.time.internmanagement.util.DataBaseManager;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();
            UIManager.put("Component.arc", 12);
            UIManager.put("Button.arc", 12);
            UIManager.put("TextComponent.arc", 10);
            UIManager.put("ProgressBar.arc", 10);
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.showVerticalLines", false);
            UIManager.put("Table.intercellSpacing", new Dimension(0, 6));
            UIManager.put("Table.rowHeight", 32);

            DataBaseManager databaseManager = new DataBaseManager();
            InternService internService = new InternService(databaseManager);
            TaskService taskService = new TaskService(internService, databaseManager);
            AuthService authService = new AuthService(databaseManager);
            SupervisorService supervisorService = new SupervisorService(databaseManager);

            LoginFrame loginFrame = new LoginFrame(
                    authService, internService, taskService, supervisorService);
            loginFrame.setVisible(true);
        });
    }
}