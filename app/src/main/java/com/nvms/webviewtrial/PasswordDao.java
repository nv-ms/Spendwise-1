package com.nvms.webviewtrial;

import androidx.room.*;

@Dao
public interface PasswordDao {
    @Insert
    void createPassword(Password password);
    @Delete
    void deletePassword(Password password);
    @Update
    void updatePassword(Password password);
    @Query("SELECT * FROM Passwords")
    Password getPassword();

}
