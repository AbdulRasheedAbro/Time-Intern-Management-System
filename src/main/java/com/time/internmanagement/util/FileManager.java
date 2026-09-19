package com.time.internmanagement.util;

import com.time.internmanagement.entity.Intern;
import com.time.internmanagement.entity.Task;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileManager {

    private static final String DATA_DIR = "data";

    private static final String INTERNS_FILE =
            DATA_DIR + File.separator + "interns.csv";

    private static final String TASKS_FILE =
            DATA_DIR + File.separator + "tasks.csv";


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public FileManager() {
        ensureDataDirectoryExists();
    }


    // =========================================================
    // DATA DIRECTORY
    // =========================================================

    private void ensureDataDirectoryExists() {

        try {

            Path directoryPath = Paths.get(DATA_DIR);

            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }

        } catch (IOException e) {

            System.err.println(
                    "Failed to create data directory: "
                            + e.getMessage()
            );
        }
    }


    // =========================================================
    // INTERN CSV OPERATIONS
    // =========================================================

    public void saveInterns(List<Intern> interns) {

        if (interns == null) {
            return;
        }

        try (BufferedWriter writer =
                     new BufferedWriter(
                             new FileWriter(INTERNS_FILE))) {

            for (Intern intern : interns) {

                if (intern == null) {
                    continue;
                }

                writer.write(intern.toCsvLine());
                writer.newLine();
            }

        } catch (IOException e) {

            System.err.println(
                    "Error saving interns: "
                            + e.getMessage()
            );
        }
    }


    public List<Intern> loadInterns() {

        List<Intern> interns = new ArrayList<>();

        File file = new File(INTERNS_FILE);

        if (!file.exists()) {
            return interns;
        }

        try (BufferedReader reader =
                     new BufferedReader(
                             new FileReader(file))) {

            String line;

            while ((line = reader.readLine()) != null) {

                if (line.trim().isEmpty()) {
                    continue;
                }

                try {

                    Intern intern =
                            Intern.fromCsvLine(line);

                    if (intern != null) {
                        interns.add(intern);
                    }

                } catch (Exception e) {

                    System.err.println(
                            "Skipping malformed intern record: "
                                    + line
                    );
                }
            }

        } catch (IOException e) {

            System.err.println(
                    "Error loading interns: "
                            + e.getMessage()
            );
        }

        return interns;
    }


    // =========================================================
    // TASK CSV OPERATIONS
    // =========================================================

    public void saveTasks(List<Task> tasks) {

        if (tasks == null) {
            return;
        }

        try (BufferedWriter writer =
                     new BufferedWriter(
                             new FileWriter(TASKS_FILE))) {

            for (Task task : tasks) {

                if (task == null) {
                    continue;
                }

                writer.write(task.toCsvLine());
                writer.newLine();
            }

        } catch (IOException e) {

            System.err.println(
                    "Error saving tasks: "
                            + e.getMessage()
            );
        }
    }


    public List<Task> loadTasks() {

        List<Task> tasks = new ArrayList<>();

        File file = new File(TASKS_FILE);

        if (!file.exists()) {
            return tasks;
        }

        try (BufferedReader reader =
                     new BufferedReader(
                             new FileReader(file))) {

            String line;

            while ((line = reader.readLine()) != null) {

                if (line.trim().isEmpty()) {
                    continue;
                }

                try {

                    Task task =
                            Task.fromCsvLine(line);

                    if (task != null) {
                        tasks.add(task);
                    }

                } catch (Exception e) {

                    System.err.println(
                            "Skipping malformed task record: "
                                    + line
                    );
                }
            }

        } catch (IOException e) {

            System.err.println(
                    "Error loading tasks: "
                            + e.getMessage()
            );
        }

        return tasks;
    }
}