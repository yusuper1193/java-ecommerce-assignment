import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Admin extends User. All admins are saved in the file "data/users.txt".
 */
public class Admin extends User {

    // User's ID generator is private, so Admin keeps its own counter.
    // It starts at a high range so default admin IDs don't collide with
    // the IDs that User auto-generates (u_0000000001, u_0000000002, ...).
    private static long nextAdminIdNumber = 9000000000L;

    /**
     * Constructs an admin object.
     * @param userId           Must be unique, format: u_10 digits, such as u_1234567890
     * @param userName         The user's name
     * @param userPassword     The user's password
     * @param userRegisterTime Format: "DD-MM-YYYY_HH:MM:SS"
     * @param userRole         Default value: "admin"
     */
    public Admin(String userId, String userName, String userPassword,
                 String userRegisterTime, String userRole) {
        // User would default an empty role to "customer", so pass "admin" explicitly
        super(userId, userName, userPassword, userRegisterTime,
                (userRole == null || userRole.isEmpty()) ? "admin" : userRole);
    }

    /**
     * Default constructor
     */
    public Admin() {
        super(String.format("u_%010d", nextAdminIdNumber++), "", "",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy_HH:mm:ss")),
                "admin");
    }

    /**
     * Returns the admin's attributes as a formatted string.
     * @return String in JSON-like format
     */
    @Override
    public String toString() {
        return super.toString(); // already in the required format, role is "admin"
    }
}