package data;

import java.sql.Connection;
import java.util.logging.Level;

import com.mchange.v2.c3p0.ComboPooledDataSource;

/**
 * This class represents a database that can be accessed/updated.
 * 
 * @author Smc
 */
public class Database {
	
	private ComboPooledDataSource m_connectionPool;

	public Database(String p_jdbcUrl, String p_user, String p_pass) {
		m_connectionPool = new ComboPooledDataSource();
		
		try {
			m_connectionPool.setDriverClass("com.mysql.cj.jdbc.Driver");
			m_connectionPool.setJdbcUrl(p_jdbcUrl + "?serverTimezone=UTC");
			m_connectionPool.setUser(p_user);
			m_connectionPool.setPassword(p_pass);
			m_connectionPool.setMinPoolSize(5);
			m_connectionPool.setInitialPoolSize(5);
			m_connectionPool.setAcquireIncrement(1);
			m_connectionPool.setMaxPoolSize(20);
		} catch(Exception e) {
			Log.log(Level.SEVERE, "Could not initialize SQL connection pool for: " + p_jdbcUrl, e);
		}
	}
	
	public Connection getConnection() {
		try {
			return m_connectionPool.getConnection();
		} catch(Exception e) {
			Log.log(Level.WARNING, "Could not establish connection with database", e);
			
			return null;
		}
	}
	
	public void close() {
		m_connectionPool.close();
	}
}
