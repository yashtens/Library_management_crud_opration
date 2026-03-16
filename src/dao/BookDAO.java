package dao;

import config.DBConnection;
import exception.LibraryException;
import model.Book;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the books table.
 * All public methods throw LibraryException — callers never see raw SQLExceptions.
 */
public class BookDAO {

    // ══════════════════════════════════════════════════════════════
    //  CREATE
    // ══════════════════════════════════════════════════════════════

    /**
     * Inserts a new book row and sets the generated book_id on the object.
     *
     * @throws LibraryException DUPLICATE_ISBN if ISBN already exists
     */
    public Book addBook(Book book) throws LibraryException {
        final String SQL =
                "INSERT INTO books (title, author, isbn, genre, total_copies, available) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());
            ps.setString(3, book.getIsbn());
            ps.setString(4, book.getGenre());
            ps.setInt(5, book.getTotalCopies());
            ps.setInt(6, book.getTotalCopies()); // available = total at creation

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new LibraryException("Book insert failed — no rows affected.",
                        LibraryException.DB_ERROR);
            }

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) book.setBookId(keys.getInt(1));
            }
            return book;

        } catch (SQLIntegrityConstraintViolationException e) {
            throw new LibraryException(
                    "Duplicate ISBN: a book with ISBN '" + book.getIsbn() + "' already exists.",
                    LibraryException.DUPLICATE_ISBN, e);
        } catch (SQLException e) {
            throw new LibraryException("addBook failed: " + e.getMessage(),
                    LibraryException.DB_ERROR, e);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  READ — single
    // ══════════════════════════════════════════════════════════════

    /**
     * Finds a book by its primary key.
     *
     * @throws LibraryException BOOK_NOT_FOUND if no matching row
     */
    public Book getBookById(int bookId) throws LibraryException {
        final String SQL = "SELECT * FROM books WHERE book_id = ?";

        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(SQL)) {

            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new LibraryException(
                            "No book found with ID " + bookId,
                            LibraryException.BOOK_NOT_FOUND);
                }
                return mapRow(rs);
            }

        } catch (LibraryException e) {
            throw e;
        } catch (SQLException e) {
            throw new LibraryException("getBookById failed: " + e.getMessage(),
                    LibraryException.DB_ERROR, e);
        }
    }

    /**
     * Finds a book by ISBN.
     *
     * @throws LibraryException BOOK_NOT_FOUND if no matching row
     */
    public Book getBookByIsbn(String isbn) throws LibraryException {
        final String SQL = "SELECT * FROM books WHERE isbn = ?";

        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(SQL)) {

            ps.setString(1, isbn);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new LibraryException(
                            "No book found with ISBN " + isbn,
                            LibraryException.BOOK_NOT_FOUND);
                }
                return mapRow(rs);
            }

        } catch (LibraryException e) {
            throw e;
        } catch (SQLException e) {
            throw new LibraryException("getBookByIsbn failed: " + e.getMessage(),
                    LibraryException.DB_ERROR, e);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  READ — list
    // ══════════════════════════════════════════════════════════════

    /** Returns all books, ordered by title. */
    public List<Book> getAllBooks() throws LibraryException {
        final String SQL = "SELECT * FROM books ORDER BY title";

        try (Connection con = DBConnection.getInstance().getConnection();
             Statement  st  = con.createStatement();
             ResultSet  rs  = st.executeQuery(SQL)) {

            List<Book> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;

        } catch (SQLException e) {
            throw new LibraryException("getAllBooks failed: " + e.getMessage(),
                    LibraryException.DB_ERROR, e);
        }
    }

    /** Returns books where available > 0. */
    public List<Book> getAvailableBooks() throws LibraryException {
        final String SQL = "SELECT * FROM books WHERE available > 0 ORDER BY title";

        try (Connection con = DBConnection.getInstance().getConnection();
             Statement  st  = con.createStatement();
             ResultSet  rs  = st.executeQuery(SQL)) {

            List<Book> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;

        } catch (SQLException e) {
            throw new LibraryException("getAvailableBooks failed: " + e.getMessage(),
                    LibraryException.DB_ERROR, e);
        }
    }

    /** Full-text search across title and author (case-insensitive). */
    public List<Book> searchBooks(String keyword) throws LibraryException {
        final String SQL =
                "SELECT * FROM books WHERE title LIKE ? OR author LIKE ? ORDER BY title";

        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(SQL)) {

            String like = "%" + keyword + "%";
            ps.setString(1, like);
            ps.setString(2, like);

            List<Book> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
            return list;

        } catch (SQLException e) {
            throw new LibraryException("searchBooks failed: " + e.getMessage(),
                    LibraryException.DB_ERROR, e);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  UPDATE
    // ══════════════════════════════════════════════════════════════

    /**
     * Updates book details. ISBN is not changeable to avoid FK issues.
     *
     * @throws LibraryException BOOK_NOT_FOUND if book doesn't exist
     */
    public void updateBook(Book book) throws LibraryException {
        // Verify existence first
        getBookById(book.getBookId());

        final String SQL =
                "UPDATE books SET title=?, author=?, genre=?, total_copies=? " +
                        "WHERE book_id=?";

        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(SQL)) {

            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthor());
            ps.setString(3, book.getGenre());
            ps.setInt(4, book.getTotalCopies());
            ps.setInt(5, book.getBookId());

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new LibraryException("Update had no effect for book ID " + book.getBookId(),
                        LibraryException.DB_ERROR);
            }

        } catch (LibraryException e) {
            throw e;
        } catch (SQLException e) {
            throw new LibraryException("updateBook failed: " + e.getMessage(),
                    LibraryException.DB_ERROR, e);
        }
    }

    /**
     * Adjusts the 'available' count (used by TransactionDAO during issue/return).
     * delta = -1 for issue, +1 for return.
     */
    public void adjustAvailability(int bookId, int delta, Connection con)
            throws LibraryException {
        final String SQL =
                "UPDATE books SET available = available + ? " +
                        "WHERE book_id = ? AND available + ? >= 0 AND available + ? <= total_copies";

        try (PreparedStatement ps = con.prepareStatement(SQL)) {
            ps.setInt(1, delta);
            ps.setInt(2, bookId);
            ps.setInt(3, delta);
            ps.setInt(4, delta);

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new LibraryException(
                        "Cannot adjust availability — no copies available for book ID " + bookId,
                        LibraryException.BOOK_NOT_AVAILABLE);
            }
        } catch (LibraryException e) {
            throw e;
        } catch (SQLException e) {
            throw new LibraryException("adjustAvailability failed: " + e.getMessage(),
                    LibraryException.DB_ERROR, e);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  DELETE
    // ══════════════════════════════════════════════════════════════

    /**
     * Deletes a book. Fails if the book has any un-returned transactions.
     *
     * @throws LibraryException BOOK_NOT_FOUND or FK violation
     */
    public void deleteBook(int bookId) throws LibraryException {
        getBookById(bookId); // ensures it exists

        final String SQL = "DELETE FROM books WHERE book_id = ?";

        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(SQL)) {

            ps.setInt(1, bookId);
            ps.executeUpdate();

        } catch (SQLIntegrityConstraintViolationException e) {
            throw new LibraryException(
                    "Cannot delete book ID " + bookId + " — it has active/past transactions.",
                    LibraryException.DB_ERROR, e);
        } catch (SQLException e) {
            throw new LibraryException("deleteBook failed: " + e.getMessage(),
                    LibraryException.DB_ERROR, e);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  MAPPER
    // ══════════════════════════════════════════════════════════════

    private Book mapRow(ResultSet rs) throws SQLException {
        Book b = new Book();
        b.setBookId(rs.getInt("book_id"));
        b.setTitle(rs.getString("title"));
        b.setAuthor(rs.getString("author"));
        b.setIsbn(rs.getString("isbn"));
        b.setGenre(rs.getString("genre"));
        b.setTotalCopies(rs.getInt("total_copies"));
        b.setAvailable(rs.getInt("available"));
        b.setAddedOn(rs.getTimestamp("added_on"));
        return b;
    }
}