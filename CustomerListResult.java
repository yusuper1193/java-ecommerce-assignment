import java.util.List;

/**
 * Holds one page of customers, the current page number and the total number of pages.
 */
public class CustomerListResult {

    private final List<Customer> customers;
    private final int currentPage;
    private final int totalPages;

    public CustomerListResult(List<Customer> customers, int currentPage, int totalPages) {
        this.customers = customers;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
    }

    public List<Customer> getCustomers() { return customers; }
    public int getCurrentPage() { return currentPage; }
    public int getTotalPages() { return totalPages; }
}