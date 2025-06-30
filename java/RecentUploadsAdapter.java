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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class RecentUploadsAdapter extends RecyclerView.Adapter<RecentUploadsAdapter.ViewHolder> {

    private static final String TAG = "RecentUploadsAdapter";

    private Context context;
    private List<RoomData> roomList;
    private OnItemClickListener listener;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public RecentUploadsAdapter(Context context, List<RoomData> roomList, OnItemClickListener listener) {
        this.context = context;
        this.roomList = roomList;
        this.listener = listener;
        this.mAuth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    public interface OnItemClickListener {
        void onItemClick(RoomData room);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recent_upload, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RoomData room = roomList.get(position);

        holder.titleTextView.setText(room.getTitle());
        holder.placeTextView.setText(room.getPlace());
        holder.priceTextView.setText("Nrs. " + room.getPrice() + "/month");

        if (room.getUploadTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy", Locale.getDefault());
            holder.timestampTextView.setText(sdf.format(room.getUploadTimestamp().toDate()));
        } else {
            holder.timestampTextView.setText("");
        }

        if (room.getImageUrls() != null && !room.getImageUrls().isEmpty()) {
            Glide.with(context)
                    .load(room.getImageUrls().get(0))
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.placeholder_room)
                            .error(R.drawable.placeholder_room)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop())
                    .into(holder.roomImageView);
        } else {
            holder.roomImageView.setImageResource(R.drawable.placeholder_room);
        }

        // Handle item click for the entire card (excluding favorite icon click)
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(room);
            }
        });

        // >>>>>>>>>>>>> FAVORITE ICON LOGIC <<<<<<<<<<<<<
        updateFavoriteIcon(holder.favouriteIcon, room.isFavorite());

        holder.favouriteIcon.setOnClickListener(v -> {
            // IMPORTANT: Request parent to not intercept touch events for this specific tap
            // This ensures the icon's click listener gets the event first and processes it fully.
            v.getParent().requestDisallowInterceptTouchEvent(true);

            // Toggle favorite status in Firestore and update UI
            toggleFavoriteStatus(room, holder.favouriteIcon);

            // Return true to consume the click event, preventing it from propagating to parent views (like the CardView)
            // This is key to solving the double-tap issue.
            // Note: The lambda is within setOnClickListener, so we don't need 'return true' at the end of the lambda itself.
            // However, the `requestDisallowInterceptTouchEvent` handles the immediate prevention.
        });
        // >>>>>>>>>>>>> END FAVORITE ICON LOGIC <<<<<<<<<<<<<
    }

    @Override
    public int getItemCount() {
        return roomList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView roomImageView;
        TextView titleTextView;
        TextView placeTextView;
        TextView priceTextView;
        TextView timestampTextView;
        ImageView favouriteIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            roomImageView = itemView.findViewById(R.id.roomImageView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            placeTextView = itemView.findViewById(R.id.placeTextView);
            priceTextView = itemView.findViewById(R.id.priceTextView);
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
            favouriteIcon = itemView.findViewById(R.id.favouriteIcon);
        }
    }

    private void updateFavoriteIcon(ImageView iconView, boolean isFavorite) {
        if (isFavorite) {
            iconView.setImageResource(R.drawable.ic_heart_filled);
            iconView.setColorFilter(ContextCompat.getColor(context, R.color.black));
        } else {
            iconView.setImageResource(R.drawable.ic_heart_outline);
            iconView.setColorFilter(ContextCompat.getColor(context, android.R.color.black));
        }
    }

    private void toggleFavoriteStatus(RoomData room, ImageView iconView) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(context, "Please log in to add favorites.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        String roomId = room.getDocumentId();
        if (roomId == null || roomId.isEmpty()) {
            Log.e(TAG, "Room ID is null or empty. Cannot toggle favorite status.");
            Toast.makeText(context, "Error: Cannot identify room.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(userId)
                .collection("favorites").document(roomId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().exists()) {
                            db.collection("users").document(userId)
                                    .collection("favorites").document(roomId).delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Room unfavorited: " + roomId);
                                        room.setFavorite(false);
                                        updateFavoriteIcon(iconView, false);
                                        Toast.makeText(context, "Removed from favorites.", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error unfavoriting room: " + e.getMessage());
                                        Toast.makeText(context, "Failed to remove from favorites.", Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Map<String, Object> favoriteData = new HashMap<>();
                            favoriteData.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
                            favoriteData.put("roomRef", db.collection("roomsData").document(room.getUserId()).collection("rooms").document(roomId));

                            db.collection("users").document(userId)
                                    .collection("favorites").document(roomId).set(favoriteData)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Room favorited: " + roomId);
                                        room.setFavorite(true);
                                        updateFavoriteIcon(iconView, true);
                                        Toast.makeText(context, "Added to favorites!", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error favoriting room: " + e.getMessage());
                                        Toast.makeText(context, "Failed to add to favorites.", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        Log.e(TAG, "Error checking favorite status: " + task.getException().getMessage());
//                        Toast.makeText(context, "Error checking favorite status.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}