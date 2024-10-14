import java.sql.Connection;
import java.sql.DriverManager;

public class Utility {
	public static Connection connectToDatabase() {
		Connection c = null;
		
		try {
			Class.forName("org.postgresql.Driver");
			c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/account_info",
					"postgres", "Wukong0905");
		}
		catch(Exception e) {
			onException(e);
		}
		return c;
	}

	public static void onException(Exception e) {
		e.printStackTrace();
		System.err.println(e.getClass().getName()+ ": " + e.getMessage());
		System.exit(0);
	}
}
