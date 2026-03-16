package menu;

import dao.BookDAO;
import exception.LibraryException;
import model.Book;

import java.util.List;
import java.util.Scanner;

/**
 * Console menu for Book CRUD operations.
 */
public class BookMenu {

    private final BookDAO bookDAO = new BookDAO();
    private final Scanner sc;

    public BookMenu(Scanner sc) {
        this.sc = sc;
    }

    public void show() {
        while (true) {
            System.out.println("\n╔══════════════════════════════╗");
            System.out.println("║        BOOK MANAGEMENT       ║");
            System.out.println("╠══════════════════════════════╣");
            System.out.println("║  1. Add New Book              ║");
            System.out.println("║  2. View All Books            ║");
            System.out.println("║  3. View Available Books      ║");
            System.out.println("║  4. Search Books              ║");
            System.out.println("║  5. View Book by ID           ║");
            System.out.println("║  6. Update Book               ║");
            System.out.println("║  7. Delete Book               ║");
            System.out.println("║  0. Back to Main Menu         ║");
            System.out.println("╚══════════════════════════════╝");
            System.out.print("  Enter choice: ");

            int choice = readInt();
            switch (choice) {
                case 1 -> addBook();
                case 2 -> viewAllBooks();
                case 3 -> viewAvailableBooks();
                case 4 -> searchBooks();
                case 5 -> viewBookById();
                case 6 -> updateBook();
                case 7 -> deleteBook();
                case 0 -> { return; }
                default -> System.out.println("⚠  Invalid choice. Try again.");
            }
        }
    }

    // ── ADD ────────────────────────────────────────────────────────
    private void addBook() {
        System.out.println("\n── Add New Book ──");
        System.out.print("Title        : ");  String title  = sc.nextLine().trim();
        System.out.print("Author       : ");  String author = sc.nextLine().trim();
        System.out.print("ISBN         : ");  String isbn   = sc.nextLine().trim();
        System.out.print("Genre        : ");  String genre  = sc.nextLine().trim();
        System.out.print("Total Copies : ");  int copies    = readInt();

        if (title.isEmpty() || author.isEmpty() || isbn.isEmpty()) {
            System.out.println("❌  Title, Author and ISBN are required.");
            return;
        }
        if (copies < 1) {
            System.out.println("❌  Copies must be at least 1.");
            return;
        }

        try {
            Book book = bookDAO.addBook(new Book(title, author, isbn, genre, copies));
            System.out.println("✅  Book added successfully! ID = " + book.getBookId());
        } catch (LibraryException e) {
            System.out.println("❌  " + e.getMessage());
        }
    }

    // ── VIEW ALL ───────────────────────────────────────────────────
    private void viewAllBooks() {
        try {
            List<Book> books = bookDAO.getAllBooks();
            if (books.isEmpty()) {
                System.out.println("ℹ  No books in library.");
                return;
            }
            printHeader();
            books.forEach(b -> System.out.println(b.toRow()));
            System.out.println("─".repeat(100));
            System.out.println("  Total books: " + books.size());
        } catch (LibraryException e) {
            System.out.println("❌  " + e.getMessage());
        }
    }

    // ── VIEW AVAILABLE ─────────────────────────────────────────────
    private void viewAvailableBooks() {
        try {
            List<Book> books = bookDAO.getAvailableBooks();
            if (books.isEmpty()) {
                System.out.println("ℹ  No books currently available.");
                return;
            }
            printHeader();
            books.forEach(b -> System.out.println(b.toRow()));
            System.out.println("─".repeat(100));
        } catch (LibraryException e) {
            System.out.println("❌  " + e.getMessage());
        }
    }

    // ── SEARCH ─────────────────────────────────────────────────────
    private void searchBooks() {
        System.out.print("Search (title / author): ");
        String keyword = sc.nextLine().trim();
        if (keyword.isEmpty()) { System.out.println("⚠  Enter a keyword."); return; }

        try {
            List<Book> books = bookDAO.searchBooks(keyword);
            if (books.isEmpty()) {
                System.out.println("ℹ  No books found matching '" + keyword + "'.");
                return;
            }
            printHeader();
            books.forEach(b -> System.out.println(b.toRow()));
            System.out.println("─".repeat(100));
        } catch (LibraryException e) {
            System.out.println("❌  " + e.getMessage());
        }
    }

    // ── VIEW BY ID ─────────────────────────────────────────────────
    private void viewBookById() {
        System.out.print("Enter Book ID: ");
        int id = readInt();
        try {
            System.out.println(bookDAO.getBookById(id));
        } catch (LibraryException e) {
            System.out.println("❌  " + e.getMessage());
        }
    }

    // ── UPDATE ─────────────────────────────────────────────────────
    private void updateBook() {
        System.out.print("Enter Book ID to update: ");
        int id = readInt();

        try {
            Book existing = bookDAO.getBookById(id);
            System.out.println("Current: " + existing.toRow());
            System.out.println("(Press ENTER to keep current value)");

            System.out.print("New Title  [" + existing.getTitle()  + "]: ");
            String title = sc.nextLine().trim();

            System.out.print("New Author [" + existing.getAuthor() + "]: ");
            String author = sc.nextLine().trim();

            System.out.print("New Genre  [" + existing.getGenre()  + "]: ");
            String genre = sc.nextLine().trim();

            System.out.print("New Total Copies [" + existing.getTotalCopies() + "]: ");
            String copiesStr = sc.nextLine().trim();

            if (!title.isEmpty())   existing.setTitle(title);
            if (!author.isEmpty())  existing.setAuthor(author);
            if (!genre.isEmpty())   existing.setGenre(genre);
            if (!copiesStr.isEmpty()) {
                int copies = Integer.parseInt(copiesStr);
                if (copies < 1) { System.out.println("❌  Copies must be >= 1."); return; }
                existing.setTotalCopies(copies);
            }

            bookDAO.updateBook(existing);
            System.out.println("✅  Book updated successfully.");

        } catch (NumberFormatException e) {
            System.out.println("❌  Invalid number entered.");
        } catch (LibraryException e) {
            System.out.println("❌  " + e.getMessage());
        }
    }

    // ── DELETE ─────────────────────────────────────────────────────
    private void deleteBook() {
        System.out.print("Enter Book ID to delete: ");
        int id = readInt();
        System.out.print("⚠  Are you sure you want to delete Book ID " + id + "? (yes/no): ");
        String confirm = sc.nextLine().trim();

        if (!confirm.equalsIgnoreCase("yes")) {
            System.out.println("  Deletion cancelled.");
            return;
        }

        try {
            bookDAO.deleteBook(id);
            System.out.println("✅  Book ID " + id + " deleted.");
        } catch (LibraryException e) {
            System.out.println("❌  " + e.getMessage());
        }
    }

    // ── Utilities ──────────────────────────────────────────────────
    private void printHeader() {
        System.out.println("\n" + "─".repeat(100));
        System.out.printf("%-5s | %-30s | %-20s | %-15s | %-12s | %s%n",
                "ID", "Title", "Author", "ISBN", "Genre", "Avail/Total");
        System.out.println("─".repeat(100));
    }

    private int readInt() {
        try {
            String line = sc.nextLine().trim();
            return Integer.parseInt(line);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}