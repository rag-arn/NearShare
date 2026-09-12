package com.example.testproject;

public class HistoryRecord {
    private int serial;
    private String fileName;
    private String status;
    private String date;
    private String time;

    public HistoryRecord(int serial, String fileName, String status, String date, String time) {
        this.serial = serial;
        this.fileName = fileName;
        this.status = status;
        this.date = date;
        this.time = time;
    }

    // JavaFX TableView automatically looks for these exact "get" methods to populate columns
    public int getSerial() { return serial; }
    public String getFileName() { return fileName; }
    public String getStatus() { return status; }
    public String getDate() { return date; }
    public String getTime() { return time; }
}