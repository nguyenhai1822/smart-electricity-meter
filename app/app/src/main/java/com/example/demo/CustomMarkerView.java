package com.example.demo;

import android.content.Context;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import java.util.Locale;

public class CustomMarkerView extends MarkerView {
    private final TextView tvContent;
    private String viewMode; // Chế độ xem: "Day" hoặc "Month"
    private float offsetX = 0; // Biến tạm để lưu giá trị offset

    public CustomMarkerView(Context context, int layoutResource) {
        super(context, layoutResource);
        tvContent = findViewById(R.id.tvContent); // Tham chiếu đến TextView trong layout
    }

    // Phương thức để thiết lập chế độ xem
    public void setViewMode(String viewMode) {
        this.viewMode = viewMode;
    }

    // Phương thức khởi tạo sau khi Chart đã sẵn sàng
    public void initializeChartView() {
        if (getChartView() == null) {
            // Đảm bảo rằng Chart đã được khởi tạo đúng cách
            return;
        }

        // Đảm bảo rằng phương thức tính toán offset được gọi khi giao diện đã được vẽ
        getChartView().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Xóa listener sau khi đã lấy chiều rộng của biểu đồ
                getChartView().getViewTreeObserver().removeOnGlobalLayoutListener(this);

                // Lấy chiều rộng của biểu đồ
                int chartWidth = getChartView().getWidth();

                // Kiểm tra nếu chiều rộng của biểu đồ chưa có giá trị hợp lệ
                if (chartWidth == 0) {
                    return; // Nếu chiều rộng của biểu đồ chưa được xác định, không làm gì thêm
                }

                // Lấy mảng các đối tượng Highlight
                Highlight[] highlighted = getChartView().getHighlighted();

                // Kiểm tra nếu mảng không rỗng và lấy tọa độ X của điểm được highlight
                if (highlighted != null && highlighted.length > 0) {
                    // Lấy đối tượng Highlight đầu tiên và lấy giá trị X
                    float pointX = highlighted[0].getX(); // Lấy tọa độ X của Highlight đầu tiên

                    // Tính toán offset theo chiều ngang
                    if (pointX > chartWidth * 0.8f) {
                        offsetX = -getWidth(); // Di chuyển marker sang trái
                    } else {
                        offsetX = -(getWidth() / 2f); // Căn giữa marker
                    }

                    // Yêu cầu vẽ lại sau khi tính toán offset
                    invalidate();
                }
            }
        });
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        if ("Day".equals(viewMode)) {
            // Chế độ xem theo Ngày
            String hour = String.format(Locale.getDefault(), "%.0f", e.getX()); // Giả sử e.getX() là giờ
            float power = e.getY(); // e.getY() là điện năng
            tvContent.setText(String.format(Locale.getDefault(), "%s giờ,ĐN là: %.2f kWh", hour, power));
        } else if ("Month".equals(viewMode)) {
            // Chế độ xem theo Tháng
            String day = String.format(Locale.getDefault(), "%.0f", e.getX()); // Giả sử e.getX() là ngày trong tháng
            float totalPower = e.getY(); // e.getY() là tổng điện năng
            tvContent.setText(String.format(Locale.getDefault(), "Ngày %s,TĐN: %.2f kWh", day, totalPower));
        }
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        // Trả về offset đã tính toán trước đó, không thực hiện tính toán tại đây
        return new MPPointF(offsetX, -getHeight());
    }
}
