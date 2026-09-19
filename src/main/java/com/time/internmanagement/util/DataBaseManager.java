package com.time.internmanagement.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.time.internmanagement.entity.Administrator;
import com.time.internmanagement.entity.Intern;
import com.time.internmanagement.entity.InternStatus;
import com.time.internmanagement.entity.Supervisor;
import com.time.internmanagement.entity.Task;
import com.time.internmanagement.entity.TaskPriority;
import com.time.internmanagement.entity.TaskStatus;

public class DataBaseManager {

    private String url;
    private String user;
    private String password;

    public DataBaseManager() {
        loadConfig();
        initializeSchema();
        ensureDefaultAdminExists();
    }

    private void loadConfig() {
        Properties props = new Properties();
        try (FileInputStream input = new FileInputStream("config.properties")) {
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not load config.properties. Make sure it exists in the project root. " + e.getMessage());
        }

        String host = props.getProperty("db.host");
        String port = props.getProperty("db.port");
        String dbName = props.getProperty("db.name");
        this.user = props.getProperty("db.user");
        this.password = props.getProperty("db.password");

        this.url = "jdbc:postgresql://" + host + ":" + port + "/" + dbName + "?sslmode=require";
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    private void initializeSchema() {
        String createInterns = """
                CREATE TABLE IF NOT EXISTS interns (
                    intern_id VARCHAR(50) PRIMARY KEY,
                    full_name VARCHAR(150) NOT NULL,
                    email VARCHAR(150) NOT NULL,
                    phone_number VARCHAR(20) NOT NULL,
                    department VARCHAR(100) NOT NULL,
                    start_date DATE NOT NULL,
                    end_date DATE NOT NULL,
                    supervisor_name VARCHAR(150),
                    status VARCHAR(20) NOT NULL,
                    password VARCHAR(255) NOT NULL DEFAULT 'changeme'
                )
                """;

        String createTasks = """
                CREATE TABLE IF NOT EXISTS tasks (
                    task_id VARCHAR(50) PRIMARY KEY,
                    title VARCHAR(200) NOT NULL,
                    description TEXT,
                    assigned_intern_id VARCHAR(50) NOT NULL REFERENCES interns(intern_id),
                    assigned_date DATE NOT NULL,
                    deadline DATE NOT NULL,
                    priority VARCHAR(20) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    submission_text TEXT,
                    submitted_date DATE,
                    submission_file BYTEA,
                    submission_file_name VARCHAR(255),
                    submission_content_type VARCHAR(100),
                    assignment_file BYTEA,
                    assignment_file_name VARCHAR(255),
                    assignment_content_type VARCHAR(100)
                )
                """;

        String createAdmins = """
                CREATE TABLE IF NOT EXISTS administrators (
                    admin_id VARCHAR(50) PRIMARY KEY,
                    username VARCHAR(100) UNIQUE NOT NULL,
                    password VARCHAR(255) NOT NULL
                )
                """;

        String createSupervisors = """
                CREATE TABLE IF NOT EXISTS supervisors (
                    supervisor_id VARCHAR(50) PRIMARY KEY,
                    username VARCHAR(100) UNIQUE NOT NULL,
                    full_name VARCHAR(150) NOT NULL,
                    department VARCHAR(100),
                    email VARCHAR(150),
                    contact VARCHAR(20),
                    joining_date DATE,
                    password VARCHAR(255) NOT NULL
                )
                """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createInterns);
            stmt.execute(createTasks);
            stmt.execute(createAdmins);
            stmt.execute(createSupervisors);

            stmt.execute("ALTER TABLE interns ADD COLUMN IF NOT EXISTS password VARCHAR(255) NOT NULL DEFAULT 'changeme'");
            stmt.execute("ALTER TABLE interns ADD COLUMN IF NOT EXISTS supervisor_id VARCHAR(50)");
            stmt.execute("ALTER TABLE tasks ADD COLUMN IF NOT EXISTS submission_text TEXT");
            stmt.execute("ALTER TABLE tasks ADD COLUMN IF NOT EXISTS submitted_date DATE");
            stmt.execute("ALTER TABLE tasks ADD COLUMN IF NOT EXISTS submission_file BYTEA");
            stmt.execute("ALTER TABLE tasks ADD COLUMN IF NOT EXISTS submission_file_name VARCHAR(255)");
            stmt.execute("ALTER TABLE tasks ADD COLUMN IF NOT EXISTS submission_content_type VARCHAR(100)");
            stmt.execute("ALTER TABLE tasks ADD COLUMN IF NOT EXISTS assignment_file BYTEA");
            stmt.execute("ALTER TABLE tasks ADD COLUMN IF NOT EXISTS assignment_file_name VARCHAR(255)");
            stmt.execute("ALTER TABLE tasks ADD COLUMN IF NOT EXISTS assignment_content_type VARCHAR(100)");
            stmt.execute("ALTER TABLE supervisors ADD COLUMN IF NOT EXISTS email VARCHAR(150)");
            stmt.execute("ALTER TABLE supervisors ADD COLUMN IF NOT EXISTS contact VARCHAR(20)");
            stmt.execute("ALTER TABLE supervisors ADD COLUMN IF NOT EXISTS joining_date DATE");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema: " + e.getMessage());
        }
    }

    private void ensureDefaultAdminExists() {
        String checkSql = "SELECT COUNT(*) FROM administrators";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkSql)) {

            if (rs.next() && rs.getInt(1) == 0) {
                Administrator defaultAdmin = new Administrator("ADM001", "admin", PasswordUtils.hash("admin123"));
                saveAdministrator(defaultAdmin);
            }
        } catch (SQLException e) {
            System.err.println("Could not verify/create default admin: " + e.getMessage());
        }
    }

    // ---------- Interns ----------

    public void saveIntern(Intern intern) throws SQLException {
        String sql = """
                INSERT INTO interns (intern_id, full_name, email, phone_number, department,
                                      start_date, end_date, supervisor_name, status, password,
                                      supervisor_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (intern_id) DO UPDATE SET
                    full_name = EXCLUDED.full_name,
                    email = EXCLUDED.email,
                    phone_number = EXCLUDED.phone_number,
                    department = EXCLUDED.department,
                    start_date = EXCLUDED.start_date,
                    end_date = EXCLUDED.end_date,
                    supervisor_name = EXCLUDED.supervisor_name,
                    status = EXCLUDED.status,
                    password = EXCLUDED.password,
                    supervisor_id = EXCLUDED.supervisor_id
                """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, intern.getInternId());
            ps.setString(2, intern.getFullName());
            ps.setString(3, intern.getEmail());
            ps.setString(4, intern.getPhoneNumber());
            ps.setString(5, intern.getDepartment());
            ps.setDate(6, Date.valueOf(intern.getStartDate()));
            ps.setDate(7, Date.valueOf(intern.getEndDate()));
            ps.setString(8, intern.getSupervisorName());
            ps.setString(9, intern.getStatus().name());
            ps.setString(10, intern.getPassword());
            ps.setString(11, intern.getSupervisorId());
            ps.executeUpdate();
        }
    }

    public void deleteIntern(String internId) throws SQLException {
        String sql = "DELETE FROM interns WHERE intern_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, internId);
            ps.executeUpdate();
        } catch (SQLException e) {
            if ("23503".equals(e.getSQLState())) {
                throw new SQLException(
                        "Cannot delete this intern: they still have tasks assigned. " +
                                "Delete or reassign those tasks first.", e);
            }
            throw e;
        }
    }

    public List<Intern> loadInterns() {
        List<Intern> interns = new ArrayList<>();
        String sql = "SELECT * FROM interns";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                interns.add(mapIntern(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error loading interns: " + e.getMessage());
        }
        return interns;
    }

    public Intern authenticateIntern(String internId, String plainPassword) {
        String sql = "SELECT * FROM interns WHERE intern_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, internId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    if (PasswordUtils.matches(plainPassword, storedHash)) {
                        return mapIntern(rs);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error during intern authentication: " + e.getMessage());
        }
        return null;
    }

    private Intern mapIntern(ResultSet rs) throws SQLException {
        Intern intern = new Intern(
                rs.getString("intern_id"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("phone_number"),
                rs.getString("department"),
                rs.getDate("start_date").toLocalDate(),
                rs.getDate("end_date").toLocalDate(),
                rs.getString("supervisor_name"),
                InternStatus.valueOf(rs.getString("status")),
                rs.getString("password")
        );
        intern.setSupervisorId(rs.getString("supervisor_id"));
        return intern;
    }

    public Intern getInternById(String internId) {
        String sql = "SELECT * FROM interns WHERE intern_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, internId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapIntern(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error looking up intern: " + e.getMessage());
        }
        return null;
    }

    public Administrator getAdminByUsername(String username) {
        String sql = "SELECT * FROM administrators WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Administrator(
                            rs.getString("admin_id"),
                            rs.getString("username"),
                            rs.getString("password"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error looking up admin: " + e.getMessage());
        }
        return null;
    }

    // ---------- Supervisors ----------

    public void saveSupervisor(Supervisor supervisor) throws SQLException {
        String sql = """
                INSERT INTO supervisors (supervisor_id, username, full_name, department,
                                         email, contact, joining_date, password)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (supervisor_id) DO UPDATE SET
                    username = EXCLUDED.username,
                    full_name = EXCLUDED.full_name,
                    department = EXCLUDED.department,
                    email = EXCLUDED.email,
                    contact = EXCLUDED.contact,
                    joining_date = EXCLUDED.joining_date,
                    password = EXCLUDED.password
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, supervisor.getSupervisorId());
            ps.setString(2, supervisor.getUsername());
            ps.setString(3, supervisor.getFullName());
            ps.setString(4, supervisor.getDepartment());
            ps.setString(5, supervisor.getEmail());
            ps.setString(6, supervisor.getContact());
            if (supervisor.getJoiningDate() != null) {
                ps.setDate(7, Date.valueOf(supervisor.getJoiningDate()));
            } else {
                ps.setNull(7, Types.DATE);
            }
            ps.setString(8, supervisor.getPassword());
            ps.executeUpdate();
        }
    }

    public void deleteSupervisor(String supervisorId) throws SQLException {
        // Clear any interns pointing at this supervisor first, otherwise
        // the foreign key on interns.supervisor_id blocks the delete.
        String clearSql = "UPDATE interns SET supervisor_id = NULL WHERE supervisor_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(clearSql)) {
            ps.setString(1, supervisorId);
            ps.executeUpdate();
        }

        String deleteSql = "DELETE FROM supervisors WHERE supervisor_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(deleteSql)) {
            ps.setString(1, supervisorId);
            ps.executeUpdate();
        }
    }

    public Supervisor getSupervisorByUsername(String username) {
        String sql = "SELECT * FROM supervisors WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapSupervisor(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error looking up supervisor: " + e.getMessage());
        }
        return null;
    }

    public Supervisor getSupervisorById(String supervisorId) {
        String sql = "SELECT * FROM supervisors WHERE supervisor_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, supervisorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapSupervisor(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error looking up supervisor: " + e.getMessage());
        }
        return null;
    }

    public List<Supervisor> loadSupervisors() {
        List<Supervisor> supervisors = new ArrayList<>();
        String sql = "SELECT * FROM supervisors";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                supervisors.add(mapSupervisor(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error loading supervisors: " + e.getMessage());
        }
        return supervisors;
    }

    public Supervisor authenticateSupervisor(String username, String plainPassword) {
        String sql = "SELECT * FROM supervisors WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    if (PasswordUtils.matches(plainPassword, storedHash)) {
                        return mapSupervisor(rs);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error during supervisor authentication: " + e.getMessage());
        }
        return null;
    }

    private Supervisor mapSupervisor(ResultSet rs) throws SQLException {
        Supervisor s = new Supervisor(
                rs.getString("supervisor_id"),
                rs.getString("username"),
                rs.getString("full_name"),
                rs.getString("department"),
                rs.getString("password")
        );
        s.setEmail(rs.getString("email"));
        s.setContact(rs.getString("contact"));
        Date jd = rs.getDate("joining_date");
        if (jd != null) {
            s.setJoiningDate(jd.toLocalDate());
        }
        return s;
    }

    // ---------- Tasks ----------

    public void saveTask(Task task) throws SQLException {
        String sql = """
                INSERT INTO tasks (task_id, title, description, assigned_intern_id,
                                    assigned_date, deadline, priority, status,
                                    submission_text, submitted_date,
                                    submission_file, submission_file_name, submission_content_type,
                                    assignment_file, assignment_file_name, assignment_content_type)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (task_id) DO UPDATE SET
                    title = EXCLUDED.title,
                    description = EXCLUDED.description,
                    assigned_intern_id = EXCLUDED.assigned_intern_id,
                    assigned_date = EXCLUDED.assigned_date,
                    deadline = EXCLUDED.deadline,
                    priority = EXCLUDED.priority,
                    status = EXCLUDED.status,
                    submission_text = EXCLUDED.submission_text,
                    submitted_date = EXCLUDED.submitted_date,
                    submission_file = EXCLUDED.submission_file,
                    submission_file_name = EXCLUDED.submission_file_name,
                    submission_content_type = EXCLUDED.submission_content_type,
                    assignment_file = EXCLUDED.assignment_file,
                    assignment_file_name = EXCLUDED.assignment_file_name,
                    assignment_content_type = EXCLUDED.assignment_content_type
                """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, task.getTaskId());
            ps.setString(2, task.getTitle());
            ps.setString(3, task.getDescription());
            ps.setString(4, task.getAssignedInternId());
            ps.setDate(5, Date.valueOf(task.getAssignedDate()));
            ps.setDate(6, Date.valueOf(task.getDeadline()));
            ps.setString(7, task.getPriority().name());
            ps.setString(8, task.getStatus().name());
            ps.setString(9, task.getSubmissionText());
            if (task.getSubmittedDate() != null) {
                ps.setDate(10, Date.valueOf(task.getSubmittedDate()));
            } else {
                ps.setNull(10, Types.DATE);
            }

            if (task.getSubmissionFile() != null) {
                ps.setBytes(11, task.getSubmissionFile());
            } else {
                ps.setNull(11, Types.BINARY);
            }
            ps.setString(12, task.getSubmissionFileName());
            ps.setString(13, task.getSubmissionContentType());

            if (task.getAssignmentFile() != null) {
                ps.setBytes(14, task.getAssignmentFile());
            } else {
                ps.setNull(14, Types.BINARY);
            }
            ps.setString(15, task.getAssignmentFileName());
            ps.setString(16, task.getAssignmentContentType());

            ps.executeUpdate();
        }
    }

    public void deleteTask(String taskId) throws SQLException {
        String sql = "DELETE FROM tasks WHERE task_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, taskId);
            ps.executeUpdate();
        }
    }

    public List<Task> loadTasks() {
        List<Task> tasks = new ArrayList<>();
        String sql = """
                SELECT task_id, title, description, assigned_intern_id,
                       assigned_date, deadline, priority, status,
                       submission_text, submitted_date,
                       submission_file_name, submission_content_type,
                       assignment_file_name, assignment_content_type
                FROM tasks
                """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                tasks.add(mapTask(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error loading tasks: " + e.getMessage());
        }
        return tasks;
    }

    public Task loadTaskWithFile(String taskId) {
        String sql = "SELECT * FROM tasks WHERE task_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Task t = mapTask(rs);
                    t.setSubmissionFile(rs.getBytes("submission_file"));
                    t.setAssignmentFile(rs.getBytes("assignment_file"));
                    return t;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading task with files: " + e.getMessage());
        }
        return null;
    }

    public Task getTaskById(String taskId) {
        String sql = """
                SELECT task_id, title, description, assigned_intern_id,
                       assigned_date, deadline, priority, status,
                       submission_text, submitted_date,
                       submission_file_name, submission_content_type,
                       assignment_file_name, assignment_content_type
                FROM tasks WHERE task_id = ?
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTask(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error looking up task: " + e.getMessage());
        }
        return null;
    }

    private Task mapTask(ResultSet rs) throws SQLException {
        Date submittedDateSql = rs.getDate("submitted_date");
        Task task = new Task(
                rs.getString("task_id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("assigned_intern_id"),
                rs.getDate("assigned_date").toLocalDate(),
                rs.getDate("deadline").toLocalDate(),
                TaskPriority.valueOf(rs.getString("priority")),
                TaskStatus.valueOf(rs.getString("status")),
                rs.getString("submission_text"),
                submittedDateSql != null ? submittedDateSql.toLocalDate() : null
        );

        task.setSubmissionFileName(rs.getString("submission_file_name"));
        task.setSubmissionContentType(rs.getString("submission_content_type"));
        task.setAssignmentFileName(rs.getString("assignment_file_name"));
        task.setAssignmentContentType(rs.getString("assignment_content_type"));

        return task;
    }

    // ---------- Administrators ----------

    public void saveAdministrator(Administrator admin) throws SQLException {
        String sql = """
                INSERT INTO administrators (admin_id, username, password)
                VALUES (?, ?, ?)
                ON CONFLICT (admin_id) DO UPDATE SET
                    username = EXCLUDED.username,
                    password = EXCLUDED.password
                """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, admin.getAdminId());
            ps.setString(2, admin.getUsername());
            ps.setString(3, admin.getPassword());
            ps.executeUpdate();
        }
    }

    public Administrator authenticateAdmin(String username, String plainPassword) {
        String sql = "SELECT * FROM administrators WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    if (PasswordUtils.matches(plainPassword, storedHash)) {
                        return new Administrator(
                                rs.getString("admin_id"),
                                rs.getString("username"),
                                rs.getString("password")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error during admin authentication: " + e.getMessage());
        }
        return null;
    }
}