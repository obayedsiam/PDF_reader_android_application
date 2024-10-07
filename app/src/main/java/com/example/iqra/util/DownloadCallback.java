package com.example.iqra.util;

public interface DownloadCallback {
    void onDownloadComplete();
    void onDownloadFailed(String errorMessage);
    void onProgressUpdate(int progress);
}

