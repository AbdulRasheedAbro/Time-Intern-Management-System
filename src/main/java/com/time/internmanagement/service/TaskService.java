package com.time.internmanagement.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.time.internmanagement.entity.Intern;
import com.time.internmanagement.entity.Supervisor;
import com.time.internmanagement.entity.Task;
import com.time.internmanagement.entity.TaskPriority;
import com.time.internmanagement.entity.TaskStatus;
import com.time.internmanagement.exception.DataBaseException;
import com.time.internmanagement.exception.DuplicateIdException;
import com.time.internmanagement.exception.InternNotFoundException;
import com.time.internmanagement.exception.InvalidInputException;
import com.time.internmanagement.util.DataBaseManager;
import com.time.internmanagement.util.ValidationUtils;

public class TaskService {

    private static final long MAX_PDF_BYTES = 5L * 1024 * 1024; // 5 MB

    private final List<Task> tasks;
    private final InternService internService;
    private final DataBaseManager databaseManager;

    public TaskService(InternService internService, DataBaseManager databaseManager) {
        this.internService = internService;
        this.databaseManager = databaseManager;
        this.tasks = new ArrayList<>(databaseManager.loadTasks());
    }

    // ---------- CRUD ----------

    public void createTask(Task task)
            throws DuplicateIdException, InvalidInputException, InternNotFoundException, DataBaseException {
        validateTask(task);

        boolean idExists = tasks.stream()
                .anyMatch(t -> t.getTaskId().equalsIgnoreCase(task.getTaskId()));
        if (idExists) {
            throw new DuplicateIdException("Task ID already exists: " + task.getTaskId());
        }

        if (!internService.internExists(task.getAssignedInternId())) {
            throw new InternNotFoundException(
                    "Cannot assign task — intern not found: " + task.getAssignedInternId());
        }

        try {
            databaseManager.saveTask(task);
        } catch (SQLException e) {
            throw new DataBaseException("Failed to save task to database.", e);
        }

        tasks.add(task);
    }

    public void createTaskAsSupervisor(Task task, Supervisor supervisor)
            throws DuplicateIdException, InvalidInputException,
            InternNotFoundException, DataBaseException {

        Intern intern = internService.searchInternById(task.getAssignedInternId());

        if (intern.getSupervisorId() == null
                || !intern.getSupervisorId().equalsIgnoreCase(supervisor.getSupervisorId())) {
            throw new InvalidInputException(
                    "You can only assign tasks to interns you supervise.");
        }

        createTask(task);
    }

    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks);
    }

    public Task searchTaskById(String taskId) throws InternNotFoundException {
        return tasks.stream()
                .filter(t -> t.getTaskId().equalsIgnoreCase(taskId))
                .findFirst()
                .orElseThrow(() -> new InternNotFoundException("Task not found: " + taskId));
    }

    public List<Task> getTasksForIntern(String internId) {
        List<Task> result = new ArrayList<>();
        for (Task t : tasks) {
            if (t.getAssignedInternId().equalsIgnoreCase(internId)) {
                result.add(t);
            }
        }
        return result;
    }

    public List<Task> getTasksForInterns(Set<String> internIds) {
        List<Task> result = new ArrayList<>();
        if (internIds == null || internIds.isEmpty()) {
            return result;
        }
        for (Task t : tasks) {
            if (internIds.contains(t.getAssignedInternId())) {
                result.add(t);
            }
        }
        return result;
    }

    public void updateTaskStatus(String taskId, TaskStatus newStatus)
            throws InternNotFoundException, DataBaseException {
        Task task = searchTaskById(taskId);
        task.setStatus(newStatus);
        try {
            databaseManager.saveTask(task);
        } catch (SQLException e) {
            throw new DataBaseException("Failed to update task status in database.", e);
        }
    }

    public void editTask(String taskId, Task updatedData)
            throws InternNotFoundException, InvalidInputException, DataBaseException {
        Task existing = searchTaskById(taskId);
        validateTask(updatedData);

        if (!internService.internExists(updatedData.getAssignedInternId())) {
            throw new InternNotFoundException(
                    "Cannot assign task — intern not found: " + updatedData.getAssignedInternId());
        }

        existing.setTitle(updatedData.getTitle());
        existing.setDescription(updatedData.getDescription());
        existing.setAssignedInternId(updatedData.getAssignedInternId());
        existing.setAssignedDate(updatedData.getAssignedDate());
        existing.setDeadline(updatedData.getDeadline());
        existing.setPriority(updatedData.getPriority());
        existing.setStatus(updatedData.getStatus());

        try {
            databaseManager.saveTask(existing);
        } catch (SQLException e) {
            throw new DataBaseException("Failed to update task in database.", e);
        }
    }

    public void deleteTask(String taskId) throws InternNotFoundException, DataBaseException {
        Task existing = searchTaskById(taskId);

        try {
            databaseManager.deleteTask(taskId);
        } catch (SQLException e) {
            throw new DataBaseException("Failed to delete task from database.", e);
        }

        tasks.remove(existing);
    }

    // ---------- Submission (intern's upload) ----------

    public void submitTask(String taskId, String submissionText)
            throws InternNotFoundException, InvalidInputException, DataBaseException {
        submitTask(taskId, submissionText, null, null);
    }

    public void submitTask(String taskId,
                           String submissionText,
                           byte[] pdfBytes,
                           String pdfFileName)
            throws InternNotFoundException, InvalidInputException, DataBaseException {

        boolean hasText = !ValidationUtils.isNullOrEmpty(submissionText);
        boolean hasFile = pdfBytes != null && pdfBytes.length > 0;

        if (!hasText && !hasFile) {
            throw new InvalidInputException("Submission must include a comment or a PDF file.");
        }

        if (hasFile) {
            validatePdf(pdfBytes);
        }

        Task task = searchTaskById(taskId);

        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new InvalidInputException("This task has already been submitted.");
        }

        // A task is submittable through the end of its deadline day (local time).
        if (task.getDeadline() != null
                && LocalDate.now().isAfter(task.getDeadline())) {
            throw new InvalidInputException(
                    "The deadline has passed. You cannot submit this task now. "
                            + "Deadline was " + task.getDeadline() + ".");
        }

        task.setSubmissionText(hasText ? submissionText : null);
        task.setSubmittedDate(LocalDate.now());
        task.setStatus(TaskStatus.COMPLETED);

        if (hasFile) {
            task.setSubmissionFile(pdfBytes);
            task.setSubmissionFileName(pdfFileName);
            task.setSubmissionContentType("application/pdf");
        } else {
            task.setSubmissionFile(null);
            task.setSubmissionFileName(null);
            task.setSubmissionContentType(null);
        }

        try {
            databaseManager.saveTask(task);
        } catch (SQLException e) {
            throw new DataBaseException("Failed to save task submission.", e);
        }
    }

    // ---------- Assignment (supervisor's upload) ----------

    public void attachAssignmentPdf(String taskId, byte[] pdfBytes, String fileName)
            throws InternNotFoundException, InvalidInputException, DataBaseException {

        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new InvalidInputException("Assignment file is empty.");
        }
        validatePdf(pdfBytes);

        Task task = searchTaskById(taskId);
        task.setAssignmentFile(pdfBytes);
        task.setAssignmentFileName(fileName);
        task.setAssignmentContentType("application/pdf");

        try {
            databaseManager.saveTask(task);
        } catch (SQLException e) {
            throw new DataBaseException("Failed to attach assignment PDF.", e);
        }
    }

    public void removeAssignmentPdf(String taskId)
            throws InternNotFoundException, DataBaseException {
        Task task = searchTaskById(taskId);
        task.setAssignmentFile(null);
        task.setAssignmentFileName(null);
        task.setAssignmentContentType(null);
        try {
            databaseManager.saveTask(task);
        } catch (SQLException e) {
            throw new DataBaseException("Failed to remove assignment PDF.", e);
        }
    }

    // ---------- Load with blobs ----------

    public Task loadTaskWithFile(String taskId) throws InternNotFoundException {
        Task full = databaseManager.loadTaskWithFile(taskId);
        if (full == null) {
            throw new InternNotFoundException("Task not found: " + taskId);
        }
        return full;
    }

    // ---------- Filters and counts ----------

    public List<Task> filterByIntern(String internId) {
        return getTasksForIntern(internId);
    }

    public List<Task> filterByStatus(TaskStatus status) {
        List<Task> result = new ArrayList<>();
        for (Task t : tasks) {
            if (t.getStatus() == status) {
                result.add(t);
            }
        }
        return result;
    }

    public List<Task> filterByPriority(TaskPriority priority) {
        List<Task> result = new ArrayList<>();
        for (Task t : tasks) {
            if (t.getPriority() == priority) {
                result.add(t);
            }
        }
        return result;
    }

    public int getTotalTaskCount() {
        return tasks.size();
    }

    public int getPendingTaskCount() {
        return (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.PENDING).count();
    }

    public int getInProgressTaskCount() {
        return (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS).count();
    }

    public int getCompletedTaskCount() {
        return (int) tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
    }

    // ---------- Validation ----------

    private void validateTask(Task task) throws InvalidInputException {
        if (ValidationUtils.isNullOrEmpty(task.getTitle())) {
            throw new InvalidInputException("Task title cannot be empty.");
        }
        if (ValidationUtils.isNullOrEmpty(task.getAssignedInternId())) {
            throw new InvalidInputException("Task must be assigned to an intern.");
        }
        if (!ValidationUtils.isValidDeadline(task.getAssignedDate(), task.getDeadline())) {
            throw new InvalidInputException("Deadline cannot be before assigned date.");
        }
    }

    private void validatePdf(byte[] bytes) throws InvalidInputException {
        if (bytes.length > MAX_PDF_BYTES) {
            throw new InvalidInputException(
                    "PDF exceeds the 5 MB limit (" + (bytes.length / 1024 / 1024) + " MB).");
        }
        if (bytes.length < 5
                || bytes[0] != '%'
                || bytes[1] != 'P'
                || bytes[2] != 'D'
                || bytes[3] != 'F'
                || bytes[4] != '-') {
            throw new InvalidInputException("File is not a valid PDF.");
        }
    }
}