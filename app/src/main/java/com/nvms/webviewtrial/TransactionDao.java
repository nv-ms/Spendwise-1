package com.nvms.webviewtrial;

import androidx.room.*;

import java.util.List;

@Dao
public interface TransactionDao {
    @Insert
    void addTransaction(Transaction transaction);

    @Query("SELECT * FROM Transactions WHERE userId = :userId")
    List<Transaction> getAllTransactionsForUser(int userId);

    @Query("SELECT * FROM Transactions WHERE transactionId = :transactionId")
    Transaction getTransactionById(String transactionId);

    @Delete
    void deleteTransaction(Transaction transaction);

    @Update
    void updateTransaction(Transaction transaction);
}
