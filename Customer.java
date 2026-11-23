/**
 * Customer extends User and adds an email address and a mobile number.
 * All customers are saved in the file "data/users.txt".
 */
public class Customer extends User {

    private String userEmail;   // customer's email address
    private String userMobile;  // customer's mobile phone number

    /**
     * Constructs a customer object.
     * @param userId           Must be unique, format: u_10 digits, such as u_1234567890
     * @param userName         The user's name
     * @param userPassword     The user's password
     * @param userRegisterTime Format: "DD-MM-YYYY_HH:MM:SS"
     * @param userRole         Default value: "customer"
     * @param userEmail        The customer's email address
     * @param userMobile       The customer's mobile number
     */
    public Customer(String userId, String userName, String userPassword,
                    String userRegisterTime, String userRole,
                    String userEmail, String userMobile) {
        // User validates the ID/role and defaults a null/empty role to "customer"
        super(userId, userName, userPassword, userRegisterTime, userRole);
        this.userEmail = userEmail;
        this.userMobile = userMobile;
    }

    /**
     * Default constructor
     */
    public Customer() {
        super(); // auto-generated ID, empty name/password, current time, role "customer"
        this.userEmail = "";
        this.userMobile = "";
    }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getUserMobile() { return userMobile; }
    public void setUserMobile(String userMobile) { this.userMobile = userMobile; }

    /**
     * Returns the customer information as a formatted string.
     * @return String in JSON-like format
     */
    @Override
    public String toString() {
        // User's fields are private, so reuse its string and append the extra fields
        String base = super.toString();                      // ends with "}"
        return base.substring(0, base.length() - 1)
                + ", \"user_email\":\"" + userEmail
                + "\", \"user_mobile\":\"" + userMobile + "\"}";
    }
}