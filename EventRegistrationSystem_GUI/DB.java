import java.sql.*;

public class DB {
    static final String URL = "jdbc:mysql://localhost:3308/event_system3";
    static final String USER = "root";
    static final String PASSWORD = "";

    static {
        try {
            
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}