package com.example.collegeproj; // <<< IMPORTANT: Ensure this package matches your project's package

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

public class MyUploadsAdapter extends RecyclerView.Adapter<MyUploadsAdapter.ViewHolder> {

    private static final String TAG = "MyUploadsAdapter";

    private Context context;
    private List<RoomData> myUploadsList;
    private OnItemActionListener listener;

    public interface OnItemActionListener {
        void onItemClick(RoomData room);
        void onEditClick(RoomData room);
        void onDeleteClick(RoomData room);
    }

    public MyUploadsAdapter(Context context, List<RoomData> myUploadsList, OnItemActionListener listener) {
        this.context = context;
        this.myUploadsList = myUploadsList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_my_upload, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RoomData room = myUploadsList.get(position);

        holder.uploadTitleTextView.setText(room.getTitle());
        holder.uploadDescriptionTextView.setText(room.getDescription());

        // Set Price and Location
        // Ensure that room.getPrice() and room.getPlace() return non-null values
        // You might want to add null checks or default values if they can be empty in Firestore
        holder.uploadPriceTextView.setText("Nrs. " + (room.getPrice() != null ? room.getPrice() : "N/A") + "/month");
        holder.uploadLocationTextView.setText(room.getPlace() != null ? room.getPlace() : "Unknown Location");


        if (room.getImageUrls() != null && !room.getImageUrls().isEmpty()) {
            Glide.with(context)
                    .load(room.getImageUrls().get(0))
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.placeholder_image)
                            .error(R.drawable.placeholder_image)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop())
                    .into(holder.uploadImageView);
        } else {
            holder.uploadImageView.setImageResource(R.drawable.placeholder_image);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(room);
            }
        });

        holder.editIcon.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditClick(room);
            }
        });

        holder.deleteIcon.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(room);
            }
        });
    }

    @Override
    public int getItemCount() {
        return myUploadsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView uploadImageView;
        TextView uploadTitleTextView;
        TextView uploadDescriptionTextView;
        TextView uploadPriceTextView;
        TextView uploadLocationTextView;
        ImageView editIcon;
        ImageView deleteIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            uploadImageView = itemView.findViewById(R.id.uploadImageView);
            uploadTitleTextView = itemView.findViewById(R.id.uploadTitleTextView);
            uploadDescriptionTextView = itemView.findViewById(R.id.uploadDescriptionTextView);
            uploadPriceTextView = itemView.findViewById(R.id.uploadPriceTextView);
            uploadLocationTextView = itemView.findViewById(R.id.uploadLocationTextView);
            editIcon = itemView.findViewById(R.id.editIcon);
            deleteIcon = itemView.findViewById(R.id.deleteIcon);
        }
    }
}
