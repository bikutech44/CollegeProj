package com.example.collegeproj;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.Timestamp;

public class ChatListItem implements Parcelable {
    private String conversationId; // Unique ID for this chat (e.g., sorted UIDs of participants)
    private String otherUserId;    // The UID of the other person in this conversation
    private String otherUserName;  // The name of the other person
    private String otherUserProfileImageUrl; // Profile picture of the other person
    private String lastMessageText; // The content of the last message
    private Timestamp lastMessageTimestamp; // When the last message was sent
    private String associatedRoomId; // Optional: If the chat started from a specific room

    public ChatListItem() {
        // Required public no-argument constructor for Firestore deserialization
    }

    public ChatListItem(String conversationId, String otherUserId, String otherUserName,
                        String otherUserProfileImageUrl, String lastMessageText,
                        Timestamp lastMessageTimestamp, String associatedRoomId) {
        this.conversationId = conversationId;
        this.otherUserId = otherUserId;
        this.otherUserName = otherUserName;
        this.otherUserProfileImageUrl = otherUserProfileImageUrl;
        this.lastMessageText = lastMessageText;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.associatedRoomId = associatedRoomId;
    }

    // --- Getters ---
    public String getConversationId() {
        return conversationId;
    }

    public String getOtherUserId() {
        return otherUserId;
    }

    public String getOtherUserName() {
        return otherUserName;
    }

    public String getOtherUserProfileImageUrl() {
        return otherUserProfileImageUrl;
    }

    public String getLastMessageText() {
        return lastMessageText;
    }

    public Timestamp getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public String getAssociatedRoomId() {
        return associatedRoomId;
    }

    // --- Setters (optional, but good practice if you modify objects) ---
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public void setOtherUserId(String otherUserId) {
        this.otherUserId = otherUserId;
    }

    public void setOtherUserName(String otherUserName) {
        this.otherUserName = otherUserName;
    }

    public void setOtherUserProfileImageUrl(String otherUserProfileImageUrl) {
        this.otherUserProfileImageUrl = otherUserProfileImageUrl;
    }

    public void setLastMessageText(String lastMessageText) {
        this.lastMessageText = lastMessageText;
    }

    public void setLastMessageTimestamp(Timestamp lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public void setAssociatedRoomId(String associatedRoomId) {
        this.associatedRoomId = associatedRoomId;
    }


    // --- Parcelable implementation ---
    protected ChatListItem(Parcel in) {
        conversationId = in.readString();
        otherUserId = in.readString();
        otherUserName = in.readString();
        otherUserProfileImageUrl = in.readString();
        lastMessageText = in.readString();
        lastMessageTimestamp = in.readParcelable(Timestamp.class.getClassLoader());
        associatedRoomId = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(conversationId);
        dest.writeString(otherUserId);
        dest.writeString(otherUserName);
        dest.writeString(otherUserProfileImageUrl);
        dest.writeString(lastMessageText);
        dest.writeParcelable(lastMessageTimestamp, flags);
        dest.writeString(associatedRoomId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ChatListItem> CREATOR = new Creator<ChatListItem>() {
        @Override
        public ChatListItem createFromParcel(Parcel in) {
            return new ChatListItem(in);
        }

        @Override
        public ChatListItem[] newArray(int size) {
            return new ChatListItem[size];
        }
    };
}
