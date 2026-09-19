package com.time.internmanagement.util;

import java.time.LocalDate;
import java.util.regex.Pattern;

public class ValidationUtils {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // Prevent instantiation — this is a pure utility class
    private ValidationUtils() {
    }

    public static boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        if (isNullOrEmpty(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return false;
        }
        return !endDate.isBefore(startDate);
    }

    public static boolean isValidDeadline(LocalDate assignedDate, LocalDate deadline) {
        if (assignedDate == null || deadline == null) {
            return false;
        }
        return !deadline.isBefore(assignedDate);
    }

    public static boolean isValidPhoneNumber(String phone) {
        if (isNullOrEmpty(phone)) {
            return false;
        }
        // Allows digits, spaces, +, -, parentheses; length check keeps it reasonable
        return phone.matches("^[0-9+\\-() ]{7,15}$");
    }
}