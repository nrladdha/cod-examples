package com.cloudera.cod.example.dao;

import com.cloudera.cod.example.model.Promotion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Promotion operations in Apache Phoenix
 * Handles PROMOTIONS table with CUST_ID and PROMOTIONS fields
 */
public class PromotionDAO {
    
    private static final Logger logger = LoggerFactory.getLogger(PromotionDAO.class);
    
    private Connection connection;
    
    public PromotionDAO(Connection connection) {
        this.connection = connection;
    }
    
    public void createSchema() throws SQLException {
        String createSchemaSQL = "CREATE SCHEMA IF NOT EXISTS TEST1" ;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createSchemaSQL);
            connection.commit();
            logger.info("TEST1 schema created successfully");
        } catch (SQLException e) {
            connection.rollback();
            logger.error("Failed to create TEST1 schema", e);
            throw e;
        }
    }
    /**
     * Create the PROMOTIONS table in Phoenix
     * Table structure:
     *   - CUST_ID VARCHAR(9) PRIMARY KEY
     *   - PROMOTIONS VARCHAR
     * 
     * @throws SQLException if table creation fails
     */
    public void createTable() throws SQLException {
        String createTableSQL = 
            "CREATE TABLE IF NOT EXISTS TEST1.PROMOTIONS (" +
            "    CUST_ID VARCHAR(9) NOT NULL PRIMARY KEY, " +
            "    PROMOTIONS VARCHAR" +
            ")";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
            connection.commit();
            logger.info("PROMOTIONS table created successfully");
        } catch (SQLException e) {
            connection.rollback();
            logger.error("Failed to create PROMOTIONS table", e);
            throw e;
        }
    }
    
    /**
     * Insert a single promotion record
     * 
     * @param promotion Promotion object to insert
     * @return true if insert was successful
     * @throws SQLException if insert fails
     */
    public boolean insertPromotion(Promotion promotion) throws SQLException {
        String insertSQL = 
            "UPSERT INTO TEST1.PROMOTIONS (CUST_ID, PROMOTIONS) VALUES (?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
            pstmt.setString(1, promotion.getCustId());
            pstmt.setString(2, promotion.getPromotions());
            
            int rowsAffected = pstmt.executeUpdate();
            connection.commit();
            
            logger.info("Promotion inserted successfully: {}", promotion.getCustId());
            return rowsAffected > 0;
        } catch (SQLException e) {
            connection.rollback();
            logger.error("Failed to insert promotion: {}", promotion.getCustId(), e);
            throw e;
        }
    }
    
    /**
     * Insert a promotion with individual fields (convenience method)
     * 
     * @param custId Customer ID
     * @param promotion Promotion information
     * @return true if insert was successful
     * @throws SQLException if insert fails
     */
    public boolean insertPromotion(String custId, String promotion) throws SQLException {
        Promotion promotionObj = new Promotion(custId, promotion);
        return insertPromotion(promotionObj);
    }
    
    /**
     * Insert multiple promotion records in a batch
     * 
     * @param promotions List of Promotion objects to insert
     * @return number of records inserted
     * @throws SQLException if batch insert fails
     */
    public int insertPromotionBatch(List<Promotion> promotions) throws SQLException {
        String insertSQL = 
            "UPSERT INTO TEST1.PROMOTIONS (CUST_ID, PROMOTIONS) VALUES (?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
            for (Promotion promotion : promotions) {
                pstmt.setString(1, promotion.getCustId());
                pstmt.setString(2, promotion.getPromotions());
                pstmt.addBatch();
            }
            
            int[] results = pstmt.executeBatch();
            connection.commit();
            
            int successCount = 0;
            for (int result : results) {
                if (result > 0) successCount++;
            }
            
            logger.info("Batch insert completed: {} promotions inserted", successCount);
            return successCount;
        } catch (SQLException e) {
            connection.rollback();
            logger.error("Failed to batch insert promotions", e);
            throw e;
        }
    }
    
   
    /**
     * Retrieve a promotion by customer ID
     * 
     * @param custId Customer ID to retrieve
     * @return Promotion object or null if not found
     * @throws SQLException if query fails
     */
    public Promotion getPromotionById(String custId) throws SQLException {
        String selectSQL = "SELECT * FROM TEST1.PROMOTIONS WHERE CUST_ID = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(selectSQL)) {
            pstmt.setString(1, custId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPromotion(rs);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to retrieve promotion: {}", custId, e);
            throw e;
        }
        
        return null;
    }
    
   
    /**
     * Drop the PROMOTIONS table
     * 
     * @throws SQLException if drop fails
     */
    public void dropTable() throws SQLException {
        String dropTableSQL = "DROP TABLE IF EXISTS TEST1.PROMOTIONS";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(dropTableSQL);
            connection.commit();
            logger.info("PROMOTIONS table dropped successfully");
        } catch (SQLException e) {
            connection.rollback();
            logger.error("Failed to drop PROMOTIONS table", e);
            throw e;
        }
    }
    
    /**
     * Map a ResultSet row to a Promotion object
     * 
     * @param rs ResultSet positioned at a row
     * @return Promotion object
     * @throws SQLException if mapping fails
     */
    private Promotion mapResultSetToPromotion(ResultSet rs) throws SQLException {
        Promotion promotion = new Promotion();
        promotion.setCustId(rs.getString("CUST_ID"));
        promotion.setPromotions(rs.getString("PROMOTIONS"));
        return promotion;
    }
}
