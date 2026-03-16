package menu;

import dao.MemberDAO;
import exception.LibraryException;
import model.Member;

import java.util.List;
import java.util.Scanner;

/**
 * Console menu for Member CRUD operations.
 */
public class MemberMenu {

    private final MemberDAO memberDAO = new MemberDAO();
    private final Scanner sc;

    public MemberMenu(Scanner sc) {
        this.sc = sc;
    }

    public void show() {
        while (true) {
            System.out.println("\n╔══════════════════════════════╗");
            System.out.println("║      MEMBER MANAGEMENT       ║");
            System.out.println("╠══════════════════════════════╣");
            System.out.println("║  1. Register New Member       ║");
            System.out.println("║  2. View All Members          ║");
            System.out.println("║  3. Find Member by ID         ║");
            System.out.println("║  4. Search Members            ║");
            System.out.println("║  5. Update Member             ║");
            System.out.println("║  6. Suspend / Activate Member ║");
            System.out.println("║  7. Delete Member             ║");
            System.out.println("║  0. Back to Main Menu         ║");
            System.out.println("╚══════════════════════════════╝");
            System.out.print("  Enter choice: ");

            int choice = readInt();
            switch (choice) {
                case 1 -> registerMember();
                case 2 -> viewAllMembers();
                case 3 -> findMemberById();
                case 4 -> searchMembers();
                case 5 -> updateMember();
                case 6 -> toggleStatus();
                case 7 -> deleteMember();
                case 0 -> { return; }
                default -> System.out.println("⚠  Invalid choice.");
            }
        }
    }

    // ── REGISTER ───────────────────────────────────────────────────
    private void registerMember() {
        System.out.println("\n── Register New Member ──");
        System.out.print("Full Name : ");  String name    = sc.nextLine().trim();
        System.out.print("Email     : ");  String email   = sc.nextLine().trim();
        System.out.print("Phone     : ");  String phone   = sc.nextLine().trim();
        System.out.print("Address   : ");  String address = sc.nextLine().trim();

        if (name.isEmpty() || email.isEmpty()) {
            System.out.println("❌  Name and Email are required.");
            return;
        }

        try {
            Member m = memberDAO.addMember(new Member(name, email, phone, address));
            System.out.println("✅  Member registered! ID = " + m.getMemberId());
        } catch (LibraryException e) {
            System.out.println("❌  " + e.getMessage());
        }
    }

    // ── VIEW ALL ───────────────────────────────────────────────────
    private void viewAllMembers() {
        try {
            List<Member> members = memberDAO.getAllMembers();
            if (members.isEmpty()) { System.out.println("ℹ  No members found."); return; }
            printHeader();
            members.forEach(m -> System.out.println(m.toRow()));
            System.out.println("─".repeat(85));
            System.out.println("  Total members: " + members.size());
        } catch (LibraryException e) {
            System.out.println("❌  " + e.getMessage());
        }
    }

    // ── FIND BY ID ─────────────────────────────────────────────────
    private void findMemberById() {
        System.out.print("Enter Member ID: ");
        int id = readInt();
        try {
            System.out.println(memberDAO.getMemberById(id));
        } catch (LibraryException e) {
            System.out.println("❌  " + e.getMessage());
        }
    }

    // ── SEARCH ─────────────────────────────────────────────────────
    private void searchMembers() {
        System.out.print("Search (name / email): ");
        String keyword = sc.nextLine().trim();
        if (keyword.isEmpty()) { System.out.println("⚠  Enter a keyword."); return; }

        try {
            List<Member> members = memberDAO.searchMembers(keyword);
            if (members.isEmpty()) {
                System.out.println("ℹ  No members found for '" + keyword + "'.");
                return;
            }
            printHeader();
            members.forEach(m -> System.out.println(m.toRow()));
            System.out.println("─".repeat(85));
        } catch (LibraryException e) {
            System.out.println("❌  " + e.getMessage());
        }
    }

    // ── UPDATE ─────────────────────────────────────────────────────
    private void updateMember() {
        System.out.print("Enter Member ID to update: ");
        int id = readInt();

        try {
            Member m = memberDAO.getMemberById(id);
            System.out.println("Current: " + m.toRow());
            System.out.println("(Press ENTER to keep current value)");

            System.out.print("Name    [" + m.getName()    + "]: ");
            String name = sc.nextLine().trim();

            System.out.print("Email   [" + m.getEmail()   + "]: ");
            String email = sc.nextLine().trim();

            System.out.print("Phone   [" + m.getPhone()   + "]: ");
            String phone = sc.nextLine().trim();

            System.out.print("Address [" + m.getAddress() + "]: ");
            String address = sc.nextLine().trim();

            if (!name.isEmpty())    m.setName(name);
            if (!email.isEmpty())   m.setEmail(email);
            if (!phone.isEmpty())   m.setPhone(phone);
            if (!address.isEmpty()) m.setAddress(address);

            memberDAO.updateMember(m);
            System.out.println("✅  Member updated successfully.");

        } catch (LibraryException e) {
            System.out.println("❌  " + e.getMessage());
        }
    }

    // ── TOGGLE STATUS ──────────────────────────────────────────────
    private void toggleStatus() {
        System.out.print("Enter Member ID: ");
        int id = readInt();

        try {
            Member m = memberDAO.getMemberById(id);
            System.out.println("Current status: " + m.getStatus());
            System.out.println("1. ACTIVE  2. SUSPENDED  3. INACTIVE");
            System.out.print("Choose new status: ");
            int s = readInt();

            Member.Status newStatus = switch (s) {
                case 1 -> Member.Status.ACTIVE;
                case 2 -> Member.Status.SUSPENDED;
                case 3 -> Member.Status.INACTIVE;
                default -> null;
            };

            if (newStatus == null) { System.out.println("⚠  Invalid choice."); return; }
            m.setStatus(newStatus);
            memberDAO.updateMember(m);
            System.out.println("✅  Status changed to " + newStatus + ".");

        } catch (LibraryException e) {
            System.out.println("❌  " + e.getMessage());
        }
    }

    // ── DELETE ─────────────────────────────────────────────────────
    private void deleteMember() {
        System.out.print("Enter Member ID to delete: ");
        int id = readInt();
        System.out.print("⚠  Delete Member ID " + id + "? (yes/no): ");
        String confirm = sc.nextLine().trim();

        if (!confirm.equalsIgnoreCase("yes")) {
            System.out.println("  Cancelled.");
            return;
        }

        try {
            memberDAO.deleteMember(id);
            System.out.println("✅  Member ID " + id + " deleted.");
        } catch (LibraryException e) {
            System.out.println("❌  " + e.getMessage());
        }
    }

    // ── Utilities ──────────────────────────────────────────────────
    private void printHeader() {
        System.out.println("\n" + "─".repeat(85));
        System.out.printf("%-5s | %-20s | %-25s | %-12s | %-10s%n",
                "ID", "Name", "Email", "Phone", "Status");
        System.out.println("─".repeat(85));
    }

    private int readInt() {
        try {
            return Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}