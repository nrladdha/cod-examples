package com.cloudera.cod.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.cloudera.cod.example.dao.PromotionDAO;
import com.cloudera.cod.example.util.PhoenixConnectionManager;
import com.cloudera.cod.example.model.Promotion;
import java.sql.Connection;

/**
 * AWS Lambda handler for Phoenix thick client with Kerberos authentication
 * This handler connects to HBase via Phoenix thick driver using Kerberos
 */
public class LambdaCODHandler implements RequestHandler<LambdaCODHandler.CustomerRequest, String> {

    public static PromotionDAO promotionDAO=null;
    public static String jdbcUrl=null;


    /**
     * Customer request POJO
     */
    public static class CustomerRequest {
        private String cust_id;
        
        public CustomerRequest() {}
        
        public CustomerRequest(String cust_id) {
            this.cust_id = cust_id;
        }
        
        public String getCust_id() {
            return cust_id;
        }
        
        public void setCust_id(String cust_id) {
            this.cust_id = cust_id;
        }
        
        @Override
        public String toString() {
            return "CustomerRequest{cust_id='" + cust_id + "'}";
        }
    }

    /**
     * Initialize the PromotionDAO with Phoenix connection
     * This runs once during Lambda cold start
     */
    static {
        try {
            // Get JDBC URL from environment variable or use default
            jdbcUrl = System.getenv("JDBC_URL");
            if (jdbcUrl == null || jdbcUrl.isEmpty()) {
               // No fall back URL - fail fast - throw exception
                throw new RuntimeException("jdbcUrl is not set");

            } else {
                // ensure auto commit is always set to true
                promotionDAO = new PromotionDAO(PhoenixConnectionManager.getConnection(jdbcUrl, true));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Phoenix connection", e);
        }
    }

    @Override
    public String handleRequest(CustomerRequest event, Context context) {
        try {
            LambdaLogger logger = context.getLogger();
            logger.log("Processing request for customer: " + event.getCust_id());

            if (promotionDAO==null){
                context.getLogger().log("Failed to initiate database connection. jdbcURL :  "+jdbcUrl);
                throw new RuntimeException("Failed to initiate database connection. jdbcURL :  "+jdbcUrl);
            }
            // Measure query performance
            long startTime = System.nanoTime();
            Promotion retrievedPromo = promotionDAO.getPromotionById(event.getCust_id());
            long endTime = System.nanoTime();
            long responseTime = (endTime - startTime) / 1000000; // Convert to ms
            
            if (retrievedPromo == null) {
                logger.log("No promotion found for customer: " + event.getCust_id());
                return "{\"error\": \"Customer not found\", \"cust_id\": \"" + event.getCust_id() + "\"}";
            }
            
            logger.log("Event: " + event.toString() + ", Customer record: " + retrievedPromo.toString() + 
                      ", responseTime: " + responseTime + " ms");

            return retrievedPromo.toString();

        } catch (Exception e) {
            context.getLogger().log("Failed to process event: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error processing request", e);
        }
    }
}
