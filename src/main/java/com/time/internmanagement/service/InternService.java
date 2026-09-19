package com.time.internmanagement.service;

import com.time.internmanagement.entity.Intern;
import com.time.internmanagement.entity.InternStatus;
import com.time.internmanagement.exception.DataBaseException;
import com.time.internmanagement.exception.DuplicateIdException;
import com.time.internmanagement.exception.InternNotFoundException;
import com.time.internmanagement.exception.InvalidInputException;
import com.time.internmanagement.util.DataBaseManager;
import com.time.internmanagement.util.ValidationUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InternService {

    private final List<Intern> interns;
    private final DataBaseManager databaseManager;

    public InternService(DataBaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.interns = new ArrayList<>(databaseManager.loadInterns());
    }

    public void addIntern(Intern intern) throws DuplicateIdException, InvalidInputException, DataBaseException {
        validateIntern(intern);

        boolean idExists = interns.stream()
                .anyMatch(i -> i.getInternId().equalsIgnoreCase(intern.getInternId()));
        if (idExists) {
            throw new DuplicateIdException("Intern ID already exists: " + intern.getInternId());
        }

        try {
            databaseManager.saveIntern(intern);
        } catch (SQLException e) {
            throw new DataBaseException("Failed to save intern to database.", e);
        }

        interns.add(intern);
    }

    public List<Intern> getAllInterns() {
        return new ArrayList<>(interns); // defensive copy
    }

    public Intern searchInternById(String internId) throws InternNotFoundException {
        return interns.stream()
                .filter(i -> i.getInternId().equalsIgnoreCase(internId))
                .findFirst()
                .orElseThrow(() -> new InternNotFoundException("Intern not found: " + internId));
    }

    public List<Intern> searchInternsByName(String namePart) {
        List<Intern> result = new ArrayList<>();
        if (ValidationUtils.isNullOrEmpty(namePart)) {
            return result;
        }
        String lowerPart = namePart.toLowerCase();
        for (Intern i : interns) {
            if (i.getFullName().toLowerCase().contains(lowerPart)) {
                result.add(i);
            }
        }
        return result;
    }

    public void editIntern(String internId, Intern updatedData)
            throws InternNotFoundException, InvalidInputException, DataBaseException {
        Intern existing = searchInternById(internId);
        validateIntern(updatedData);

        existing.setFullName(updatedData.getFullName());
        existing.setEmail(updatedData.getEmail());
        existing.setPhoneNumber(updatedData.getPhoneNumber());
        existing.setDepartment(updatedData.getDepartment());
        existing.setStartDate(updatedData.getStartDate());
        existing.setEndDate(updatedData.getEndDate());
        existing.setSupervisorName(updatedData.getSupervisorName());
        existing.setSupervisorId(updatedData.getSupervisorId());
        existing.setStatus(updatedData.getStatus());

        try {
            databaseManager.saveIntern(existing);
        } catch (SQLException e) {
            throw new DataBaseException("Failed to update intern in database.", e);
        }
    }

    public void deleteIntern(String internId) throws InternNotFoundException, DataBaseException {
        Intern existing = searchInternById(internId);

        try {
            databaseManager.deleteIntern(internId);
        } catch (SQLException e) {
            throw new DataBaseException("Failed to delete intern from database.", e);
        }

        interns.remove(existing);
    }

    public List<Intern> filterByDepartment(String department) {
        List<Intern> result = new ArrayList<>();
        for (Intern i : interns) {
            if (i.getDepartment().equalsIgnoreCase(department)) {
                result.add(i);
            }
        }
        return result;
    }

    public List<Intern> filterByStatus(InternStatus status) {
        List<Intern> result = new ArrayList<>();
        for (Intern i : interns) {
            if (i.getStatus() == status) {
                result.add(i);
            }
        }
        return result;
    }

    public boolean internExists(String internId) {
        return interns.stream().anyMatch(i -> i.getInternId().equalsIgnoreCase(internId));
    }

    // ---------- Supervisor scoping ----------

    /**
     * Returns all interns whose supervisor_id matches the given supervisor.
     * Used by SupervisorPanel and (later) SupervisorTaskPanel.
     */
    public List<Intern> getInternsForSupervisor(String supervisorId) {
        List<Intern> result = new ArrayList<>();
        if (supervisorId == null) {
            return result;
        }
        for (Intern i : interns) {
            if (supervisorId.equalsIgnoreCase(i.getSupervisorId())) {
                result.add(i);
            }
        }
        return result;
    }

    // Dashboard stats
    public int getTotalInternCount() {
        return interns.size();
    }

    public int getActiveInternCount() {
        return (int) interns.stream().filter(i -> i.getStatus() == InternStatus.ACTIVE).count();
    }

    public int getCompletedInternCount() {
        return (int) interns.stream().filter(i -> i.getStatus() == InternStatus.COMPLETED).count();
    }

    private void validateIntern(Intern intern) throws InvalidInputException {
        if (ValidationUtils.isNullOrEmpty(intern.getFullName())) {
            throw new InvalidInputException("Full name cannot be empty.");
        }
        if (!ValidationUtils.isValidEmail(intern.getEmail())) {
            throw new InvalidInputException("Invalid email address: " + intern.getEmail());
        }
        if (!ValidationUtils.isValidPhoneNumber(intern.getPhoneNumber())) {
            throw new InvalidInputException("Invalid phone number: " + intern.getPhoneNumber());
        }
        if (ValidationUtils.isNullOrEmpty(intern.getDepartment())) {
            throw new InvalidInputException("Department cannot be empty.");
        }
        if (!ValidationUtils.isValidDateRange(intern.getStartDate(), intern.getEndDate())) {
            throw new InvalidInputException("End date cannot be before start date.");
        }
    }
}