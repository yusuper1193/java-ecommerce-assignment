import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

public class Order {
    public static final String DATA_FILE = "data-sample/order.csv";

    // IDs already in use + next number for auto-increment (o_00001, o_00002, ...)
    private static final Set<String> usedIds = new HashSet<>();
    private static int nextIdNumber = 1;

    //attributes
    private String orderID;
    private String userID;
    private String productID;
    private String orderTime;

    //constructor

    /**
     * Constructs an order object (one order = one product).
     * @param orderId Must be a unique string, format is o_5 digits such as o_12345
     * @param userId ID of the user who placed the order
     * @param proId ID of the product ordered
     * @param orderTime Time of the order, format DD-MM-YYYY_HH:MM:SS
     */
    public Order(String orderId, String userId, String proId, String orderTime) {
        if (orderId == null || !orderId.matches("o_\\d{5}")) {
            throw new IllegalArgumentException("Invalid order id (expected o_ + 5 digits): " + orderId);
        }
        if (!usedIds.add(orderId)) {
            throw new IllegalArgumentException("Order id already exists: " + orderId);
        }
        // keep the counter ahead of existing IDs so auto IDs never collide
        nextIdNumber = Math.max(nextIdNumber, Integer.parseInt(orderId.substring(2)) + 1);

        this.orderID = orderId;
        this.userID = userId;
        this.productID = proId;
        this.orderTime = orderTime;
    }

    public Order() { //unique ID + increasement
        this(generateId(), "", "",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy_HH:mm:ss")));
    }

    private static String generateId() {
        String id;
        do {
            id = String.format("o_%05d", nextIdNumber++);
        } while (usedIds.contains(id));
        return id;
    }

    //methods
    /**
     * Returns the order information as a formatted string.
     * @return String in JSON-like format
     */
    @Override
    public String toString() {
        return "{\"order_id\":\"" + orderID + "\", \"user_id\":\"" + userID
                + "\", \"pro_id\":\"" + productID
                + "\", \"order_time\":\"" + orderTime + "\"}";
    }
}