package com.spendtracker.pro;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;

@Dao
public interface TransactionDao {
    @Insert void insert(Transaction t);
    @Update void update(Transaction t);
    @Delete void delete(Transaction t);

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    LiveData<List<Transaction>> getAll();

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    List<Transaction> getAllSync();

    @Query("SELECT * FROM transactions WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp DESC")
    List<Transaction> getByDateRange(long start, long end);

    @Query("SELECT * FROM transactions WHERE category = :cat ORDER BY timestamp DESC")
    List<Transaction> getByCategory(String cat);

    @Query("SELECT SUM(amount) FROM transactions WHERE timestamp >= :start AND timestamp <= :end AND isSelfTransfer = 0")
    double getSumByDateRange(long start, long end);

    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE timestamp >= :start AND timestamp <= :end GROUP BY category ORDER BY total DESC")
    List<CategorySum> getCategorySumByDateRange(long start, long end);

    @Query("SELECT * FROM transactions WHERE rawSms = :sms LIMIT 1")
    Transaction findBySms(String sms);

    @Query("SELECT COUNT(*) FROM transactions")
    int getCount();

    @Query("SELECT * FROM transactions WHERE timestamp >= :start AND timestamp <= :end AND (:cat = '' OR category = :cat) AND (:method = '' OR paymentMethod = :method) AND amount >= :minAmt AND amount <= :maxAmt ORDER BY timestamp DESC")
    List<Transaction> getFiltered(long start, long end, String cat, String method, double minAmt, double maxAmt);

    @Query("DELETE FROM transactions")
    void deleteAll();


    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE category = :cat AND isSelfTransfer = 0 AND timestamp >= :start AND timestamp < :end")
    double getSumForCategoryBetween(String cat, long start, long end);

    @Query("SELECT merchant, SUM(amount) as total FROM transactions WHERE timestamp >= :start AND timestamp < :end AND isSelfTransfer = 0 GROUP BY merchant ORDER BY total DESC LIMIT :limit")
    List<MerchantSum> getTopMerchantsByDateRange(long start, long end, int limit);

    static class MerchantSum {
        public String merchant;
        public double total;
    }

    static class CategorySum {
        public String category;
        public double total;
    }
}
