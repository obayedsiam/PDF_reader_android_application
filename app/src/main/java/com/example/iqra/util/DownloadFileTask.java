package com.example.iqra.util;

import android.os.AsyncTask;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadFileTask extends AsyncTask<String, Integer, String> {
    private ProgressBar progressBar;
    private TextView tvProgress;

    public DownloadFileTask(ProgressBar progressBar, TextView tvProgress) {
        this.progressBar = progressBar;
        this.tvProgress = tvProgress;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressBar.setProgress(0);  // Initialize progress
        tvProgress.setText("0%");
    }

    @Override
    protected String doInBackground(String... params) {
        String fileUrl = params[0];  // The URL of the file to download
        String fileName = params[1]; // File name
        String saveDir = params[2];  // Directory to save file

        try {
            URL url = new URL(fileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            // Get file size
            int fileLength = connection.getContentLength();

            // Input stream to read file
            InputStream input = new BufferedInputStream(connection.getInputStream());

            // Output stream to write file
            OutputStream output = new FileOutputStream(saveDir + "/" + fileName);

            byte[] data = new byte[4096];
            long total = 0;
            int count;

            while ((count = input.read(data)) != -1) {
                total += count;

                // Calculate progress percentage and publish it
                if (fileLength > 0) {
                    int progress = (int) (total * 100 / fileLength);
                    publishProgress(progress);
                }

                output.write(data, 0, count);
            }

            // Close streams
            output.flush();
            output.close();
            input.close();

        } catch (Exception e) {
            e.printStackTrace();
            return "Failed";
        }

        return "Downloaded";
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        super.onProgressUpdate(progress);
        progressBar.setProgress(progress[0]);
        tvProgress.setText(progress[0] + "%");  // Update TextView with percentage
    }

    @Override
    protected void onPostExecute(String result) {
        if (result.equals("Downloaded")) {
            tvProgress.setText("Download Complete");
        } else {
            tvProgress.setText("Download Failed");
        }
    }
}
