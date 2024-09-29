package com.example.iqra.entity;

public class Book {
    Long bookId;
    String bookName;
    String category;
    String bookUrl;
    String bookHash;

    public Book(Long bookId, String bookName, String category, String bookUrl, String bookHash) {
        this.bookId = bookId;
        this.bookName = bookName;
        this.category = category;
        this.bookUrl = bookUrl;
        this.bookHash = bookHash;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getBookHash() {
        return bookHash;
    }

    public void setBookHash(String bookHash) {
        this.bookHash = bookHash;
    }

    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getBookUrl() {
        return bookUrl;
    }

    public void setBookUrl(String bookUrl) {
        this.bookUrl = bookUrl;
    }
}
