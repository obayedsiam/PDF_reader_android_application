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
import com.google.firebase.FirebaseApp;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;

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
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnPdfSelectListener {

    private MainAdapter adapter;
    private List<File> pdfList = new ArrayList<>();
    private List<File> selectedPdfFiles = new ArrayList<>();
    private RecyclerView recyclerView;
    private String localBookSavedFolderName = "IQRA_PDFS";
    private ActionMode actionMode;
    private ActionModeCallback actionModeCallback = new ActionModeCallback();

    private Handler handler = new Handler();
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.loader); // Show loader layout initially
        FirebaseApp.initializeApp(this);
        Map <String, String> firebaseBookList = new HashMap<>();
        DatabaseReference booksRef = FirebaseDatabase.getInstance().getReference("Book");

        booksRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot categorySnapshot : dataSnapshot.getChildren()) {
                    String categoryName = categorySnapshot.getKey(); // e.g., "categoryA"
                    for (DataSnapshot bookSnapshot : categorySnapshot.getChildren()) {
                        Long bookId = bookSnapshot.child("id").getValue(Long.class);
                        String bookName = bookSnapshot.child("name").getValue(String.class);
                        String bookUrl = bookSnapshot.child("url").getValue(String.class);
                        firebaseBookList.put(bookName,bookUrl);
                        saveBookToLocalDatabase(categoryName, bookId, bookName, bookUrl);
                    }
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
                                case R.id.share_app:
//                                    shareApp();
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

    // Method to share selected PDF files
    public void shareSelectedFiles() {
        if (selectedPdfFiles != null && !selectedPdfFiles.isEmpty()) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.setType("application/pdf");

            ArrayList<Uri> files = new ArrayList<>();
            for (File pdf : selectedPdfFiles) {
                Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", pdf);
                files.add(uri);
            }

            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
            startActivity(Intent.createChooser(shareIntent, "Share PDF files"));
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
        }
    }

    // Override onBackPressed to clear selection
    @Override
    public void onBackPressed() {
        if (((MainAdapter) recyclerView.getAdapter()).isSelectionMode()) {
            ((MainAdapter) recyclerView.getAdapter()).clearSelection();
        } else {
            super.onBackPressed();
        }
    }

    private void checkAndDownloadBooks(Map<String, String> firebaseBookList) {
        // Define the "IQRA_PDFS" folder inside the Downloads directory
        System.out.println("Checking downloadable files .....");

        boolean isNewBookDownloaded = false;
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File iqraPdfDir = new File(downloadsDir, localBookSavedFolderName);

        // If directory does not exist, create it
        if (!iqraPdfDir.exists()) {
            iqraPdfDir.mkdirs();
        }

        for (Map.Entry<String, String> entry : firebaseBookList.entrySet()) {

            String bookName = entry.getKey();  // The key is the book name
            String bookUrl = entry.getValue(); // The value is the URL of the book

            // Define the local file path for this book
            File localFile = new File(iqraPdfDir, bookName + ".pdf");  // Using book name to create file name

            if (localFile.exists()) {
                // File already exists, skip downloading
                System.out.println(bookName+" : File already exists");
            } else {
                // File does not exist, download it
                System.out.println("File does not exist, downloading: " + bookName);
                downloadBookFromFirebase(bookUrl, bookName);  // Download book from Firebase Storage
                isNewBookDownloaded = true;
            }
        }
        if(isNewBookDownloaded){
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
        File localFile = new File(iqraPdfDir, bookName+".pdf");

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

        System.out.println("Directory exists : " +iqraPdfDir.exists());

        // Check if the directory exists
        if (iqraPdfDir.exists() && iqraPdfDir.isDirectory()) {
            // List all files in the directory
            File[] files = iqraPdfDir.listFiles();
            System.out.println("Total Files Found : "+files.length);

            if (files != null) {
                for (File file : files) {
                    // Check if the file is a PDF file
                    if (file.isFile() && file.getName().toLowerCase().endsWith(".pdf")  && !file.getName().toLowerCase().startsWith(".trashed")) {
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

        System.out.println("PDF List Size : "+pdfList.size());
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
            System.out.println("Filtered PDF List Size : "+filteredList.size());
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
        startActivity(new Intent(MainActivity.this,PdfActivity.class)
                .putExtra("path",file.getAbsolutePath()));
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