package model;

import java.sql.Timestamp;

/**
 * Represents a library member.
 */
public class Member {

    public enum Status { ACTIVE, SUSPENDED, INACTIVE }

    private int       memberId;
    private String    name;
    private String    email;
    private String    phone;
    private String    address;
    private Status    status;
    private Timestamp joinedOn;

    // ── Constructors ─────────────────────────────────────────────
    public Member() {}

    public Member(String name, String email, String phone, String address) {
        this.name    = name;
        this.email   = email;
        this.phone   = phone;
        this.address = address;
        this.status  = Status.ACTIVE;
    }

    // ── Getters & Setters ────────────────────────────────────────
    public int      getMemberId()        { return memberId; }
    public void     setMemberId(int v)   { this.memberId = v; }

    public String   getName()            { return name; }
    public void     setName(String v)    { this.name = v; }

    public String   getEmail()           { return email; }
    public void     setEmail(String v)   { this.email = v; }

    public String   getPhone()           { return phone; }
    public void     setPhone(String v)   { this.phone = v; }

    public String   getAddress()         { return address; }
    public void     setAddress(String v) { this.address = v; }

    public Status   getStatus()          { return status; }
    public void     setStatus(Status v)  { this.status = v; }
    public void     setStatus(String v)  { this.status = Status.valueOf(v); }

    public Timestamp getJoinedOn()             { return joinedOn; }
    public void      setJoinedOn(Timestamp v)  { this.joinedOn = v; }

    // ── Display ──────────────────────────────────────────────────
    @Override
    public String toString() {
        return String.format(
                "┌─ Member ID : %-5d\n│  Name     : %s\n│  Email    : %s\n" +
                        "│  Phone    : %s\n│  Address  : %s\n│  Status   : %s\n" +
                        "└─ Joined   : %s",
                memberId, name, email, phone, address, status, joinedOn);
    }

    public String toRow() {
        return String.format("%-5d | %-20s | %-25s | %-12s | %-10s",
                memberId, name, email, phone, status);
    }
}