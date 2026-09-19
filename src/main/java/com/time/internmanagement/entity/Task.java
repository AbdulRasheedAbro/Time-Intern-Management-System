package com.time.internmanagement.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Task {

    private String taskId;
    private String title;
    private String description;
    private String assignedInternId;
    private LocalDate assignedDate;
    private LocalDate deadline;
    private TaskPriority priority;
    private TaskStatus status;
    private String submissionText;
    private LocalDate submittedDate;

    // ---------- Submission file (intern's upload) ----------
    private byte[] submissionFile;
    private String submissionFileName;
    private String submissionContentType;

    // ---------- Assignment file (admin's upload) ----------
    private byte[] assignmentFile;
    private String assignmentFileName;
    private String assignmentContentType;

    public Task(String taskId, String title, String description,
                String assignedInternId, LocalDate assignedDate, LocalDate deadline,
                TaskPriority priority, TaskStatus status,
                String submissionText, LocalDate submittedDate) {
        this.taskId = taskId;
        this.title = title;
        this.description = description;
        this.assignedInternId = assignedInternId;
        this.assignedDate = assignedDate;
        this.deadline = deadline;
        this.priority = priority;
        this.status = status;
        this.submissionText = submissionText;
        this.submittedDate = submittedDate;
    }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAssignedInternId() { return assignedInternId; }
    public void setAssignedInternId(String assignedInternId) { this.assignedInternId = assignedInternId; }

    public LocalDate getAssignedDate() { return assignedDate; }
    public void setAssignedDate(LocalDate assignedDate) { this.assignedDate = assignedDate; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public String getSubmissionText() { return submissionText; }
    public void setSubmissionText(String submissionText) { this.submissionText = submissionText; }

    public LocalDate getSubmittedDate() { return submittedDate; }
    public void setSubmittedDate(LocalDate submittedDate) { this.submittedDate = submittedDate; }

    // ---------- Submission file ----------

    public byte[] getSubmissionFile() { return submissionFile; }
    public void setSubmissionFile(byte[] submissionFile) { this.submissionFile = submissionFile; }

    public String getSubmissionFileName() { return submissionFileName; }
    public void setSubmissionFileName(String submissionFileName) { this.submissionFileName = submissionFileName; }

    public String getSubmissionContentType() { return submissionContentType; }
    public void setSubmissionContentType(String submissionContentType) { this.submissionContentType = submissionContentType; }

    // ---------- Assignment file ----------

    public byte[] getAssignmentFile() { return assignmentFile; }
    public void setAssignmentFile(byte[] assignmentFile) { this.assignmentFile = assignmentFile; }

    public String getAssignmentFileName() { return assignmentFileName; }
    public void setAssignmentFileName(String assignmentFileName) { this.assignmentFileName = assignmentFileName; }

    public String getAssignmentContentType() { return assignmentContentType; }
    public void setAssignmentContentType(String assignmentContentType) { this.assignmentContentType = assignmentContentType; }

    @Override
    public String toString() {
        return taskId + " | " + title + " | " + assignedInternId + " | " + status;
    }

    // =========================================================
    // CSV SERIALIZATION
    // Column order:
    // taskId, title, description, assignedInternId, assignedDate,
    // deadline, priority, status, submissionText, submittedDate,
    // submissionFileName, submissionContentType,
    // assignmentFileName, assignmentContentType
    // =========================================================

    public String toCsvLine() {
        return String.join(",",
                csv(taskId),
                csv(title),
                csv(description),
                csv(assignedInternId),
                csv(assignedDate != null ? assignedDate.toString() : ""),
                csv(deadline != null ? deadline.toString() : ""),
                csv(priority != null ? priority.name() : ""),
                csv(status != null ? status.name() : ""),
                csv(submissionText),
                csv(submittedDate != null ? submittedDate.toString() : ""),
                csv(submissionFileName),
                csv(submissionContentType),
                csv(assignmentFileName),
                csv(assignmentContentType)
        );
    }

    public static Task fromCsvLine(String line) {
        List<String> parts = parseCsv(line);
        if (parts.size() < 10) {
            throw new IllegalArgumentException(
                    "Expected at least 10 columns but found " + parts.size());
        }

        Task task = new Task(
                emptyToNull(parts.get(0)),
                emptyToNull(parts.get(1)),
                emptyToNull(parts.get(2)),
                emptyToNull(parts.get(3)),
                parseDate(parts.get(4)),
                parseDate(parts.get(5)),
                TaskPriority.valueOf(parts.get(6)),
                TaskStatus.valueOf(parts.get(7)),
                emptyToNull(parts.get(8)),
                parseDate(parts.get(9))
        );

        if (parts.size() >= 12) {
            task.setSubmissionFileName(emptyToNull(parts.get(10)));
            task.setSubmissionContentType(emptyToNull(parts.get(11)));
        }
        if (parts.size() >= 14) {
            task.setAssignmentFileName(emptyToNull(parts.get(12)));
            task.setAssignmentContentType(emptyToNull(parts.get(13)));
        }

        return task;
    }

    // ---------- CSV helpers ----------

    private static String csv(String value) {
        if (value == null) return "";
        boolean needsQuotes = value.contains(",")
                || value.contains("\"")
                || value.contains("\n")
                || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }

    private static List<String> parseCsv(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else {
                if (c == ',') {
                    result.add(cur.toString());
                    cur.setLength(0);
                } else if (c == '"') {
                    inQuotes = true;
                } else {
                    cur.append(c);
                }
            }
        }
        result.add(cur.toString());
        return result;
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isEmpty()) ? null : s;
    }

    private static LocalDate parseDate(String s) {
        return (s == null || s.isEmpty()) ? null : LocalDate.parse(s);
    }
}