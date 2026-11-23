import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class Product {
    // IDs already in use + next number for auto-increment (p001, p002, ...)
    private static final Set<String> usedIds = new HashSet<>();
    private static int nextIdNumber = 1;

    //attributes
    private String productID; //unique
    private String productModel;
    private String productCategory;
    private String productName;
    private double productCurrentPrice;
    private double productRawPrice;
    private double productDiscount;
    private int productLikesCount; //each object has its own count

    //constructor
    /**
     * Constructs a product object.
     * @param productID Product ID (must be unique)
     * @param productModel Product model
     * @param productCategory Product category
     * @param productName Product name
     * @param productCurrentPrice Current price of the product
     * @param productRawPrice Original price of the product
     * @param productDiscount Discount percentage
     * @param productLikesCount Number of likes
     */
    public Product(String productID, String productModel, String productCategory, String productName,
                   double productCurrentPrice, double productRawPrice, double productDiscount,
                   int productLikesCount) {
        if (productID == null || !usedIds.add(productID)) {
            throw new IllegalArgumentException("Product ID is missing or already exists: " + productID);
        }
        // keep the counter ahead of any ID like p020 so new IDs never collide
        if (productID.matches("p\\d+")) {
            nextIdNumber = Math.max(nextIdNumber, Integer.parseInt(productID.substring(1)) + 1);
        }
        this.productID = productID;
        this.productModel = productModel;
        this.productCategory = productCategory;
        this.productName = productName;
        this.productCurrentPrice = productCurrentPrice;
        this.productRawPrice = productRawPrice;
        this.productDiscount = productDiscount;
        this.productLikesCount = productLikesCount;
    }

    /**
     * Default constructor: ID is auto-incremented, other values are defaults.
     */
    public Product() {
        this(generateId(), "", "", "", 0.0, 0.0, 0.0, 0);
    }

    private static String generateId() {
        String id;
        do {
            id = String.format("p%03d", nextIdNumber++);
        } while (usedIds.contains(id));
        return id;
    }

    //methods
    /**
     * Returns the product information as a formatted string.
     * @return String in JSON-like format
     */
    @Override
    public String toString() {
        return String.format(Locale.US,
                "{\"pro_id\":\"%s\",\"pro_model\":\"%s\",\"pro_category\":\"%s\",\"pro_name\":\"%s\","
                        + "\"pro_current_price\":%.2f,\"pro_raw_price\":%.2f,\"pro_discount\":%.1f,"
                        + "\"pro_likes_count\":%d}",
                productID, productModel, productCategory, productName,
                productCurrentPrice, productRawPrice, productDiscount, productLikesCount);
    }
}