package com.example.testproject;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private static final String DB_DIR = System.getProperty("user.home") + File.separator + ".nearshare";
    private static final String DB_FILE = DB_DIR + File.separator + "history.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE;

    private static Connection connection;

    private static synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            File dir = new File(DB_DIR);
            if (!dir.exists()) dir.mkdirs();
            connection = DriverManager.getConnection(DB_URL);
            initializeTable(connection);
        }
        return connection;
    }

    private static void initializeTable(Connection conn) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS transfers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "status TEXT NOT NULL," +
                "file_name TEXT NOT NULL," +
                "peer TEXT NOT NULL," +
                "date TEXT NOT NULL," +
                "time TEXT NOT NULL," +
                "UNIQUE(status, file_name, peer, date, time)" +
                ")";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    public static synchronized void insertTransfer(String status, String fileName, String peer, String date, String time) {
        String sql = "INSERT OR IGNORE INTO transfers (status, file_name, peer, date, time) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, fileName);
            ps.setString(3, peer);
            ps.setString(4, date);
            ps.setString(5, time);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static synchronized List<HistoryRecord> getAllTransfers() {
        List<HistoryRecord> records = new ArrayList<>();
        String sql = "SELECT status, file_name, date, time FROM transfers ORDER BY id DESC";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            int serial = 1;
            while (rs.next()) {
                records.add(new HistoryRecord(
                        serial++,
                        rs.getString("file_name"),
                        rs.getString("status"),
                        rs.getString("date"),
                        rs.getString("time")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return records;
    }

    public static synchronized void clearAllTransfers() {
        String sql = "DELETE FROM transfers";
        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}