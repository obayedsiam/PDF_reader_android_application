package com.example.iqra.Activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iqra.R;
import com.example.iqra.entity.Book;
import com.example.iqra.util.DownloadCallback;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.IOException;

public class MainAdapter extends RecyclerView.Adapter<MainViewHolder> implements Filterable {
    private Context context;
    private List<File> pdfFiles;
    private List<File> adapterUnDownloadedList;
    private List<File> pdfListFiltered;
    private OnPdfSelectListener listener;
    private List<File> selectedFiles = new ArrayList<>(); // To track selected files
    private boolean isSelectionMode = false; // To track if we're in selection mode

    private SparseBooleanArray selectedItems = new SparseBooleanArray();  // To track selected items

    public MainAdapter(Context context, List<File> pdfFiles, OnPdfSelectListener listener) {
        this.context = context;
        this.pdfFiles = pdfFiles;
        this.pdfListFiltered = pdfFiles;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MainViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MainViewHolder(LayoutInflater.from(context).inflate(R.layout.rv_item, parent, false));
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull MainViewHolder holder, @SuppressLint("RecyclerView") int position) {
        File pdfFile = pdfFiles.get(position);
        holder.txtName.setText(pdfFiles.get(position).getName());
        holder.txtName.setSelected(true);
        holder.pdfSize.setText(getFileSize(pdfFiles.get(position)));

        // Highlight if the item is selected
        holder.itemView.setSelected(selectedItems.get(position, false));

        holder.cardView.setOnClickListener(v -> {
            checkDownloadAndOpen(position);
        });

        holder.cardView.setOnClickListener(v -> {
            if (isSelectionMode) {
                toggleSelection(pdfFile);
            } else {
                checkDownloadAndOpen(position);
            }
        });

        holder.cardView.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                isSelectionMode = true;
                toggleSelection(pdfFile);
            }
            return true;
        });

        if(((MainActivity)context).unDownloadedFileList.contains(pdfFile)){
            holder.imageView.setImageResource(R.drawable.undownloaded_image_foreground);
        }
        else{
            Bitmap pdfFirstPage = null;
            try {
                pdfFirstPage = renderFirstPage(pdfFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
            holder.imageView.setImageBitmap(pdfFirstPage);
        }

        // Show selected state visually (e.g., background color change)
        if (selectedFiles.contains(pdfFile)) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, androidx.cardview.R.color.cardview_dark_background));  // Set background color
            holder.txtName.setTextColor(ContextCompat.getColor(context, android.R.color.white));  // Set text color to white
            holder.pdfSize.setTextColor(ContextCompat.getColor(context, android.R.color.white));  // If you want to change the size text too

        } else {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
            holder.txtName.setTextColor(ContextCompat.getColor(context, android.R.color.black));  // Set text color to white
            holder.pdfSize.setTextColor(ContextCompat.getColor(context, android.R.color.black));
        }

    }

    public Bitmap renderFirstPage(File pdfFile) throws IOException {
        ParcelFileDescriptor fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
        PdfRenderer pdfRenderer = new PdfRenderer(fileDescriptor);

        PdfRenderer.Page page = pdfRenderer.openPage(0); // Open the first page
        Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        // Close the page and PDFRenderer
        page.close();
        pdfRenderer.close();
        fileDescriptor.close();

        return bitmap;
    }


//    public void checkDownloadAndOpen(int position) {
//        File currentFile = pdfFiles.get(position); // The clicked file
//
//        // Check if the file exists in the list of undownloaded files
//        boolean isDownloaded = !((MainActivity)context).unDownloadedFileList.contains(currentFile.getName());
//
//        if (!isDownloaded) {
//            // If not downloaded, show the download dialog
//            System.out.println(currentFile.getName() + " is not downloaded");
//            boolean isDownloadCompleted = ((MainActivity)context).showDownloadDialog(currentFile);
//
//            // If download completed successfully, proceed to open the file
//            if (isDownloadCompleted) {
//                listener.onPdfSelected(currentFile);
//            }
//        } else {
//            // If the file is already downloaded, open it directly
//            System.out.println(currentFile.getName() + " is already downloaded");
//            listener.onPdfSelected(currentFile);
//        }
//    }

    public void checkDownloadAndOpen(int position) {
        File currentFile = pdfFiles.get(position);

        boolean isDownloaded = true;
        for (File unDownloadedFile : ((MainActivity) context).unDownloadedFileList) {
            System.out.println("Iterating " + unDownloadedFile.getName());
            if (unDownloadedFile.getName().equals(currentFile.getName())) {
                System.out.println(currentFile + " is not downloaded");
                isDownloaded = false;
                break;
            }
        }

        if (!isDownloaded) {
            // Show the download dialog first
            System.out.println(currentFile + " going for download");
            boolean isDownloadCompleted = ((MainActivity) context).showDownloadDialog(currentFile);
            if (isDownloadCompleted) {
                listener.onPdfSelected(currentFile);
            }
        } else {
            // If the file is already downloaded, proceed to open it
            System.out.println(currentFile.getName() + " is already downloaded");
            listener.onPdfSelected(currentFile);
        }
    }

    // Toggle selection of PDF file
    @SuppressLint("NotifyDataSetChanged")
    private void toggleSelection(File file) {
        if (selectedFiles.contains(file)) {
            selectedFiles.remove(file);
            if (selectedFiles.size() == 0) isSelectionMode = false;
        } else {
            selectedFiles.add(file);
        }

        notifyDataSetChanged(); // Update UI for selection
        ((MainActivity) context).updateToolbarState(selectedFiles); // Notify activity to update toolbar
    }

    public List<File> getSelectedFiles() {
        List<File> selectedFiles = new ArrayList<>();
        for (int i = 0; i < selectedItems.size(); i++) {
            int key = selectedItems.keyAt(i);
            selectedFiles.add(pdfFiles.get(key));
        }
        return selectedFiles;
    }

    public void clearSelection() {
        selectedItems.clear();
        notifyDataSetChanged();
        isSelectionMode = false;
    }

    public String getLastAccessDate(File file) {
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

    public String getFileSize(File file) {
        long fileSizeBytes = file.length();
        if (fileSizeBytes == 0.0) return "Download";
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

    public boolean isSelectionMode() {
        return isSelectionMode;
    }
}
