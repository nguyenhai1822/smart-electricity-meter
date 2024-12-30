package com.example.demo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
    private List<CustomNotification> notificationList;
    private boolean isAdmin;

    public NotificationAdapter(List<CustomNotification> notificationList, boolean isAdmin) {
        this.notificationList = notificationList;
        this.isAdmin = isAdmin;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        CustomNotification notification = notificationList.get(position);
        holder.title.setText(notification.getTitle());
        holder.content.setText(notification.getContent());

        if (isAdmin) {
            // Ẩn nút xác nhận và nhãn "New" đối với tài khoản admin
            holder.newBadge.setVisibility(View.GONE);
            holder.confirmButton.setVisibility(View.GONE);
        } else {
            // Hiển thị nút xác nhận và nhãn "New" nếu không phải admin
            if (notification.isNew()) {
                holder.newBadge.setVisibility(View.VISIBLE);
                holder.confirmButton.setVisibility(View.VISIBLE);
            } else {
                holder.newBadge.setVisibility(View.GONE);
                holder.confirmButton.setVisibility(View.GONE);
            }

            // Xử lý sự kiện nhấn nút xác nhận cho tài khoản không phải admin
            holder.confirmButton.setOnClickListener(v -> {
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                notification.setNew(false); // Cập nhật trạng thái local
                holder.newBadge.setVisibility(View.GONE); // Ẩn nhãn "New"
                holder.confirmButton.setVisibility(View.GONE); // Ẩn nút xác nhận

                // Lưu trạng thái xác nhận cho người dùng hiện tại
                DatabaseReference userNotificationRef = FirebaseDatabase.getInstance()
                        .getReference("notifications")
                        .child(notification.getId())
                        .child("users")
                        .child(currentUserId)
                        .child("isNew");

                userNotificationRef.setValue(false)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(holder.itemView.getContext(), "Đã xác nhận", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(holder.itemView.getContext(), "Lỗi khi xác nhận: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            holder.newBadge.setVisibility(View.VISIBLE); // Hiển thị lại nếu lỗi
                            holder.confirmButton.setVisibility(View.VISIBLE);
                        });
            });
        }
    }



    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView title, content, newBadge;
        Button confirmButton;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvTitle);
            content = itemView.findViewById(R.id.tvContent);
            newBadge = itemView.findViewById(R.id.tvNewBadge);
            confirmButton = itemView.findViewById(R.id.btnConfirm);
        }
    }
}
