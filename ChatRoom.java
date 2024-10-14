import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Scanner;
public class ChatRoom {
	
	private Scanner scnr;
	private Connection connection;
	private String userId;
	private String chatroomName;
	private String lastMsg;
	private String username;
	private boolean inChatroom = true;
	private Thread periodicFetchingThread;

	public ChatRoom(String username, Scanner scanner) {
		scnr = scanner;
		this.username = username;
		connection = Utility.connectToDatabase();
		userId = getID();
	}
		
	public void createChatRoom() {
		System.out.print("-c ");
		chatroomName = scnr.nextLine();
		System.out.println(chatroomName);
		
		if(checkChatroomName(chatroomName) && ifExisted(chatroomName)) {
			createNewChatroomTableSql(chatroomName);
			updateChatroomColumnInAccTable(chatroomName);
			printChatroomWelcomeMsg(chatroomName);
			setLastMsg();
			periodicallyCheckForNewMsg();
			chat();
		}
		else {
			Account acc = new Account(scnr);
			acc.userPrompt();
		}
	}
	
	public void join() {
		System.out.print("-j ");
		if(scnr.hasNextLine()) {
			chatroomName = scnr.nextLine();
		}
		System.out.println(chatroomName);
		
		if(ifChatroomExist(chatroomName)) {
			updateChatroomColumnInAccTable(chatroomName);
			printChatroomWelcomeMsg(chatroomName);
			setLastMsg();
			periodicallyCheckForNewMsg();
			chat();
		}
		
	}

	private void chat() {
		String newMsg = "";
		while(inChatroom) {
			if(scnr.hasNextLine()) {
				newMsg = scnr.nextLine();
				if(newMsg.isEmpty()) {
				}
				else if(newMsg.charAt(0) != '/') {
					try {
						Statement stmt = connection.createStatement();
						stmt.executeUpdate("INSERT INTO " + chatroomName  + 
						"(id , message) VALUES (\'" + userId + "\', \'" + newMsg + "\');" );
						connection.commit();
						stmt.close();
						lastMsg = username + " : " + newMsg;
					}
					catch(Exception e){
						Utility.onException(e);
					}
				}
				else {
					commandInChatroom(newMsg);
				}
			}
		}
	}
	
	private void leaveChatRoom() {
		inChatroom = false;
		updateChatroomColumnInAccTable("NULL");
		try {
			connection.close();
			periodicFetchingThread.interrupt();
		}
		catch (Exception e) {
			Utility.onException(e);
		}
	}
	
	private void showHistory() {
		try {
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT accounts.username, " + chatroomName + ".message"
					+ " FROM accounts JOIN " + chatroomName 
					+  " ON " + chatroomName + ".id = accounts.id;");
			while(rs.next()) {
				String username = rs.getString("username");
				String message = rs.getString("message");
				System.out.println(username + ":> " + message);
			}
			System.out.println("That is all of history");
			rs.close();
			stmt.close();
		}
		catch(Exception e){
			Utility.onException(e);
		}
	}
	
	private void listUsers() {
		try {
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT username FROM accounts "
					+ "WHERE chatroom = \'" + chatroomName + "\';");
			while(rs.next() ) {
				String userName = rs.getString("username");
				System.out.println("-" + userName);
			}
			stmt.close();
		}
		catch(Exception e) {
			Utility.onException(e);
		}
	}
	
	private void showHelpList() {
		System.out.println("");
		System.out.println("/list (Return a list of users currently in this chat room.)");
		System.out.println("/leave (Exits the chat room.)");
		System.out.println("/history (Print all past mesasges for the room.)");
		System.out.println("/help (Show this list)");
	}
	
	private void commandInChatroom(String cmd) {
		if(cmd.equals("/list")) {
			listUsers();
		}
		else if(cmd.equals("/leave")) {
			leaveChatRoom();
		}
		else if(cmd.equals("/history")) {
			showHistory();
		}
		else if(cmd.equals("/help")) {
			showHelpList();
		}
		else {
			System.out.println("Unkown command");
		}
	}
	
	private String getID() {
		String username = this.username;
		String id = "";
		try {
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT id FROM accounts WHERE username LIKE \'" + username + "\';");
			if(rs.next()) {
				id = rs.getString("id");
			}
			stmt.close();
		}
		catch(Exception e) {
			Utility.onException(e);
		}
		return id;
	}
	
	private void updateChatroomColumnInAccTable(String chatroomName) {
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "UPDATE accounts " + 
					"SET chatroom = \'" + chatroomName +
					"\' WHERE id = " + userId +";";
			stmt.executeUpdate(sql);
			stmt.close();
			connection.commit();
		}
		catch(Exception e){
			Utility.onException(e);
		}
	}
	
	private void createNewChatroomTableSql(String chatroomName) {
		try {
			connection.setAutoCommit(false);
			Statement stmt = connection.createStatement();
			String sql = "CREATE TABLE " + chatroomName
					+ " (id INT NOT NULL, "
					+ "FOREIGN KEY (id) REFERENCES accounts (id), "
					+ "message VARCHAR(1024));";
			stmt.executeUpdate(sql);
			sql = "INSERT INTO " + chatroomName + 
					" (id) VALUES (" + getID() + ");";
			stmt.executeUpdate(sql);
			stmt.close();
			connection.commit();
			System.out.println("Chatroom " + chatroomName + " created!");
		}
		catch(Exception e){
			Utility.onException(e);
		}
	}
	
	private boolean checkChatroomName(String name) {
		boolean accepted = false;
		Pattern specialChar = Pattern.compile ("[!@#$%&*()_+=|<>?{}\\[\\]~-]");
		Matcher hasSpecial = specialChar.matcher(name);
		boolean result = hasSpecial.find();
		if(result == true) {
			System.out.println("Can not have weird character\n");
		}
		else {
			accepted = true;
			System.out.println("Valid chatroom name");
		}
		return accepted;
	}
	
	private boolean ifExisted(String name) {
		boolean accepted = false;
		
		try {
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM pg_catalog.pg_tables WHERE schemaname = 'public' "
					+ "AND tablename = \'" + name + "\';");
			if(rs.next()) {
				System.out.println("Chatroom " + name + " already existed\n");
			}
			else {
				accepted = true;
			}
			
			stmt.close();
		}
		catch(Exception e) {
		}
		return accepted;
	}
	
	private boolean ifChatroomExist(String name) {
		Account ac = new Account(scnr);
		boolean isExisted = false;

		try {
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM pg_catalog.pg_tables WHERE schemaname = 'public' "
					+ "AND tablename = \'" + name + "\';");
			if(rs.next()) {
				isExisted = true;
			}
			else {
				System.out.println("Chatroom does not exist\n");
				ac.userPrompt();
			}
			stmt.close();
		}
		catch(Exception e) {
			Utility.onException(e);
		}
		return isExisted;
	}
	
	private void printChatroomWelcomeMsg(String chatroomName) {
		System.out.print("Welcome to " + chatroomName + ", "  + username);
		System.out.println(" (/help for commands)");
	}
	
    private void setLastMsg() {
    	String sql = "SELECT accounts.username, " + chatroomName + ".message FROM accounts JOIN " + chatroomName 
				+ " ON " + chatroomName + ".id = accounts.id;";
		try {			
	    	connection.setAutoCommit(false);
			Statement stmt = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
	        ResultSet rs = stmt.executeQuery(sql);
	        rs.afterLast();
	        rs.previous();
	        lastMsg = rs.getString("username") + " : " + rs.getString("message");
			stmt.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
    }
	
	private void periodicallyCheckForNewMsg() {
		try {
			String sql = "SELECT accounts.username, " + chatroomName + ".message FROM accounts JOIN " + chatroomName 
					+ " ON " + chatroomName + ".id = accounts.id;";
						
			periodicFetchingThread = new Thread(new Runnable() {
			    public void run() {

			    	while(true) {
				        if(!inChatroom) {
				        	return;
				        }
				        else {
				            try {
				            	connection.setAutoCommit(false);
				    			Statement stmt = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
				                ResultSet rs = stmt.executeQuery(sql);
				                for (String msg: checkNewMsgs(rs)) {
				                	System.out.println(msg);
				                }
				                connection.commit();
				                stmt.close();
				                Thread.sleep(100);
				            }
				            catch (Exception e) {
				            }
				        }
			    	}
			    }
			    
			    private ArrayList<String> checkNewMsgs(ResultSet rs){
			    	String currentMsg;
			    	ArrayList<String> newMsgs = new ArrayList<String>();
			    	try {
			    		rs.afterLast();
			    		while(rs.previous()){
			    			currentMsg =  rs.getString("username") + " : " + rs.getString("message");
			    			if (currentMsg == null || currentMsg.equals(lastMsg)) {
			    				break;
			    			} 
			    			else {
			    				newMsgs.add(0, currentMsg);
			    			}
			    		}
			    		if (newMsgs.size() > 0) {
			    			lastMsg = newMsgs.get(newMsgs.size() - 1);
			    		}
			    	}
			    	catch (Exception e) {
			    		Utility.onException(e);
			    	}
			    	return newMsgs;
			    }
			});
			periodicFetchingThread.start();
		} 
		
		catch (Exception e) {
			Utility.onException(e);
		}
	}
}


