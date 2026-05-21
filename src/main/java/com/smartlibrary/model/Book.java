package com.smartlibrary.model;

public class Book {
    private final int id;
    private String title;
    private String author;
    private String isbn;
    private String category;
    private int publicationYear;
    private int quantity;
    private int availableCopies;
    private String shelfLocation;
    private String status;
    private String coverImagePath;

    // Creates a Book object with the supplied values.
    // Parameters: id is the database id; title is the title text; author is the book author; isbn is the book ISBN value
    // Parameters: category is the selected book category; publicationYear is the year the book was published
    // Parameters: quantity is the total number of copies; availableCopies is the number of copies ready to borrow
    // Parameters: shelfLocation is the shelf code shown in the catalog; status is the selected status filter or saved status
    // Parameters: coverImagePath is the optional image file or resource path.
    public Book(int id, String title, String author, String isbn, String category, int publicationYear,
                int quantity, int availableCopies, String shelfLocation, String status, String coverImagePath) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.category = category;
        this.publicationYear = publicationYear;
        this.quantity = quantity;
        this.availableCopies = availableCopies;
        this.shelfLocation = shelfLocation;
        this.status = status;
        this.coverImagePath = coverImagePath;
    }

    // Returns the id value.
    public int getId() { return id; }
    // Returns the title value.
    public String getTitle() { return title; }
    // Updates the title value.
    // Parameters: title is the title text.
    public void setTitle(String title) { this.title = title; }
    // Returns the author value.
    public String getAuthor() { return author; }
    // Updates the author value.
    // Parameters: author is the book author.
    public void setAuthor(String author) { this.author = author; }
    // Returns the isbn value.
    public String getIsbn() { return isbn; }
    // Updates the isbn value.
    // Parameters: isbn is the book ISBN value.
    public void setIsbn(String isbn) { this.isbn = isbn; }
    // Returns the category value.
    public String getCategory() { return category; }
    // Updates the category value.
    // Parameters: category is the selected book category.
    public void setCategory(String category) { this.category = category; }
    // Returns the publication year value.
    public int getPublicationYear() { return publicationYear; }
    // Updates the publication year value.
    // Parameters: publicationYear is the year the book was published.
    public void setPublicationYear(int publicationYear) { this.publicationYear = publicationYear; }
    // Returns the quantity value.
    public int getQuantity() { return quantity; }
    // Updates the quantity value.
    // Parameters: quantity is the total number of copies.
    public void setQuantity(int quantity) { this.quantity = quantity; }
    // Returns the available copies value.
    public int getAvailableCopies() { return availableCopies; }
    // Updates the available copies value.
    // Parameters: availableCopies is the number of copies ready to borrow.
    public void setAvailableCopies(int availableCopies) { this.availableCopies = availableCopies; }
    // Returns the shelf location value.
    public String getShelfLocation() { return shelfLocation; }
    // Updates the shelf location value.
    // Parameters: shelfLocation is the shelf code shown in the catalog.
    public void setShelfLocation(String shelfLocation) { this.shelfLocation = shelfLocation; }
    // Returns the status value.
    public String getStatus() { return status; }
    // Updates the status value.
    // Parameters: status is the selected status filter or saved status.
    public void setStatus(String status) { this.status = status; }
    // Returns the cover image path value.
    public String getCoverImagePath() { return coverImagePath; }
    // Updates the cover image path value.
    // Parameters: coverImagePath is the optional image file or resource path.
    public void setCoverImagePath(String coverImagePath) { this.coverImagePath = coverImagePath; }
}
