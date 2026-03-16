import config.DBConnection;
import exception.LibraryException;
import menu.BookMenu;
import menu.MemberMenu;
import menu.TransactionMenu;

import java.util.Scanner;

/**
 * ╔══════════════════════════════════════════════════════╗
 * ║     Library Management System — JDBC + MySQL          ║
 * ║     Manages: Books | Members | Transactions           ║
 * ╚══════════════════════════════════════════════════════╝
 *
 * Entry point.  Edit DBConnection.java with your credentials
 * then run:  mvn package && java -jar LibraryManagement-1.0.0.jar
 */
public class Main {

    public static void main(String[] args) {

        // ── Startup banner ───────────────────────────────────────
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║    📚  Library Management System  📚          ║");
        System.out.println("║                          ");
        System.out.println("╚══════════════════════════════════════════════╝");

        // ── Verify DB connectivity ───────────────────────────────
        try {
            DBConnection.getInstance();
        } catch (LibraryException e) {
            System.err.println("💥  FATAL: " + e.getMessage());
            System.err.println("    Check your DB credentials in config/DBConnection.java");
            System.exit(1);
        }

        // ── Main menu loop ───────────────────────────────────────
        Scanner sc = new Scanner(System.in);
        BookMenu        bookMenu   = new BookMenu(sc);
        MemberMenu      memberMenu = new MemberMenu(sc);
        TransactionMenu txnMenu    = new TransactionMenu(sc);

        while (true) {
            System.out.println("\n╔══════════════════════════════╗");
            System.out.println("║         MAIN MENU            ║");
            System.out.println("╠══════════════════════════════╣");
            System.out.println("║  1. 📖  Book Management       ║");
            System.out.println("║  2. 👤  Member Management     ║");
            System.out.println("║  3. 🔄  Transactions          ║");
            System.out.println("║  0. 🚪  Exit                  ║");
            System.out.println("╚══════════════════════════════╝");
            System.out.print("  Your choice: ");

            String input = sc.nextLine().trim();
            int choice;
            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("⚠  Please enter a number.");
                continue;
            }

            switch (choice) {
                case 1 -> bookMenu.show();
                case 2 -> memberMenu.show();
                case 3 -> txnMenu.show();
                case 0 -> {
                    System.out.println("\n  Closing database connection...");
                    try { DBConnection.getInstance().closeConnection(); } catch (LibraryException ex) { /* ignore */ }
                    System.out.println("  Goodbye! 👋");
                    sc.close();
                    System.exit(0);
                }
                default -> System.out.println("⚠  Invalid choice. Enter 0-3.");
            }
        }
    }
}