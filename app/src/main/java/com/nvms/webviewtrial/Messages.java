package com.nvms.webviewtrial;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Messages")
public class Messages {
    @PrimaryKey
    @NonNull
    private String messageId;
    private String messageContent;

    public void Message(String messageId, String messageContent) {
        this.messageId = messageId;
        this.messageContent = messageContent;
    }
    @NonNull
    public String getMessageIdId() {
        return messageId;
    }
    public void setMessageId(@NonNull String messageId) {
        this.messageId = messageId;
    }
    public String getMessageContent() {
        return messageContent;
    }
    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

}
