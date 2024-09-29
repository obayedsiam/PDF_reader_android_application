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
import android.content.Intent;

import com.example.iqra.entity.Book;
import com.example.iqra.entity.Link;
import com.example.iqra.util.FileHashUtils;
import com.google.firebase.FirebaseApp;

import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.ToggleButton;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements OnPdfSelectListener {

    private MainAdapter adapter;
    private List<File> pdfList = new ArrayList<>();
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.loader); // Show loader layout initially
        FirebaseApp.initializeApp(this);
        DatabaseReference booksRef = FirebaseDatabase.getInstance().getReference("Book");
        DatabaseReference linkReference = FirebaseDatabase.getInstance().getReference("Link");

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
                        saveBookToLocalDatabase(categoryName, bookId, bookName, bookUrl);
                    }
                }
                checkAndDownloadBooks(firebaseBookList);
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
                checkAndDownloadBooks(firebaseBookList);
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
        }, 100); // Change YOUR_DELAY_TIME to your desired delay time in milliseconds
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
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
        builder.setTitle("Select Categories");

        // Inflate the filter dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_filter, null);
        builder.setView(dialogView);

        LinearLayout switchContainer = dialogView.findViewById(R.id.switchContainer);
        Button buttonCancel = dialogView.findViewById(R.id.buttonCancel);
        Button buttonApply = dialogView.findViewById(R.id.buttonApply);

        // Convert the categoryFilter Set to an ArrayList for easier manipulation
        ArrayList<String> categoriesList = new ArrayList<>(categoryFilter);

        // Create Switches for each category
        for (String category : categoriesList) {
            // Create Switch
            Switch switchView = new Switch(this);
            switchView.setText(category);
            switchView.setTextOn(category);
            switchView.setTextOff(category);
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
                    }
                }
            }
            // Call your filter application method here
            applyFilter(selectedCategories);
            dialog.dismiss();
        });

        // Show the dialog after setting up the listeners
        dialog.show();
    }

    private void applyFilter(Set<String> selectedCategories) {
        for (Map.Entry<String, Book> entry : firebaseBookList.entrySet()) {
            Book book = entry.getValue();
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
                    System.out.println("File Corrupted. Need to redownload");
                    localFile.delete();

                    downloadBookFromFirebase(bookUrl, bookName);
                }
                System.out.println(bookName + " : File already exists");
            } else {
                // File does not exist, download it
                System.out.println("File does not exist, downloading: " + bookName);
                downloadBookFromFirebase(bookUrl, bookName);  // Download book from Firebase Storage
                isNewBookDownloaded = true;
            }
        }
        if (isNewBookDownloaded) {
            loadPdfFilesFromDirectory();
            isNewBookDownloaded = false;
        }
    }

    private void saveBookToLocalDatabase(String category, Long id, String name, String url) {
//            SQLiteDatabase db = getWritableDatabase();
//            ContentValues values = new ContentValues();
//            values.put("category", category);
//            values.put("id", id);
//            values.put("name", name);
//            values.put("url", url);
//
//            db.insert("Books", null, values);
    }

    private void downloadBookFromFirebase(String fileUrl, String bookName) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReferenceFromUrl(fileUrl); // Use the URL to get the StorageReference

        // Define the "IQRA_PDFS" folder inside the Downloads directory
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File iqraPdfDir = new File(downloadsDir, localBookSavedFolderName);

        // Create the directory if it doesn't exist
        if (!iqraPdfDir.exists()) {
            iqraPdfDir.mkdirs();  // Create directory and any necessary parent directories
        }

        // Define the file inside the IQRA_PDFS folder
        File localFile = new File(iqraPdfDir, bookName + ".pdf");

        // Download the file from Firebase Storage
        storageRef.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
        }).addOnFailureListener(exception -> {
            // Handle any errors during download
            System.out.println("Download failed: " + exception.getMessage());
        });
    }

    private void loadPdfFilesFromDirectory() {
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
            System.out.println("File is corrupted or tampered with. Redownloading...");
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


    public void displayPdf(String searchText) {
        recyclerView = findViewById(R.id.rv);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 1));

        System.out.println("PDF List Size : " + pdfList.size());
        // Filter the list based on the search text
        List<File> filteredList = new ArrayList<>();
        if (searchText != null && !searchText.isEmpty()) {
            for (File file : pdfList) {
                if (file.getName().toLowerCase().contains(searchText.toLowerCase())) {
                    filteredList.add(file);
                }
            }
        } else {
            filteredList.addAll(pdfList);
            System.out.println("Filtered PDF List Size : " + filteredList.size());
        }

        adapter = new MainAdapter(this, filteredList, this);
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