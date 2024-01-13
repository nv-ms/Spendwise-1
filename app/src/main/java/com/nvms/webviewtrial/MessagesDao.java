package com.nvms.webviewtrial;

import androidx.room.*;
import java.util.List;
@Dao
public interface MessagesDao {
    @Insert
    void addMessage(Messages message);
    @Query("SELECT * FROM Messages")
    List<Messages> getAllMessages();
    @Query("SELECT * FROM Messages WHERE messageId = :id")
    Messages getMessageById(long id);
    @Query("DELETE FROM Messages")
    void deleteAllMessages();
    @Update
    void updateMessage(Messages message);
    @Delete
    void deleteMessage(Messages message);
}
