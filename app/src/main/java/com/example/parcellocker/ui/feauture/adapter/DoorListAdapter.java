package com.example.parcellocker.ui.feauture.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.parcellocker.R;
import com.example.parcellocker.cu16.CU16Service;
import com.example.parcellocker.db.DoorEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class DoorListAdapter extends ListAdapter<DoorEntity, DoorListAdapter.DoorViewHolder> {

    private final CU16Service cu16Service;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public DoorListAdapter(CU16Service cu16Service) {
        super(DIFF_CALLBACK);
        this.cu16Service = cu16Service;
    }

    private static final DiffUtil.ItemCallback<DoorEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<DoorEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull DoorEntity oldItem, @NonNull DoorEntity newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull DoorEntity oldItem, @NonNull DoorEntity newItem) {
                    return oldItem.locked == newItem.locked &&
                            oldItem.occupied == newItem.occupied;
                }
            };

    @NonNull
    @Override
    public DoorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_door, parent, false);
        return new DoorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DoorViewHolder holder, int position) {
        DoorEntity door = getItem(position);
        holder.bind(door);

        // Add click listener to unlock the door
        holder.itemView.setOnClickListener(v -> {
            executor.submit(() -> {
                try {
                    cu16Service.unlockDoorAsync(door.doorIndex).get();
                    holder.itemView.post(() ->
                            Toast.makeText(holder.itemView.getContext(),
                                    "Door " + (door.doorIndex + 1) + " unlocked",
                                    Toast.LENGTH_SHORT).show()
                    );
                } catch (Exception e) {
                    holder.itemView.post(() ->
                            Toast.makeText(holder.itemView.getContext(),
                                    "Error unlocking door: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show()
                    );
                }
            });
        });
    }

    static class DoorViewHolder extends RecyclerView.ViewHolder {
        private final TextView doorName;
        private final TextView doorStatus;

        public DoorViewHolder(@NonNull View itemView) {
            super(itemView);
            doorName = itemView.findViewById(R.id.door_name);
            doorStatus = itemView.findViewById(R.id.door_status);
        }

        public void bind(DoorEntity door) {
            doorName.setText("Door " + (door.doorIndex + 1));
            doorStatus.setText(door.locked == 1 ? "Locked" : "Unlocked");
        }
    }
}