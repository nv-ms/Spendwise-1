package com.nvms.webviewtrial;

import android.content.Context;
import androidx.room.*;

@Database(entities = {User.class, Transaction.class, Password.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase INSTANCE;

    public static synchronized AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "expense-manager-db")
                    .build();
        }
        return INSTANCE;
    }

    public abstract UserDao userDao();
    public abstract TransactionDao transactionDao();
    public abstract PasswordDao passwordDao();
}
