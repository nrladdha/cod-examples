package com.cloudera.cod.example.model;

/**
 * Promotion entity representing a record in the PROMOTIONS Phoenix table
 * Table structure: CUST_ID VARCHAR(9) PRIMARY KEY, PROMOTIONS VARCHAR
 */
public class Promotion {
    
    private String custId;
    private String promotions;
    
    // Constructors
    public Promotion() {
    }
    
    public Promotion(String custId, String promotions) {
        this.custId = custId;
        this.promotions = promotions;
    }
    
    // Getters and Setters
    public String getCustId() {
        return custId;
    }
    
    public void setCustId(String custId) {
        this.custId = custId;
    }
    
    public String getPromotions() {
        return promotions;
    }
    
    public void setPromotions(String promotions) {
        this.promotions = promotions;
    }
    
    @Override
    public String toString() {
        return "Promotion{" +
                "custId='" + custId + '\'' +
                ", promotions='" + promotions + '\'' +
                '}';
    }
}
