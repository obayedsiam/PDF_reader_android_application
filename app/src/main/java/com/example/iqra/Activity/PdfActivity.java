package com.example.iqra.Activity;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.appcompat.widget.SearchView;


import com.example.iqra.R;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.shockwave.pdfium.PdfDocument;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PdfActivity extends AppCompatActivity  implements OnPageChangeListener{

    private String filePath = "";

    private String fileName = "";


    PDFView pdfView ;
    Integer pageNumber;
    String pdfFileName;

    Integer currentPageNumber;
    Integer savedPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf);



        pdfView = findViewById(R.id.pdfView);
        filePath = getIntent().getStringExtra("path");
        File file = new File(filePath);
        Uri path = Uri.fromFile(file);
        fileName = file.getName();

        SharedPreferences preferences = getSharedPreferences(fileName, MODE_PRIVATE);
        int lastReadPage = preferences.getInt("hello", 0);

        pdfView.fromUri(path)
                .defaultPage(lastReadPage)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .onPageChange(this)
                .enableAnnotationRendering(true)
//                .onLoad(this)
                .scrollHandle(new DefaultScrollHandle(this))
                .load();

    }


    public void printBookmarksTree(List<PdfDocument.Bookmark> tree, String sep) {
        for (PdfDocument.Bookmark b : tree) {

            Log.e(TAG, String.format("%s %s, p %d", sep, b.getTitle(), b.getPageIdx()));

            if (b.hasChildren()) {
                printBookmarksTree(b.getChildren(), sep + "-");
            }
        }
    }

    @Override
    public void onPageChanged(int page, int pageCount) {
        pageNumber = page;
        setTitle(String.format("%s %s / %s", pdfFileName, page + 1, pageCount));
    }

    @Override
    protected void onStop() {
        super.onStop();

        SharedPreferences mySharedPreferences = getSharedPreferences(fileName,Context.MODE_PRIVATE);
        SharedPreferences.Editor myEditor = mySharedPreferences.edit();


        savedPage = pdfView.getCurrentPage();
        myEditor.putInt("hello",savedPage);
        myEditor.apply();


    }
}