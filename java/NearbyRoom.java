package com.example.collegeproj;

import android.os.Parcel;
import android.os.Parcelable;

public class NearbyRoom implements Parcelable {
    private String imageUrl;
    private String price;
    private String roomType; // Changed from 'description' to 'roomType' for consistency
    private String distance;
    private boolean isFavorite;

    private String documentId; // Firestore document ID of the room
    private String uploaderUserId; // ID of the user who uploaded the room

    public NearbyRoom() {
        // Default constructor needed for Firestore or Parcelable
    }

    public NearbyRoom(String imageUrl, String price, String roomType, String distance, boolean isFavorite, String documentId, String uploaderUserId) {
        this.imageUrl = imageUrl;
        this.price = price;
        this.roomType = roomType;
        this.distance = distance;
        this.isFavorite = isFavorite;
        this.documentId = documentId;
        this.uploaderUserId = uploaderUserId;
    }

    // Getters
    public String getImageUrl() { return imageUrl; }
    public String getPrice() { return price; }
    public String getRoomType() { return roomType; } // Consistent getter for roomType
    public String getDistance() { return distance; }
    public boolean isFavorite() { return isFavorite; }
    public String getDocumentId() { return documentId; }
    public String getUploaderUserId() { return uploaderUserId; }

    // Setters (if needed for specific data manipulation)
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setPrice(String price) { this.price = price; }
    public void setRoomType(String roomType) { this.roomType = roomType; } // Consistent setter for roomType
    public void setDistance(String distance) { this.distance = distance; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public void setUploaderUserId(String uploaderUserId) { this.uploaderUserId = uploaderUserId; }

    // --- Parcelable Implementation ---
    protected NearbyRoom(Parcel in) {
        imageUrl = in.readString();
        price = in.readString();
        roomType = in.readString();
        distance = in.readString();
        isFavorite = in.readByte() != 0;
        documentId = in.readString();
        uploaderUserId = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(imageUrl);
        dest.writeString(price);
        dest.writeString(roomType);
        dest.writeString(distance);
        dest.writeByte((byte) (isFavorite ? 1 : 0));
        dest.writeString(documentId);
        dest.writeString(uploaderUserId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<NearbyRoom> CREATOR = new Creator<NearbyRoom>() {
        @Override
        public NearbyRoom createFromParcel(Parcel in) {
            return new NearbyRoom(in);
        }

        @Override
        public NearbyRoom[] newArray(int size) {
            return new NearbyRoom[size];
        }
    };
}