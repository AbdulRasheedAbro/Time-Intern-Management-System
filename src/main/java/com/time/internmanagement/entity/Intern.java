package com.time.internmanagement.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Intern {

    private String internId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String department;
    private LocalDate startDate;
    private LocalDate endDate;
    private String supervisorName;
    private String supervisorId;          // <-- NEW
    private InternStatus status;
    private String password;

    public Intern(String internId, String fullName, String email, String phoneNumber,
                  String department, LocalDate startDate, LocalDate endDate,
                  String supervisorName, InternStatus status, String password) {
        this.internId = internId;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.department = department;
        this.startDate = startDate;
        this.endDate = endDate;
        this.supervisorName = supervisorName;
        this.status = status;
        this.password = password;
    }

    public String getInternId() {
        return internId;
    }

    public void setInternId(String internId) {
        this.internId = internId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getSupervisorName() {
        return supervisorName;
    }

    public void setSupervisorName(String supervisorName) {
        this.supervisorName = supervisorName;
    }

    // ---------- NEW: supervisor foreign key ----------

    public String getSupervisorId() {
        return supervisorId;
    }

    public void setSupervisorId(String supervisorId) {
        this.supervisorId = supervisorId;
    }

    public InternStatus getStatus() {
        return status;
    }

    public void setStatus(InternStatus status) {
        this.status = status;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return internId + " | " + fullName + " | " + department + " | " + status;
    }

    // =========================================================
    // CSV SERIALIZATION
    // =========================================================
    // Column order:
    // internId, fullName, email, phoneNumber, department,
    // startDate, endDate, supervisorName, status, password,
    // supervisorId
    //
    // The last column (supervisorId) is appended for backward
    // compatibility. Old CSV files with 10 columns still load;
    // they simply leave supervisorId null.
    // =========================================================

    public String toCsvLine() {
        return String.join(",",
                csv(internId),
                csv(fullName),
                csv(email),
                csv(phoneNumber),
                csv(department),
                csv(startDate != null ? startDate.toString() : ""),
                csv(endDate != null ? endDate.toString() : ""),
                csv(supervisorName),
                csv(status != null ? status.name() : ""),
                csv(password),
                csv(supervisorId)
        );
    }

    public static Intern fromCsvLine(String line) {
        List<String> parts = parseCsv(line);
        if (parts.size() < 10) {
            throw new IllegalArgumentException(
                    "Expected 10 columns but found " + parts.size());
        }
        Intern intern = new Intern(
                emptyToNull(parts.get(0)),
                emptyToNull(parts.get(1)),
                emptyToNull(parts.get(2)),
                emptyToNull(parts.get(3)),
                emptyToNull(parts.get(4)),
                parseDate(parts.get(5)),
                parseDate(parts.get(6)),
                emptyToNull(parts.get(7)),
                InternStatus.valueOf(parts.get(8)),
                emptyToNull(parts.get(9))
        );

        // Older CSVs (10 columns) won't have the supervisorId — that's fine.
        if (parts.size() >= 11) {
            intern.setSupervisorId(emptyToNull(parts.get(10)));
        }

        return intern;
    }

    // =========================================================
    // CSV HELPERS
    // =========================================================

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
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
