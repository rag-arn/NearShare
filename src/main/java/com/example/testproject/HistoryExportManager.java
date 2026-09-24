package com.example.testproject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryExportManager {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public static void exportToFile(File file) throws IOException {
        List<HistoryRecord> records = HistoryManager.getHistory();

        Map<String, Object> envelope = new HashMap<>();
        envelope.put("exportedFrom", DiscoveryManager.myDeviceName);
        envelope.put("exportedAt", System.currentTimeMillis());
        envelope.put("records", records);

        MAPPER.writeValue(file, envelope);
    }

    @SuppressWarnings("unchecked")
    public static int importFromFile(File file) throws IOException {
        Map<String, Object> envelope = MAPPER.readValue(file, Map.class);
        List<Map<String, Object>> rawRecords = (List<Map<String, Object>>) envelope.get("records");

        int count = 0;
        if (rawRecords != null) {
            for (Map<String, Object> r : rawRecords) {
                String fileName = String.valueOf(r.get("fileName"));
                String status = String.valueOf(r.get("status"));
                String date = String.valueOf(r.get("date"));
                String time = String.valueOf(r.get("time"));
                // Peer isn't in HistoryRecord's JSON shape, so we mark it as imported.
                DatabaseManager.insertTransfer(status, fileName, "Imported", date, time);
                count++;
            }
        }
        return count;
    }
}