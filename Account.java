import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Scanner;

public class Account {
	private static String username;
	private Scanner scnr;
	
	public Account(Scanner scanner) {
		scnr = scanner;
		username = getUsername();
	}
	
	public void setUsername(String userName) {
		username = userName;
	}
	
	public String getUsername() {
		return username;
	}

	private boolean userNameChecker(String name) {
		Connection c = Utility.connectToDatabase();
		boolean accepted = true;

		try {
			Statement stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT username FROM accounts WHERE username LIKE \'" + name + "\';");
				
			if(rs.next()) {
				System.out.println("Username is already taken");
				accepted = false;
			}
			else {
				accepted = true;
			}
			stmt.close();
			c.close();
		}
		catch(Exception e) {
			Utility.onException(e);
		}
		return accepted;
	}
	
	private boolean loginChecker(String username, String password) {
		Connection c = Utility.connectToDatabase();
		boolean everythingMatch = false;

		try {
			Statement stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT username, password FROM accounts "
					+ "WHERE username = \'" + username + "\' AND password = \'" + password + "\';");
			if(rs.next()) {
				String userNameData = rs.getString("username");
				String passwordData = rs.getString("password");
				if(username.equals(userNameData) && password.equals(passwordData)) {
					everythingMatch = true;
				}
				else {
					everythingMatch = false;
				}
			}
			stmt.close();
			c.close();
		}
		catch(Exception e) {
			Utility.onException(e);
		}
		return everythingMatch;
	}
	
	public void register() {
		Connection c = Utility.connectToDatabase();
		String userName = "", password = "";
		boolean notCreated = true;
		
		while(notCreated) {
			System.out.println("Username: ");
			userName = scnr.nextLine();
			System.out.print(userName + "\n");
			
			System.out.println("Password: ");
			password = scnr.nextLine();
			System.out.print(password + "\n");
			
			boolean result = userNameChecker(userName);
			if(result == true) {
				System.out.println("Account created!");
				notCreated = false;
			}
		}
		
		try {
			c.setAutoCommit(false);
			Statement stmt = c.createStatement();
			String sql = "INSERT INTO accounts"  
					+ "(username, password) " 
					+ "VALUES (\'" + userName + "\', \'" + password + "\');";
			stmt.executeUpdate(sql);
			stmt.close();
			c.commit();
			c.close();
		}
		catch(Exception e) {
			Utility.onException(e);
		}
	}
	
	public void login() {
		String userName, password;
		boolean isMatched;
		
		System.out.println("Username: ");
		userName = scnr.nextLine();
		System.out.println("Password: ");
		password = scnr.nextLine();
		
		isMatched = loginChecker(userName, password);
		if(isMatched == false) {
			System.out.println("Username or Password doesn't match\n");
		}
		else {
			setUsername(userName);
			System.out.println("Welcome " + username + "!\n");
			userPrompt();
		}
	}
	
	private void logout() {
		Connection c = Utility.connectToDatabase();
		
		try {
			Statement stmt = c.createStatement();
			c.setAutoCommit(false);
			stmt = c.createStatement();
			String sql = "UPDATE accounts"
					+ " SET chatroom = NULL"
					+ " WHERE username = \'" + username + "\';";
			stmt.executeUpdate(sql);
			stmt.close();
			c.commit();
			c.close();
			System.out.println("Log out successfully");
			setUsername(null);
		}
		catch(Exception e) {
			Utility.onException(e);
		}
	}
	
	public void quit() {
		Runtime.getRuntime().halt(0);
	}
	
	public void manageAccount() {
		String command = "";

		System.out.println("Change (U)sername or (P)assword: ");
		command = scnr.nextLine();
		
		if(command.equals("u") ||command.equals("U")) {
			changeUsername();
		}
		else if(command.equals("p") ||command.equals("p")) {
			changePassword();
		}
	}
	
	public void changeUsername() {
		Connection c = Utility.connectToDatabase();
		String newUsername = "";
		
		System.out.println("New username: ");
		newUsername = scnr.nextLine();
		System.out.print(newUsername + "\n");
		
		if(userNameChecker(newUsername) == true) {
			try {
				c.setAutoCommit(false);
				Statement stmt = c.createStatement();
				String sql = "UPDATE accounts"
						+ " SET username = \'" + newUsername
						+ "\' WHERE username = \'" + username + "\';";  
				stmt.executeUpdate(sql);
				stmt.close();
				c.commit();
				c.close();
				username = newUsername;
				System.out.println("Username changed!\n");
			}
			catch(Exception e) {
				Utility.onException(e);
			}
		}
		else {
			manageAccount();
		}
	}
	
	public void changePassword() {
		Connection c = Utility.connectToDatabase();
		String newPassword = "";
		System.out.println("New password: ");
		newPassword = scnr.nextLine();
			
		try {
			c.setAutoCommit(false);
			Statement stmt = c.createStatement();
			String sql = "UPDATE accounts"
					+ " SET password = " + newPassword 
					+ " WHERE username = \'" + username + "\';";  
			stmt.executeUpdate(sql);
			stmt.close();
			c.commit();
			c.close();
			System.out.println("Password changed!\n");
		}
		catch(Exception e) {
			Utility.onException(e);
		}
	}
	
	public int commandsAfterLogin(String cmd) {	

		if(cmd.equals("J")|| cmd.equals("j")) {
			ChatRoom room = new ChatRoom(username, scnr);
			room.join();
			return 3;
		}
		else if(cmd.equals("C")|| cmd.equals("c")) {
			ChatRoom room = new ChatRoom(username, scnr);
			room.createChatRoom();
			return 2;
		}
		else if(cmd.equals("A")|| cmd.equals("a")) {
			manageAccount();
			return 1;
		}
		else if(cmd.equals("L")|| cmd.equals("l")) {
			logout();
			return 0;
		}
		else {
			System.out.println("Unknown command");
			cmd = scnr.nextLine();
			return commandsAfterLogin(cmd);
			
		}
	}
	
	public int commandsBeforeLogin(String cmd) {		
		
		if(cmd.equals("R")|| cmd.equals("r")) {
			register();
			return 5;
		}
		else if(cmd.equals("L")|| cmd.equals("l")) {
			login();
			return 6;
		}
		else if(cmd.equals("Q")|| cmd.equals("q")) {
			return 0;
		} 
		else {
			System.out.println("Unkown command");
			cmd = scnr.nextLine();
			return commandsBeforeLogin(cmd);
		}
	}
	
	public void userPrompt() {
		String command = "";
		while(true) {
			System.out.println("Please select from the following options:");
			System.out.println("(J)oin, (C)reate, (A)ccount, (L)ogout");
			System.out.println("-----------------------------------------\n");
			
			if(scnr.hasNextLine()) {
				command = scnr.nextLine();
			}
			int returnCode = commandsAfterLogin(command);
			if (returnCode == 0) {
				break;
			}
		}
	}
	
	public void onBoarding() {
		String command;
		while(true) {
			System.out.println("Please select from the following options:");
			System.out.println("(R)egister, (L)ogin, (Q)uit");
			System.out.println("-----------------------------------------\n");
			command = scnr.nextLine();
			int returnCode = commandsBeforeLogin(command);
			if (returnCode == 0) {
				break;
			}
		}
	}
}