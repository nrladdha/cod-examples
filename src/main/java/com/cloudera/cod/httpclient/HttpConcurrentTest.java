package com.cloudera.cod.httpclient;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Multi-threaded test class for concurrent access to PromotionDAO
 * Demonstrates how to use multiple threads to read promotions from Phoenix
 */
public class HttpConcurrentTest {

    private static final Logger logger = LoggerFactory.getLogger(HttpConcurrentTest.class);

    private final int threadCount;
    private final int iterationsPerThread;

    // Statistics tracking
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger errorCount = new AtomicInteger(0);
    
    // Pattern to extract CODreqduration from JSON response
    private static final Pattern COD_DURATION_PATTERN = Pattern.compile("\"CODreq_duration\"\\s*:\\s*\"([0-9.]+)\"");




    /**
     * Constructor
     *
     * @param threadCount Number of concurrent threads
     * @param iterationsPerThread Number of iterations each thread will perform

     */
    public HttpConcurrentTest(int threadCount, int iterationsPerThread) {
        this.threadCount = threadCount;
        this.iterationsPerThread = iterationsPerThread;
    }
    
    /**
     * Execute the multi-threaded test
     */
    public void execute() {
        logger.info("Starting concurrent test with {} threads, {} queries per thread",
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
        
        // Wait for all threads to complete and collect statistics
        long codElapsedTime = 0;
        long codMinStartTime = 0; 
        long codMaxEndTime = 0;
        double totalCodDuration = 0.0;
        int totalCodCount = 0;
        double totalResponseDuration = 0.0;
        int totalResponseCount=0;
        List<String> threadSummaryList = new ArrayList<>();
        String threadSummary=null;

        for (Future<ThreadResult> future : futures) {
            try {
                ThreadResult result = future.get();
                totalResponseDuration+= result.totalResponseTime;
                totalResponseCount+=result.responseCount;
                totalCodDuration += result.getTotalCodDuration();
                if (codMinStartTime == 0 || result.getStartTimeinNanoSeconds() < codMinStartTime) {
                    codMinStartTime = result.getStartTimeinNanoSeconds();
                }
                if (codMaxEndTime == 0 || result.getEndTimeinNanoSeconds() > codMaxEndTime) {
                    codMaxEndTime = result.getEndTimeinNanoSeconds();
                }
                totalCodCount += result.codDurationCount;
                threadSummary=new StringBuffer("For a single thread, Thread-").append(result.threadId).append("[queries completed =>  ").append(result.successCount).append("  successes, ")
                        .append(result.errorCount).append(" errors ], Avg  response time (client side): ").append(result.avgResponseTime()).append(" ms, avg COD response time (server side): ")
                        .append(result.avgCodDuration()).append(" sec, total COD response time: ").append(result.getTotalCodDuration()).append(" sec").toString();

                threadSummaryList.add(threadSummary);
                logger.info(threadSummary);
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Error waiting for thread completion", e);
            }
        }
        
        executorService.shutdown();
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        codElapsedTime = codMaxEndTime - codMinStartTime;
        
        // Print summary
        printSummary(totalTime, totalResponseDuration,totalResponseCount,codElapsedTime, totalCodDuration, totalCodCount);
    }
    
    /**
     * Extract CODreqduration from JSON response string
     * @param jsonResponse JSON response string
     * @return COD duration in seconds, or 0.0 if not found
     */
    private static double extractCodDuration(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.isEmpty()) {
            return 0.0;
        }
        
        Matcher matcher = COD_DURATION_PATTERN.matcher(jsonResponse);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                logger.warn("Failed to parse CODreqduration: {}", matcher.group(1));
                return 0.0;
            }
        }
        return 0.0;
    }
    
    /**
     * Print test summary
     */
    private void printSummary(long totalTime, double totalResponseDuration,int totalResponseCount, long codElapsedTime, double totalCodDuration, int totalCodCount ) {

        int totalOperations = threadCount * iterationsPerThread;
        double codElapsedTimeinSec = (double) codElapsedTime / 1000000000;
        double avgTimePerResponse = (double) totalResponseDuration / totalResponseCount;
        double totalElapsedTimeinSec= totalTime/1000.0;
        double responsesPerSec = ((double) totalResponseCount / totalElapsedTimeinSec) ;
        double avgTimePerCodResponse = totalCodDuration / totalCodCount ;
        double codTPS = ((double) totalCodCount / codElapsedTimeinSec) ;


        logger.info("\n" + String.format("%060d", 0).replace("0", "="));
        logger.info("CONCURRENT TEST SUMMARY");
        logger.info(String.format("%060d", 0).replace("0", "="));
        logger.info("Configuration:");
        logger.info("  Threads: {}", threadCount);
        logger.info("  Queries per thread: {}", iterationsPerThread);
        logger.info("  Total requests: {}", totalOperations);
        logger.info("  Total responses: {}", totalResponseCount);
        logger.info("  Test elapsed time : {} sec", totalElapsedTimeinSec);
        logger.info("  Test processing time utilised : {} sec", totalResponseDuration);
        logger.info("");
        logger.info("Performance Metrics (client-side):");
        logger.info("  Total duration (elapsed time) spent by client : {} sec ", totalElapsedTimeinSec);
        logger.info("  Throughput- TPS (responses per second) : {} ", responsesPerSec);
        logger.info("  Average response time: {} sec ", avgTimePerResponse / 1000);
        logger.info("");
        logger.info("Results:");
        logger.info("  Success count: {}", successCount.get());
        logger.info("  Error count: {}", errorCount.get());
        logger.info("");
        logger.info("COD Performance (Server-side):");
        logger.info("  Total COD duration (across all threads): {} sec", totalCodDuration);
        logger.info("  Total COD elapsed time (across all threads): {} sec", codElapsedTimeinSec);
        logger.info("  Average COD response time per query: {} sec", avgTimePerCodResponse);
        logger.info("  COD TPS (Transactions per second): {} ", codTPS);
        logger.info("  Total COD queries measured: {}", totalCodCount);
        logger.info(String.format("%060d", 0).replace("0", "="));


    }
    
    /**
     * Callable task that queries promotions
     */
    private class PromotionQueryTask implements Callable<ThreadResult> {
        private final int threadId;

        private final String endPointUrl = "https://x2owmptl3c.execute-api.us-east-2.amazonaws.com/default/testphoenixdb-x86_64?cust_id=";

        public PromotionQueryTask(int threadId) {
            this.threadId = threadId;
        }
        
        @Override
        public ThreadResult call() {
            ThreadResult result = new ThreadResult(threadId);

            logger.info("Thread {} started", threadId);
            try {
                // Each thread gets its own HttpClient with connection pooling and timeouts
                HttpClient httpClient = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(20))
                    .build();
                
                int custIdBase = 1000001;

                for (int i = 0; i < iterationsPerThread; i++) {
                    // Select a customer ID (round-robin)
                    String custId = StringUtils.leftPad(Integer.toString(custIdBase + i), 9, "0");
                    String apiEndPointWithCustId = endPointUrl + custId;
                    try {
                        // Call API endpoint with customer ID parameter
                        long startTime = System.nanoTime();

                        HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(apiEndPointWithCustId))
                            .timeout(Duration.ofSeconds(60))
                            .GET()
                            .build();
                        
                        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                        long endTime = System.nanoTime();
                        long responseTime = (endTime - startTime) / 1000000; // Convert to ms
                        result.setStartTimeinNanoSeconds(startTime);
                        result.setEndTimeinNanoSeconds(endTime);
                        result.addResponseTime(responseTime);

                        if (response.statusCode() == 200) {
                            result.successCount++;
                            successCount.incrementAndGet();

                            // Extract and track COD duration from response
                            String responseString = response.body();
                            double codDuration = extractCodDuration(responseString);
                            result.addCodDuration(codDuration);
                            
                        } else {
                            result.errorCount++;
                            errorCount.incrementAndGet();
//                            if (i % 100 == 0) {
//                                logger.error("Thread {} - Iteration {}: No promotion found for {} - Status: {}, Body: {}",
//                                    threadId, i, custId, response.statusCode(), response.body());
//                            }
                        }
//                        if (i % 100 == 0) {
//                            logger.info("Thread {} - queries {}: completed",threadId, i);
//                        }
                       
                    } catch (Exception e) {
                        result.errorCount++;
                        errorCount.incrementAndGet();
                        logger.error("Thread {} - Iteration {}: Error querying promotion for {}",
                            threadId, i, custId, e);
                    }
                }
                
                logger.info("Thread {} completed successfully", threadId);
                
            } catch (Exception e) {
                logger.error("Thread {} failed to get connection", threadId, e);
                result.errorCount = iterationsPerThread;
            } finally {

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
        double totalCodDuration = 0.0;
        int codDurationCount = 0;
        long startTimeinNanoSeconds = 0;
        long endTimeinNanoSeconds = 0;
        
        
        public ThreadResult(int threadId) {
            this.threadId = threadId;
        }
        
        public void addResponseTime(long responseTime) {
            totalResponseTime += responseTime;
            responseCount++;
        }
        
        public void addCodDuration(double codDuration) {
            if (codDuration > 0) {
                totalCodDuration += codDuration;
                codDurationCount++;
            }
        }
        
        public double avgResponseTime() {
            return responseCount > 0 ? (double) totalResponseTime / responseCount : 0;
        }
        
        public double avgCodDuration() {
            return codDurationCount > 0 ? totalCodDuration / codDurationCount : 0;
        }
        
        public double getTotalCodDuration() {
            return totalCodDuration;
        }

        public long getStartTimeinNanoSeconds() {
            return startTimeinNanoSeconds;
        }

        public long getEndTimeinNanoSeconds() {
            return endTimeinNanoSeconds;
        }

        public void setStartTimeinNanoSeconds(long startTimeinNanoSeconds) {
            this.startTimeinNanoSeconds = startTimeinNanoSeconds;
        }

        public void setEndTimeinNanoSeconds(long endTimeinNanoSeconds) {
            this.endTimeinNanoSeconds = endTimeinNanoSeconds;
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
            

            logger.info(String.format("%060d", 0));
            logger.info("PHOENIX PROMOTION CONCURRENT TEST");
            logger.info(String.format("%060d", 0));
            logger.info("Configuration:");
            logger.info("  Threads: {}", threadCount);
            logger.info("  Iterations per thread: {}", iterationsPerThread);
            logger.info("  Total operations: {}", threadCount * iterationsPerThread);
            logger.info(String.format("%060d", 0) + "\n");
            
            // Create and execute test
            HttpConcurrentTest test = new HttpConcurrentTest(
                threadCount, 
                iterationsPerThread
            );
            
            test.execute();
            
        } catch (Exception e) {
            logger.error("Failed to run concurrent test", e);
            e.printStackTrace();
        }
    }
}

