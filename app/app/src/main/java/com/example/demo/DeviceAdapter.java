package com.example.demo;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private final List<String> deviceList;
    private final OnDeviceRemoveListener onDeviceRemoveListener;

    // Constructor
    public DeviceAdapter(List<String> deviceList, OnDeviceRemoveListener listener) {
        this.deviceList = deviceList;
        this.onDeviceRemoveListener = listener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate layout for each item
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        String deviceName = deviceList.get(position);
        holder.deviceNameTextView.setText(deviceName);
        // Xử lý sự kiện click vào tên thiết bị
        holder.deviceNameTextView.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), gd_thiet_bi.class);
            intent.putExtra("device_name", deviceName); // Truyền tên thiết bị sang Activity mới
            holder.itemView.getContext().startActivity(intent);
        });

        holder.deleteButton.setOnClickListener(v -> {
            // Hiển thị dialog xác nhận
            new AlertDialog.Builder(holder.itemView.getContext())
                    .setTitle("Xác nhận xóa")
                    .setMessage("Bạn có chắc chắn muốn xóa thiết bị này không?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        // Xóa thiết bị
                        onDeviceRemoveListener.onDeviceRemove(position);
                    })
                    .setNegativeButton("Hủy", (dialog, which) -> {
                        dialog.dismiss(); // Đóng dialog nếu hủy
                    })
                    .show();
        });

    }


    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    // ViewHolder class
    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        final TextView deviceNameTextView;
        ImageButton deleteButton; // Đổi từ Button sang ImageButton

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ensure IDs match with `item_device.xml`
            deviceNameTextView = itemView.findViewById(R.id.txt_device_name);
            deleteButton = itemView.findViewById(R.id.btn_delete);
        }
    }

    // Interface for device removal
    public interface OnDeviceRemoveListener {
        void onDeviceRemove(int position);
    }

}
