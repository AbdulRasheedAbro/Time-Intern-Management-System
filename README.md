# TIME Intern Management System

A desktop application for managing interns, supervisors, and tasks — built in **Java 17** with **Java Swing** and a **PostgreSQL** backend hosted on Aiven.

The system has three roles that all sign in from a single login page:

- **Administrators** manage interns, supervisors, and tasks, and can attach PDF briefs to any task.
- **Supervisors** see only the interns assigned to them, assign tasks to those interns, and review their submissions.
- **Interns** view tasks assigned to them, download the attached brief, and submit their work as a PDF before the deadline.

The application follows a layered architecture — Swing UI, service layer, and a JDBC data layer — with all database access centralised in a single `DataBaseManager` class. Tasks and submissions are stored as `BYTEA` columns in PostgreSQL, and passwords are hashed with SHA-256 before being saved.

---

## Table of Contents

- [Features](#features)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [Database Schema](#database-schema)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Usage](#usage)
- [Project Structure](#project-structure)
- [Security Notes](#security-notes)
- [Future Enhancements](#future-enhancements)
- [License](#license)

---

## Features

### Authentication

- **Single login page** for all three roles — intern, supervisor, and admin
- Credentials are tried against each role in sequence; the matching role's dashboard opens
- Passwords stored as SHA-256 hashes (never plain text)

### Admin

- **Dashboard** with live counts of interns (total, active, completed) and tasks (total, pending, in progress, completed)
- **Intern management** — add, edit, delete interns; assign each intern to a supervisor via a dropdown; filter by department and status; search by name
- **Task management** — create, edit, delete tasks; attach task brief PDFs; view intern submissions
- **Supervisor management** — add, edit, delete supervisors with full contact details and joining date

### Supervisor

- **My Interns** — read-only list of interns assigned to this supervisor only
- **Tasks** — create tasks for own interns only; attach a task brief PDF; view submissions from interns
- Access control enforced at the service layer — a supervisor cannot assign a task to another supervisor's intern

### Intern

- **My Tasks** — table of tasks assigned to this intern with columns for Task ID, Title, Priority, Status, Deadline, Task PDF, and Submission
- **Download Task PDF** — retrieves the brief the supervisor attached
- **Submit Task** — uploads a PDF with an optional comment
- Business rules enforced in `TaskService`:
    - No submissions after the deadline
    - No duplicate submissions — a completed task is locked
    - PDFs only, validated by both file extension and magic bytes (`%PDF-`)
    - Maximum file size: 5 MB

### Common

- Modern Swing UI with a shared colour palette (`UiTheme`) across all screens
- Logout from any dashboard returns to the login page with a freshly rebuilt service graph
- PDFs stored as `BYTEA` columns in PostgreSQL

---

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| UI Framework | Java Swing |
| Look & Feel | FlatLaf 3.5.4 |
| Database | PostgreSQL (hosted on Aiven) |
| JDBC Driver | PostgreSQL JDBC 42.7.4 |
| Build Tool | Maven |
| IDE | IntelliJ IDEA |
| Password Storage | SHA-256 hashing |
| PDF Handling | `byte[]` + `BYTEA` column |

---

## Architecture

The application is organised in four layers. Each layer only talks to the one below it — the UI never issues SQL, and the services never touch Swing.

```
┌─────────────────────────────────────────────┐
│  UI Layer (Swing)                            │
│  LoginFrame, MainFrame, SupervisorFrame,     │
│  InternDashboardFrame, panels, dialogs       │
└───────────────────┬───────────────────────────┘
                     │
┌───────────────────▼───────────────────────────┐
│  Service Layer                                │
│  AuthService, InternService,                  │
│  SupervisorService, TaskService               │
└───────────────────┬───────────────────────────┘
                     │
┌───────────────────▼───────────────────────────┐
│  Data Layer                                   │
│  DataBaseManager (JDBC)                       │
└───────────────────┬───────────────────────────┘
                     │
┌───────────────────▼───────────────────────────┐
│  PostgreSQL on Aiven (cloud, SSL required)    │
└─────────────────────────────────────────────────┘
```

---

## Database Schema

Four tables. Foreign keys enforce referential integrity.

### `administrators`

| Column | Type | Notes |
|---|---|---|
| `admin_id` | VARCHAR(50) | Primary key |
| `username` | VARCHAR(100) | Unique |
| `password` | VARCHAR(255) | SHA-256 hash |

### `supervisors`

| Column | Type | Notes |
|---|---|---|
| `supervisor_id` | VARCHAR(50) | Primary key |
| `username` | VARCHAR(100) | Unique |
| `full_name` | VARCHAR(150) | |
| `department` | VARCHAR(100) | Nullable |
| `email` | VARCHAR(150) | Nullable |
| `contact` | VARCHAR(20) | Nullable |
| `joining_date` | DATE | Nullable |
| `password` | VARCHAR(255) | SHA-256 hash |

### `interns`

| Column | Type | Notes |
|---|---|---|
| `intern_id` | VARCHAR(50) | Primary key |
| `full_name` | VARCHAR(150) | |
| `email` | VARCHAR(150) | |
| `phone_number` | VARCHAR(20) | |
| `department` | VARCHAR(100) | |
| `start_date` | DATE | |
| `end_date` | DATE | |
| `supervisor_name` | VARCHAR(150) | Display name |
| `supervisor_id` | VARCHAR(50) | FK → `supervisors.supervisor_id` |
| `status` | VARCHAR(20) | `ACTIVE`, `COMPLETED`, `TERMINATED` |
| `password` | VARCHAR(255) | SHA-256 hash |

### `tasks`

| Column | Type | Notes |
|---|---|---|
| `task_id` | VARCHAR(50) | Primary key |
| `title` | VARCHAR(200) | |
| `description` | TEXT | |
| `assigned_intern_id` | VARCHAR(50) | FK → `interns.intern_id` |
| `assigned_date` | DATE | |
| `deadline` | DATE | |
| `priority` | VARCHAR(20) | `LOW`, `MEDIUM`, `HIGH` |
| `status` | VARCHAR(20) | `PENDING`, `IN_PROGRESS`, `COMPLETED` |
| `submission_text` | TEXT | Optional comment from the intern |
| `submitted_date` | DATE | Nullable |
| `submission_file` | BYTEA | Intern's PDF |
| `submission_file_name` | VARCHAR(255) | |
| `submission_content_type` | VARCHAR(100) | |
| `assignment_file` | BYTEA | Supervisor's brief PDF |
| `assignment_file_name` | VARCHAR(255) | |
| `assignment_content_type` | VARCHAR(100) | |

---

## Getting Started

### Prerequisites

- **Java 17** or later
- **Maven** (or IntelliJ's bundled Maven)
- A **PostgreSQL database** — this project uses [Aiven](https://aiven.io) free tier, but any PostgreSQL 12+ works

### Clone the repository

```bash
git clone https://github.com/your-username/time-intern-management-system.git
cd time-intern-management-system
```

### Configure the connection

Copy the example config and fill in your own credentials:

```bash
cp config.properties.example config.properties
```

Then edit `config.properties`:

```properties
db.host=your-instance.a.aivencloud.com
db.port=24714
db.name=defaultdb
db.user=avnadmin
db.password=your-password-here
```

`config.properties` is listed in `.gitignore` and is never committed — only `config.properties.example` (with placeholder values) is tracked in the repository.

### Build and run

```bash
mvn clean install
mvn exec:java -Dexec.mainClass="com.time.internmanagement.Main"
```

Or open the project in IntelliJ and run `Main`.

### First login

On first launch, the app creates the schema automatically and inserts a default admin:

| Username | Password |
|---|---|
| `admin` | `admin123` |

**Change this password after first login in any real deployment.**

---

## Configuration

### `config.properties`

| Key | Description |
|---|---|
| `db.host` | PostgreSQL hostname |
| `db.port` | Port (Aiven uses 24714 by default) |
| `db.name` | Database name (usually `defaultdb` on Aiven) |
| `db.user` | Database user |
| `db.password` | Database password |

The file is listed in `.gitignore` — **never commit it**. Use `config.properties.example` as a template.

---

## Usage

### Log in

Open the app, enter your ID/username and password. The login logic tries:

1. **Intern** — matches on `intern_id`
2. **Supervisor** — matches on `username`
3. **Admin** — matches on `username`

Whichever succeeds opens that role's dashboard.

### Admin workflow

1. **Supervisors** tab → add a supervisor (e.g. `SUP001`, username `ali.khan`)
2. **Interns** tab → add an intern, select the supervisor from the dropdown
3. **Tasks** tab → create a task for that intern, then attach a PDF brief
4. When the intern submits work, use **View Submission** to download the PDF

### Supervisor workflow

1. Log in with supervisor credentials
2. **My Interns** shows only interns assigned to you
3. **Tasks** tab → create a task for one of your interns (the dropdown is limited to your own interns), then **Attach Task PDF**
4. **View Submission** downloads the intern's PDF

### Intern workflow

1. Log in with your intern ID and password
2. **My Tasks** shows tasks assigned to you
3. Select a task → **Download Task PDF** to get the brief
4. Select a task → **Submit Task** → pick a PDF, add an optional comment → submit
5. Submissions after the deadline are rejected; already-completed tasks cannot be re-submitted

---

## Project Structure

```
src/main/java/com/time/internmanagement/
├── Main.java
├── entity/
│   ├── Administrator.java
│   ├── Intern.java
│   ├── InternStatus.java
│   ├── Supervisor.java
│   ├── Task.java
│   ├── TaskPriority.java
│   └── TaskStatus.java
├── exception/
│   ├── DataBaseException.java
│   ├── DuplicateIdException.java
│   ├── InternNotFoundException.java
│   └── InvalidInputException.java
├── service/
│   ├── AuthService.java
│   ├── InternService.java
│   ├── SupervisorService.java
│   └── TaskService.java
├── ui/
│   ├── LoginFrame.java
│   ├── MainFrame.java
│   ├── SupervisorFrame.java
│   ├── InternDashboardFrame.java
│   ├── DashboardPanel.java
│   ├── InternPanel.java
│   ├── TaskPanel.java
│   ├── AdminSupervisorPanel.java
│   ├── SupervisorPanel.java
│   ├── SupervisorTaskPanel.java
│   └── UiTheme.java
└── util/
    ├── DataBaseManager.java
    ├── FileManager.java
    ├── PasswordUtils.java
    └── ValidationUtils.java
```

---

## Security Notes

This is a **course project**. Several simplifications were made for scope reasons. If you deploy this in production:

1. **Replace SHA-256 with BCrypt** or Argon2. SHA-256 is fast, which makes brute-forcing easier.
2. **Add a salt** to hashes if you keep SHA-256. Currently identical passwords produce identical hashes.
3. Database credentials are already kept out of version control (`config.properties` is in `.gitignore`, only `config.properties.example` is tracked) — keep it that way, and use environment variables or a secrets manager for any real deployment.
4. **Move PDFs out of the database** to object storage (S3, Supabase Storage) to avoid bloating the DB.
5. **Add input sanitisation** — email and phone validation exist but are basic.
6. **Enable row-level access control** if the app grows.

---

## Future Enhancements

- Email notifications when tasks are assigned or deadlines are near
- Report generation — export intern performance as PDF or Excel
- Support for multiple supervisors per intern
- Web version using Spring Boot + React
- Migrate PDF storage to S3-style object storage
- Switch password hashing to BCrypt
- Soft-delete for interns and supervisors (currently hard delete)

---

## License

This project is provided for educational purposes.