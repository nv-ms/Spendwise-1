package com.nvms.webviewtrial;

import androidx.room.*;

import java.util.List;

@Dao
public interface UserDao {
    @Insert
    void addUser(User user);

    @Update
    void updateUser(User user);

    @Delete
    void deleteUser(User user);
    @Query("SELECT * FROM Users")
    List<User> getAllUsers();
    @Query("SELECT * FROM Users WHERE userId = :userId")
    User getUserById(int userId);
}
