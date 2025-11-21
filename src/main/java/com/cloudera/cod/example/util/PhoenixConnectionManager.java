package com.cloudera.cod.example.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Phoenix Connection Manager for managing JDBC connections to Apache Phoenix
 * using the thick client approach.
 */
public class PhoenixConnectionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(PhoenixConnectionManager.class);
    
    // Phoenix JDBC driver class
    private static final String PHOENIX_DRIVER = "org.apache.phoenix.jdbc.PhoenixDriver";

    private static String jdbcUrl;
    
        
    static {
        try {

            Properties props = ApplicationPropertyLoader.builder().build();
            jdbcUrl = props.getProperty("cod.jdbc.url");

            if (jdbcUrl==null || jdbcUrl.trim().length()==0){
                System.out.println("ERROR - cod.jdbc.url property is not defined in application.properties file.");
                System.exit(-1);
            }

            // Load Phoenix JDBC driver

            Class.forName(PHOENIX_DRIVER);

            logger.info("Phoenix JDBC driver loaded successfully");
        } catch (ClassNotFoundException e) {
            logger.error("Failed to load Phoenix JDBC driver", e);
            throw new RuntimeException("Phoenix JDBC driver not found", e);
        }
    }

    /**
     * Get a connection to Phoenix using the default JDBC URL with autocommit enabled
     * 
     * @return Connection object
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        return getConnection(jdbcUrl, true);
    }

    /**
     * Get a connection to Phoenix using the default JDBC URL
     * 
     * @param autocommit enable/disable autocommit
     * @return Connection object
     * @throws SQLException if connection fails
     */
    public static Connection getConnection(boolean autocommit) throws SQLException {
         return getConnection(jdbcUrl, autocommit);
    }


    /**
     * Get a connection with custom properties
     *
     * @param jdbcUrl    JDBC URL
     * @param autocommit
     * @return Connection object
     * @throws SQLException if connection fails
     */
    public static Connection getConnection(String jdbcUrl, boolean autocommit) throws SQLException {
        logger.info("Creating Phoenix connection with custom properties to: {}", jdbcUrl);
        Properties props = new Properties();
        // Optional: Add Phoenix configuration properties
        props.setProperty("phoenix.query.timeoutMs", "30000");
        props.setProperty("phoenix.query.keepAliveMs", "60000");
        Connection connection = DriverManager.getConnection(jdbcUrl, props);
        connection.setAutoCommit(autocommit);
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

