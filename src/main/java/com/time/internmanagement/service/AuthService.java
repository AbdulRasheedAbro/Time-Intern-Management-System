package com.time.internmanagement.service;

import com.time.internmanagement.entity.Administrator;
import com.time.internmanagement.entity.Intern;
import com.time.internmanagement.entity.InternStatus;
import com.time.internmanagement.entity.Supervisor;
import com.time.internmanagement.util.DataBaseManager;
import com.time.internmanagement.util.PasswordUtils;

import java.sql.SQLException;
import java.time.LocalDate;

public class AuthService {

    private final DataBaseManager databaseManager;

    public AuthService(DataBaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    // ---------- Login ----------

    public Intern loginIntern(String internId, String password) {
        return databaseManager.authenticateIntern(internId, password);
    }

    public Administrator loginAdmin(String username, String password) {
        return databaseManager.authenticateAdmin(username, password);
    }

    public Supervisor loginSupervisor(String username, String password) {
        return databaseManager.authenticateSupervisor(username, password);
    }

    // ---------- Registration ----------

    /**
     * Registers a new intern. Returns true on success, false if the
     * Intern ID is already taken.
     */
    public boolean registerIntern(String id,
                                  String fullName,
                                  String email,
                                  String phone,
                                  String department,
                                  LocalDate start,
                                  LocalDate end,
                                  String supervisorName,
                                  String plainPassword) throws SQLException {

        // Duplicate check
        if (databaseManager.getInternById(id) != null) {
            return false;
        }

        Intern intern = new Intern(
                id,
                fullName,
                email,
                phone,
                department,
                start,
                end,
                supervisorName,
                InternStatus.ACTIVE,           // default status for a fresh sign-up
                PasswordUtils.hash(plainPassword)
        );

        databaseManager.saveIntern(intern);
        return true;
    }

    /**
     * Registers a new administrator. Returns true on success, false if
     * the username is already taken.
     */
    public boolean registerAdmin(String username, String plainPassword) throws SQLException {

        // Duplicate check
        if (databaseManager.getAdminByUsername(username) != null) {
            return false;
        }

        // Generate a unique admin ID. Using the timestamp guarantees uniqueness
        // for interactive sign-ups; a real system would use a sequence or UUID.
        String adminId = "ADM" + System.currentTimeMillis();

        Administrator admin = new Administrator(
                adminId,
                username,
                PasswordUtils.hash(plainPassword)
        );

        databaseManager.saveAdministrator(admin);
        return true;
    }
}