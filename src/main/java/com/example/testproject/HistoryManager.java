package com.example.testproject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistoryManager {

    private static final String HISTORY_DIR = System.getProperty("user.home") + File.separator + ".nearshare";
    private static final String HISTORY_FILE = HISTORY_DIR + File.separator + "history.txt";

    public static void logTransfer(String status, String fileName, String peer) {
        try {
            File dir = new File(HISTORY_DIR);
            if (!dir.exists()) dir.mkdirs();

            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"));

            // Format: Status|FileName|Date|Time
            String logEntry = String.format("%s|%s|%s|%s\n", status, fileName, date, time);

            Files.write(Paths.get(HISTORY_FILE), logEntry.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<HistoryRecord> getHistory() {
        List<HistoryRecord> history = new ArrayList<>();
        try {
            Path path = Paths.get(HISTORY_FILE);
            if (Files.exists(path)) {
                List<String> lines = Files.readAllLines(path);

                // Reverse the lines so the most recent is at the top
                Collections.reverse(lines);

                int serialCounter = 1;
                for (String line : lines) {
                    String[] parts = line.split("\\|");
                    // Make sure it matches our new 4-part format to prevent crashing on old logs
                    if (parts.length >= 4) {
                        // Parts mapping: [0]=Status, [1]=FileName, [2]=Date, [3]=Time
                        history.add(new HistoryRecord(serialCounter++, parts[1], parts[0], parts[2], parts[3]));
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return history;
    }

    public static void clearHistory() {
        try {
            Files.deleteIfExists(Paths.get(HISTORY_FILE));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}