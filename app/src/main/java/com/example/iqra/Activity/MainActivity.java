package com.example.iqra.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.RawRes;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.iqra.BuildConfig;
import com.example.iqra.R;
import com.github.barteksc.pdfviewer.util.FileUtils;
import com.google.android.material.navigation.NavigationView;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.single.PermissionListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import androidx.appcompat.app.AlertDialog;

public class MainActivity extends AppCompatActivity implements OnPdfSelectListener {

    private MainAdapter adapter;
    private List<File> pdfList = new ArrayList<>();
    private RecyclerView recyclerView;

    private Handler handler = new Handler();
    private Runnable runnable;

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.loader); // Show loader layout initially

        // Post a delayed action to switch to the main activity layout after a certain time
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Set the main activity layout
                setContentView(R.layout.activity_main);

                // Initialize toolbar and navigation drawer if no action bar is set
                if (getSupportActionBar() == null) {
                    toolbar = findViewById(R.id.toolbar);
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
                                    shareText("");
                                    return true;
                                case R.id.nav_settings:
                                    // Handle settings click
                                    return true;
                                case R.id.nav_links:
                                    // Show pop-up menu
                                    showCustomPopupMenu();
                                default:
                                    return false;
                            }
                        }
                    });

                    // Set OnClickListener for the filter button
                    ImageView filterButton = findViewById(R.id.filterButton);
                    filterButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showFilterPopupMenu();
                        }
                    });
                }
                // Request runtime permissions
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    runtimePermission();
                }
            }
        }, 100); // Change YOUR_DELAY_TIME to your desired delay time in milliseconds
    }

    private void showFilterPopupMenu() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.filter_option_layout, null);
        builder.setView(dialogView);

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch switchArticle = dialogView.findViewById(R.id.switch_article);
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch switchLocal = dialogView.findViewById(R.id.switch_local);
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch switchBook = dialogView.findViewById(R.id.switch_book);
        switchLocal.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    displayPdfFromLocalStorage();
                } else {
                    // Handle when the local switch is turned off
                }
            }
        });
        // Set switch states or add listeners as needed

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showCustomPopupMenu() {
        // Inflate the custom layout
        View popupView = getLayoutInflater().inflate(R.layout.popup_menu_layout, null);

        // Create a dialog to show the layout
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(popupView);
        builder.setTitle("Select Links");

        // Add action buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Handle OK button click
                // Get the state of checkboxes and perform actions accordingly
                CheckBox facebookCheckbox = popupView.findViewById(R.id.facebook_checkbox);
                CheckBox podcastCheckbox = popupView.findViewById(R.id.podcast_checkbox);

                // Initialize other checkboxes if needed

                // Check the state of each checkbox
                List<String> selectedOptions = new ArrayList<>();
                if (facebookCheckbox.isChecked()) {
                    selectedOptions.add("Facebook : www.facebook.com\n");
                }
                if (podcastCheckbox.isChecked()) {
                    selectedOptions.add("Podcast : www.podcast.com\n");
                }
                // Add more options if needed

                // If any checkbox is checked, send the selected strings through media
                if (!selectedOptions.isEmpty()) {
                    sendStringsThroughMedia(selectedOptions);
                }

                // Dismiss the dialog
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Cancel", null);

        // Show the dialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    private void sendStringsThroughMedia(List<String> selectedOptions) {
        // Send the selected strings through media (e.g., share via Intent)
        StringBuilder linkBuilder = new StringBuilder();
        for (String link : selectedOptions) {
            linkBuilder.append(link);
        }
        // Example: Share via Intent
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, linkBuilder.toString());
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        showShareOption(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_share) {
            adapter.shareSelectedFiles(); // Call the method from the adapter
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void showShareOption(boolean show) {
        Menu menu = toolbar.getMenu();
        MenuItem shareMenuItem = menu.findItem(R.id.action_share);
        if (shareMenuItem != null) {
            shareMenuItem.setVisible(show);
        }
    }

    public void setToolbarVisibility(boolean visible) {
        if (getSupportActionBar() != null) {
            if (visible) {
                getSupportActionBar().show();
            } else {
                getSupportActionBar().hide();
            }
        }
    }

    private void shareText(String text) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Share via"));
    }


//    private void shareApp() {
//        // Get the path of the APK file in your app's private directory
//        String apkFilePath = "app/release/app-release.apk";
////        String apkFilePath = getApplicationContext().getPackageCodePath();
//
//        // Copy the APK file to a publicly accessible directory (external storage)
//        File externalDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
//        File apkFile = new File(externalDir, "app-release.apk");
//        System.out.println("Entered into share app");
//        try {
//            // Copy the APK file
//            FileInputStream in = new FileInputStream(new File(apkFilePath));
//            FileOutputStream out = new FileOutputStream(apkFile);
//            byte[] buf = new byte[1024];
//            int len;
//            while ((len = in.read(buf)) > 0) {
//                out.write(buf, 0, len);
//            }
//            in.close();
//            out.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//            // Handle error
//            return;
//        }
//
//        // Generate a FileProvider URI for the copied APK file
//        Uri apkUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", apkFile);
//
//        // Create an intent to share the APK file
//        Intent sendIntent = new Intent();
//        sendIntent.setAction(Intent.ACTION_SEND);
//        sendIntent.putExtra(Intent.EXTRA_STREAM, apkUri);
//        sendIntent.setType("application/vnd.android.package-archive");
//
//        // Grant permission to the receiving app
//        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//
//        // Start an activity to share the APK file
//        startActivity(Intent.createChooser(sendIntent, "Share APK via"));
//    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    public void runtimePermission() {
        Dexter.withActivity(MainActivity.this)
                .withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
//                            displayPdfFromLocalStorage();
                           getAllRawPDFs();   // Function to read app internal pdf
                        } else {
                            // Handle denied permissions if needed
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                })
                .check();
        displayPdf("");
    }


    public ArrayList<File> findPdf(File file) {
        ArrayList<File> arrayList = new ArrayList<>();
        File[] files = file.listFiles();
//        System.out.println("Hello ");

        if(files!=null){
            for (File singleFile : files) {
                if (singleFile.isDirectory() && !singleFile.isHidden()) {
                    arrayList.addAll(findPdf(singleFile));
                } else {
                    if (singleFile.getName().endsWith(".pdf")) {
                        arrayList.add(singleFile);
                    }
                }
            }
        }

        return arrayList;
    }

    public void displayPdf(String searchText) {
        recyclerView = findViewById(R.id.rv);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 1));
//        pdfList = new ArrayList<>();
//        pdfList.addAll(findPdf(Environment.getExternalStorageDirectory()));

        System.out.println("PDF List Size : "+pdfList.size());
        // Filter the list based on the search text
        List<File> filteredList = new ArrayList<>();
        if (searchText != null || !searchText.isEmpty()) {
            for (File file : pdfList) {
                if (file.getName().toLowerCase().contains(searchText.toLowerCase())) {
                    filteredList.add(file);
                }
            }
        } else {
            filteredList.addAll(pdfList);
            System.out.println("Filtered PDF List Size : "+filteredList.size());
        }

        if(searchText.equals("")){
            filteredList = pdfList;
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

    public void getAllRawPDFs() {

        System.out.println("Entered into raw pdf folder creation ");

        Field[] fields = R.raw.class.getFields();
        System.out.println("Resource file length : "+fields.length);
//
//        System.out.println("File name in res raw :"+fields[0].getName());

        File externalFolder = new File(getExternalFilesDir(null), "pdf_files");



        if (!externalFolder.exists()) {
            System.out.println("pdf files location doesn't exists");
            externalFolder.mkdirs();
        }
        else{
            System.out.println("Folder name found : "+externalFolder.getName());
        }


        // Copy each PDF file to the external folder
        for (int i = 0; i < fields.length; i++) {
            String fileName = fields[i].getName();
            int resId = getResources().getIdentifier(fileName, "raw", getPackageName());
            InputStream inputStream = getResources().openRawResource(resId);

            File outputFile = new File(externalFolder, fileName + ".pdf");
            try {
                OutputStream outputStream = new FileOutputStream(outputFile);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                outputStream.close();
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
 launchPdfViewer();
    }

    private void launchPdfViewer() {
        // Example: Launch PDF viewer for the first PDF file in the external folder
        File[] pdfFiles = new File(getExternalFilesDir("pdf_files").getPath()).listFiles();

        pdfList.addAll(Arrays.asList(pdfFiles));

    }


    public List<File> getPDFFilesFromExternalStorage() {
        List<File> pdfFiles = new ArrayList<>();
        File pdfFolder = new File(Environment.getExternalStorageDirectory().toURI());

        if (pdfFolder.exists()) {
            File[] files = pdfFolder.listFiles();
            if (files != null) {

                for(File file:files) {
                    pdfFiles.addAll(Arrays.asList(file));
                }
//                pdfFiles.add(findPdf(files));
//                pdfFiles.addAll(Arrays.asList(files));
            }
        }
        pdfList = pdfFiles;

        adapter = new MainAdapter(this, pdfList, this);
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
                adapter.getFilter().filter(newText);
                return true;
            }
        });
//return pdfList;
        return pdfFiles;
    }


// Reading pdf from storage

    public void displayPdfFromLocalStorage() {
        recyclerView = findViewById(R.id.rv);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 1));
        pdfList = new ArrayList<>();
//        pdfList = getPDFFilesFromExternalStorage();
        pdfList.addAll(findPdf(Environment.getExternalStorageDirectory()));
        System.out.println("Size of Local PDF files : "+pdfList.size());
        adapter = new MainAdapter(this, pdfList, this);
        recyclerView.setAdapter(adapter);

        displayPdf("");

        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Not needed for this implementation
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return true;
            }
        });
    }



//    public ArrayList<File> findPdf(File directory) {
//        ArrayList<File> pdfFiles = new ArrayList<>();
//        File[] files = directory.listFiles();
//
//            for (File file : files) {
//
//                if (file.isDirectory() && !file.isHidden()) {
//                    // Recursive call to search PDF files in subdirectories
//
//                    pdfFiles.addAll(findPdf(file));
//                } else {
//                    // Check if the file is a PDF file
//
//                    if (file.getName().endsWith(".pdf")) {
//                        System.out.println("FILE NAME : "+file.getName());
//                        pdfFiles.add(file);
//                    }
//                }
//
//        }
//        return pdfFiles;
//    }

    @Override
    public void onPdfSelected(File file) {
        startActivity(new Intent(MainActivity.this,PdfActivity.class)
                .putExtra("path",file.getAbsolutePath()));
    }

}