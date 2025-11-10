package com.cloudera.cod.example;

import com.cloudera.cod.example.dao.PromotionDAO;
import com.cloudera.cod.example.model.Promotion;
import com.cloudera.cod.example.util.PhoenixConnectionManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
public class PromotionLoader {
    
    private static final Logger logger = LoggerFactory.getLogger(PromotionLoader.class);
    public static void main(String[] args) {
        Connection connection = null;
        
        try {
            // Step 1: Establish connection to Phoenix
            logger.info("=== Step 1: Connecting to Apache Phoenix ===");
            connection = PhoenixConnectionManager.getConnection();
            logger.info("Successfully connected to Phoenix");

            // Create DAO instance
            PromotionDAO promotionDAO = new PromotionDAO(connection);

            // Step 2: Create table

            logger.info("\n=== Step 2: Creating PROMOTIONS table ===");
            promotionDAO.createTable();
            logger.info("Table structure: CUST_ID VARCHAR(9) PRIMARY KEY, PROMOTIONS VARCHAR");

            // Step 3: Insert single promotion

//                logger.info("\n=== Step 3: Inserting a single promotion ===");
//                Promotion promo1 = new Promotion("CUST00001", "SUMMER_SALE");
//                promotionDAO.insertPromotion(promo1);
//                logger.info("Inserted: {}", promo1);
//                        // Step 4: Insert using convenience method
//                logger.info("\n=== Step 4: Inserting promotion using convenience method ===");
//                promotionDAO.insertPromotion("CUST00002", "WINTER_DISCOUNT");
//                logger.info("Inserted promotion for CUST00002 with convenience method");
//
            // Step 5: Batch insert multiple promotions
            logger.info("\n=== Step 5: Batch inserting multiple promotions ===");

            Path path = Paths.get("input_data.txt");
            BufferedReader reader = Files.newBufferedReader(path);
            String promoStr = reader.readLine();
            logger.info("\npromoStr :"+promoStr);
            if ((promoStr == null) || (promoStr.length()==0)){
                System.exit(-1);
            }



            int batchSize = 10000;
            List<Promotion> promotionList = new ArrayList<>(batchSize);
            int custId = 5000001;
            Promotion p = null;
            int insertedCount = 0;
            for (int batch = 1; batch <= 500; batch++){
                for (int ct = 1; ct <= batchSize; ct++) {
                    promotionList.add(new Promotion(StringUtils.leftPad(Integer.toString(custId), 9, "0"), promoStr));
                    custId++;
                }
                insertedCount = promotionDAO.insertPromotionBatch(promotionList);
                logger.info("Batch no : {} , inserted {} promotions", batch, insertedCount);
                promotionList.clear();

            }
     
            
//            // Step 6: Read single promotion by ID
//            logger.info("\n=== Step 6: Reading promotion by customer ID ===");
//            Promotion retrievedPromo = promotionDAO.getPromotionById("CUST00001");
//            logger.info("Retrieved promotion:");
//            logger.info("  CUST_ID: {}", retrievedPromo.getCustId());
//            logger.info("  PROMOTIONS: {}", retrievedPromo.getPromotions());

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

   
}
