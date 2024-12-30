package com.example.demo;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class gd_thiet_bi extends AppCompatActivity {
    private LineChart electricityUsageChart;
    private DatabaseReference databaseReference;
    private Spinner spinnerChartFilter;
    private String deviceName;  // Tên thiết bị
    private TextView tv_nhan_xet;
    private boolean isFirstTime = true; // Biến kiểm soát lần đầu mở giao diện

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gd_tb);

        // Khởi tạo biểu đồ
        electricityUsageChart = findViewById(R.id.electricity_usage_chart);

        // Khởi tạo Spinner
        spinnerChartFilter = findViewById(R.id.spinner_chart_filter);
        tv_nhan_xet = findViewById(R.id.tv_nhan_xet);

        // Lấy ngày hiện tại khi giao diện mở lên
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1; // Tháng bắt đầu từ 0
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Gọi hàm cập nhật dữ liệu ngay khi giao diện được mở
        updateChartsWithFirebaseData(year, month, day);

        // Kết nối Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Lấy tên thiết bị từ Intent
        deviceName = getIntent().getStringExtra("device_name");

        // Cài đặt Spinner
        setupSpinner();
    }
    private int previousSelection = -1;
    private boolean isFirstSelection = true; // Cờ kiểm tra lần đầu

    private void setupSpinner() {
        ArrayList<String> filterOptions = new ArrayList<>();
        filterOptions.add("Xem theo ngày");
        filterOptions.add("Xem theo tháng");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filterOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerChartFilter.setAdapter(adapter);

        // Lấy ngày hiện tại
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Hiển thị dữ liệu ngày hôm nay khi mở giao diện
        spinnerChartFilter.setSelection(0); // Đặt giá trị mặc định là "Xem theo ngày"
        previousSelection = 0; // Lưu lại giá trị mặc định
        updateChartsWithFirebaseData(year, month, day);
        spinnerChartFilter.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    // Khi người dùng bấm vào Spinner
                    showSelectionDialog();
                }
                return true; // Ngăn sự kiện mặc định của Spinner
            }
        });

    }
    private void showSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn chế độ hiển thị");

        String[] options = {"Xem theo ngày", "Xem theo tháng"};
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                showDatePickerDialog(); // Hiển thị DatePickerDialog
            } else if (which == 1) {
                showMonthPickerDialog(); // Hiển thị MonthPickerDialog
            }
            previousSelection = which; // Cập nhật lựa chọn
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.show();
    }



    private void showDatePickerDialog() {
        // Lấy ngày hiện tại
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Hiển thị DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // Cập nhật biểu đồ theo ngày đã chọn
                    updateChartsWithFirebaseData(selectedYear, selectedMonth + 1, selectedDay);

                    // Hiển thị ngày đã chọn trên Spinner
                    String selectedDate = String.format(Locale.getDefault(), "Ngày %d/%d/%d", selectedDay, selectedMonth + 1, selectedYear);
                    updateSpinnerSelection(selectedDate);
                },
                year,
                month,
                day
        );
        datePickerDialog.show();
    }


    private void showMonthPickerDialog() {
        // Lấy năm hiện tại
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);

        // Hiển thị MonthPickerDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.activity_ngay_thang_nam, null);
        builder.setView(dialogView);

        NumberPicker monthPicker = dialogView.findViewById(R.id.month_picker);
        NumberPicker yearPicker = dialogView.findViewById(R.id.year_picker);

        // Cấu hình MonthPicker
        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setValue(calendar.get(Calendar.MONTH) + 1);

        // Cấu hình YearPicker
        yearPicker.setMinValue(2000); // Giới hạn năm tối thiểu
        yearPicker.setMaxValue(year + 10); // Giới hạn năm tối đa
        yearPicker.setValue(year);

        builder.setPositiveButton("Chọn", (dialog, which) -> {
            int selectedMonth = monthPicker.getValue();
            int selectedYear = yearPicker.getValue();

            // Cập nhật biểu đồ theo tháng đã chọn
            updateChartsWithFirebaseData(selectedYear, selectedMonth, null);

            // Hiển thị tháng đã chọn trên Spinner
            String selectedMonthText = String.format(Locale.getDefault(), "Tháng %d/%d", selectedMonth, selectedYear);
            updateSpinnerSelection(selectedMonthText);
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
    private void updateSpinnerSelection(String selectedText) {
        ArrayList<String> newOptions = new ArrayList<>();
        newOptions.add(selectedText); // Hiển thị nội dung được chọn
        newOptions.add("Chọn lại");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, newOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerChartFilter.setAdapter(adapter);

        spinnerChartFilter.setSelection(0); // Đặt lựa chọn đầu tiên là nội dung đã chọn
    }



    private String generateDatabasePath(String deviceName) {
        if (deviceName == null || deviceName.isEmpty()) {
            Log.e("FirebaseData", "Tên thiết bị null hoặc rỗng.");
            return null;
        }

        if (deviceName.startsWith("Phòng ")) {
            String roomNumber = deviceName.replace("Phòng ", "").trim();
            try {
                int number = Integer.parseInt(roomNumber);
                return "pzem_data" + (number == 1 ? "" : number);
            } catch (NumberFormatException e) {
                Log.e("FirebaseData", "Lỗi định dạng số từ tên thiết bị.", e);
            }
        } else {
            Log.e("FirebaseData", "Tên thiết bị không hợp lệ: " + deviceName);
        }
        return null;
    }



    // Trong phương thức updateChartsWithFirebaseData()
    private void updateChartsWithFirebaseData(int year, int month, Integer day) {
        String databasePath = generateDatabasePath(deviceName);
        if (databasePath == null) {
            electricityUsageChart.setNoDataText("Tên thiết bị không hợp lệ hoặc đường dẫn Firebase bị lỗi.");
            return;
        }

        String currentDatePath = databasePath + "/" + year + "/" + month + (day != null ? "/" + day : "");
        boolean isMonthlyView = (day == null);
        String viewMode = isMonthlyView ? "Month" : "Day";

        // Tính số ngày trong tháng linh hoạt
        int daysInMonth;
        if (month == 2) {
            daysInMonth = (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) ? 29 : 28;
        } else if (month == 4 || month == 6 || month == 9 || month == 11) {
            daysInMonth = 30;
        } else {
            daysInMonth = 31;
        }

        Log.d("FirebaseData", "Đường dẫn Firebase: " + currentDatePath);

        databaseReference.child(currentDatePath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    if (!isMonthlyView) { // Chế độ xem ngày
                        ArrayList<Entry> usageEntries = new ArrayList<>();
                        for (DataSnapshot timeSnapshot : dataSnapshot.getChildren()) {
                            try {
                                String timeKey = timeSnapshot.getKey();
                                if (timeKey != null && timeKey.contains(":")) {
                                    int hour = Integer.parseInt(timeKey.split(":")[0]);
                                    Float energy = timeSnapshot.child("energy").getValue(Float.class);
                                    energy = (energy != null) ? energy : 0f;

                                    usageEntries.add(new Entry(hour, energy));
                                }
                            } catch (Exception e) {
                                Log.e("FirebaseData", "Lỗi xử lý dữ liệu tại giờ: " + timeSnapshot.getKey(), e);
                            }
                        }

                        // Tính chênh lệch năng lượng giữa các giờ
                        ArrayList<Entry> hourlyUsageEntries = new ArrayList<>();
                        for (int i = 0; i < usageEntries.size(); i++) {
                            if (i + 1 < usageEntries.size()) {
                                float diff = usageEntries.get(i + 1).getY() - usageEntries.get(i).getY();
                                hourlyUsageEntries.add(new Entry(usageEntries.get(i).getX(), diff));
                            }
                        }

                        // Vẽ biểu đồ ngay lập tức với các giờ đã có dữ liệu
                        setupChart(electricityUsageChart, hourlyUsageEntries, "Điện năng tiêu thụ (kWh)", Color.GREEN, "Giờ", "kWh", 0, 23, viewMode);
                        // Phân tích số liệu từ chênh lệch năng lượng
                        if (!hourlyUsageEntries.isEmpty()) {
                            float maxDiff = Float.MIN_VALUE;
                            float minDiff = Float.MAX_VALUE;
                            int maxDiffHour = -1;
                            int minDiffHour = -1;

                            float totalEnergy = 0f;
                            for (Entry entry : hourlyUsageEntries) {
                                float energy = entry.getY();
                                totalEnergy += energy;

                                if (energy > maxDiff) {
                                    maxDiff = energy;
                                    maxDiffHour = (int) entry.getX();
                                }
                                if (energy < minDiff) {
                                    minDiff = energy;
                                    minDiffHour = (int) entry.getX();
                                }
                            }

                            float diffEnergy = maxDiff - minDiff;
                            float maxDiffPercentage = (maxDiff / totalEnergy) * 100;

                            // Nhận xét dựa trên chênh lệch năng lượng
                            String dailyComment = String.format(
                                    "Giờ tiêu thụ cao nhất: %02d:00 với %.2f kWh (%.1f%% tổng tiêu thụ).\n" +
                                            "Giờ tiêu thụ thấp nhất: %02d:00 với %.2f kWh.\n" +
                                            "Chênh lệch giữa giờ cao nhất và thấp nhất là %.2f kWh.\n" +
                                            "Mức tiêu thụ năng lượng trong ngày có sự %s.",
                                    maxDiffHour, maxDiff, maxDiffPercentage,
                                    minDiffHour, minDiff,
                                    diffEnergy,
                                    (diffEnergy > (0.3 * totalEnergy) ? "chênh lệch lớn" : "phân bổ đồng đều")
                            );
                            tv_nhan_xet.setText(dailyComment);
                        } else {
                            tv_nhan_xet.setText("Không có đủ dữ liệu để phân tích.");
                        }

                    } else { // Chế độ xem tháng
                        ArrayList<Entry> monthlyUsageEntries = new ArrayList<>();
                        final int[] completedDays = {0}; // Biến đếm số ngày đã hoàn thành

                        for (int dayOfMonth = 1; dayOfMonth <= daysInMonth; dayOfMonth++) {
                            String dayPath = currentDatePath + "/" + dayOfMonth;
                            int finalDayOfMonth = dayOfMonth;

                            final Float[] firstHourEnergyWrapper = {null};
                            final Float[] lastHourEnergyWrapper = {null};

                            databaseReference.child(dayPath).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot daySnapshot) {
                                    if (daySnapshot.exists()) {
                                        for (DataSnapshot timeSnapshot : daySnapshot.getChildren()) {
                                            try {
                                                String timeKey = timeSnapshot.getKey();
                                                if (timeKey != null && timeKey.contains(":")) {
                                                    Float energy = timeSnapshot.child("energy").getValue(Float.class);
                                                    energy = (energy != null) ? energy : 0f;

                                                    if (firstHourEnergyWrapper[0] == null) {
                                                        firstHourEnergyWrapper[0] = energy;
                                                    }
                                                    lastHourEnergyWrapper[0] = energy;
                                                }
                                            } catch (Exception e) {
                                                Log.e("FirebaseData", "Lỗi xử lý dữ liệu tại giờ: " + timeSnapshot.getKey(), e);
                                            }
                                        }
                                    }

                                    String nextDayPath = currentDatePath + "/" + (finalDayOfMonth + 1);
                                    databaseReference.child(nextDayPath).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot nextDaySnapshot) {
                                            final Float[] nextDayFirstHourEnergyWrapper = {null};

                                            if (nextDaySnapshot.exists()) {
                                                for (DataSnapshot timeSnapshot : nextDaySnapshot.getChildren()) {
                                                    String timeKey = timeSnapshot.getKey();
                                                    if (timeKey != null && timeKey.contains(":")) {
                                                        nextDayFirstHourEnergyWrapper[0] = timeSnapshot.child("energy").getValue(Float.class);
                                                        nextDayFirstHourEnergyWrapper[0] = (nextDayFirstHourEnergyWrapper[0] != null) ? nextDayFirstHourEnergyWrapper[0] : 0f;
                                                        break;
                                                    }
                                                }
                                            }

                                            float dailyTotalEnergy = 0f;
                                            if (nextDayFirstHourEnergyWrapper[0] != null && firstHourEnergyWrapper[0] != null) {
                                                dailyTotalEnergy = nextDayFirstHourEnergyWrapper[0] - firstHourEnergyWrapper[0];
                                            } else if (lastHourEnergyWrapper[0] != null && firstHourEnergyWrapper[0] != null) {
                                                dailyTotalEnergy = lastHourEnergyWrapper[0] - firstHourEnergyWrapper[0];
                                            }

                                            monthlyUsageEntries.add(new Entry(finalDayOfMonth, dailyTotalEnergy));

                                            completedDays[0]++;
                                            if (completedDays[0] == daysInMonth) {
                                                setupChart(electricityUsageChart, monthlyUsageEntries, "Điện năng tiêu thụ (kWh)", Color.GREEN, "Ngày", "kWh", 1, daysInMonth, viewMode);
                                                // Phân tích dữ liệu và nhận xét
                                                float maxEnergy = Float.MIN_VALUE;
                                                float minEnergy = Float.MAX_VALUE;
                                                int maxDay = -1;
                                                int minDay = -1;
                                                float totalEnergy = 0f;

                                                for (Entry entry : monthlyUsageEntries) {
                                                    float energy = entry.getY();
                                                    int day = (int) entry.getX();

                                                    totalEnergy += energy;

                                                    if (energy > maxEnergy) {
                                                        maxEnergy = energy;
                                                        maxDay = day;
                                                    }
                                                    if (energy < minEnergy) {
                                                        minEnergy = energy;
                                                        minDay = day;
                                                    }
                                                }

                                                float diffEnergy = maxEnergy - minEnergy;
                                                float maxEnergyPercentage = (maxEnergy / totalEnergy) * 100;

                                                // Nhận xét
                                                String monthlyComment = String.format(
                                                        "Ngày tiêu thụ cao nhất: %02d với %.2f kWh (%.1f%% tổng tiêu thụ).\n" +
                                                                "Ngày tiêu thụ thấp nhất: %02d với %.2f kWh.\n" +
                                                                "Chênh lệch giữa ngày cao nhất và thấp nhất là %.2f kWh.\n" +
                                                                "Mức tiêu thụ năng lượng trong tháng có sự %s.",
                                                        maxDay, maxEnergy, maxEnergyPercentage,
                                                        minDay, minEnergy,
                                                        diffEnergy,
                                                        (diffEnergy > (0.3 * totalEnergy) ? "chênh lệch lớn" : "phân bổ đồng đều")
                                                );
                                                tv_nhan_xet.setText(monthlyComment);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Log.e("FirebaseData", "Lỗi Firebase: " + error.getMessage(), error.toException());
                                        }
                                    });
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                    Log.e("FirebaseData", "Lỗi Firebase: " + error.getMessage(), error.toException());
                                }
                            });
                        }
                    }
                } else {
                    ArrayList<Entry> defaultEntries = new ArrayList<>();
                    int maxRange = isMonthlyView ? daysInMonth : 23;
                    for (int i = 0; i <= maxRange; i++) {
                        defaultEntries.add(new Entry(i, 0f));
                    }
                    setupChart(electricityUsageChart, defaultEntries, "Không có dữ liệu", Color.RED, "Ngày", "kWh", 1, maxRange, viewMode);
                    electricityUsageChart.setNoDataText("Không có dữ liệu cho thời gian này.");
                    Log.w("FirebaseData", "Không có dữ liệu tại đường dẫn: " + currentDatePath);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                electricityUsageChart.setNoDataText("Lỗi tải dữ liệu.");
                Log.e("FirebaseData", "Lỗi Firebase: " + error.getMessage(), error.toException());
            }
        });
    }






    private void setupChart(LineChart chart, ArrayList<Entry> entries, String label, int lineColor, String valueType, String unit, float xMin, float xMax, String viewMode) {
        LineDataSet dataSet = new LineDataSet(entries, label);

        // Tắt các điểm chấm và sử dụng đường mềm mại
        dataSet.setDrawCircles(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        // Tăng độ rộng của đường biểu đồ
        dataSet.setLineWidth(3f);

        // Tạo gradient fill với màu riêng
        dataSet.setDrawFilled(true);
        GradientDrawable gradientDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {
                        Color.argb(50, Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor)),
                        Color.argb(0, Color.red(lineColor), Color.green(lineColor), Color.blue(lineColor))
                }
        );
        dataSet.setFillDrawable(gradientDrawable);

        dataSet.setColor(lineColor);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setDrawValues(false);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        chart.getDescription().setEnabled(false);
        chart.getAxisRight().setEnabled(false);

        // Cấu hình trục X
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAxisMinimum(xMin);
        xAxis.setAxisMaximum(xMax);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false); // Tắt đường lưới trên trục X
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.0f", value);
            }
        });

        // Cấu hình trục Y
        YAxis yAxis = chart.getAxisLeft();
        yAxis.setAxisMinimum(0f);
        yAxis.setGranularity(1f);
        yAxis.setDrawGridLines(false); // Tắt đường lưới trên trục Y

        // Thêm MarkerView
        CustomMarkerView markerView = new CustomMarkerView(chart.getContext(), R.layout.custom_marker_view);
        markerView.setViewMode(viewMode); // Truyền chế độ xem cho MarkerView
        chart.setMarker(markerView);
        // Cấu hình cảm ứng và highlight
        chart.setTouchEnabled(true);
        chart.setHighlightPerTapEnabled(true); // Chỉ highlight khi chạm chính xác
        chart.setHighlightPerDragEnabled(false);
        chart.setDrawMarkers(true);

        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);

        chart.invalidate();
    }
}