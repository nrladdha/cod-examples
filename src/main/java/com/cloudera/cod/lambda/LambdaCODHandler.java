package com.cloudera.cod.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.cloudera.cod.example.dao.PromotionDAO;

import com.cloudera.cod.example.util.PhoenixConnectionManager;
import com.cloudera.cod.example.model.Promotion;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.Process;
import java.lang.Runtime;
import java.security.PrivilegedAction;
import java.sql.Connection;

/**
 * AWS Lambda handler for Phoenix thick client with Kerberos authentication
 * This handler connects to HBase via Phoenix thick driver using Kerberos
 */
public class LambdaCODHandler implements RequestHandler<LambdaCODHandler.CustomerRequest, String> {

    public static PromotionDAO promotionDAO=null;
    public static String jdbcUrl=null;

    public record CustomerRequest(String cust_id) {}
    /**
     * Customer request POJO
     */
   /* public static class CustomerRequest {
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
    } */

    /**
     * Initialize the PromotionDAO with Phoenix connection
     * This runs once during Lambda cold start
     */
    static {
        try {
            // Get JDBC URL from environment variable or use default
            jdbcUrl = System.getenv("JDBC_URL");
            System.out.println("JAVA_TOOL_OPTIONS = " + System.getenv("JAVA_TOOL_OPTIONS"));
            System.out.println("JDBC_URL = " + System.getenv("JDBC_URL"));
            System.out.println("LAMBDA_JAVA_OPTS = " + System.getenv("LAMBDA_JAVA_OPTS"));
           /* System.out.println ("Before Kinit  ");
            Process p = Runtime.getRuntime().exec ("kinit -kt ps-sandbox-aws-srv_nl_machineuser.keytab srv_nl_machineuser@PS-SANDB.A465-9Q4K.CLOUDERA.SITE");int e = p.exitValue();
            System.out.println ("Kinit exit code = " + e);
            */

            // Set environment variables
            System.setProperty("java.security.krb5.conf", "krb5.conf");
            String tasPath= System.getenv("LAMBDA_TASK_ROOT");
            System.out.println("tasPath : "+tasPath);
            // Run kinit to populate ticket cache
            System.out.println( "about to start process builder  for kinit");

            ProcessBuilder pb = new ProcessBuilder(
                    tasPath+"/bin/kinit",  // From Lambda layer
                    "-kt", "ps-sandbox-aws-srv_nl_machineuser.keytab",
                    "srv_nl_machineuser"
            );
            pb.environment().put("KRB5CCNAME", "krb5cc_lambda");
            Process p1 = pb.start();
            printoutput(p1, "Kinit process");
            System.out.println( "process builder -kinit execution complete");

            ProcessBuilder pbreview = new ProcessBuilder(
                    tasPath+"/bin/klist"
            );
            pbreview.environment().put("KRB5CCNAME", "krb5cc_lambda");

            Process p= pbreview.start();
            printoutput(p, "Klist process");


            //pb.environment().put("KRB5CCNAME", "/tmp/krb5cc_lambda");


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

    private static void printoutput(Process p, String message) throws Exception{
        BufferedReader stdout = new BufferedReader(
                new InputStreamReader(p.getInputStream()));
        BufferedReader stderr = new BufferedReader(
                new InputStreamReader(p.getErrorStream()));

        System.out.print("======= Output for "+message);
        System.out.print("Standard out : " );
        String line;
        while ((line = stdout.readLine()) != null) {
            System.out.println(line); // Goes to CloudWatch Logs
        }
        System.out.println("=====");
        System.out.print("Standard error : " );
        while ((line = stderr.readLine()) != null) {
            System.out.println(line); // Goes to CloudWatch Logs
        }
    }
    @Override
    public String handleRequest(CustomerRequest event, Context context) {
        try {
            LambdaLogger logger = context.getLogger();
            logger.log("Processing request for customer: " + event.cust_id());

            if (promotionDAO==null){
                context.getLogger().log("Failed to initiate database connection. jdbcURL :  "+jdbcUrl);
                throw new RuntimeException("Failed to initiate database connection. jdbcURL :  "+jdbcUrl);
            }
            // Measure query performance
            long startTime = System.nanoTime();
            Promotion retrievedPromo = promotionDAO.getPromotionById(event.cust_id());
            long endTime = System.nanoTime();
            long responseTime = (endTime - startTime) / 1000000; // Convert to ms
            // Place your Kerberos-enabled code here (e.g., GSS-API calls,
            // Hadoop access, etc.)

            if (retrievedPromo == null) {
                logger.log("No promotion found for customer: " + event.cust_id());
                return"{\"error\": \"Customer not found\", \"cust_id\": \"" + event.cust_id() + "\"}";
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
