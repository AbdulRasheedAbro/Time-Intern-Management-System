package com.time.internmanagement.entity;

import java.time.LocalDate;

public class Supervisor {

    private String supervisorId;
    private String username;
    private String fullName;
    private String department;
    private String email;
    private String contact;
    private LocalDate joiningDate;
    private String password;

    public Supervisor(String supervisorId,
                      String username,
                      String fullName,
                      String department,
                      String password) {
        this.supervisorId = supervisorId;
        this.username = username;
        this.fullName = fullName;
        this.department = department;
        this.password = password;
    }

    public String getSupervisorId() {
        return supervisorId;
    }

    public void setSupervisorId(String supervisorId) {
        this.supervisorId = supervisorId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    // ---------- NEW ----------

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public LocalDate getJoiningDate() {
        return joiningDate;
    }

    public void setJoiningDate(LocalDate joiningDate) {
        this.joiningDate = joiningDate;
    }

    // ---------- Existing ----------

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return fullName + " (" + username + ")";
    }
}