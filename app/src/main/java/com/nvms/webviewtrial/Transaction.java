package com.nvms.webviewtrial;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.UUID;

@Entity(tableName = "Transactions")
public class Transaction {
    @PrimaryKey
    @NonNull
    private String transactionId;
    private String transactionTitle;
    private int userId;
    private long amount;
    private long transactionCost;
    private String description;
    private String refDescription;
    private long dateAdded;
    private String imageName;
    public Transaction(@NonNull String transactionId, String transactionTitle, int userId, long amount, long transactionCost, String description, String refDescription, String imageName) {
        this.transactionId = transactionId;
        this.transactionTitle = transactionTitle;
        this.userId = userId;
        this.description = description;
        this.refDescription = refDescription;
        this.amount = amount;
        this.transactionCost = transactionCost;
        this.dateAdded = System.currentTimeMillis();
        this.imageName = imageName;
    }
    @NonNull
    public String getTransactionId() {
        return transactionId;
    }
    public void setTransactionId(@NonNull String transactionId) {
        this.transactionId = transactionId;
    }
    public String getTransactionTitle() {
        return transactionTitle;
    }
    public void setTransactionTitle(String transactionTitle) {
        this.transactionTitle = transactionTitle;
    }
    public int getUserId() {
        return userId;
    }
    public void setUserId(int userId) {
        this.userId = userId;
    }
    public long getAmount(){
        return amount;
    }
    public void setAmount(long amount){
        this.amount = amount;
    }
    public long getTransactionCost(){
        return transactionCost;
    }
    public void setTransactionCost(long transactionCost){
        this.transactionCost = transactionCost;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getRefDescription(){ return refDescription; }
    public void setRefDescription(String refDescription) { this.refDescription = refDescription; }
    public long getDateAdded() {
        return dateAdded;
    }
    public void setDateAdded(long dateAdded) {
        this.dateAdded = dateAdded;
    }
    public String getImageName(){
        return imageName;
    }
    public void setImageName(String imageName){
        this.imageName = imageName;
    }
}
