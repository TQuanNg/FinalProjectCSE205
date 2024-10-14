import java.util.Scanner;

//TAN MINH QUAN NGUYEN
//CSE 205
///FINAL PROJECT CHAT APP
public class Main {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Scanner scnr = new Scanner(System.in);
		Account acc = new Account(scnr);
		
		System.out.println("Welcome to Chat App!");
		System.out.println();
		acc.onBoarding();
		scnr.close();
		System.exit(0);
	}
}

/*Reference:
https://www.codejava.net/java-se/jdbc/how-to-use-scrollable-result-sets-with-jdbc
for line 292, ChatRoom class
//----
 
 https://stackoverflow.com/questions/1795402/check-if-a-string-contains-a-special-character
 for line 217, ChatRoom class
 //----
  
 https://www.geeksforgeeks.org/a-group-chat-application-in-java/#
 for Thread, ChatRoom class
 //----
 
https://www.tutorialspoint.com/java-resultset-previous-method-with-example#
for lines 307, 308, ChatRoom class
*/
