package com.example.collegeproj;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.Timestamp;

import java.util.List;

public class RoomData implements Parcelable {
    private String title;
    private String roomType;
    private String furnishingStatus;
    private String price;
    private String place;
    private String description;
    private double latitude;
    private double longitude;
    private String userId; // ID of the user who uploaded the room
    private Timestamp uploadTimestamp;
    private String imageUrl; // This field might be redundant if imageUrls is always used
    private List<String> imageUrls; // For multiple image URLs

    // NEW: Add a field for the Firestore document ID of this room
    private String documentId;

    // NEW: Add a transient field to track favorite status for UI purposes
    private transient boolean isFavorite;

    // NEW: Add a transient field to store the timestamp when this room was favorited
    // This is useful for sorting on the favorites screen.
    private transient Timestamp favoriteTimestamp;


    public RoomData() {
        // Public no-argument constructor needed for Firestore
    }

    public RoomData(String title, String roomType, String furnishingStatus, String price, String place, String description, double latitude, double longitude, String userId, Timestamp uploadTimestamp, List<String> imageUrls) {
        this.title = title;
        this.roomType = roomType;
        this.furnishingStatus = furnishingStatus;
        this.price = price;
        this.place = place;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.userId = userId;
        this.uploadTimestamp = uploadTimestamp;
        this.imageUrls = imageUrls;
    }


    // Getters
    public String getTitle() { return title; }
    public String getRoomType() { return roomType; }
    public String getFurnishingStatus() { return furnishingStatus; }
    public String getPrice() { return price; }
    public String getPlace() { return place; }
    public String getDescription() { return description; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getUserId() { return userId; }
    public Timestamp getUploadTimestamp() { return uploadTimestamp; }

    public String getImageUrl() {
        if (imageUrls != null && !imageUrls.isEmpty()) {
            return imageUrls.get(0);
        }
        return null;
    }
    public List<String> getImageUrls() { return imageUrls; }


    // Setters
    public void setTitle(String title) { this.title = title; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public void setFurnishingStatus(String furnishingStatus) { this.furnishingStatus = furnishingStatus; }
    public void setPrice(String price) { this.price = price; }
    public void setPlace(String place) { this.place = place; }
    public void setDescription(String description) { this.description = description; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setUploadTimestamp(Timestamp uploadTimestamp) { this.uploadTimestamp = uploadTimestamp; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    // Getters and Setters for documentId and isFavorite
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

    // NEW: Getter and Setter for favoriteTimestamp
    public Timestamp getFavoriteTimestamp() { return favoriteTimestamp; }
    public void setFavoriteTimestamp(Timestamp favoriteTimestamp) { this.favoriteTimestamp = favoriteTimestamp; }


    // Parcelable implementation
    protected RoomData(Parcel in) {
        title = in.readString();
        roomType = in.readString();
        furnishingStatus = in.readString();
        price = in.readString();
        place = in.readString();
        description = in.readString();
        latitude = in.readDouble();
        longitude = in.readDouble();
        userId = in.readString();
        uploadTimestamp = in.readParcelable(Timestamp.class.getClassLoader());
        imageUrls = in.createStringArrayList();
        documentId = in.readString();
        isFavorite = in.readByte() != 0;
        favoriteTimestamp = in.readParcelable(Timestamp.class.getClassLoader()); // NEW: Read favoriteTimestamp
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(roomType);
        dest.writeString(furnishingStatus);
        dest.writeString(price);
        dest.writeString(place);
        dest.writeString(description);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeString(userId);
        dest.writeParcelable(uploadTimestamp, flags);
        dest.writeStringList(imageUrls);
        dest.writeString(documentId);
        dest.writeByte((byte) (isFavorite ? 1 : 0));
        dest.writeParcelable(favoriteTimestamp, flags); // NEW: Write favoriteTimestamp
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<RoomData> CREATOR = new Creator<RoomData>() {
        @Override
        public RoomData createFromParcel(Parcel in) {
            return new RoomData(in);
        }

        @Override
        public RoomData[] newArray(int size) {
            return new RoomData[size];
        }
    };
}