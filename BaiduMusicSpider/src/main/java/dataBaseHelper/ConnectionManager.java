package dataBaseHelper;

import java.sql.Connection;
import java.sql.DriverManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionManager {
	private static Logger logger = LoggerFactory.getLogger(ConnectionManager.class);
	private static final ThreadLocal<Connection> connectionHolder = new ThreadLocal<Connection>();
	private static final String DBURL = "jdbc:mysql://127.0.0.1:3306/db_baidu_music?useSSL=true&useUnicode=true&characterEncoding=utf8&autoReconnect=true&user=root&password=";

	public static Connection getConnectionFromThreadLocal() {
		Connection conn = connectionHolder.get();
		try {
			if (conn == null || conn.isClosed()) {
				Connection con = ConnectionManager.getConnection();
				connectionHolder.set(con);
				logger.info("[Thread]" + Thread.currentThread().getName());
				return con;
			}
			return conn;
		} catch (Exception e) {
			// TODO: handle exception
			logger.error("[ThreadLocal Get Connection Error]" + e.getMessage());
		}
		return null;
	}

	public static Connection getConnection() {
		Connection conn = null;
		try{
			Class.forName("com.mysql.jdbc.Driver");
			conn=(Connection)DriverManager.getConnection(DBURL);
		}catch (Exception e) {
			// TODO: handle exception
			logger.error("[Get Connection Error]" + e.getMessage());
		}
		return conn;
	}
}
