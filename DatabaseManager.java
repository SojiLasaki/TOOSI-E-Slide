import java.sql.*;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/library_db";
    private static final String USER = "root";
    private static final String PASSWORD = "your_password"; // Replace this!

    private Connection conn;

    public DatabaseManager() {
        try {
            conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
            System.out.println("Connected to DB.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createTables() {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS books (" +
                    "book_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "title VARCHAR(255)," +
                    "author VARCHAR(255)," +
                    "category VARCHAR(100)," +
                    "availability BOOLEAN)");

            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "user_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "name VARCHAR(255)," +
                    "email VARCHAR(255) UNIQUE," +
                    "password VARCHAR(255))");

            stmt.execute("CREATE TABLE IF NOT EXISTS admins (" +
                    "admin_id INT PRIMARY KEY," +
                    "role VARCHAR(50)," +
                    "FOREIGN KEY (admin_id) REFERENCES users(user_id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS regular_users (" +
                    "user_id INT PRIMARY KEY," +
                    "borrowed_books INT," +
                    "FOREIGN KEY (user_id) REFERENCES users(user_id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS transactions (" +
                    "transaction_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "user_id INT," +
                    "book_id INT," +
                    "due_date DATE," +
                    "status VARCHAR(20)," +
                    "FOREIGN KEY (user_id) REFERENCES regular_users(user_id)," +
                    "FOREIGN KEY (book_id) REFERENCES books(book_id))");

            System.out.println("All tables created.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Add user to users table
    public void addUser(String name, String email, String password) {
        String sql = "INSERT INTO users (name, email, password) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setString(3, password); // Store as encrypted in real apps!
            stmt.executeUpdate();
            System.out.println("User added.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Promote user to regular_user
    public void addRegularUser(int userId) {
        String sql = "INSERT INTO regular_users (user_id, borrowed_books) VALUES (?, 0)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
            System.out.println("Regular user added.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Promote user to admin
    public void addAdmin(int userId, String role) {
        String sql = "INSERT INTO admins (admin_id, role) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, role);
            stmt.executeUpdate();
            System.out.println("Admin added.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Add a book
    public void addBook(String title, String author, String category, boolean availability) {
        String sql = "INSERT INTO books (title, author, category, availability) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, title);
            stmt.setString(2, author);
            stmt.setString(3, category);
            stmt.setBoolean(4, availability);
            stmt.executeUpdate();
            System.out.println("Book added.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Issue a book
    public void issueBook(int userId, int bookId, Date dueDate) {
        String sql = "INSERT INTO transactions (user_id, book_id, due_date, status) VALUES (?, ?, ?, 'Issued')";
        String updateBook = "UPDATE books SET availability = false WHERE book_id = ?";
        String updateBorrowed = "UPDATE regular_users SET borrowed_books = borrowed_books + 1 WHERE user_id = ?";

        try {
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 PreparedStatement stmt2 = conn.prepareStatement(updateBook);
                 PreparedStatement stmt3 = conn.prepareStatement(updateBorrowed)) {

                stmt.setInt(1, userId);
                stmt.setInt(2, bookId);
                stmt.setDate(3, dueDate);
                stmt.executeUpdate();

                stmt2.setInt(1, bookId);
                stmt2.executeUpdate();

                stmt3.setInt(1, userId);
                stmt3.executeUpdate();

                conn.commit();
                System.out.println("Book issued.");
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
