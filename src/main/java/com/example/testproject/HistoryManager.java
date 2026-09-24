package com.example.testproject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

public class HistoryManager {

    private static final String LEGACY_HISTORY_DIR = System.getProperty("user.home") + File.separator + ".nearshare";
    private static final String LEGACY_HISTORY_FILE = LEGACY_HISTORY_DIR + File.separator + "history.txt";

    static {
        migrateLegacyHistoryIfNeeded();
    }

    public static void logTransfer(String status, String fileName, String peer) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"));

        // Offload write to the DB executor
        AppExecutors.getDbExecutor().execute(() -> {
            DatabaseManager.insertTransfer(status, fileName, peer, date, time);
        });
    }

    public static List<HistoryRecord> getHistory() {
        try {
            // Block and fetch safely on the DB thread to preserve the synchronous return signature
            return AppExecutors.getDbExecutor().submit(() -> DatabaseManager.getAllTransfers()).get();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public static void clearHistory() {
        AppExecutors.getDbExecutor().execute(() -> {
            DatabaseManager.clearAllTransfers();
        });
    }

    private static void migrateLegacyHistoryIfNeeded() {
        try {
            Path legacyPath = Paths.get(LEGACY_HISTORY_FILE);
            if (!Files.exists(legacyPath)) return;

            List<String> lines = Files.readAllLines(legacyPath);
            for (String line : lines) {
                String[] parts = line.split("\\|");
                if (parts.length >= 4) {
                    DatabaseManager.insertTransfer(parts[0], parts[1], "Unknown", parts[2], parts[3]);
                }
            }

            Files.move(legacyPath, Paths.get(LEGACY_HISTORY_FILE + ".bak"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}