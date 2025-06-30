package com.example.collegeproj;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import androidx.core.content.ContextCompat;



public class ChatListAdaptor extends RecyclerView.Adapter<ChatListAdaptor.ViewHolder> {

    private static final String TAG = "ChatListAdaptor";
    private Context context;
    private List<ChatListItem> chatListItems;
    private OnItemClickListener listener;

    // Interface for item click listener
    public interface OnItemClickListener {
        void onItemClick(ChatListItem chatListItem);
    }

    public ChatListAdaptor(Context context, List<ChatListItem> chatListItems, OnItemClickListener listener) {
        this.context = context;
        this.chatListItems = chatListItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatListItem item = chatListItems.get(position);

        // Set other user's name
        holder.chatUserNameTextView.setText(item.getOtherUserName());

        // Set last message
        holder.chatLastMessageTextView.setText(item.getLastMessageText());

        // Set timestamp (formatted nicely)
        if (item.getLastMessageTimestamp() != null) {
            holder.chatTimestampTextView.setText(formatTimestamp(item.getLastMessageTimestamp()));
        } else {
            holder.chatTimestampTextView.setText(""); // No timestamp available
        }

        // Load profile image
        if (item.getOtherUserProfileImageUrl() != null && !item.getOtherUserProfileImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(item.getOtherUserProfileImageUrl())
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.profile_icon) // Default placeholder
                            .error(R.drawable.profile_icon)      // Error placeholder
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop())
                    .into(holder.chatProfileImageView);
        } else {
            // Set default profile icon if no URL or it's empty
            holder.chatProfileImageView.setImageResource(R.drawable.profile_icon);
            holder.chatProfileImageView.setColorFilter(ContextCompat.getColor(context, R.color.dark_bg));
        }

        // Set click listener for the entire item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatListItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView chatProfileImageView;
        TextView chatUserNameTextView;
        TextView chatLastMessageTextView;
        TextView chatTimestampTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            chatProfileImageView = itemView.findViewById(R.id.chatProfileImageView);
            chatUserNameTextView = itemView.findViewById(R.id.chatUserNameTextView);
            chatLastMessageTextView = itemView.findViewById(R.id.chatLastMessageTextView);
            chatTimestampTextView = itemView.findViewById(R.id.chatTimestampTextView);
        }
    }

    // Helper method to format timestamp
    private String formatTimestamp(Timestamp timestamp) {
        Date messageDate = timestamp.toDate();
        Date now = new Date();

        long diff = now.getTime() - messageDate.getTime();
        long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        long days = TimeUnit.MILLISECONDS.toDays(diff);

        if (seconds < 60) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + " min ago";
        } else if (hours < 24) {
            return hours + " hr ago";
        } else if (days == 1) {
            return "Yesterday";
        } else if (days < 7) {
            return days + " days ago";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            return sdf.format(messageDate);
        }
    }
}
