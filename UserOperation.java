import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserOperation {

    // ---------- Singleton ----------
    private static volatile UserOperation instance;

    // Package-private so CustomerOperation uses the exact same file.
    static final Path USER_FILE = Paths.get("data-sample", "users.csv");
    private static final String CHARSET =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final SecureRandom random = new SecureRandom();

    // Admin only allows one object per ID, so logged-in admins are kept here and reused.
    private final Map<String, Admin> adminCache = new HashMap<>();

    /** Private constructor: prevents instantiation from outside. */
    private UserOperation() {
    }

    /**
     * Returns the single instance of UserOperation.
     * @return UserOperation instance
     */
    public static UserOperation getInstance() {
        if (instance == null) {                       // 1st check (no lock)
            synchronized (UserOperation.class) {
                if (instance == null) {               // 2nd check (locked)
                    instance = new UserOperation();
                }
            }
        }
        return instance;
    }

    // ---------- Required operations ----------

    /**
     * Generates and returns a 10-digit unique user id starting with 'u_'
     * every time when a new user is registered.
     * @return A string value in the format 'u_10digits', e.g., 'u_1234567890'
     */
    public String generateUniqueUserId() {
        Set<String> existingIds = new HashSet<>();
        for (String line : readUserLines()) {
            String id = extractField(line, "user_id");
            if (id != null) {
                existingIds.add(id);
            }
        }

        String newId;
        do {
            StringBuilder sb = new StringBuilder("u_");
            for (int i = 0; i < 10; i++) {
                sb.append(random.nextInt(10));
            }
            newId = sb.toString();
        } while (existingIds.contains(newId));

        return newId;
    }

    /**
     * Encode a user-provided password.
     * Format: ^^ + (rand, rand, passwordChar) repeated + $$
     * @param userPassword The password to encrypt
     * @return Encrypted password
     */
    public String encryptPassword(String userPassword) {
        if (userPassword == null) {
            return null;
        }

        int len = userPassword.length();
        StringBuilder randomStr = new StringBuilder();
        for (int i = 0; i < len * 2; i++) {
            randomStr.append(CHARSET.charAt(random.nextInt(CHARSET.length())));
        }

        StringBuilder encrypted = new StringBuilder("^^");
        for (int i = 0; i < len; i++) {
            encrypted.append(randomStr.charAt(2 * i));
            encrypted.append(randomStr.charAt(2 * i + 1));
            encrypted.append(userPassword.charAt(i));
        }
        encrypted.append("$$");

        return encrypted.toString();
    }

    /**
     * Decode the encrypted password with a similar rule as the encryption method.
     * @param encryptedPassword The encrypted password to decrypt
     * @return Original user-provided password
     */
    public String decryptPassword(String encryptedPassword) {
        if (encryptedPassword == null
                || !encryptedPassword.startsWith("^^")
                || !encryptedPassword.endsWith("$$")
                || encryptedPassword.length() < 4) {
            return null;
        }

        // strip the "^^" prefix and "$$" suffix
        String body = encryptedPassword.substring(2, encryptedPassword.length() - 2);

        // every 3rd character (index 2, 5, 8, ...) is a real password character
        StringBuilder original = new StringBuilder();
        for (int i = 2; i < body.length(); i += 3) {
            original.append(body.charAt(i));
        }

        return original.toString();
    }

    /**
     * Verify whether a user is already registered or exists in the system.
     * @param userName The username to check
     * @return true if exists, false otherwise
     */
    public boolean checkUsernameExist(String userName) {
        if (userName == null) {
            return false;
        }
        for (String line : readUserLines()) {
            String name = extractField(line, "user_name");
            if (userName.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validate the user's name. Only letters or underscores, at least 5 characters.
     * @param userName The username to validate
     * @return true if valid, false otherwise
     */
    public boolean validateUsername(String userName) {
        return userName != null && userName.matches("[A-Za-z_]{5,}");
    }

    /**
     * Validate the user's password. Must contain at least one letter and one
     * number, and be at least 5 characters long.
     * @param userPassword The password to validate
     * @return true if valid, false otherwise
     */
    public boolean validatePassword(String userPassword) {
        if (userPassword == null || userPassword.length() < 5) {
            return false;
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : userPassword.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        return hasLetter && hasDigit;
    }

    /**
     * Verify the provided user's name and password combination against
     * stored user data to determine the authorization status.
     * @param userName The username for login
     * @param userPassword The password for login
     * @return A User object (Customer or Admin) if successful, null otherwise
     */
    public User login(String userName, String userPassword) {
        if (userName == null || userPassword == null) {
            return null;
        }

        for (String line : readUserLines()) {
            String name = extractField(line, "user_name");
            if (!userName.equals(name)) {
                continue;
            }

            String storedPassword = extractField(line, "user_password");
            String decrypted = decryptPassword(storedPassword);
            if (!userPassword.equals(decrypted)) {
                return null; // username matched, password did not
            }

            String id = extractField(line, "user_id");
            String role = extractField(line, "user_role");

            if ("admin".equalsIgnoreCase(role)) {
                return getAdmin(id, name, storedPassword,
                        extractField(line, "user_register_time"), role);
            } else if ("customer".equalsIgnoreCase(role)) {
                // CustomerOperation owns all Customer objects, so reuse its instance
                return CustomerOperation.getInstance().getCustomerById(id);
            }
            return null;
        }
        return null;
    }

    // ---------- Private helpers ----------

    /** Returns the cached Admin for this ID, creating it only the first time. */
    private Admin getAdmin(String id, String name, String password,
                           String registerTime, String role) {
        Admin admin = adminCache.get(id);
        if (admin == null) {
            try {
                admin = new Admin(id, name, password, registerTime, role);
            } catch (IllegalArgumentException e) {
                return null; // ID already used by an object we don't hold
            }
            adminCache.put(id, admin);
        }
        return admin;
    }

    /** Reads all non-empty lines from the user data file (empty list if missing). */
    private List<String> readUserLines() {
        List<String> lines = new ArrayList<>();
        if (!Files.exists(USER_FILE)) {
            return lines;
        }
        try {
            for (String line : Files.readAllLines(USER_FILE, StandardCharsets.UTF_8)) {
                if (!line.trim().isEmpty()) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read " + USER_FILE + ": " + e.getMessage());
        }
        return lines;
    }

    /** Extracts the value of "key":"value" from a JSON-style line, or null if absent. */
    private String extractField(String line, String key) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(line);
        return m.find() ? m.group(1) : null;
    }
}