package controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import model.Transaction;
import model.User;

/**
 * Allows clients to query and update the database in order to log in, add transactions, and 
 * breakdown expenses via date, price, categories, and more.
 */
public class TransactionsDB {

	/** Holds the connection to the database. */
	private Connection conn;
	
	// Statements which will be used for beginning, rolling back, and committing DB transactions.
	private PreparedStatement beginTxnStmt;
	private PreparedStatement commitTxnStmt;
	private PreparedStatement abortTxnStmt;

	/** Initializes the 3 DB transactions statements. 
	 *  @throws SQLException if a database access error occurs or the database connection 
	 *  	is closed.
	 */
	public void prepare() throws SQLException {
	    beginTxnStmt = conn.prepareStatement(
	        "SET TRANSACTION ISOLATION LEVEL SERIALIZABLE; BEGIN TRANSACTION;");
	    commitTxnStmt = conn.prepareStatement("COMMIT TRANSACTION");
	    abortTxnStmt = conn.prepareStatement("ROLLBACK TRANSACTION");
	  }
  
	/** 
	 * Begins a new transaction which will only be committed when explicitly requested.
	 *  @throws SQLException if a database access error occurs, the database connection is closed,
	 *  	or the beginTxnStmt is closed.
	 */    
    public void beginTransaction() throws SQLException {
    	conn.setAutoCommit(false);  // do not commit until explicitly requested
        beginTxnStmt.executeUpdate();  
    }

    /**
     * Commits the current transaction and new transactions will auto commit.
     * @throws SQLException if a database access error occurs, the database connection is closed,
	 *  	the commitTxnStmt is closed, or currently participating in a distributed transaction.
     */
    public void commitTransaction() throws SQLException {
        commitTxnStmt.executeUpdate(); 
        conn.setAutoCommit(true);  // go back to one transaction per statement
    }

    /**
     * Rolls back the current transaction and new transaction will auto commit.
     * @throws SQLException if a database access error occurs, the database connection is closed,
	 *  	the abortTxnStmt is closed, or currently participating in a distributed transaction.
     */
    public void rollbackTransaction() throws SQLException {
        abortTxnStmt.executeUpdate();
        conn.setAutoCommit(true);  // go back to one transaction per statement
  	}
    
    /** Opens a connection with the TransactionsTracker database **/
    public void open() {
    	try {
    		Class.forName("org.sqlite.JDBC");
	        conn = DriverManager.getConnection(
	        		 "jdbc:sqlite:/C:/sqlite/TransactionsTracker.db");
	    } catch ( Exception e ) {
	        System.err.println( e.getClass().getName() + ": " + e.getMessage() );
	        System.exit(0);
	    }
    }
    
    /** Closes the connection to the database. */
    public void close() throws SQLException {
      conn.close();
      conn = null;
    }
    
    /**
     * Checks to see if a user name is already taken.
     * @param username is the user name being checked.
     * @return True if the user name is taken and false if it is available.
     */
	public boolean isUsernameTaken(String username) {
		// Initialize query and statement.
		PreparedStatement check;
		String sqlStmt = "SELECT *\n"
					   + "FROM Users \n"
					   + "WHERE username = ?";
		
		try {
			// Clear parameters and sanitize string.
			check = this.conn.prepareStatement(sqlStmt);
			check.clearParameters();
			
			// Fill in parameter and execute query
			check.setString(1, username);
			ResultSet users = check.executeQuery();
			
			// If there are no values in the result set then this user name is not taken.
			if (!users.next()) {
				return false;
			} else {
				return true;
			}
			
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			System.out.println("Please try again later.");
			return true;
		}
	}
	
	/**
	 * Adds a new user to the database.
	 * @param newUser is the user object which contains the information of the new user.
	 * @return True if the user was added successfully.
	 */
	public boolean addNewUser(User newUser) {
		// Initialize query and SQL statement.
		PreparedStatement addUser;
		String sqlStmt = "INSERT INTO Users VALUES (?, ?, ?, ?)";
		
		try {
			// Sanitize string and clear parameters.
			addUser = this.conn.prepareStatement(sqlStmt);
			addUser.clearParameters();
			
			// Add in parameter info.
			addUser.setString(1, newUser.getUsername());
			addUser.setString(2, newUser.getName());
			addUser.setFloat(3, (float) 0.00);
			addUser.setString(4, newUser.getPassword());
			
			// Execute query
			addUser.execute();
			
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			return false;
		}
		
		return true;
	}
    
    /**
     * Attempts to log in a user to this database.
     * @returns A user object containing the information for the user which just logged in or 
     * 		null if login failed.
     */
    public User logIn(String username, String password) {
      
  	  // Create query and statement
  	  PreparedStatement logIn;
  	  String sqlStmt = 
  			  "SELECT *\n"
  			  + "FROM Users\n"
  			  + "WHERE username = ? AND password = ?\n";
  	  
  	ResultSet results;
  	  try {
  	  	  // Prepare the statement and clear parameters
  	  	  logIn = conn.prepareStatement(sqlStmt);
  	  	  logIn.clearParameters();
  	  	  
  	  	  // Set the information in this statement
  	  	  logIn.setString(1, username);
  	  	  logIn.setString(2, password);
  	  	  
  	  	  // Execute the query
  	  	  results = logIn.executeQuery();
  	  	  
  	  	  // If no results then return null
  	  	  if (!results.next()) {
  	  		  return null;
  	  	  }
  	  	  
  	  	  return new User(results.getString("username"), results.getString("name"), 
  	  			  (double) results.getFloat("balance"), results.getString("password"));
  	  	  
  	  } catch (SQLException e) {
  		  System.out.println(e.getMessage());
  		  return null;
  	  }
    }
    
    /**
     * Adds an expense to a specified user.
     * @param expense is the transaction to be added to the database.
     * @param username is the user who the transaction will be added to.
     * @return The new account balance of the user or null if an exception occurred.
     */
    public Double addExpense(Transaction expense, String username) {
    	// Initialize query and statement.
    	PreparedStatement insert;
    	String sqlStmt = "INSERT INTO Transactions VALUES (?, ?, ?, ? , ?, ?)";
    	
    	try {        	
        	// Clear parameters
        	insert = this.conn.prepareStatement(sqlStmt);
        	insert.clearParameters();
        	
        	// Insert parameters from Transaction object.
        	insert.setString(1, expense.getDescription());
        	insert.setFloat(2, (float)expense.getAmount());
        	insert.setString(3, expense.getDate().toString());
        	insert.setString(4, expense.getMemo());
        	insert.setString(5, expense.getCategory());
        	insert.setString(6, username);
        	
    		// Add transaction
    		insert.execute();
    		
    		// Update user's account balance
        	return updateBalance(username, expense.getAmount());
        	
    	} catch (SQLException e) {
    		System.out.println(e.getMessage());
    		return null;
    	}
    }
    
    /**
     * Updates a users balance by an amount in the database.
     * @param username is the user whose balance will be updated in the database.
     * @param amount is the change applied to the user's balance.
     * @return The new account balance or null if there was an exception.
     */
    private Double updateBalance(String username, double amount) {
    	// Initialize both SQL statement and the prepared statements.
    	PreparedStatement check;
    	String checkStmt = "SELECT balance FROM Users WHERE username = ?";
    	PreparedStatement update;
    	String updateStmt = "UPDATE Users SET balance = ? WHERE username = ?";
    	
    	try {
        	// Get current balance value
    		check = this.conn.prepareStatement(checkStmt);
    		check.clearParameters();
    		check.setString(1, username);
    		ResultSet checkResult = check.executeQuery();
    		
    		// Move the cursor forward
    		checkResult.next();
    		double balance = checkResult.getFloat(1);
    		
    		// Calculate new balance
    		balance = balance + amount;
    		
    		// Update user balance
    		update = this.conn.prepareStatement(updateStmt);
    		update.clearParameters();
    		update.setFloat(1, (float) balance);
    		update.setString(2, username);
    		update.execute();
    		return balance;
    		
    	} catch (SQLException e) {
    		System.out.println(e.getMessage());
    		return null;
    	}
    }

    /**
     * Gets the categories which are available in this program.
     * @return A list of strings containing the available categories or null if there was
     * 		an exception which prevented the database from being accessed properly.
     */
	public String[] getCategories() {
		// Find the number of categories in the DB
		PreparedStatement query;
		String sqlStmt = "SELECT count(*) \n"
				   + "FROM Categories";
		
		try {
			query = conn.prepareStatement(sqlStmt);
			ResultSet result = query.executeQuery();
			
			// Move cursor to the first row and grab the number of categories.
			result.next();
			int rows = result.getInt(1);
			String[] categories = new String[rows];
			
			// Find the categories in the DB.
			sqlStmt = "SELECT catName \n"
						   + "FROM Categories";
			
			// Execute query
			query = conn.prepareStatement(sqlStmt);
			result = query.executeQuery();
			
			// Add categories from the result set to the string array.
			for (int i = 0; i < rows; i++) {
				// Add next category name to categories
				// Only 1 column, always getting the string i the first column.
				result.next();
				categories[i] = result.getString(1);
			}

			return categories;
			
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			return null;
		}
	}
}