import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * User is the abstract base class for Customer and Admin classes.
 */
public abstract class User {

    private static final Pattern ID_PATTERN = Pattern.compile("^u_\\d{10}$");
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy_HH:mm:ss");

    // Tracks every ID in use so that IDs stay unique.
    private static final Set<String> usedIds = new HashSet<>();

    private String userId;            // format: u_10 digits
    private String userName;
    private String userPassword;      // should be encrypted for storage
    private String userRegisterTime;  // format: DD-MM-YYYY_HH:MM:SS
    private String userRole;          // "customer" or "admin"

    /**
     * Constructs a user object.
     * @param userId           Must be unique, format: u_10 digits, such as u_1234567890
     * @param userName         The user's name
     * @param userPassword     The user's password
     * @param userRegisterTime Format: "DD-MM-YYYY_HH:MM:SS"
     * @param userRole         Default value: "customer"
     */
    public User(String userId, String userName, String userPassword,
                String userRegisterTime, String userRole) {
        if (userId == null || !ID_PATTERN.matcher(userId).matches()) {
            throw new IllegalArgumentException("Invalid user id (expected u_ + 10 digits): " + userId);
        }
        if (!usedIds.add(userId)) {
            throw new IllegalArgumentException("User id already exists: " + userId);
        }
        this.userId = userId;
        this.userName = userName;
        this.userPassword = userPassword;
        this.userRegisterTime = userRegisterTime;
        this.userRole = (userRole == null || userRole.isEmpty()) ? "customer" : userRole;
    }

    /**
     * Default constructor (not registered in the unique-ID set).
     */
    public User() {
        this.userId = "u_0000000000";
        this.userName = "";
        this.userPassword = "";
        this.userRegisterTime = LocalDateTime.now().format(TIME_FORMAT);
        this.userRole = "customer";
    }

    public static boolean isIdTaken(String id) {
        return usedIds.contains(id);
    }

    // Getters / setters (the ID is immutable once created)
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserPassword() { return userPassword; }
    public void setUserPassword(String userPassword) { this.userPassword = userPassword; }
    public String getUserRegisterTime() { return userRegisterTime; }
    public void setUserRegisterTime(String userRegisterTime) { this.userRegisterTime = userRegisterTime; }
    public String getUserRole() { return userRole; }
    protected void setUserRole(String userRole) { this.userRole = userRole; }

    /**
     * Returns the user information as a formatted string.
     * @return String in JSON-like format
     */
    @Override
    public String toString() {
        return "{\"user_id\":\"" + userId + "\", \"user_name\":\"" + userName
                + "\", \"user_password\":\"" + userPassword
                + "\", \"user_register_time\":\"" + userRegisterTime
                + "\", \"user_role\":\"" + userRole + "\"}";
    }
}