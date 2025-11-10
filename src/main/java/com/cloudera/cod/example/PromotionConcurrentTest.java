package com.cloudera.cod.example;

import com.cloudera.cod.example.dao.PromotionDAO;
import com.cloudera.cod.example.model.Promotion;
import com.cloudera.cod.example.util.PhoenixConnectionManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Multi-threaded test class for concurrent access to PromotionDAO
 * Demonstrates how to use multiple threads to read promotions from Phoenix
 */
public class PromotionConcurrentTest {
    
    private static final Logger logger = LoggerFactory.getLogger(PromotionConcurrentTest.class);
    
    private final int threadCount;
    private final int iterationsPerThread;
    private final String[] customerIds;
    
    // Statistics tracking
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger errorCount = new AtomicInteger(0);
    
    /**
     * Constructor
     * 
     * @param threadCount Number of concurrent threads
     * @param iterationsPerThread Number of iterations each thread will perform
     * @param customerIds Array of customer IDs to query
     */
    public PromotionConcurrentTest(int threadCount, int iterationsPerThread, String[] customerIds) {
        this.threadCount = threadCount;
        this.iterationsPerThread = iterationsPerThread;
        this.customerIds = customerIds;
    }
    
    /**
     * Execute the multi-threaded test
     */
    public void execute() {
        logger.info("Starting concurrent test with {} threads, {} iterations per thread", 
            threadCount, iterationsPerThread);
        
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<Future<ThreadResult>> futures = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        
        // Submit tasks to thread pool
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            Future<ThreadResult> future = executorService.submit(new PromotionQueryTask(threadId));
            futures.add(future);
        }
        
        // Wait for all threads to complete
        for (Future<ThreadResult> future : futures) {
            try {
                ThreadResult result = future.get();
                logger.info("Thread {} completed: {} successes, {} errors, avg time: {} ms",
                    result.threadId, result.successCount, result.errorCount, result.avgResponseTime());
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Error waiting for thread completion", e);
            }
        }
        
        executorService.shutdown();
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        // Print summary
        printSummary(totalTime);
    }
    
    /**
     * Print test summary
     */
    private void printSummary(long totalTime) {
        int totalOperations = threadCount * iterationsPerThread;
        double avgTimePerOps;
        avgTimePerOps = (double) totalTime /totalOperations;
        double opsPerSec;
        opsPerSec = (double) (totalOperations /totalTime) * 1000;

        logger.info("\n" + String.format("%060d", 0));
        logger.info("CONCURRENT TEST SUMMARY");
        logger.info(String.format("%060d", 0));
        logger.info("Threads: {}", threadCount);
        logger.info("Iterations per thread: {}", iterationsPerThread);
        logger.info("Total operations: {}", totalOperations);
        logger.info("Total time: {} ms", totalTime);
        logger.info("Throughput: {} ops/sec", opsPerSec);
        logger.info("Success count: {}", successCount.get());
        logger.info("Error count: {}", errorCount.get());
        logger.info("Average response time per operation: {} ms", avgTimePerOps);
        logger.info(String.format("%060d", 0));
    }
    
    /**
     * Callable task that queries promotions
     */
    private class PromotionQueryTask implements Callable<ThreadResult> {
        private final int threadId;
        
        public PromotionQueryTask(int threadId) {
            this.threadId = threadId;
        }
        
        @Override
        public ThreadResult call() {
            ThreadResult result = new ThreadResult(threadId);
            Connection connection = null;
            
            try {
                // Each thread gets its own connection
                connection = PhoenixConnectionManager.getConnection();
                PromotionDAO promotionDAO = new PromotionDAO(connection);
                
                logger.info("Thread {} started", threadId);
                int custIdBase=threadId * 100000;

                for (int i = 0; i < iterationsPerThread; i++) {
                    // Select a customer ID (round-robin)
                    String custId = StringUtils.leftPad(Integer.toString(custIdBase+i), 9, "0");

                            //customerIds[i % customerIds.length];
                    
                    long startTime = System.nanoTime();
                    
                    try {
                        // Call getPromotionById
                        Promotion promotion = promotionDAO.getPromotionById(custId);
                        
                        long endTime = System.nanoTime();
                        long responseTime = (endTime - startTime) / 1000000; // Convert to ms
                        
                        result.addResponseTime(responseTime);
                        
                        if (promotion != null) {
                            result.successCount++;
                            successCount.incrementAndGet();
                            
                            if (i % 100 == 0) { // Log every 100th iteration
                                logger.debug("Thread {} - Iteration {}: Found promotion for {} = {}",
                                    threadId, i, custId, promotion.getPromotions());
                            }
                        } else {
                            logger.warn("Thread {} - Iteration {}: No promotion found for {}",
                                threadId, i, custId);
                        }
                        
                    } catch (SQLException e) {
                        result.errorCount++;
                        errorCount.incrementAndGet();
                        logger.error("Thread {} - Iteration {}: Error querying promotion for {}",
                            threadId, i, custId, e);
                    }
                }
                
                logger.info("Thread {} completed successfully", threadId);
                
            } catch (SQLException e) {
                logger.error("Thread {} failed to get connection", threadId, e);
                result.errorCount = iterationsPerThread;
            } finally {
                if (connection != null) {
                    PhoenixConnectionManager.closeConnection(connection);
                }
            }
            
            return result;
        }
    }
    
    /**
     * Result object for each thread
     */
    private static class ThreadResult {
        int threadId;
        int successCount = 0;
        int errorCount = 0;
        long totalResponseTime = 0;
        int responseCount = 0;
        
        public ThreadResult(int threadId) {
            this.threadId = threadId;
        }
        
        public void addResponseTime(long responseTime) {
            totalResponseTime += responseTime;
            responseCount++;
        }
        
        public double avgResponseTime() {
            return responseCount > 0 ? (double) totalResponseTime / responseCount : 0;
        }
    }
    
    /**
     * Main method to run the concurrent test
     */
    public static void main(String[] args) {
        try {
            // Configuration
            int threadCount = args.length > 0 ? Integer.parseInt(args[0]) : 10;
            int iterationsPerThread = args.length > 1 ? Integer.parseInt(args[1]) : 100;
            
            // Customer IDs to query
            String[] customerIds = {
                "CUST00001", "CUST00002", "CUST00003", "CUST00004", "CUST00005",
                "CUST00006", "CUST00007", "CUST00008", "CUST00009"
            };
            
            logger.info(String.format("%060d", 0));
            logger.info("PHOENIX PROMOTION CONCURRENT TEST");
            logger.info(String.format("%060d", 0));
            logger.info("Configuration:");
            logger.info("  Threads: {}", threadCount);
            logger.info("  Iterations per thread: {}", iterationsPerThread);
            logger.info("  Customer IDs: {} different IDs", customerIds.length);
            logger.info("  Total operations: {}", threadCount * iterationsPerThread);
            logger.info(String.format("%060d", 0) + "\n");
            
            // Create and execute test
            PromotionConcurrentTest test = new PromotionConcurrentTest(
                threadCount, 
                iterationsPerThread, 
                customerIds
            );
            
            test.execute();
            
        } catch (Exception e) {
            logger.error("Failed to run concurrent test", e);
            e.printStackTrace();
        }
    }
}

