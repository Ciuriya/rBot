package managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import data.Database;

/**
 * This class acts as the manager for all databases used by this bot.
 * To access a database, this must be used.
 * 
 * @author Smc
 */
public class DatabaseManager {
	
	private static DatabaseManager instance;
	private Map<String, Database> m_databases;
	
	public static DatabaseManager getInstance() {
		if(instance == null) instance = new DatabaseManager();
		
		return instance;
	}
	
	public DatabaseManager() {
		m_databases = new HashMap<>();
		
		System.setProperty("com.mchange.v2.log.FallbackMLog.DEFAULT_CUTOFF_LEVEL", "INFO");
	}
	
	public Database get(String p_databaseName) {
		return m_databases.get(p_databaseName);
	}
	
	public void setup(String p_databaseName, String p_jdbcUrl, String p_user, String p_pass) {
		m_databases.put(p_databaseName, new Database(p_jdbcUrl, p_user, p_pass));
	}
	
	public void close() {
		if(m_databases.size() > 0) {
			for(Database db : new ArrayList<Database>(m_databases.values()))
				db.close();
			
			m_databases.clear();
		}
	}
}
