package com.cloudera.cod.example.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.io.InputStream; 
import java.io.IOException;
/**
 * Phoenix Connection Manager for managing JDBC connections to Apache Phoenix
 * using the thick client approach.
 */
public class PhoenixConnectionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(PhoenixConnectionManager.class);
    
    // Phoenix JDBC driver class
    private static final String PHOENIX_DRIVER = "org.apache.phoenix.jdbc.PhoenixDriver";
    private static final String PHOENIX_THIN_DRIVER = "org.apache.phoenix.jdbc.PhoenixDriver";
   
    private static String jdbcUrl;
    
        
    static {
        try {
            Properties props = new Properties();
            InputStream inputStream = PhoenixConnectionManager.class.getResourceAsStream("/application.properties");
            props.load(inputStream);
            jdbcUrl = props.getProperty("cod.jdbc.url");

            // Load Phoenix JDBC driver
            if(jdbcUrl.contains("thin"))
                Class.forName(PHOENIX_DRIVER);
            else
                Class.forName(PHOENIX_DRIVER);
            logger.info("Phoenix JDBC driver loaded successfully");
        } catch (ClassNotFoundException e) {
            logger.error("Failed to load Phoenix JDBC driver", e);
            throw new RuntimeException("Phoenix JDBC driver not found", e);
        }catch (IOException e) {
            logger.error("Failed to load application.properties", e);
            throw new RuntimeException("applicatiion.properties not found", e);
        }
    }
    
    
    /**
     * Get a connection to Phoenix using the default JDBC URL
     * 
     * @return Connection object
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        return getConnection(jdbcUrl);
    }
    
    /**
     * Get a connection to Phoenix using a custom JDBC URL
     * 
     * @param jdbcUrl JDBC URL (format: jdbc:phoenix:zookeeper_quorum:port:znode_parent)
     * @return Connection object
     * @throws SQLException if connection fails
     */
    public static Connection getConnection(String jdbcUrl) throws SQLException {
        logger.info("Creating Phoenix connection to: {}", jdbcUrl);
        Properties props = new Properties();
        
        // Optional: Add Phoenix configuration properties
        props.setProperty("phoenix.query.timeoutMs", "30000");
        props.setProperty("phoenix.query.keepAliveMs", "60000");
        
        Connection connection = DriverManager.getConnection(jdbcUrl, props);
        
        // Phoenix uses auto-commit by default, but it's good practice to set it explicitly
        connection.setAutoCommit(false);
        
        logger.info("Phoenix connection established successfully");
        return connection;
    }
    
    /**
     * Get a connection with custom properties
     * 
     * @param jdbcUrl JDBC URL
     * @param properties Custom connection properties
     * @return Connection object
     * @throws SQLException if connection fails
     */
    public static Connection getConnection(String jdbcUrl, Properties properties) throws SQLException {
        logger.info("Creating Phoenix connection with custom properties to: {}", jdbcUrl);
        Connection connection = DriverManager.getConnection(jdbcUrl, properties);
        connection.setAutoCommit(false);
        logger.info("Phoenix connection established successfully");
        return connection;
    }
    
    /**
     * Close a connection safely
     * 
     * @param connection Connection to close
     */
    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
                logger.info("Phoenix connection closed successfully");
            } catch (SQLException e) {
                logger.error("Error closing Phoenix connection", e);
            }
        }
    }
   
}

