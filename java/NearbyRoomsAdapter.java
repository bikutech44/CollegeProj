package com.example.collegeproj;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class NearbyRoomsAdapter extends RecyclerView.Adapter<NearbyRoomsAdapter.ViewHolder> {

    private static final String TAG = "NearbyRoomsAdapter";

    private Context context;
    private List<NearbyRoom> nearbyRoomList;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // <<< IMPORTANT: This is the correct place for the public OnItemClickListener interface <<<
    public interface OnItemClickListener {
        void onItemClick(NearbyRoom room);
    }

    private OnItemClickListener listener; // The listener instance

    // Constructor now correctly takes a listener
    public NearbyRoomsAdapter(Context context, List<NearbyRoom> nearbyRoomList, OnItemClickListener listener) {
        this.context = context;
        this.nearbyRoomList = nearbyRoomList;
        this.listener = listener;
        this.mAuth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    // Overloaded constructor for compatibility if listener is not always provided immediately (less common now)
    public NearbyRoomsAdapter(Context context, List<NearbyRoom> nearbyRoomList) {
        this(context, nearbyRoomList, null);
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_nearby_room, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NearbyRoom room = nearbyRoomList.get(position);

        holder.nearbyPriceTextView.setText("Nrs. " + room.getPrice() + "/month");
        holder.nearbyRoomTypeTextView.setText(room.getRoomType()); // Use getRoomType()
        holder.nearbyDistanceTextView.setText(room.getDistance());

        if (room.getImageUrl() != null && !room.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(room.getImageUrl())
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.placeholder_room)
                            .error(R.drawable.placeholder_room)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop())
                    .into(holder.nearbyRoomImageView);
        } else {
            holder.nearbyRoomImageView.setImageResource(R.drawable.placeholder_room);
        }

        // Handle item click for the entire card
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(room);
            }
        });

        // >>>>>>>>>>>>> FAVORITE ICON LOGIC <<<<<<<<<<<<<
        updateFavoriteIcon(holder.favouriteIconNearby, room.isFavorite());

        holder.favouriteIconNearby.setOnClickListener(v -> {
            // IMPORTANT: Request parent to not intercept touch events for this specific tap
            v.getParent().requestDisallowInterceptTouchEvent(true);
            toggleFavoriteStatus(room, holder.favouriteIconNearby);
        });
        // >>>>>>>>>>>>> END FAVORITE ICON LOGIC <<<<<<<<<<<<<
    }

    @Override
    public int getItemCount() {
        return nearbyRoomList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView nearbyRoomImageView;
        TextView nearbyPriceTextView;
        TextView nearbyRoomTypeTextView; // Corrected ID usage
        TextView nearbyDistanceTextView;
        ImageView favouriteIconNearby;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nearbyRoomImageView = itemView.findViewById(R.id.nearbyRoomImageView);
            nearbyPriceTextView = itemView.findViewById(R.id.nearbyPriceTextView);
            nearbyRoomTypeTextView = itemView.findViewById(R.id.nearbyRoomTypeTextView); // Corrected ID usage
            nearbyDistanceTextView = itemView.findViewById(R.id.nearbyDistanceTextView);
            favouriteIconNearby = itemView.findViewById(R.id.favouriteIconNearby);
        }
    }

    // >>>>>>>>>>>>> Favorite Logic Methods <<<<<<<<<<<<<

    private void updateFavoriteIcon(ImageView iconView, boolean isFavorite) {
        if (isFavorite) {
            iconView.setImageResource(R.drawable.ic_heart_filled);
            iconView.setColorFilter(ContextCompat.getColor(context, R.color.black));
        } else {
            iconView.setImageResource(R.drawable.ic_heart_outline);
            iconView.setColorFilter(ContextCompat.getColor(context, android.R.color.black));
        }
    }

    private void toggleFavoriteStatus(NearbyRoom room, ImageView iconView) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(context, "Please log in to add favorites.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        String roomId = room.getDocumentId(); // Use documentId from NearbyRoom
        String uploaderId = room.getUploaderUserId(); // Use uploaderUserId from NearbyRoom

        if (roomId == null || roomId.isEmpty() || uploaderId == null || uploaderId.isEmpty()) {
            Log.e(TAG, "Room ID or Uploader ID is null/empty. Cannot toggle favorite. RoomId: " + roomId + ", UploaderId: " + uploaderId);
            Toast.makeText(context, "Error: Cannot identify room for favoriting.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(userId)
                .collection("favorites").document(roomId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().exists()) {
                            // Room is currently favorited, so unfavorite it
                            db.collection("users").document(userId)
                                    .collection("favorites").document(roomId).delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Nearby room unfavorited: " + roomId);
                                        room.setFavorite(false);
                                        updateFavoriteIcon(iconView, false);
                                        Toast.makeText(context, "Removed from favorites.", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error unfavoriting nearby room: " + e.getMessage());
                                        Toast.makeText(context, "Failed to remove from favorites.", Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            // Room is not favorited, so favorite it
                            Map<String, Object> favoriteData = new HashMap<>();
                            favoriteData.put("timestamp", FieldValue.serverTimestamp());
                            // Store a DocumentReference to the original room in roomsData collection
                            favoriteData.put("roomRef", db.collection("roomsData").document(uploaderId).collection("rooms").document(roomId));

                            db.collection("users").document(userId)
                                    .collection("favorites").document(roomId).set(favoriteData)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Nearby room favorited: " + roomId);
                                        room.setFavorite(true);
                                        updateFavoriteIcon(iconView, true);
                                        Toast.makeText(context, "Added to favorites!", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error favoriting nearby room: " + e.getMessage());
                                        Toast.makeText(context, "Failed to add to favorites.", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        Log.e(TAG, "Error checking favorite status for nearby room: " + task.getException().getMessage());
                        Toast.makeText(context, "Error checking favorite status.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}