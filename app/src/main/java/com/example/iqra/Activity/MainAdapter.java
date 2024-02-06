package com.example.iqra.Activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.iqra.R;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class MainAdapter extends RecyclerView.Adapter<MainViewHolder> implements Filterable {
    private Context context;
    private List<File> pdfFiles;
    private List<File> pdfListFiltered;
    private OnPdfSelectListener listener;

    private HashSet<Integer> selectedItems = new HashSet<>();

    private boolean isSelectableMode = false;

//    private Set<Integer> selectedItems = new HashSet<>();

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
    public void onBindViewHolder(@NonNull MainViewHolder holder, int position) {
        holder.txtName.setText(pdfFiles.get(position).getName());
        holder.txtName.setSelected(true);
        holder.pdfSize.setText(getFileSize(pdfFiles.get(position)));
        loadFirstPageAsImage(pdfFiles.get(position), holder.pdfImageView);
//        holder.lastAccessDate.setText("");
//        holder.lastAccessDate.setText(getLastAccessDate(pdfFiles.get(position)));

        // Set click listener to handle item selection
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSelectableMode) {
                    // Toggle selection on single click
                    toggleSelection(position);
                } else {
                    // Handle normal click event
                    listener.onPdfSelected(pdfFiles.get(position));
                }
            }
        });

        // Set long press listener to enter selectable mode
        holder.cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                toggleSelection(position);
                enterSelectableMode();
                return true; // Consume the long press event
            }
        });

        // Update UI based on selection state
        boolean isSelected = selectedItems.contains(position);
        holder.cardView.setActivated(isSelected);
        holder.itemView.setBackgroundColor(isSelected ? ContextCompat.getColor(context, R.color.selected_card_color) : Color.TRANSPARENT);

        int textColor = isSelected ? Color.WHITE : Color.BLACK;
        holder.txtName.setTextColor(textColor);
        holder.pdfSize.setTextColor(textColor);

    }



//    @SuppressLint("ClickableViewAccessibility")
//    @Override
//    public void onBindViewHolder(@NonNull MainViewHolder holder, @SuppressLint("RecyclerView") int position) {
//        holder.txtName.setText(pdfFiles.get(position).getName());
//        holder.txtName.setSelected(true);
//        holder.pdfSize.setText(getFileSize(pdfFiles.get(position)));
//        loadFirstPageAsImage(pdfFiles.get(position), holder.pdfImageView);
////        holder.lastAccessDate.setText("");
////        holder.lastAccessDate.setText(getLastAccessDate(pdfFiles.get(position)));
//
//        holder.cardView.setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View v) {
//              listener.onPdfSelected(pdfFiles.get(position));
//            }
//        });
//
//        // Set long press listener to handle item selection
//        holder.cardView.setOnLongClickListener(v -> {
//            if (selectedItems.contains(position)) {
//                selectedItems.remove(position);
//            } else {
//                selectedItems.add(position);
//            }
//            notifyItemChanged(position);
//            updateShareOptionVisibility();
//            return true; // Consume the long press event
//        });
//
//        // Update UI based on selection state
//        holder.cardView.setActivated(selectedItems.contains(position));
//    }

    private void updateShareOptionVisibility() {
        boolean showShareOption = !selectedItems.isEmpty();
        ((MainActivity) context).showShareOption(showShareOption); // Show/hide share option in Toolbar
    }

    private void toggleSelection(int position) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position);
        } else {
            selectedItems.add(position);
        }
        notifyItemChanged(position);
        updateShareOptionVisibility();
    }

    private void enterSelectableMode() {
        isSelectableMode = true;
        notifyDataSetChanged(); // Refresh UI to reflect selectable mode
        updateShareOptionVisibility();
    }

    private void exitSelectableMode() {
        isSelectableMode = false;
        selectedItems.clear();
        notifyDataSetChanged(); // Refresh UI to exit selectable mode
        updateShareOptionVisibility();
    }

    // Method to share selected files
    void shareSelectedFiles() {
        // Gather selected files and share them via media
        List<File> selectedFiles = new ArrayList<>();
        for (Integer position : selectedItems) {
            selectedFiles.add(pdfFiles.get(position));
        }

        // Create a list of URIs for selected files
        ArrayList<Uri> fileUris = new ArrayList<>();
        for (File file : selectedFiles) {
            fileUris.add(FileProvider.getUriForFile(context, "com.example.iqra.fileprovider", file));
        }

        // Create an intent to share multiple files
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris);
        shareIntent.setType("*/*");
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Start an activity to share the selected files
        context.startActivity(Intent.createChooser(shareIntent, "Share files"));
    }

    private void loadFirstPageAsImage(File pdfFile, ImageView imageView) {
        try {
            // Create a PdfRenderer object
            PdfRenderer renderer = new PdfRenderer(ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY));

            // Open the first page of the PDF
            PdfRenderer.Page page = renderer.openPage(0);

            // Create a Bitmap to hold the image of the first page
            Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);

            // Render the first page of the PDF onto the Bitmap
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

            // Close the page and the renderer
            page.close();
            renderer.close();

            // Set the Bitmap as the image in the ImageView
            imageView.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
            // Handle any errors that occur while loading the PDF
        }
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
