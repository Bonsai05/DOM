package com.capstone.testapp;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class Message {

    public enum Status { SENDING, SENT, FAILED }

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String textContent;
    public long timestamp;
    public boolean isSentByMe;
    public Status status;

    public Message(String textContent, long timestamp, boolean isSentByMe) {
        this.textContent = textContent;
        this.timestamp = timestamp;
        this.isSentByMe = isSentByMe;
        this.status = isSentByMe ? Status.SENDING : Status.SENT; // Default status
    }
}

