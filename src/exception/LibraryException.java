package exception;

/**
 * Custom checked exception for all Library business-rule violations.
 * Wraps SQLExceptions and domain errors in one unified type so the
 * menu layer only needs to catch a single exception family.
 */
public class LibraryException extends Exception {

    private final int errorCode;

    // ── Common error codes ──────────────────────────────────────
    public static final int DB_ERROR            = 1000;
    public static final int BOOK_NOT_FOUND      = 1001;
    public static final int BOOK_NOT_AVAILABLE  = 1002;
    public static final int MEMBER_NOT_FOUND    = 1003;
    public static final int MEMBER_SUSPENDED    = 1004;
    public static final int TXN_NOT_FOUND       = 1005;
    public static final int DUPLICATE_ISBN      = 1006;
    public static final int DUPLICATE_EMAIL     = 1007;
    public static final int INVALID_INPUT       = 1008;
    public static final int ALREADY_RETURNED    = 1009;

    public LibraryException(String message) {
        super(message);
        this.errorCode = 0;
    }

    public LibraryException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public LibraryException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = DB_ERROR;
    }

    public LibraryException(String message, int errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        return String.format("[LibraryException | Code %d] %s", errorCode, getMessage());
    }
}