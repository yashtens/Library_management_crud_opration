package menu;

import dao.TransactionDAO;
import exception.LibraryException;
import model.Transaction;

import java.util.List;
import java.util.Scanner;

/**
 * Console menu for Transaction operations: issue book, return book, view history.
 */
public class TransactionMenu {

    private final TransactionDAO txnDAO = new TransactionDAO();
    private final Scanner sc;

    public TransactionMenu(Scanner sc) {
        this.sc = sc;
    }

    public void show() {
        while (true) {
            System.out.println("\n╔══════════════════════════════════╗");
            System.out.println("║      TRANSACTION MANAGEMENT      ║");
            System.out.println("╠══════════════════════════════════╣");
            System.out.println("║  1. Issue a Book                  ║");
            System.out.println("║  2. Return a Book                 ║");
            System.out.println("║  3. View All Transactions         ║");
            System.out.println("║  4. View Active (Issued) Books    ║");
            System.out.println("║  5. View Overdue Transactions     ║");
            System.out.println("║  6. Transaction by ID             ║");
            System.out.println("║  7. Member's Transaction History  ║");
            System.out.println("║  8. Delete Transaction Record     ║");
            System.out.println("║  0. Back to Main Menu             ║");
            System.out.println("╚══════════════════════════════════╝");
            System.out.print("  Enter choice: ");

            int choice = readInt();
            switch (choice) {
                case 1 -> issueBook();
                case 2 -> returnBook();
                case 3 -> viewAll();
                case 4 -> viewActive();
                case 5 -> viewOverdue();
                case 6 -> viewById();
                case 7 -> memberHistory();
                case 8 -> deleteTransaction();
                case 0 -> { return; }
                default -> System.out.println("⚠  Invalid choice.");
            }
        }
    }

    // ── ISSUE ──────────────────────────────────────────────────────
    private void issueBook() {
        System.out.println("\n── Issue Book ──");
        System.out.print("Book ID   : ");  int bookId   = readInt();
        System.out.print("Member ID : ");  int memberId = readInt();
        System.out.print("Loan days (default 14): ");
        String daysStr = sc.nextLine().trim();
        int days = daysStr.isEmpty() ? 14 : Integer.parseInt(daysStr);

        if (bookId < 1 || memberId < 1 || days < 1) {
            System.out.println("❌  Invalid input values.");
            return;
        }

        try {
            Transaction txn = txnDAO.issueBook(bookId, memberId, days);
            System.out.println("✅  Book issued successfully!");
            System.out.println(txn);
        } catch (LibraryException e) {
            System.out.println("❌  " + e.getMessage());
        }
    }

    // ── RETURN ─────────────────────────────────────────────────────
    private void returnBook() {
        System.out.println("\n── Return Book ──");
        System.out.print("Transaction ID: ");
        int txnId = readInt();
        if (txnId < 1) { System.out.println("❌  Invalid transaction ID."); return; }

        try {
            Transaction txn = txnDAO.returnBook(txnId);
            System.out.println("✅  Book returned successfully!");
            System.out.println(txn);
            if (txn.getFineAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                System.out.println("💰  Overdue fine: ₹" + txn.getFineAmount());
            }
        } catch (LibraryException e) {
            System.out.println("❌  " + e.getMessage());
        }
    }

    // ── VIEW ALL ───────────────────────────────────────────────────
    private void viewAll() {
        try {
            List<Transaction> list = txnDAO.getAllTransactions();
            if (list.isEmpty()) { System.out.println("ℹ  No transactions found."); return; }
            printHeader();
            list.forEach(t -> System.out.println(t.toRow()));
            System.out.println("─".repeat(110));
            System.out.println("  Total: " + list.size() + " transaction(s).");
        } catch (LibraryException e) {
            System.out.println("❌  " + e.getMessage());
        }
    }

    // ── ACTIVE ─────────────────────────────────────────────────────
    private void viewActive() {
        try {
            List<Transaction> list = txnDAO.getActiveTransactions();
            if (list.isEmpty()) { System.out.println("ℹ  No books currently issued."); return; }
            System.out.println("\n  📖  Currently issued books:");
            printHeader();
            list.forEach(t -> System.out.println(t.toRow()));
            System.out.println("─".repeat(110));
        } catch (LibraryException e) {
            System.out.println("❌  " + e.getMessage());
        }
    }

    // ── OVERDUE ────────────────────────────────────────────────────
    private void viewOverdue() {
        try {
            List<Transaction> list = txnDAO.getOverdueTransactions();
            if (list.isEmpty()) { System.out.println("✅  No overdue transactions!"); return; }
            System.out.println("\n  ⚠  OVERDUE BOOKS (fine: ₹5/day):");
            printHeader();
            list.forEach(t -> System.out.println(t.toRow()));
            System.out.println("─".repeat(110));
            System.out.println("  ⚠  " + list.size() + " overdue transaction(s).");
        } catch (LibraryException e) {
            System.out.println("❌  " + e.getMessage());
        }
    }

    // ── VIEW BY ID ─────────────────────────────────────────────────
    private void viewById() {
        System.out.print("Transaction ID: ");
        int id = readInt();
        try {
            System.out.println(txnDAO.getTransactionById(id));
        } catch (LibraryException e) {
            System.out.println("❌  " + e.getMessage());
        }
    }

    // ── MEMBER HISTORY ─────────────────────────────────────────────
    private void memberHistory() {
        System.out.print("Member ID: ");
        int id = readInt();
        try {
            List<Transaction> list = txnDAO.getTransactionsByMember(id);
            if (list.isEmpty()) {
                System.out.println("ℹ  No transactions for member ID " + id + ".");
                return;
            }
            printHeader();
            list.forEach(t -> System.out.println(t.toRow()));
            System.out.println("─".repeat(110));
        } catch (LibraryException e) {
            System.out.println("❌  " + e.getMessage());
        }
    }

    // ── DELETE ─────────────────────────────────────────────────────
    private void deleteTransaction() {
        System.out.print("Transaction ID to delete: ");
        int id = readInt();
        System.out.print("⚠  Delete transaction ID " + id + "? (yes/no): ");
        String confirm = sc.nextLine().trim();
        if (!confirm.equalsIgnoreCase("yes")) { System.out.println("  Cancelled."); return; }

        try {
            txnDAO.deleteTransaction(id);
            System.out.println("✅  Transaction ID " + id + " deleted.");
        } catch (LibraryException e) {
            System.out.println("❌  " + e.getMessage());
        }
    }

    // ── Utilities ──────────────────────────────────────────────────
    private void printHeader() {
        System.out.println("\n" + "─".repeat(110));
        System.out.printf("%-5s | %-25s | %-20s | %-10s | %-10s | %-9s | %s%n",
                "ID", "Book", "Member", "Issued", "Due", "Status", "Fine");
        System.out.println("─".repeat(110));
    }

    private int readInt() {
        try {
            return Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}