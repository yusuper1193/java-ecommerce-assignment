import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contains all the operations related to the customer.
 * This class follows the Singleton pattern.
 */
public class CustomerOperation {

    private static final Path USER_FILE = Paths.get("data", "users.txt");
    private static final int PAGE_SIZE = 10;
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy_HH:mm:ss");

    // username@domain.extension
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@([A-Za-z0-9-]+\\.)+[A-Za-z]{2,}$");
    // exactly 10 digits, starting with 04 or 03
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^0[34]\\d{8}$");
    // "key":"value" pairs inside one line of users.txt
    private static final Pattern FIELD_PATTERN =
            Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"");

    private static CustomerOperation instance;

    // User only allows one object per ID, so loaded customers are kept here and reused.
    private final Map<String, Customer> cache = new HashMap<>();
    // Every ID generated, loaded or deleted by this class, so new IDs never reuse them.
    private final Set<String> seenIds = new HashSet<>();

    private CustomerOperation() { }

    /**
     * Returns the single instance of CustomerOperation.
     * @return CustomerOperation instance
     */
    public static synchronized CustomerOperation getInstance() {
        if (instance == null) {
            instance = new CustomerOperation();
        }
        return instance;
    }

    /**
     * Validate the provided email address format. An email address
     * consists of username@domain.extension format.
     * @param userEmail The email to validate
     * @return true if valid, false otherwise
     */
    public boolean validateEmail(String userEmail) {
        return userEmail != null && EMAIL_PATTERN.matcher(userEmail).matches();
    }

    /**
     * Validate the provided mobile number format. The mobile number
     * should be exactly 10 digits long, consisting only of numbers,
     * and starting with either '04' or '03'.
     * @param userMobile The mobile number to validate
     * @return true if valid, false otherwise
     */
    public boolean validateMobile(String userMobile) {
        return userMobile != null && MOBILE_PATTERN.matcher(userMobile).matches();
    }

    /**
     * Save the information of the new customer into the data/users.txt file.
     * @param userName     Customer's username
     * @param userPassword Customer's password
     * @param userEmail    Customer's email
     * @param userMobile   Customer's mobile number
     * @return true if success, false if failure
     */
    public boolean registerCustomer(String userName, String userPassword,
                                    String userEmail, String userMobile) {
        if (!isValidText(userName) || !isValidText(userPassword)
                || !validateEmail(userEmail) || !validateMobile(userMobile)) {
            return false;
        }
        try {
            List<String> lines = readLines();
            Set<String> fileIds = new HashSet<>();
            for (String line : lines) {
                Map<String, String> r = parseRecord(line);
                if (r == null) continue;
                if (userName.equals(r.get("user_name"))) {
                    return false; // username already exists
                }
                fileIds.add(r.get("user_id"));
            }

            String time = LocalDateTime.now().format(TIME_FORMAT);
            Customer customer = null;
            while (customer == null) {
                String id = String.format("u_%010d",
                        ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
                if (fileIds.contains(id) || seenIds.contains(id)) continue;
                seenIds.add(id);
                try {
                    customer = new Customer(id, userName, userPassword, time,
                            "customer", userEmail, userMobile);
                } catch (IllegalArgumentException e) {
                    // ID already used by another object: try a different one
                }
            }

            lines.add(customer.toString());
            writeLines(lines);
            cache.put(customer.getUserId(), customer);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Update the given customer object's attribute value. According to
     * different attributes, perform appropriate validations.
     * Supported attributes: user_name, user_password, user_email, user_mobile.
     * @param attributeName  The attribute to update
     * @param value          The new value
     * @param customerObject The customer object to update
     * @return true if updated, false if failed
     */
    public boolean updateProfile(String attributeName, String value, Customer customerObject) {
        if (attributeName == null || value == null || customerObject == null) {
            return false;
        }
        String field;
        switch (attributeName.replace("_", "").toLowerCase()) {
            case "username":
                if (!isValidText(value)) return false;
                field = "user_name";
                break;
            case "userpassword":
                if (!isValidText(value)) return false;
                field = "user_password";
                break;
            case "useremail":
                if (!validateEmail(value)) return false;
                field = "user_email";
                break;
            case "usermobile":
                if (!validateMobile(value)) return false;
                field = "user_mobile";
                break;
            default:
                return false; // id, register time and role cannot be changed
        }

        try {
            List<String> lines = readLines();
            String id = customerObject.getUserId();
            int index = -1;
            for (int i = 0; i < lines.size(); i++) {
                Map<String, String> r = parseRecord(lines.get(i));
                if (r == null) continue;
                if (id.equals(r.get("user_id")) && "customer".equals(r.get("user_role"))) {
                    index = i;
                } else if (field.equals("user_name") && value.equals(r.get("user_name"))) {
                    return false; // username already taken by someone else
                }
            }
            if (index < 0) return false;

            String oldValue = readAttribute(customerObject, field);
            writeAttribute(customerObject, field, value);
            lines.set(index, customerObject.toString());
            try {
                writeLines(lines);
            } catch (IOException e) {
                writeAttribute(customerObject, field, oldValue); // undo
                return false;
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Delete the customer from the data/users.txt file based on the
     * provided customer_id.
     * @param customerId The ID of the customer to delete
     * @return true if deleted, false if failed
     */
    public boolean deleteCustomer(String customerId) {
        if (customerId == null) return false;
        try {
            List<String> lines = readLines();
            boolean removed = false;
            Iterator<String> it = lines.iterator();
            while (it.hasNext()) {
                Map<String, String> r = parseRecord(it.next());
                if (r != null && customerId.equals(r.get("user_id"))
                        && "customer".equals(r.get("user_role"))) {
                    it.remove();
                    removed = true;
                }
            }
            if (!removed) return false;
            writeLines(lines);
            cache.remove(customerId);
            seenIds.add(customerId);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Retrieve one page of customers from the data/users.txt.
     * One page contains a maximum of 10 customers.
     * @param pageNumber The page number to retrieve (starting from 1)
     * @return A list of Customer objects, the current page number, and total pages
     */
    public CustomerListResult getCustomerList(int pageNumber) {
        List<Map<String, String>> records = new ArrayList<>();
        try {
            for (String line : readLines()) {
                Map<String, String> r = parseRecord(line);
                if (r != null && "customer".equals(r.get("user_role"))) {
                    records.add(r);
                }
            }
        } catch (IOException e) {
            // treat an unreadable file as having no customers
        }

        int totalPages = (int) Math.ceil(records.size() / (double) PAGE_SIZE);
        List<Customer> page = new ArrayList<>();
        if (pageNumber >= 1 && pageNumber <= totalPages) {
            int from = (pageNumber - 1) * PAGE_SIZE;
            int to = Math.min(from + PAGE_SIZE, records.size());
            for (int i = from; i < to; i++) {
                Customer c = toCustomer(records.get(i));
                if (c != null) page.add(c);
            }
        }
        return new CustomerListResult(page, pageNumber, totalPages);
    }

    /**
     * Removes all the customers from the data/users.txt file.
     * Admin records in the file are kept.
     */
    public void deleteAllCustomers() {
        try {
            List<String> kept = new ArrayList<>();
            for (String line : readLines()) {
                Map<String, String> r = parseRecord(line);
                if (r != null && "customer".equals(r.get("user_role"))) {
                    seenIds.add(r.get("user_id"));
                } else {
                    kept.add(line);
                }
            }
            writeLines(kept);
            cache.clear();
        } catch (IOException e) {
            System.err.println("Could not delete customers: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- helpers

    /** Non-empty text that cannot break the one-line JSON-like record. */
    private boolean isValidText(String s) {
        return s != null && !s.trim().isEmpty()
                && s.indexOf('"') < 0 && s.indexOf('\n') < 0 && s.indexOf('\r') < 0;
    }

    private List<String> readLines() throws IOException {
        if (!Files.exists(USER_FILE)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Files.readAllLines(USER_FILE, StandardCharsets.UTF_8));
    }

    private void writeLines(List<String> lines) throws IOException {
        Files.createDirectories(USER_FILE.getParent());
        Files.write(USER_FILE, lines, StandardCharsets.UTF_8);
    }

    /** Turns one line of users.txt into a key/value map, or null if it is not a user record. */
    private Map<String, String> parseRecord(String line) {
        Matcher m = FIELD_PATTERN.matcher(line);
        Map<String, String> record = new HashMap<>();
        while (m.find()) {
            record.put(m.group(1), m.group(2));
        }
        return record.containsKey("user_id") ? record : null;
    }

    /** Returns the Customer object for a file record, reusing the cached one if it exists. */
    private Customer toCustomer(Map<String, String> r) {
        String id = r.get("user_id");
        String name = r.getOrDefault("user_name", "");
        String password = r.getOrDefault("user_password", "");
        String email = r.getOrDefault("user_email", "");
        String mobile = r.getOrDefault("user_mobile", "");

        Customer c = cache.get(id);
        if (c == null) {
            try {
                c = new Customer(id, name, password, r.get("user_register_time"),
                        "customer", email, mobile);
            } catch (IllegalArgumentException e) {
                return null; // malformed ID, or ID already used by an object we don't hold
            }
            cache.put(id, c);
            seenIds.add(id);
        } else {
            // keep the cached object in sync with the file
            c.setUserName(name);
            c.setUserPassword(password);
            c.setUserEmail(email);
            c.setUserMobile(mobile);
        }
        return c;
    }

    private String readAttribute(Customer c, String field) {
        switch (field) {
            case "user_name": return c.getUserName();
            case "user_password": return c.getUserPassword();
            case "user_email": return c.getUserEmail();
            default: return c.getUserMobile();
        }
    }

    private void writeAttribute(Customer c, String field, String value) {
        switch (field) {
            case "user_name": c.setUserName(value); break;
            case "user_password": c.setUserPassword(value); break;
            case "user_email": c.setUserEmail(value); break;
            default: c.setUserMobile(value); break;
        }
    }
}