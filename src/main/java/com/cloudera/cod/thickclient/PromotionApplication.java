    package com.cloudera.cod.thickclient;

import com.cloudera.cod.example.dao.PromotionDAO;
import com.cloudera.cod.example.model.Promotion;
import com.cloudera.cod.example.util.PhoenixConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Main application demonstrating Apache Phoenix thick client operations
 * with PROMOTIONS table
 * 
 * This example shows how to:
 * 1. Connect to Phoenix
 * 2. Create PROMOTIONS table with CUST_ID and PROMOTIONS fields
 * 3. Insert records
 * 4. Read data
 * 5. Update data
 * 6. Delete records
 * 7. Query by promotion type
 */
public class PromotionApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(PromotionApplication.class);
    public static void main(String[] args) {
        Connection connection = null;
        
        try {
            // Step 1: Establish connection to Phoenix
            logger.info("=== Step 1: Connecting to Apache Phoenix ===");
            connection = PhoenixConnectionManager.getConnection(true);
            logger.info("Successfully connected to Phoenix");
            
            // Create DAO instance
            PromotionDAO promotionDAO = new PromotionDAO(connection);
            
            // Step 2: Create table
          
                logger.info("\n=== Step 2: Creating PROMOTIONS table ===");
                promotionDAO.createSchema();
                promotionDAO.createTable();
                logger.info("Table structure: CUST_ID VARCHAR(9) PRIMARY KEY, PROMOTIONS VARCHAR");
         
            // Step 3: Insert single promotion

                logger.info("\n=== Step 3: Inserting a single promotion ===");
                Promotion promo1 = new Promotion("CUST00001", "SUMMER_SALE");
                promotionDAO.insertPromotion(promo1);
                logger.info("Inserted: {}", promo1);
                        // Step 4: Insert using convenience method
                logger.info("\n=== Step 4: Inserting promotion using convenience method ===");
                promotionDAO.insertPromotion("CUST00002", "WINTER_DISCOUNT");
                logger.info("Inserted promotion for CUST00002 with convenience method");
                
                // Step 5: Batch insert multiple promotions
                logger.info("\n=== Step 5: Batch inserting multiple promotions ===");
                List<Promotion> promotionList = createSamplePromotions();
                int insertedCount = promotionDAO.insertPromotionBatch(promotionList);
                logger.info("Batch inserted {} promotions", insertedCount);
     
            
            // Step 6: Read single promotion by ID
            logger.info("\n=== Step 6: Reading promotion by customer ID ===");
            Promotion retrievedPromo = promotionDAO.getPromotionById("CUST00001");
            logger.info("Retrieved promotion:");
            logger.info("  CUST_ID: {}", retrievedPromo.getCustId());
            logger.info("  PROMOTIONS: {}", retrievedPromo.getPromotions());

        } catch (SQLException e) {
            logger.error("Database operation failed", e);
            e.printStackTrace();
        } catch (Exception e) {
            logger.error("Application error", e);
            e.printStackTrace();
        } finally {
            // Close connection
            if (connection != null) {
                PhoenixConnectionManager.closeConnection(connection);
            }
        }
    }
    
    /**
     * Create sample promotions for batch insert
     */
    private static List<Promotion> createSamplePromotions() {
        List<Promotion> promotions = new ArrayList<>();
        promotions.add(new Promotion("CUST00001", "SPRING_SPECIAL"));
        promotions.add(new Promotion("CUST00002", "SPRING_SPECIAL"));
        promotions.add(new Promotion("CUST00003", "SPRING_SPECIAL"));
        promotions.add(new Promotion("CUST00004", "SUMMER_SALE"));
        promotions.add(new Promotion("CUST00005", "NO_PROMOTION"));
        promotions.add(new Promotion("CUST00006", "LOYALTY_REWARD"));
        promotions.add(new Promotion("CUST00007", "SUMMER_SALE"));
        promotions.add(new Promotion("CUST00008", "REFERRAL_BONUS"));
        promotions.add(new Promotion("CUST00009", "BIRTHDAY_SPECIAL"));
        promotions.add(new Promotion("CUST00010", "BIRTHDAY_SPECIAL"));
        promotions.add(new Promotion("CUST00011", "SPRING_SPECIAL"));
        promotions.add(new Promotion("CUST00012", "SPRING_SPECIAL"));
        promotions.add(new Promotion("CUST00013", "SPRING_SPECIAL"));
        promotions.add(new Promotion("CUST00014", "SUMMER_SALE"));
        promotions.add(new Promotion("CUST00015", "NO_PROMOTION"));
        promotions.add(new Promotion("CUST00016", "LOYALTY_REWARD"));
        promotions.add(new Promotion("CUST00017", "SUMMER_SALE"));
        promotions.add(new Promotion("CUST00018", "REFERRAL_BONUS"));
        promotions.add(new Promotion("CUST00019", "BIRTHDAY_SPECIAL"));
        promotions.add(new Promotion("CUST00020", "BIRTHDAY_SPECIAL"));
        
        return promotions;
    }
    
   
}
