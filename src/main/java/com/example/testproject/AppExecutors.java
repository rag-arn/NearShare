package com.example.testproject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppExecutors {

    private static final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();


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