package com.example.iqra.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.RawRes;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
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

public class MainActivity extends AppCompatActivity implements OnPdfSelectListener {

    private MainAdapter adapter;
    private List<File> pdfList = new ArrayList<>();
    private RecyclerView recyclerView;

    private Handler handler = new Handler();
    private Runnable runnable;

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
                                    shareApp();
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

                // Request runtime permissions
                runtimePermission();
            }
        }, 100); // Change YOUR_DELAY_TIME to your desired delay time in milliseconds
    }


//    private void shareApp() {
//        Intent sendIntent = new Intent();
//        sendIntent.setAction(Intent.ACTION_SEND);
//        sendIntent.putExtra(Intent.EXTRA_TEXT, "Check out this cool app!");
//        sendIntent.setType("text/plain");
//        startActivity(Intent.createChooser(sendIntent, "Share via"));
//    }

    private void shareApp() {
        // Get the path of the APK file in your app's private directory
        String apkFilePath = getApplicationContext().getPackageCodePath();

        // Copy the APK file to a publicly accessible directory (external storage)
        File externalDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File apkFile = new File(externalDir, "base.apk");

        try {
            // Copy the APK file
            FileInputStream in = new FileInputStream(new File(apkFilePath));
            FileOutputStream out = new FileOutputStream(apkFile);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            // Handle error
            return;
        }

        // Generate a FileProvider URI for the copied APK file
        Uri apkUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", apkFile);

        // Create an intent to share the APK file
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_STREAM, apkUri);
        sendIntent.setType("application/vnd.android.package-archive");

        // Grant permission to the receiving app
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Start an activity to share the APK file
        startActivity(Intent.createChooser(sendIntent, "Share APK via"));
    }

    public void runtimePermission() {
        Dexter.withActivity(MainActivity.this)
                .withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                           getAllRawPDFs();
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

            for (File singleFile : files) {
                if (singleFile.isDirectory() && !singleFile.isHidden()) {
                    arrayList.addAll(findPdf(singleFile));
                } else {
                    if (singleFile.getName().endsWith(".pdf")) {
                        arrayList.add(singleFile);
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

    public void getAllRawPDFs() {

        System.out.println("Entered into raw pdf folder creation ");
        Field[] fields = R.raw.class.getFields();
//        System.out.println("Resource file length : "+fields.length);
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
        File pdfFolder = new File(Environment.getExternalStorageDirectory(), "IQRA_PDFS");

        if (pdfFolder.exists()) {
            File[] files = pdfFolder.listFiles();
            if (files != null) {
                pdfFiles.addAll(Arrays.asList(files));
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

        return pdfFiles;
    }


// Reading pdf from storage

//    public void displayPdf() {
//        recyclerView = findViewById(R.id.rv);
//        recyclerView.setHasFixedSize(true);
//        recyclerView.setLayoutManager(new GridLayoutManager(this, 1));
//        pdfList = new ArrayList<>();
//        pdfList.addAll(findPdf(Environment.getExternalStorageDirectory()));
//        adapter = new MainAdapter(this, pdfList, this);
//        recyclerView.setAdapter(adapter);
//
//        SearchView searchView = findViewById(R.id.searchView);
//        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//            @Override
//            public boolean onQueryTextSubmit(String query) {
//                // Not needed for this implementation
//                return false;
//            }
//
//            @Override
//            public boolean onQueryTextChange(String newText) {
//                adapter.getFilter().filter(newText);
//                return true;
//            }
//        });
//    }



    @Override
    public void onPdfSelected(File file) {
        startActivity(new Intent(MainActivity.this,PdfActivity.class)
                .putExtra("path",file.getAbsolutePath()));
    }

}