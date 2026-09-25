package com.example.testproject;

public interface TransferListener {
    void onMessage(String message);
    void onProgress(double progress);
    void onComplete(String finalMessage);
    void onError(String errorMessage);
}