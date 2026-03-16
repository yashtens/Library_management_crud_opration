package model;

import java.sql.Timestamp;

/**
 * Represents a book in the library.
 */
public class Book {

    private int       bookId;
    private String    title;
    private String    author;
    private String    isbn;
    private String    genre;
    private int       totalCopies;
    private int       available;
    private Timestamp addedOn;

    // ── Constructors ─────────────────────────────────────────────
    public Book() {}

    /** Constructor for INSERT (id assigned by DB). */
    public Book(String title, String author, String isbn,
                String genre, int totalCopies) {
        this.title       = title;
        this.author      = author;
        this.isbn        = isbn;
        this.genre       = genre;
        this.totalCopies = totalCopies;
        this.available   = totalCopies; // all copies available at creation
    }

    // ── Getters & Setters ────────────────────────────────────────
    public int       getBookId()      { return bookId; }
    public void      setBookId(int v) { this.bookId = v; }

    public String    getTitle()          { return title; }
    public void      setTitle(String v)  { this.title = v; }

    public String    getAuthor()         { return author; }
    public void      setAuthor(String v) { this.author = v; }

    public String    getIsbn()           { return isbn; }
    public void      setIsbn(String v)   { this.isbn = v; }

    public String    getGenre()          { return genre; }
    public void      setGenre(String v)  { this.genre = v; }

    public int       getTotalCopies()    { return totalCopies; }
    public void      setTotalCopies(int v){ this.totalCopies = v; }

    public int       getAvailable()      { return available; }
    public void      setAvailable(int v) { this.available = v; }

    public Timestamp getAddedOn()        { return addedOn; }
    public void      setAddedOn(Timestamp v) { this.addedOn = v; }

    // ── Display ──────────────────────────────────────────────────
    @Override
    public String toString() {
        return String.format(
                "┌─ Book ID : %-5d\n│  Title   : %s\n│  Author  : %s\n" +
                        "│  ISBN    : %s\n│  Genre   : %s\n" +
                        "│  Copies  : %d total / %d available\n" +
                        "└─ Added   : %s",
                bookId, title, author, isbn, genre,
                totalCopies, available, addedOn);
    }

    /** Compact single-line row for lists. */
    public String toRow() {
        return String.format("%-5d | %-30s | %-20s | %-15s | %-12s | %d/%d",
                bookId, title, author, isbn, genre, available, totalCopies);
    }
}