package model;

import java.math.BigDecimal;
import java.sql.Date;

/**
 * Represents a book issue/return transaction.
 */
public class Transaction {

    public enum Status { ISSUED, RETURNED, OVERDUE }

    private int        txnId;
    private int        bookId;
    private int        memberId;
    private Date       issueDate;
    private Date       dueDate;
    private Date       returnDate;
    private BigDecimal fineAmount;
    private Status     status;

    // For display convenience (JOIN data)
    private String bookTitle;
    private String memberName;

    // ── Constructors ─────────────────────────────────────────────
    public Transaction() {}

    public Transaction(int bookId, int memberId, Date issueDate, Date dueDate) {
        this.bookId    = bookId;
        this.memberId  = memberId;
        this.issueDate = issueDate;
        this.dueDate   = dueDate;
        this.status    = Status.ISSUED;
        this.fineAmount = BigDecimal.ZERO;
    }

    // ── Getters & Setters ────────────────────────────────────────
    public int        getTxnId()              { return txnId; }
    public void       setTxnId(int v)         { this.txnId = v; }

    public int        getBookId()             { return bookId; }
    public void       setBookId(int v)        { this.bookId = v; }

    public int        getMemberId()           { return memberId; }
    public void       setMemberId(int v)      { this.memberId = v; }

    public Date       getIssueDate()          { return issueDate; }
    public void       setIssueDate(Date v)    { this.issueDate = v; }

    public Date       getDueDate()            { return dueDate; }
    public void       setDueDate(Date v)      { this.dueDate = v; }

    public Date       getReturnDate()         { return returnDate; }
    public void       setReturnDate(Date v)   { this.returnDate = v; }

    public BigDecimal getFineAmount()         { return fineAmount; }
    public void       setFineAmount(BigDecimal v) { this.fineAmount = v; }

    public Status     getStatus()             { return status; }
    public void       setStatus(Status v)     { this.status = v; }
    public void       setStatus(String v)     { this.status = Status.valueOf(v); }

    public String     getBookTitle()          { return bookTitle; }
    public void       setBookTitle(String v)  { this.bookTitle = v; }

    public String     getMemberName()         { return memberName; }
    public void       setMemberName(String v) { this.memberName = v; }

    // ── Display ──────────────────────────────────────────────────
    @Override
    public String toString() {
        return String.format(
                "┌─ Txn ID    : %-5d\n│  Book      : %s (ID:%d)\n│  Member    : %s (ID:%d)\n" +
                        "│  Issued    : %s\n│  Due       : %s\n│  Returned  : %s\n" +
                        "│  Fine      : ₹%.2f\n└─ Status    : %s",
                txnId,
                (bookTitle != null ? bookTitle : ""), bookId,
                (memberName != null ? memberName : ""), memberId,
                issueDate, dueDate,
                (returnDate != null ? returnDate.toString() : "Not returned"),
                fineAmount, status);
    }

    public String toRow() {
        return String.format("%-5d | %-25s | %-20s | %s | %s | %-9s | ₹%.2f",
                txnId,
                (bookTitle != null ? bookTitle : "Book#" + bookId),
                (memberName != null ? memberName : "Member#" + memberId),
                issueDate, dueDate, status, fineAmount);
    }
}