package com.example.collegeproj;

import com.google.firebase.Timestamp;

// This class represents a single message in a chat conversation.
public class Message {
    private String senderId;    // The UID of the user who sent the message
    private String receiverId;  // The UID of the user who is supposed to receive the message (optional, but good for clarity)
    private String text;        // The content of the message
    private Timestamp timestamp; // When the message was sent

    // Required public no-argument constructor for Firestore deserialization
    public Message() {
    }

    // Constructor to create a new message object
    public Message(String senderId, String receiverId, String text, Timestamp timestamp) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.text = text;
        this.timestamp = timestamp;
    }

    // --- Getters ---
    public String getSenderId() {
        return senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public String getText() {
        return text;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    // --- Setters (optional, but good practice if you modify objects) ---
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
