package com.example.iqra.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

import com.example.iqra.entity.Book;
import com.example.iqra.entity.Link;
import com.example.iqra.helper.DatabaseHelper;
import com.example.iqra.util.DownloadCallback;
import com.example.iqra.util.FileHashUtils;
import com.google.firebase.FirebaseApp;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.iqra.BuildConfig;
import com.example.iqra.R;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements OnPdfSelectListener {

    private MainAdapter adapter;
    private List<File> pdfList = new ArrayList<>();
    public List<File> unDownloadedFileList = new ArrayList<>();
    private List<File> selectedPdfFiles = new ArrayList<>();
    private RecyclerView recyclerView;
    private String localBookSavedFolderName = "IQRA_PDFS";
    private ActionMode actionMode;
    private ActionModeCallback actionModeCallback = new ActionModeCallback();
    Map<String, Book> firebaseBookList = new HashMap<>();
    Map<String, Link> linkList = new HashMap<>();
    Set<String> categoryFilter = new HashSet<>();
    private Handler handler = new Handler();
    private Runnable runnable;
    private boolean downloadCompleted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.loader); // Show loader layout initially

        FirebaseApp.initializeApp(this);
        DatabaseReference booksRef = FirebaseDatabase.getInstance().getReference("Book");
        DatabaseReference linkReference = FirebaseDatabase.getInstance().getReference("Link");

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        booksRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot categorySnapshot : dataSnapshot.getChildren()) {
                    String categoryName = categorySnapshot.getKey(); // e.g., "categoryA"
                    for (DataSnapshot bookSnapshot : categorySnapshot.getChildren()) {
                        Long bookId = bookSnapshot.child("id").getValue(Long.class);
                        String bookName = bookSnapshot.child("name").getValue(String.class);
                        String bookUrl = bookSnapshot.child("url").getValue(String.class);
                        String bookHash = bookSnapshot.child("hash").getValue(String.class);
                        String bookCategory = bookSnapshot.child("category").getValue(String.class);
                        categoryFilter.add(bookCategory);
                        Book book = new Book(bookId, bookName, bookCategory, bookUrl, bookHash);
                        firebaseBookList.put(bookName, book);
                        dbHelper.getWritableDatabase();
                        dbHelper.saveBookToLocalDatabase(book, db);
                    }
                }
                addFilterToDatabaseIfNeeded(MainActivity.this);
                checkAndDownloadBooks(firebaseBookList);
                loadPdfFilesFromDirectory();
                displayPdf("");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        linkReference.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot linkSnapshot : dataSnapshot.getChildren()) {
                    Long linkId = linkSnapshot.child("id").getValue(Long.class);
                    String linkName = linkSnapshot.child("name").getValue(String.class);
                    String linkUrl = linkSnapshot.child("url").getValue(String.class);
                    Link link = new Link(linkId, linkName, linkUrl);
                    linkList.put(linkName, link);
                    //saveBookToLocalDatabase(categoryName, bookId, bookName, bookUrl);

                }
//                checkAndDownloadBooks(firebaseBookList);
//                loadPdfFilesFromDirectory();
                displayPdf("");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        // Post a delayed action to switch to the main activity layout after a certain time
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                // Set the main activity layout
                setContentView(R.layout.activity_main);

                // Initialize toolbar and navigation drawer if no action bar is set
                if (getSupportActionBar() == null) {
                    Toolbar toolbar = findViewById(R.id.toolbar);
                    setSupportActionBar(toolbar);

                    DrawerLayout drawer = findViewById(R.id.drawer_layout);
                    ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                            MainActivity.this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
                    drawer.addDrawerListener(toggle);
                    toggle.syncState();

                    NavigationView navigationView = findViewById(R.id.navigationView);
                    navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
                        @Override
                        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                            // Handle navigation view item clicks here
                            switch (item.getItemId()) {
                                case R.id.share_link:
                                    shareLink();
                                    return true;
                                case R.id.nav_settings:
                                    // Handle settings click
                                    return true;
                                default:
                                    return false;
                            }
                        }
                    });
                }
                loadPdfFilesFromDirectory();
                displayPdf("");
                // Request runtime permissions
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                    runtimePermission();
                }
            }
        }, 500); // Change YOUR_DELAY_TIME to your desired delay time in milliseconds
    }


//    public void showCircularLoader(String bookName) {
//        // Inflate the custom loader layout
//        LayoutInflater inflater = getLayoutInflater();
//        View loaderView = inflater.inflate(R.layout.loader_layout, null);
//
//        // Create a dialog for the loader
//        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//        builder.setView(loaderView);
//        builder.setCancelable(false); // Prevent closing the loader by clicking outside
//
//        // Get the TextView from the inflated layout and set the message
//        TextView tvProgressMessage = loaderView.findViewById(R.id.tvProgressMessage);
//        tvProgressMessage.setText("Downloading " + bookName + ", please wait...");
//
//        // Create and show the dialog
//        AlertDialog loaderDialog = builder.create();
//        loaderDialog.show();
//
//        // Call your download method and dismiss the dialog once the download completes
//        downloadBookFromFirebase(bookUrl, bookName, new DownloadCallback() {
//            @Override
//            public void onDownloadComplete() {
//                loaderDialog.dismiss();  // Dismiss the loader when download is complete
//                Toast.makeText(MainActivity.this, bookName + " downloaded!", Toast.LENGTH_SHORT).show();
//                loadPdfFilesFromDirectory();  // Refresh files
//            }
//
//            @Override
//            public void onDownloadFailed(String errorMessage) {
//                loaderDialog.dismiss();  // Dismiss the loader if download fails
//                Toast.makeText(MainActivity.this, "Failed to download " + bookName + ": " + errorMessage, Toast.LENGTH_SHORT).show();
//            }
//
//            @Override
//            public void onProgressUpdate(int progress) {
//                // Optional: Update the message if you want to show progress
//                tvProgressMessage.setText("Downloading " + bookName + ", " + progress + "% complete...");
//            }
//        });
//    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    public void addFilterToDatabaseIfNeeded(Context context) {
        // Initialize the database helper
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase(); // Get writeable database instance

        // Iterate through the Set of categories
        for (String category : categoryFilter) {

            System.out.println("Calling category database from addFilterToDatabaseIfNeeded");
            // Check if category already exists
            Cursor cursor = db.rawQuery("SELECT * FROM Categories WHERE name = ?", new String[]{category});
            System.out.println("Calling successfully category database from addFilterToDatabaseIfNeeded");
            if (!cursor.moveToFirst()) {
                // If not found, insert it with isSelected as true (1)
                ContentValues values = new ContentValues();
                values.put("name", category);
                values.put("isSelected", 1);  // True = 1, False = 0
                db.insert("Categories", null, values);
            }
            cursor.close();
        }

        db.close(); // Always close the database when done
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_filter:
                showFilterDialog();
                return true;
            // Handle other menu items
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Filter List");

        // Inflate the filter dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter, null);
        builder.setView(dialogView);

        LinearLayout switchContainer = dialogView.findViewById(R.id.switchContainer);
        Button buttonCancel = dialogView.findViewById(R.id.buttonCancel);
        Button buttonApply = dialogView.findViewById(R.id.buttonApply);

        // Convert the categoryFilter Set to an ArrayList for easier manipulation
        ArrayList<String> categoriesList = new ArrayList<>(categoryFilter);
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        boolean isAllCategorySame = dbHelper.areAllCategoriesSame();
        // Create Switches for each category
        for (String category : categoriesList) {
            // Create Switch

            Switch switchView = new Switch(this);
            switchView.setText(category);
            switchView.setTextOn(category);
            switchView.setTextOff(category);

            if (!isAllCategorySame) {
                boolean isSelected = dbHelper.isCategorySelected(category);
                switchView.setChecked(isSelected);
            }

            switchContainer.addView(switchView);

            // Create a line separator
            View line = new View(this);
            line.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    2 // Height of the line
            ));
            line.setBackgroundColor(Color.GRAY); // Change color as needed
            switchContainer.addView(line);
        }

        // Create the dialog
        AlertDialog dialog = builder.create();

        // Handle Cancel Button
        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        // Handle Apply Button
        buttonApply.setOnClickListener(v -> {
            Set<String> selectedCategories = new HashSet<>();
            for (int i = 0; i < switchContainer.getChildCount(); i++) {
                View view = switchContainer.getChildAt(i);
                if (view instanceof Switch) {
                    Switch switchView = (Switch) view;
                    if (switchView.isChecked()) {
                        selectedCategories.add(switchView.getText().toString());
                        Toast.makeText(MainActivity.this, switchView.getText().toString() + " checked", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            // Call your filter application method here

            applyFilter(selectedCategories);
            displayPdf("");
            dialog.dismiss();
        });

        // Show the dialog after setting up the listeners
        dialog.show();
    }

    private void applyFilter(Set<String> selectedCategories) {
        // Initialize dbHelper (if not already done)
        DatabaseHelper dbHelper = new DatabaseHelper(this);
//        Toast.makeText(MainActivity.this, "Applying filter", Toast.LENGTH_SHORT).show();

        // Fetch all categories from the database
        System.out.println("Calling all categories from DB");
        List<String> allCategories = new ArrayList<>();

        try {
            allCategories = dbHelper.getAllCategories();
//            System.out.println("All categories found");
        } catch (Exception e) {
            System.out.println("Error while reading values from DB " + e.getMessage());
        }


        for (String category : allCategories) {
            if (selectedCategories.contains(category)) {
                // Mark selected categories as isSelected = true
//                System.out.println("Filter selected category : "+category);
                dbHelper.updateCategorySelection(category, true);
//                System.out.println("Filter selected category updated true "+category);
            } else {
                // Mark other categories as isSelected = false
                dbHelper.updateCategorySelection(category, false);
//                System.out.println("Filter selected category updated false "+category);
            }
        }
    }


    private void shareLink() {
        // Inflate the custom dialog layout
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_share_links, null);
        builder.setView(dialogView);

        LinearLayout linkContainer = dialogView.findViewById(R.id.link_container);
        Button buttonCancel = dialogView.findViewById(R.id.button_cancel);
        Button buttonSend = dialogView.findViewById(R.id.button_send);

        // Populate the dialog with CheckBox for each link name
        for (Map.Entry<String, Link> entry : linkList.entrySet()) {
            Link link = entry.getValue();

            // Create a new CheckBox for each link name
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(link.getName());
            checkBox.setTag(link); // Store the Link object as a tag
            linkContainer.addView(checkBox);
        }

        AlertDialog dialog = builder.create();

        // Set up the Cancel button
        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        // Set up the Send button
        buttonSend.setOnClickListener(v -> {
            StringBuilder shareContent = new StringBuilder();

            for (int i = 0; i < linkContainer.getChildCount(); i++) {
                View view = linkContainer.getChildAt(i);
                if (view instanceof CheckBox) {
                    CheckBox checkBox = (CheckBox) view;
                    if (checkBox.isChecked()) {
                        Link link = (Link) checkBox.getTag();
                        // Concatenate link name and URL
                        shareContent.append(link.getName()).append(" : ").append(link.getUrl()).append("\n");
                    }
                }
            }

            // Convert StringBuilder to String
            String shareText = shareContent.toString().trim();

            // Call your share method with the combined string
            shareSelectedLinks(shareText);
            dialog.dismiss();
        });

        dialog.show();
    }

    // Method to get Link object by name
    private Link getLinkByName(String name) {
        for (Link link : linkList.values()) {
            if (link.getName().equals(name)) {
                return link;
            }
        }
        return null;
    }

    private void shareSelectedLinks(String shareText) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Share Links"));
    }

    public void shareSelectedFiles() {
        if (selectedPdfFiles != null && !selectedPdfFiles.isEmpty()) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.setType("*/*");  // More general MIME type to show more apps

            ArrayList<Uri> files = new ArrayList<>();
            for (File pdf : selectedPdfFiles) {
                Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", pdf);
                files.add(uri);
            }

            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);  // Grant URI read permissions

            startActivity(Intent.createChooser(shareIntent, "Share PDF files"));

            ((MainAdapter) recyclerView.getAdapter()).clearSelection();
        }
    }

    public void updateToolbarState(List<File> selectedFiles) {
        selectedPdfFiles = selectedFiles;

        if (selectedFiles.size() > 0) {
            if (actionMode == null) {
                // Start the ActionMode if it's not already started
                actionMode = startSupportActionMode(actionModeCallback);
            }
            // Update the ActionMode title with the number of selected files
            actionMode.setTitle(selectedFiles.size() + " selected");
        } else if (actionMode != null) {
            // No files selected, finish the ActionMode
            actionMode.finish();
            ((MainAdapter) recyclerView.getAdapter()).clearSelection();
        }
    }

    // Override onBackPressed to clear selection
    @Override
    public void onBackPressed() {
        if (((MainAdapter) recyclerView.getAdapter()).isSelectionMode()) {
            System.out.println(" Back Pressed #################33");
        } else {
            super.onBackPressed();
        }
        ((MainAdapter) recyclerView.getAdapter()).clearSelection();
    }

    private void checkAndDownloadBooks(Map<String, Book> firebaseBookList) {
        // Define the "IQRA_PDFS" folder inside the Downloads directory
        System.out.println("Checking downloadable files .....");

        boolean isNewBookDownloaded = false;
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File iqraPdfDir = new File(downloadsDir, localBookSavedFolderName);

        // If directory does not exist, create it
        if (!iqraPdfDir.exists()) {
            iqraPdfDir.mkdirs();
        }

        for (Map.Entry<String, Book> entry : firebaseBookList.entrySet()) {

            String bookName = entry.getKey();  // The key is the book name
            Book book = entry.getValue();
            String bookUrl = book.getBookUrl(); // The value is the URL of the book
            String hash = book.getBookHash();

            // Define the local file path for this book
            File localFile = new File(iqraPdfDir, bookName + ".pdf");  // Using book name to create file name

            if (localFile.exists()) {
                // File already exists, skip downloading
                boolean isCorrupted = isCorrupted(localFile, hash);
                if (isCorrupted) {
                    System.out.println(localFile.getName() + "is corrupted");
                    localFile.delete();
                    unDownloadedFileList.add(localFile);

                    System.out.println(localFile.getName() + " File added to undlownlaoded list");

                }
//                System.out.println(bookName + " : File already exists");
            } else {
                System.out.println(localFile.getName() + " File doesn't exists");
                localFile.delete();
                unDownloadedFileList.add(localFile);
                System.out.println(localFile.getName() + " File added to undlownlaoded list");

            }
        }

        System.out.println("Undownloaded File size " + unDownloadedFileList.size());
    }

    private void saveBookToLocalDatabase(Book book) {

    }

    public void downloadBookFromFirebase(String fileUrl, String bookName, DownloadCallback callback) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReferenceFromUrl(fileUrl); // Use the URL to get the StorageReference

        // Define the "IQRA_PDFS" folder inside the Downloads directory
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File iqraPdfDir = new File(downloadsDir, localBookSavedFolderName); // You can use a constant or variable for the folder name

        // Create the directory if it doesn't exist
        if (!iqraPdfDir.exists()) {
            iqraPdfDir.mkdirs();  // Create directory and any necessary parent directories
        }

        // Define the file inside the IQRA_PDFS folder
        File localFile = new File(iqraPdfDir, bookName + ".pdf");

        // Download the file from Firebase Storage
        storageRef.getFile(localFile)
                .addOnProgressListener(taskSnapshot -> {
                    // Calculate the download progress as a percentage
                    long bytesTransferred = taskSnapshot.getBytesTransferred();
                    long totalBytes = taskSnapshot.getTotalByteCount();
                    int progress = (int) ((bytesTransferred * 100) / totalBytes);
                    System.out.println("Progress : " + progress);
                    // Call the progress update method in the callback
                    callback.onProgressUpdate(progress);
                })
                .addOnSuccessListener(taskSnapshot -> {
                    // File download succeeded
                    System.out.println("Size before remove and after downlaod : "+unDownloadedFileList.size());
                    boolean result = unDownloadedFileList.remove(localFile);
                    pdfList.add(localFile);
                    System.out.println("File removed status "+result);
                    System.out.println("Size after remove: "+unDownloadedFileList.size());

                    callback.onDownloadComplete();
                    displayPdf("");
                })
                .addOnFailureListener(exception -> {
                    // File download failed
                    callback.onDownloadFailed(exception.getMessage());
                });
    }


    public void loadPdfFilesFromDirectory() {
        // Define the "IQRA_PDFS" folder inside the Downloads directory

        System.out.println("Loading from directory");
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File iqraPdfDir = new File(downloadsDir, localBookSavedFolderName);

        System.out.println("Directory exists : " + iqraPdfDir.exists());

        // Check if the directory exists
        if (iqraPdfDir.exists() && iqraPdfDir.isDirectory()) {
            // List all files in the directory
            File[] files = iqraPdfDir.listFiles();
            System.out.println("Total Files Found : " + files.length);

            if (files != null) {
                for (File file : files) {
                    // Check if the file is a PDF file
                    if (file.getName().toLowerCase().startsWith(".trashed")) {
                        file.delete();
                    }
                    if (file.isFile() && file.getName().toLowerCase().endsWith(".pdf")) {
                        // Add the PDF file to the pdfList
                        pdfList.add(file);
                        System.out.println("PDF added: " + file.getAbsolutePath());
                    }
                }
            } else {
                System.out.println("No files found in the directory.");
            }
        } else {
            System.out.println("Directory not found: " + iqraPdfDir.getAbsolutePath());
        }
    }

    public boolean isCorrupted(File file, String storedHash) {
        // Calculate the hash of the downloaded file (e.g., using MD5)
        String calculatedHash = FileHashUtils.calculateFileHash(file, "MD5");

        if (calculatedHash != null && calculatedHash.equals(storedHash)) {
            System.out.println("File is intact and matches the hash.");
            return false;
        } else {
            System.out.println("File is corrupted or tampered. Re-downloading");
            return true;
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.R)
    public void runtimePermission() {
        System.out.println("runtime permission called");
        Dexter.withContext(MainActivity.this)
                .withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                .withPermission(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {      // Permissions granted, proceed to load PDF files
                            System.out.println("Permission granted");
                            loadPdfFilesFromDirectory();

                        } else if (report.isAnyPermissionPermanentlyDenied()) {
                            // If permissions are permanently denied, guide user to app settings
                            System.out.println("Permissions denied.");
                            showSettingsDialog();
                        } else {
                            // Handle case where permissions are denied but not permanently
                            System.out.println("Permission not granted");
                            showRetryPermissionDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();  // Continue asking for permission
                    }
                }).check();
    }


    // Show a dialog to guide the user to app settings
    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Permissions Required")
                .setMessage("This app needs storage permissions to access and display PDF files. Please enable them in the app settings.")
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    // Redirect user to the app's settings
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", getPackageName(), null));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    // Show a dialog to retry requesting permission
    private void showRetryPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setTitle("Permissions Required")
                    .setMessage("You need to grant storage permissions to proceed. Retry?")
                    .setPositiveButton("Retry", (dialog, which) -> runtimePermission())  // Retry permission request
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .create()
                    .show();
        }
    }


    public void showCircularLoader(String bookName) {
        // Inflate the custom loader layout
        LayoutInflater inflater = getLayoutInflater();
        View loaderView = inflater.inflate(R.layout.loader_layout, null);

        // Create a dialog for the loader
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setView(loaderView);
        builder.setCancelable(false); // Prevent closing the loader by clicking outside

        // Get the TextView from the inflated layout and set the message
        TextView tvProgressMessage = loaderView.findViewById(R.id.tvProgressMessage);
        tvProgressMessage.setText("Downloading files, please wait...");

        // Create and show the dialog
        AlertDialog loaderDialog = builder.create();
        loaderDialog.show();
    }


    @SuppressLint("SetTextI18n")
    public boolean showDownloadDialog(File file) {

        // Step 1: Create the first AlertDialog for Download Confirmation
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
        alertBuilder.setTitle("Download " + file.getName().replace(".pdf", "") + " ?");
        alertBuilder.setMessage("This file is not downloaded. Would you like to download it?");

        // Step 2: Handle the "Download" button click
        alertBuilder.setPositiveButton("Download", (dialog, which) -> {
            // Step 3: Dismiss the confirmation dialog
            dialog.dismiss();

            // Step 4: Show the loader dialog
            LayoutInflater inflater = getLayoutInflater();
            View loaderView = inflater.inflate(R.layout.loader_layout, null);

            // Get the TextView from the loader layout and set the initial message
            TextView tvProgressMessage = loaderView.findViewById(R.id.tvProgressMessage);
            tvProgressMessage.setText("Downloading " + file.getName().replace(".pdf", "") + ", please wait...");

            AlertDialog.Builder loaderBuilder = new AlertDialog.Builder(MainActivity.this);
            loaderBuilder.setView(loaderView);
            loaderBuilder.setCancelable(false); // Prevent closing the loader by clicking outside

            AlertDialog loaderDialog = loaderBuilder.create();
            loaderDialog.show();

            // Step 5: Proceed with the download
            Book book = firebaseBookList.get(file.getName().replace(".pdf", "")); // Fetch book details

            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File iqraPdfDir = new File(downloadsDir, localBookSavedFolderName);

            // If directory does not exist, create it
            if (!iqraPdfDir.exists()) {
                iqraPdfDir.mkdirs();
            }

            File localFile = new File(iqraPdfDir, book.getBookName());

            // Start the download process
            downloadBookFromFirebase(book.getBookUrl(), localFile.getName(), new DownloadCallback() {
                @Override
                public void onDownloadComplete() {
                    // On Download Completion
                    Toast.makeText(MainActivity.this, localFile.getName() + " downloaded!", Toast.LENGTH_SHORT).show();
                    downloadCompleted = true;
                    loaderDialog.dismiss(); // Dismiss the loader dialog after download is complete
                }

                @Override
                public void onDownloadFailed(String errorMessage) {
                    // Handle failure
                    Toast.makeText(MainActivity.this, "Failed to download: " + errorMessage, Toast.LENGTH_SHORT).show();
                    loaderDialog.dismiss(); // Dismiss the loader dialog if download fails
                }

                @Override
                public void onProgressUpdate(int progress) {
                    // Update the loader message with progress
                    tvProgressMessage.setText("Downloaded " + progress + "%");
                }
            });
        });

        // Step 6: Handle the "Cancel" button click
        alertBuilder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        // Step 7: Show the confirmation dialog
        AlertDialog confirmationDialog = alertBuilder.create();
        confirmationDialog.show();

        return downloadCompleted;
    }


    public List<File> filterTextWise(List<File> files, String searchText) {

        List<File> filteredList = new ArrayList<>();

        if (searchText != null && !searchText.isEmpty()) {
            for (File file : files) {
                if (file.getName().toLowerCase().contains(searchText.toLowerCase())) {   // Comparing search text with the file name
                    filteredList.add(file);
                }
            }
        } else {
            filteredList.addAll(files);
            System.out.println("Filtered PDF List Size : " + filteredList.size());
        }
        return filteredList;
    }


    public List<File> filterCategoryWise(List<File> tempFilterList){

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        boolean isAllCategorySame = dbHelper.areAllCategoriesSame();

        List<File> finalFilteredList = new ArrayList<>();
        finalFilteredList.addAll(tempFilterList);

        for (File file : tempFilterList) {
            String fileName = file.getName().substring(0, file.getName().lastIndexOf('.'));
            Book book = dbHelper.getBookByName(fileName);

            System.out.println(book.getBookName());
            String category = book.getCategory();
            if (category != null) {
                System.out.println("Category : " + category);
            }
            if (!isAllCategorySame) {
                boolean isThisCategorySelected = dbHelper.isCategorySelected(category);
                System.out.println(category + " : is category Selected : " + isThisCategorySelected);
                if (!isThisCategorySelected) {
                    System.out.println("Removing " + category + " from list");
                    finalFilteredList.remove(file);
                }
            }

        }
        finalFilteredList = removeDuplicateFilesByName(finalFilteredList);
        return finalFilteredList;
    }

    public void setDisplayView(){
        recyclerView = findViewById(R.id.rv);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 1));
    }

    public void placeFilesInRecycleView(List<File> displayFileList){
        adapter = new MainAdapter(this, displayFileList, this);
        recyclerView.setAdapter(adapter);

        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Not needed for this implementation
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (runnable != null) {
                    handler.removeCallbacks(runnable); // Remove any previously scheduled callbacks
                }
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        displayPdf(newText); // Filter the list after 2 seconds
                    }
                };
                handler.postDelayed(runnable, 200);
                return true;
            }
        });
    }


    public void displayPdf(String searchText) {

        setDisplayView();

        System.out.println("PDF List Size : " + pdfList.size());
        removeDuplicateFilesByName(pdfList);
        removeDuplicateFilesByName(unDownloadedFileList);

        System.out.println("After duplicate removal for first time pdf file size " + pdfList.size());

        // Filter the list based on the search text

        List<File> filteredList = filterTextWise(pdfList,searchText);
        List<File> filteredUnDownloadedList = filterTextWise(unDownloadedFileList,searchText);

        List<File> tempFilterList = new ArrayList<>();

        tempFilterList.addAll(filteredList);
        tempFilterList.addAll(filteredUnDownloadedList);

        List<File> finalFilteredListForDisplay = filterCategoryWise(tempFilterList);

        System.out.println("File size after category filtering for display "+finalFilteredListForDisplay.size());
        placeFilesInRecycleView(finalFilteredListForDisplay);

    }


    public ArrayList<File> removeDuplicateFilesByName(List<File> filesList) {
        // A set to store unique file names
        HashSet<String> fileNamesSet = new HashSet<>();

        // A new list to store files without duplicates
        ArrayList<File> uniqueFiles = new ArrayList<>();

        // Iterate through the original list
        for (File file : filesList) {
            String fileName = file.getName(); // Get the file name

            // Check if the file name is already in the set
            if (!fileNamesSet.contains(fileName)) {
                // If not, add it to the set and the unique files list
                fileNamesSet.add(fileName);
                uniqueFiles.add(file);
            }
        }

        // Return the list of files with duplicates removed
        return uniqueFiles;
    }

    @Override
    public void onPdfSelected(File file) {
        startActivity(new Intent(MainActivity.this, PdfActivity.class)
                .putExtra("path", file.getAbsolutePath()));
    }


    private class ActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_action_mode, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.action_share) {
                shareSelectedFiles();
                mode.finish();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            selectedPdfFiles.clear();
            // Make sure to notify adapter of any changes
            adapter.notifyDataSetChanged();
        }
    }

}