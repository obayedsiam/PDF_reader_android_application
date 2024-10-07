package com.example.iqra.helper;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.iqra.entity.Book;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "app_database.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        System.out.println("Category and Book table start to create. ");
        // This is where you define the structure of the database
        String createCategoryTable= "CREATE TABLE IF NOT EXISTS Categories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT UNIQUE, " +
                "isSelected INTEGER)";
        db.execSQL(createCategoryTable);

        String CREATE_BOOK_TABLE = "CREATE TABLE IF NOT EXISTS Books (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT UNIQUE," +  // Ensure that the book name is unique
                "category TEXT," +
                "hash TEXT," +
                "url TEXT)";
        db.execSQL(CREATE_BOOK_TABLE);
        System.out.println("Category and Book table created. ");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For upgrading database structure if needed in future versions
        db.execSQL("DROP TABLE IF EXISTS Categories");
        db.execSQL("DROP TABLE IF EXISTS Books");
        onCreate(db);
    }

    @SuppressLint("Range")
    public boolean isCategorySelected(String categoryName) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT isSelected FROM Categories WHERE name = ?";
        Cursor cursor = db.rawQuery(query, new String[]{categoryName});

        boolean isSelected = false; // Default to false if not found
        if (cursor.moveToFirst()) {
            isSelected = cursor.getInt(cursor.getColumnIndex("isSelected")) == 1; // 1 means true
        }
        cursor.close();
        System.out.println("Is Selected : "+isSelected);
        return isSelected;
    }

    public List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query("Categories", new String[]{"name"}, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") String category = cursor.getString(cursor.getColumnIndex("name"));
                categories.add(category);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return categories;
    }

    public void updateCategorySelection(String category, boolean isSelected) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("isSelected", isSelected ? 1 : 0);  // Store 1 for true, 0 for false

        db.update("Categories", values, "name = ?", new String[]{category});
    }

    public boolean areAllCategoriesSame() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT isSelected FROM Categories", null);

        if (cursor != null && cursor.moveToFirst()) {
            // Get the value of the first category's isSelected field
            @SuppressLint("Range") int initialIsSelected = cursor.getInt(cursor.getColumnIndex("isSelected"));

            // Check if all other categories have the same isSelected value
            do {
                @SuppressLint("Range") int currentIsSelected = cursor.getInt(cursor.getColumnIndex("isSelected"));
                if (currentIsSelected != initialIsSelected) {
                    cursor.close();
                    return false;  // Found a mismatch, so not all are the same
                }
            } while (cursor.moveToNext());

            cursor.close();
            return true;  // All categories have the same isSelected value
        }

        if (cursor != null) {
            cursor.close();
        }

        return true;  // If there are no categories, return true
    }

    public void saveBookToLocalDatabase(Book book, SQLiteDatabase db) {
        System.out.println("Save book started in method");
        String CREATE_BOOK_TABLE = "CREATE TABLE IF NOT EXISTS Books (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT UNIQUE," +  // Ensure that the book name is unique
                "category TEXT," +
                "hash TEXT," +
                "url TEXT)";
        db.execSQL(CREATE_BOOK_TABLE);

        System.out.println("Database created... in save");
//        SQLiteDatabase db = this.getWritableDatabase();
        // Step 2: Check if the book already exists by name
        String selectQuery = "SELECT * FROM Books WHERE name = ?";
        Cursor cursor = db.rawQuery(selectQuery, new String[]{book.getBookName()});

        if (cursor != null && cursor.moveToFirst()) {
            // Book already exists, so don't insert
            cursor.close();
        } else {
            // Step 3: Book doesn't exist, insert the new book
            ContentValues values = new ContentValues();
            values.put("id",book.getBookId());
            values.put("name", book.getBookName());
            values.put("category", book.getCategory());
            values.put("hash",book.getBookHash());
            values.put("url", book.getBookUrl());
            // Insert the row
            db.insert("Books", null, values);
            cursor.close();
        }
    }

    public Book getBookByName(String bookName) {
        SQLiteDatabase db = this.getReadableDatabase(); // Open the database for reading

        // Query to select a book by name
        String selectQuery = "SELECT * FROM Books WHERE name = ?";
        Cursor cursor = db.rawQuery(selectQuery, new String[]{bookName});

        Book book = null;

        // Check if a result is found
        if (cursor != null && cursor.moveToFirst()) {
            // Retrieve column values
            Long bookId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
            String category = cursor.getString(cursor.getColumnIndexOrThrow("category"));
            String hash = cursor.getString(cursor.getColumnIndexOrThrow("hash"));
            String url = cursor.getString(cursor.getColumnIndexOrThrow("url"));

            // Create a new Book object
            book = new Book(bookId, bookName, category, url, hash);

            cursor.close(); // Close the cursor after use
        }

        db.close(); // Close the database
        return book; // Return the found book or null if not found
    }




}

