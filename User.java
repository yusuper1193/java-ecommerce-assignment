import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

/**
 * User is the abstract base class for Customer and Admin classes.
 */
public abstract class User {
    // IDs already in use + next number for auto-increment (u_0000000001, ...)
    private static final Set<String> usedIds = new HashSet<>();
    private static int nextIdNumber = 1;

    private String userId;            // format: u_ + 10 digits, unique
    private String userName;
    private String userPassword;      // should be encrypted for storage
    private String userRegisterTime;  // format: DD-MM-YYYY_HH:MM:SS
    private String userRole;          // "customer" or "admin"

    /**
     * Constructs a user object.
     * @param userId Must be unique, format: u_10 digits, such as u_1234567890
     * @param userName The user's name
     * @param userPassword The user's password
     * @param userRegisterTime Format: "DD-MM-YYYY_HH:MM:SS"
     * @param userRole Default value: "customer"
     */
    public User(String userId, String userName, String userPassword,
                String userRegisterTime, String userRole) {
        if (userId == null || !userId.matches("u_\\d{10}")) {
            throw new IllegalArgumentException("Invalid user id (expected u_ + 10 digits): " + userId);
        }
        if (userRole == null || userRole.isEmpty()) {
            userRole = "customer";
        }
        if (!userRole.equals("customer") && !userRole.equals("admin")) {
            throw new IllegalArgumentException("Role must be \"customer\" or \"admin\": " + userRole);
        }
        if (!usedIds.add(userId)) {
            throw new IllegalArgumentException("User id already exists: " + userId);
        }
        // keep the counter ahead of existing IDs so auto IDs never collide
        nextIdNumber = Math.max(nextIdNumber, Integer.parseInt(userId.substring(2)) + 1);

        this.userId = userId;
        this.userName = userName;
        this.userPassword = userPassword;
        this.userRegisterTime = userRegisterTime;
        this.userRole = userRole;
    }

    /**
     * Default constructor: ID is auto-incremented, other values are defaults.
     */
    public User() {
        this(generateId(), "", "",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy_HH:mm:ss")),
                "customer");
    }

    private static String generateId() {
        String id;
        do {
            id = String.format("u_%010d", nextIdNumber++);
        } while (usedIds.contains(id));
        return id;
    }

    // ----- Added: getters and a protected role setter (needed by Admin) -----
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getUserPassword() { return userPassword; }
    public String getUserRegisterTime() { return userRegisterTime; }
    public String getUserRole() { return userRole; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setUserPassword(String userPassword) { this.userPassword = userPassword; }

    protected void setUserRole(String userRole) {
        if (!"customer".equals(userRole) && !"admin".equals(userRole)) {
            throw new IllegalArgumentException("Role must be \"customer\" or \"admin\": " + userRole);
        }
        this.userRole = userRole;
    }

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