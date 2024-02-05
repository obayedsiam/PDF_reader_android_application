package com.example.iqra.Activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iqra.R;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainAdapter extends RecyclerView.Adapter<MainViewHolder> implements Filterable {
    private Context context;
    private List<File> pdfFiles;
    private List<File> pdfListFiltered;
    private OnPdfSelectListener listener;

    public MainAdapter(Context context, List<File> pdfFiles, OnPdfSelectListener listener) {
        this.context = context;
        this.pdfFiles = pdfFiles;
        this.pdfListFiltered = pdfFiles;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MainViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MainViewHolder(LayoutInflater.from(context).inflate(R.layout.rv_item,parent,false));
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull MainViewHolder holder, @SuppressLint("RecyclerView") int position) {
        holder.txtName.setText(pdfFiles.get(position).getName());
        holder.txtName.setSelected(true);
        holder.pdfSize.setText(getFileSize(pdfFiles.get(position)));
//        holder.lastAccessDate.setText("");
//        holder.lastAccessDate.setText(getLastAccessDate(pdfFiles.get(position)));

        holder.cardView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
              listener.onPdfSelected(pdfFiles.get(position));
            }
        });

        holder.cardView.setOnTouchListener(new View.OnTouchListener() {
            private final Handler handler = new Handler();
            private boolean isLongPress = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        handler.postDelayed(longPressRunnable, 2000); // 2 seconds
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        handler.removeCallbacks(longPressRunnable);
                        if (!isLongPress) {
                            // Perform normal click action
                        }
                        break;
                }
                return true;
            }

            private Runnable longPressRunnable = new Runnable() {
                @Override
                public void run() {
                    isLongPress = true;
                    holder.itemView.setSelected(true);
                    System.out.println("Selection True");
                }
            };
        });

    }

    public String getLastAccessDate(File file){
        // Get the last modified time of the file
        long lastModified = file.lastModified();

     return String.valueOf(formatDate(lastModified));
    }

    private static String formatFileSize(long sizeBytes) {
        DecimalFormat df = new DecimalFormat("#.##");
        double sizeKB = sizeBytes / 1024.0; // Convert bytes to KB
        if (sizeKB < 1024) {
            return df.format(sizeKB) + " KB";
        } else {
            double sizeMB = sizeKB / 1024.0; // Convert KB to MB
            return df.format(sizeMB) + " MB";
        }
    }

    public String getFileSize(File file){
        long fileSizeBytes = file.length();
        return formatFileSize(fileSizeBytes);
    }

    private static String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy, hh:mm a", Locale.ENGLISH);
        return sdf.format(new Date(timestamp));
    }

    @Override
    public int getItemCount() {
        return pdfFiles.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String query = constraint.toString().toLowerCase();

                List<File> filteredList = new ArrayList<>();

                for (File pdfFile : pdfFiles) {
                    if (pdfFile.getName().toLowerCase().contains(query)) {
                        filteredList.add(pdfFile);
                    }
                }

                FilterResults results = new FilterResults();
                results.count = filteredList.size();
                results.values = filteredList;

                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                pdfFiles.clear();
                pdfFiles.addAll((List) results.values);
                notifyDataSetChanged();
            }
        };
    }
}
