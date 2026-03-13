package com.spendtracker.pro;

import android.content.Context;
import androidx.room.*;

@Database(entities = {Transaction.class, Budget.class, RecurringTransaction.class, NetWorthItem.class},
          version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;
    public abstract TransactionDao transactionDao();
    public abstract BudgetDao budgetDao();
    public abstract RecurringDao recurringDao();
    public abstract NetWorthDao netWorthDao();

    public static synchronized AppDatabase getInstance(Context ctx) {
        if (instance == null) {
            instance = Room.databaseBuilder(ctx.getApplicationContext(), AppDatabase.class, "stp_db")
                    .fallbackToDestructiveMigration().build();
        }
        return instance;
    }
}
