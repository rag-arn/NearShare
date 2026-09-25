package com.example.testproject;

public abstract class TransferTask implements Runnable {
    protected final int PORT = 8080;
    protected final int BUFFER_SIZE = 4096;
    protected TransferListener listener;

    public TransferTask(TransferListener listener) {
        this.listener = listener;
    }

    // Abstract method forcing child classes to define their specific network behavior
    protected abstract void executeTransfer() throws Exception;

    @Override
    public void run() {
        try {
            executeTransfer();
        } catch (Exception e) {
            if (listener != null) {
                listener.onError("Transfer Error: " + e.getMessage());
            }
        }
    }
}