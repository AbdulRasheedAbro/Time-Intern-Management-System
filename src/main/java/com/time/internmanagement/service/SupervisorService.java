package com.time.internmanagement.service;

import com.time.internmanagement.entity.Supervisor;
import com.time.internmanagement.util.DataBaseManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SupervisorService {

    private final List<Supervisor> supervisors;
    private final DataBaseManager databaseManager;

    public SupervisorService(DataBaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.supervisors = new ArrayList<>(databaseManager.loadSupervisors());
    }

    public List<Supervisor> getAllSupervisors() {
        return new ArrayList<>(supervisors);
    }

    public Supervisor searchById(String supervisorId) {
        for (Supervisor s : supervisors) {
            if (s.getSupervisorId().equalsIgnoreCase(supervisorId)) {
                return s;
            }
        }
        return null;
    }

    public Supervisor searchByUsername(String username) {
        for (Supervisor s : supervisors) {
            if (s.getUsername().equalsIgnoreCase(username)) {
                return s;
            }
        }
        return null;
    }

    public boolean supervisorExists(String supervisorId) {
        return searchById(supervisorId) != null;
    }

    public void addSupervisor(Supervisor s) throws Exception {
        if (searchById(s.getSupervisorId()) != null) {
            throw new Exception("Supervisor ID already exists: " + s.getSupervisorId());
        }
        if (searchByUsername(s.getUsername()) != null) {
            throw new Exception("Username already taken: " + s.getUsername());
        }
        try {
            databaseManager.saveSupervisor(s);
        } catch (SQLException e) {
            throw new Exception("Failed to save supervisor: " + e.getMessage(), e);
        }
        supervisors.add(s);
    }

    public void deleteSupervisor(String supervisorId) throws Exception {
        Supervisor existing = searchById(supervisorId);
        if (existing == null) {
            throw new Exception("Supervisor not found: " + supervisorId);
        }

        try {
            databaseManager.deleteSupervisor(supervisorId);
        } catch (SQLException e) {
            throw new Exception("Failed to delete supervisor: " + e.getMessage(), e);
        }

        supervisors.remove(existing);
    }

    public int getTotalSupervisorCount() {
        return supervisors.size();
    }
}