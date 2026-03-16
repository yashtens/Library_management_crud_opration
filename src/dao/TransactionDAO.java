package dao;

import config.DBConnection;
import exception.LibraryException;
import model.Member;
import model.Transaction;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for transactions.
 * Issue and Return use DB transactions (BEGIN / COMMIT / ROLLBACK)
 * to keep books.available and transactions in sync atomically.
 *
 * Fine rate: ₹5 per overdue day.
 */
public class TransactionDAO {

    private static final BigDecimal FINE_PER_DAY = new BigDecimal("5.00");

    private final BookDAO   bookDAO   = new BookDAO();
    private final MemberDAO memberDAO = new MemberDAO();

    // ══════════════════════════════════════════════════════════════
    //  ISSUE BOOK
    // ══════════════════════════════════════════════════════════════

    /**
     * Issues a book to a member — wraps the two-table update in one transaction.
     *
     * @param bookId   the book to issue
     * @param memberId the borrowing member
     * @param days     loan period in days
     * @throws LibraryException BOOK_NOT_AVAILABLE, MEMBER_SUSPENDED, etc.
     */
    public Transaction issueBook(int bookId, int memberId, int days)
            throws LibraryException {

        // ── Pre-condition checks ─────────────────────────────────
        Member member = memberDAO.getMemberById(memberId);
        if (member.getStatus() == Member.Status.SUSPENDED ||
                member.getStatus() == Member.Status.INACTIVE) {
            throw new LibraryException(
                    "Member '" + member.getName() + "' is " + member.getStatus() +
                            " and cannot borrow books.",
                    LibraryException.MEMBER_SUSPENDED);
        }

        bookDAO.getBookById(bookId); // verify book exists

        // ── DB transaction ───────────────────────────────────────
        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            con.setAutoCommit(false);

            // 1) decrement available
            bookDAO.adjustAvailability(bookId, -1, con);

            // 2) insert transaction row
            LocalDate today  = LocalDate.now();
            LocalDate due    = today.plusDays(days);
            Date issueDate   = Date.valueOf(today);
            Date dueDate     = Date.valueOf(due);

            final String SQL =
                    "INSERT INTO transactions (book_id, member_id, issue_date, due_date, status) " +
                            "VALUES (?,?,?,?,'ISSUED')";

            int txnId;
            try (PreparedStatement ps = con.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, bookId);
                ps.setInt(2, memberId);
                ps.setDate(3, issueDate);
                ps.setDate(4, dueDate);
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new LibraryException("Failed to get txn ID.",
                            LibraryException.DB_ERROR);
                    txnId = keys.getInt(1);
                }
            }

            con.commit();

            Transaction txn = new Transaction(bookId, memberId, issueDate, dueDate);
            txn.setTxnId(txnId);
            txn.setBookTitle(bookDAO.getBookById(bookId).getTitle());
            txn.setMemberName(member.getName());
            return txn;

        } catch (LibraryException e) {
            rollback(con);
            throw e;
        } catch (SQLException e) {
            rollback(con);
            throw new LibraryException("issueBook failed: " + e.getMessage(),
                    LibraryException.DB_ERROR, e);
        } finally {
            resetAutoCommit(con);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  RETURN BOOK
    // ══════════════════════════════════════════════════════════════

    /**
     * Processes a book return, calculates overdue fine, and commits atomically.
     *
     * @throws LibraryException TXN_NOT_FOUND, ALREADY_RETURNED
     */
    public Transaction returnBook(int txnId) throws LibraryException {
        Transaction txn = getTransactionById(txnId);

        if (txn.getStatus() == Transaction.Status.RETURNED) {
            throw new LibraryException(
                    "Transaction " + txnId + " is already marked as RETURNED.",
                    LibraryException.ALREADY_RETURNED);
        }

        Connection con = null;
        try {
            con = DBConnection.getInstance().getConnection();
            con.setAutoCommit(false);

            LocalDate today      = LocalDate.now();
            LocalDate due        = txn.getDueDate().toLocalDate();
            BigDecimal fine      = BigDecimal.ZERO;
            Transaction.Status newStatus = Transaction.Status.RETURNED;

            if (today.isAfter(due)) {
                long overdueDays = java.time.temporal.ChronoUnit.DAYS.between(due, today);
                fine = FINE_PER_DAY.multiply(BigDecimal.valueOf(overdueDays));
                newStatus = Transaction.Status.RETURNED; // still RETURNED but fine recorded
            }

            // 1) Update transaction row
            final String SQL =
                    "UPDATE transactions SET return_date=?, fine_amount=?, status='RETURNED' " +
                            "WHERE txn_id=?";

            try (PreparedStatement ps = con.prepareStatement(SQL)) {
                ps.setDate(1, Date.valueOf(today));
                ps.setBigDecimal(2, fine);
                ps.setInt(3, txnId);
                ps.executeUpdate();
            }

            // 2) Restore availability
            bookDAO.adjustAvailability(txn.getBookId(), +1, con);

            con.commit();

            txn.setReturnDate(Date.valueOf(today));
            txn.setFineAmount(fine);
            txn.setStatus(newStatus);
            return txn;

        } catch (LibraryException e) {
            rollback(con);
            throw e;
        } catch (SQLException e) {
            rollback(con);
            throw new LibraryException("returnBook failed: " + e.getMessage(),
                    LibraryException.DB_ERROR, e);
        } finally {
            resetAutoCommit(con);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  READ
    // ══════════════════════════════════════════════════════════════

    public Transaction getTransactionById(int txnId) throws LibraryException {
        final String SQL =
                "SELECT t.*, b.title, m.name AS member_name " +
                        "FROM transactions t " +
                        "JOIN books   b ON t.book_id   = b.book_id " +
                        "JOIN members m ON t.member_id = m.member_id " +
                        "WHERE t.txn_id = ?";

        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(SQL)) {

            ps.setInt(1, txnId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new LibraryException(
                            "No transaction found with ID " + txnId,
                            LibraryException.TXN_NOT_FOUND);
                }
                return mapRow(rs);
            }

        } catch (LibraryException e) {
            throw e;
        } catch (SQLException e) {
            throw new LibraryException("getTransactionById failed: " + e.getMessage(),
                    LibraryException.DB_ERROR, e);
        }
    }

    public List<Transaction> getAllTransactions() throws LibraryException {
        final String SQL =
                "SELECT t.*, b.title, m.name AS member_name " +
                        "FROM transactions t " +
                        "JOIN books   b ON t.book_id   = b.book_id " +
                        "JOIN members m ON t.member_id = m.member_id " +
                        "ORDER BY t.txn_id DESC";

        return queryList(SQL);
    }

    public List<Transaction> getTransactionsByMember(int memberId) throws LibraryException {
        memberDAO.getMemberById(memberId); // verify member exists

        final String SQL =
                "SELECT t.*, b.title, m.name AS member_name " +
                        "FROM transactions t " +
                        "JOIN books   b ON t.book_id   = b.book_id " +
                        "JOIN members m ON t.member_id = m.member_id " +
                        "WHERE t.member_id = ? ORDER BY t.txn_id DESC";

        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(SQL)) {
            ps.setInt(1, memberId);
            List<Transaction> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
            return list;
        } catch (LibraryException e) {
            throw e;
        } catch (SQLException e) {
            throw new LibraryException("getTransactionsByMember failed: " + e.getMessage(),
                    LibraryException.DB_ERROR, e);
        }
    }

    public List<Transaction> getActiveTransactions() throws LibraryException {
        final String SQL =
                "SELECT t.*, b.title, m.name AS member_name " +
                        "FROM transactions t " +
                        "JOIN books   b ON t.book_id   = b.book_id " +
                        "JOIN members m ON t.member_id = m.member_id " +
                        "WHERE t.status = 'ISSUED' ORDER BY t.due_date";

        return queryList(SQL);
    }

    public List<Transaction> getOverdueTransactions() throws LibraryException {
        final String SQL =
                "SELECT t.*, b.title, m.name AS member_name " +
                        "FROM transactions t " +
                        "JOIN books   b ON t.book_id   = b.book_id " +
                        "JOIN members m ON t.member_id = m.member_id " +
                        "WHERE t.status = 'ISSUED' AND t.due_date < CURRENT_DATE " +
                        "ORDER BY t.due_date";

        return queryList(SQL);
    }

    // ══════════════════════════════════════════════════════════════
    //  DELETE
    // ══════════════════════════════════════════════════════════════

    public void deleteTransaction(int txnId) throws LibraryException {
        Transaction txn = getTransactionById(txnId);

        if (txn.getStatus() == Transaction.Status.ISSUED) {
            throw new LibraryException(
                    "Cannot delete an active (ISSUED) transaction. Return the book first.",
                    LibraryException.DB_ERROR);
        }

        final String SQL = "DELETE FROM transactions WHERE txn_id = ?";
        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(SQL)) {
            ps.setInt(1, txnId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new LibraryException("deleteTransaction failed: " + e.getMessage(),
                    LibraryException.DB_ERROR, e);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════

    private List<Transaction> queryList(String sql) throws LibraryException {
        try (Connection con = DBConnection.getInstance().getConnection();
             Statement  st  = con.createStatement();
             ResultSet  rs  = st.executeQuery(sql)) {

            List<Transaction> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;

        } catch (SQLException e) {
            throw new LibraryException("Query failed: " + e.getMessage(),
                    LibraryException.DB_ERROR, e);
        }
    }

    private Transaction mapRow(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setTxnId(rs.getInt("txn_id"));
        t.setBookId(rs.getInt("book_id"));
        t.setMemberId(rs.getInt("member_id"));
        t.setIssueDate(rs.getDate("issue_date"));
        t.setDueDate(rs.getDate("due_date"));
        t.setReturnDate(rs.getDate("return_date"));
        t.setFineAmount(rs.getBigDecimal("fine_amount"));
        t.setStatus(rs.getString("status"));
        t.setBookTitle(rs.getString("title"));
        t.setMemberName(rs.getString("member_name"));
        return t;
    }

    private void rollback(Connection con) {
        if (con != null) {
            try { con.rollback(); }
            catch (SQLException e) { System.err.println("Rollback failed: " + e.getMessage()); }
        }
    }

    private void resetAutoCommit(Connection con) {
        if (con != null) {
            try { con.setAutoCommit(true); }
            catch (SQLException e) { System.err.println("AutoCommit reset failed."); }
        }
    }
}