package dao;

import config.DBConnection;
import exception.LibraryException;
import model.Member;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the members table.
 */
public class MemberDAO {

    // ══════════════════════════════════════════════════════════════
    //  CREATE
    // ══════════════════════════════════════════════════════════════

    /**
     * Inserts a new member and sets the generated member_id.
     *
     * @throws LibraryException DUPLICATE_EMAIL if email already exists
     */
    public Member addMember(Member member) throws LibraryException {
        final String SQL =
                "INSERT INTO members (name, email, phone, address, status) VALUES (?,?,?,?,?)";

        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, member.getName());
            ps.setString(2, member.getEmail());
            ps.setString(3, member.getPhone());
            ps.setString(4, member.getAddress());
            ps.setString(5, member.getStatus() != null
                    ? member.getStatus().name() : "ACTIVE");

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new LibraryException("Member insert failed — no rows affected.",
                        LibraryException.DB_ERROR);
            }

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) member.setMemberId(keys.getInt(1));
            }
            return member;

        } catch (SQLIntegrityConstraintViolationException e) {
            throw new LibraryException(
                    "Duplicate email: '" + member.getEmail() + "' is already registered.",
                    LibraryException.DUPLICATE_EMAIL, e);
        } catch (SQLException e) {
            throw new LibraryException("addMember failed: " + e.getMessage(),
                    LibraryException.DB_ERROR, e);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  READ — single
    // ══════════════════════════════════════════════════════════════

    /**
     * @throws LibraryException MEMBER_NOT_FOUND
     */
    public Member getMemberById(int memberId) throws LibraryException {
        final String SQL = "SELECT * FROM members WHERE member_id = ?";

        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(SQL)) {

            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new LibraryException(
                            "No member found with ID " + memberId,
                            LibraryException.MEMBER_NOT_FOUND);
                }
                return mapRow(rs);
            }

        } catch (LibraryException e) {
            throw e;
        } catch (SQLException e) {
            throw new LibraryException("getMemberById failed: " + e.getMessage(),
                    LibraryException.DB_ERROR, e);
        }
    }

    public Member getMemberByEmail(String email) throws LibraryException {
        final String SQL = "SELECT * FROM members WHERE email = ?";

        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(SQL)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new LibraryException(
                            "No member found with email " + email,
                            LibraryException.MEMBER_NOT_FOUND);
                }
                return mapRow(rs);
            }

        } catch (LibraryException e) {
            throw e;
        } catch (SQLException e) {
            throw new LibraryException("getMemberByEmail failed: " + e.getMessage(),
                    LibraryException.DB_ERROR, e);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  READ — list
    // ══════════════════════════════════════════════════════════════

    public List<Member> getAllMembers() throws LibraryException {
        final String SQL = "SELECT * FROM members ORDER BY name";

        try (Connection con = DBConnection.getInstance().getConnection();
             Statement  st  = con.createStatement();
             ResultSet  rs  = st.executeQuery(SQL)) {

            List<Member> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;

        } catch (SQLException e) {
            throw new LibraryException("getAllMembers failed: " + e.getMessage(),
                    LibraryException.DB_ERROR, e);
        }
    }

    public List<Member> searchMembers(String keyword) throws LibraryException {
        final String SQL =
                "SELECT * FROM members WHERE name LIKE ? OR email LIKE ? ORDER BY name";

        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(SQL)) {

            String like = "%" + keyword + "%";
            ps.setString(1, like);
            ps.setString(2, like);

            List<Member> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
            return list;

        } catch (SQLException e) {
            throw new LibraryException("searchMembers failed: " + e.getMessage(),
                    LibraryException.DB_ERROR, e);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  UPDATE
    // ══════════════════════════════════════════════════════════════

    public void updateMember(Member member) throws LibraryException {
        getMemberById(member.getMemberId()); // verify exists

        final String SQL =
                "UPDATE members SET name=?, email=?, phone=?, address=?, status=? " +
                        "WHERE member_id=?";

        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(SQL)) {

            ps.setString(1, member.getName());
            ps.setString(2, member.getEmail());
            ps.setString(3, member.getPhone());
            ps.setString(4, member.getAddress());
            ps.setString(5, member.getStatus().name());
            ps.setInt(6, member.getMemberId());

            ps.executeUpdate();

        } catch (SQLIntegrityConstraintViolationException e) {
            throw new LibraryException(
                    "Email '" + member.getEmail() + "' is already used by another member.",
                    LibraryException.DUPLICATE_EMAIL, e);
        } catch (LibraryException e) {
            throw e;
        } catch (SQLException e) {
            throw new LibraryException("updateMember failed: " + e.getMessage(),
                    LibraryException.DB_ERROR, e);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  DELETE
    // ══════════════════════════════════════════════════════════════

    public void deleteMember(int memberId) throws LibraryException {
        getMemberById(memberId); // verify exists

        final String SQL = "DELETE FROM members WHERE member_id = ?";

        try (Connection con = DBConnection.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(SQL)) {

            ps.setInt(1, memberId);
            ps.executeUpdate();

        } catch (SQLIntegrityConstraintViolationException e) {
            throw new LibraryException(
                    "Cannot delete member ID " + memberId + " — they have transaction records.",
                    LibraryException.DB_ERROR, e);
        } catch (LibraryException e) {
            throw e;
        } catch (SQLException e) {
            throw new LibraryException("deleteMember failed: " + e.getMessage(),
                    LibraryException.DB_ERROR, e);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  MAPPER
    // ══════════════════════════════════════════════════════════════

    private Member mapRow(ResultSet rs) throws SQLException {
        Member m = new Member();
        m.setMemberId(rs.getInt("member_id"));
        m.setName(rs.getString("name"));
        m.setEmail(rs.getString("email"));
        m.setPhone(rs.getString("phone"));
        m.setAddress(rs.getString("address"));
        m.setStatus(rs.getString("status"));
        m.setJoinedOn(rs.getTimestamp("joined_on"));
        return m;
    }
}