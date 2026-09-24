package com.example.testproject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppExecutors {
    // Single-thread executor guarantees database operations run sequentially, preventing SQLite locks
    private static final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    // Cached thread pool reuses threads for network operations and scales as needed
    private static final ExecutorService generalExecutor = Executors.newCachedThreadPool();

    public static ExecutorService getDbExecutor() {
        return dbExecutor;
    }

    public static ExecutorService getGeneralExecutor() {
        return generalExecutor;
    }

    public static void shutdown() {
        dbExecutor.shutdown();
        generalExecutor.shutdown();
    }
}