package com.example.demo;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;


public class Bill_Activity extends AppCompatActivity {
    private DatabaseReference databaseReference;

    private static final String SELECTED_ITEM = "selected_item";
    private int selectedItemId;
    private Spinner spHouseholds, spDayStart, spDayBegin;
    private TextView tvGdBillHd,tvHouseholds, tvTotalEnergy , tvtotalPayment, tvtienchuathue , tvtienthue;
    private String selectedRoom;
    // Biến toàn cục để theo dõi số phòng đã xử lý
    private float[] totalEnergyForRooms = new float[2]; // Giả sử bạn có 2 phòng
    private int MAX_RETRIES ;
    private float energyConsumed;
    private int deviceCount = 0;
    private boolean isAdmin = false;
    private List<String> adminEmails = Arrays.asList(
            "nguyenhai01082002@gmail.com", // Email admin
            "admin2@example.com"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gd_bill);
        spDayStart = findViewById(R.id.sp_day_start);
        spDayBegin = findViewById(R.id.sp_day_begin);
        spHouseholds = findViewById(R.id.sp_households);
        tvTotalEnergy = findViewById(R.id.totalConsumption);
        tvHouseholds = findViewById(R.id.households);
        tvtotalPayment = findViewById(R.id.totalPayment);
        tvtienchuathue = findViewById(R.id.totalWithoutTax);
        tvtienthue = findViewById(R.id.vatTax);
        tvGdBillHd = findViewById(R.id.tv_gd_bill_hd);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        // Kết nối Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();
        if (databaseReference == null) {
            Log.e("FirebaseError", "Database reference is null.");
            return;
        }
        if (savedInstanceState != null) {
            selectedItemId = savedInstanceState.getInt(SELECTED_ITEM, R.id.ng_bill);
        } else {
            selectedItemId = R.id.ng_bill;
        }
        tvGdBillHd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Chuyển sang Activity mới
                Intent intent = new Intent(Bill_Activity.this, Calculation_Activity.class);
                startActivity(intent);
            }
        });
        bottomNavigationView.setSelectedItemId(selectedItemId);
        setupBottomNavigationView(bottomNavigationView);
        // Setup Spinners with static data
        setupHouseholdsSpinner();
        setupSpinner(spDayStart);
        setupSpinner(spDayBegin);
        // Lấy ngày bắt đầu và ngày kết thúc từ các spinner
        String startDateStr = (String) spDayStart.getSelectedItem();
        String endDateStr = (String) spDayBegin.getSelectedItem();
        // Kiểm tra xem ngày bắt đầu và ngày kết thúc đã được chọn chưa
        if (startDateStr.equals("Chọn ngày...") || endDateStr.equals("Chọn ngày...")) {
            Toast.makeText(Bill_Activity.this, "Vui lòng chọn ngày bắt đầu và ngày kết thúc.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Tạo Calendar từ ngày bắt đầu và ngày kết thúc
        Calendar startDate = Calendar.getInstance();
        Calendar endDate = Calendar.getInstance();
        // Chuyển đổi chuỗi ngày thành Calendar (giả sử định dạng là "dd/MM/yyyy")
        String[] startDateParts = startDateStr.split("/");
        startDate.set(Integer.parseInt(startDateParts[2]), Integer.parseInt(startDateParts[1]) - 1, Integer.parseInt(startDateParts[0]));
        String[] endDateParts = endDateStr.split("/");
        endDate.set(Integer.parseInt(endDateParts[2]), Integer.parseInt(endDateParts[1]) - 1, Integer.parseInt(endDateParts[0]));
        // Gọi phương thức với đủ ba tham số
        calculateEnergyForFirstAndLastHourWithData(selectedRoom, startDate, endDate, new DataCallback() {
            @Override
            public void onDataReceived(Float firstHourEnergy) {

            }
        });
        // TextView navigation setup
        Button tvGdBillHd = findViewById(R.id.tv_gd_bill_hd);
        tvGdBillHd.setOnClickListener(view -> {
            Intent intent = new Intent(Bill_Activity.this, Calculation_Activity.class);
            startActivity(intent);
        });
        tvTotalEnergy.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Gọi hàm kiểm tra giá trị của TextView sau mỗi thay đổi giao diện
                String totalEnergyStr = tvTotalEnergy.getText().toString().trim();

                if (!totalEnergyStr.isEmpty()) {
                    totalEnergyStr = totalEnergyStr.replace(",", ".");
                    try {
                        double soDien = Double.parseDouble(totalEnergyStr);
                        // Gọi phương thức tính tiền điện hoặc cập nhật UI
                        tinhTienDien(soDien);
                    } catch (NumberFormatException e) {
                        // Xử lý khi chuỗi không thể chuyển đổi thành số
                        Log.e("Error", "Không thể chuyển đổi chuỗi thành số: " + totalEnergyStr);
                        Toast.makeText(Bill_Activity.this, "Giá trị không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Xử lý nếu chuỗi trống
                    Toast.makeText(Bill_Activity.this, "Vui lòng nhập số điện", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void setupBottomNavigationView(BottomNavigationView bottomNavigationView) {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (selectedItemId != itemId) {
                selectedItemId = itemId;

                Intent intent = null;
                if (itemId == R.id.ng_home) {
                    intent = new Intent(Bill_Activity.this, MainActivity.class);
                } else if (itemId == R.id.ng_news) {
                    intent = new Intent(Bill_Activity.this, Notification_Activity.class);
                } else if (itemId == R.id.ng_profile) {
                    intent = new Intent(Bill_Activity.this, UserActivity.class);
                }

                if (intent != null) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    finish(); // Đảm bảo Activity hiện tại được kết thúc
                    finish();
                }
            }
            return true;
        });
        // Đặt mục được chọn cho Bill_Activity
        bottomNavigationView.setSelectedItemId(R.id.ng_bill);
    }

    private void setupHouseholdsSpinner() {
        ArrayList<String> householdsList = new ArrayList<>();
        householdsList.add("Chọn phòng...");
        // Kiểm tra quyền admin
        String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        if (currentUserEmail != null && adminEmails.contains(currentUserEmail)) {
            isAdmin = true;
            householdsList.add("Chọn tất cả"); // Chỉ thêm "Chọn tất cả" nếu là admin
        } else {
            isAdmin = false;
        }


        // Initial setup of the adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, householdsList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spHouseholds.setAdapter(adapter);

        // Get the current user's UID
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("devices");

        // Fetch data from Firebase
        fetchHouseholdsData(userId, dbRef, householdsList, adapter);

        spHouseholds.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                String selectedRoom = parent.getItemAtPosition(position).toString();

                if (selectedRoom.equals("Chọn tất cả")) {
                    // Khi chọn "Chọn tất cả", tính tổng năng lượng cho tất cả các thiết bị
                    String startDateStr = (String) spDayStart.getSelectedItem();
                    String endDateStr = (String) spDayBegin.getSelectedItem();

                    if (startDateStr.equals("Chọn ngày...") || endDateStr.equals("Chọn ngày...")) {
                        Toast.makeText(Bill_Activity.this, "Vui lòng chọn ngày bắt đầu và ngày kết thúc.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Tạo Calendar từ ngày bắt đầu và ngày kết thúc
                    Calendar startDate = Calendar.getInstance();
                    Calendar endDate = Calendar.getInstance();

                    String[] startDateParts = startDateStr.split("/");
                    startDate.set(Integer.parseInt(startDateParts[2]), Integer.parseInt(startDateParts[1]) - 1, Integer.parseInt(startDateParts[0]));

                    String[] endDateParts = endDateStr.split("/");
                    endDate.set(Integer.parseInt(endDateParts[2]), Integer.parseInt(endDateParts[1]) - 1, Integer.parseInt(endDateParts[0]));

                    // Gọi phương thức tính tổng năng lượng cho tất cả các thiết bị
                    calculateEnergyForAllRooms(startDate, endDate);

                    // Cập nhật tổng năng lượng và tính tiền điện
                    updateTotalEnergyAndBill();

                    // Cập nhật TextView số phòng
                    tvHouseholds.setText("Tất cả thiết bị");

                } else if (!selectedRoom.equals("Chọn phòng...")) {
                    tvHouseholds.setText(selectedRoom);
                    Toast.makeText(Bill_Activity.this, "Đã chọn: " + selectedRoom, Toast.LENGTH_SHORT).show();

                    // Lấy ngày bắt đầu và ngày kết thúc từ các spinner
                    String startDateStr = (String) spDayStart.getSelectedItem();
                    String endDateStr = (String) spDayBegin.getSelectedItem();

                    // Kiểm tra xem ngày bắt đầu và kết thúc đã được chọn chưa
                    if (startDateStr.equals("Chọn ngày...") || endDateStr.equals("Chọn ngày...")) {
                        Toast.makeText(Bill_Activity.this, "Vui lòng chọn ngày bắt đầu và ngày kết thúc.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Tạo Calendar từ ngày bắt đầu và ngày kết thúc
                    Calendar startDate = Calendar.getInstance();
                    Calendar endDate = Calendar.getInstance();

                    String[] startDateParts = startDateStr.split("/");
                    startDate.set(Integer.parseInt(startDateParts[2]), Integer.parseInt(startDateParts[1]) - 1, Integer.parseInt(startDateParts[0]));

                    String[] endDateParts = endDateStr.split("/");
                    endDate.set(Integer.parseInt(endDateParts[2]), Integer.parseInt(endDateParts[1]) - 1, Integer.parseInt(endDateParts[0]));

                    // Gọi phương thức với đủ ba tham số
                    calculateEnergyForFirstAndLastHourWithData(selectedRoom, startDate, endDate, new DataCallback() {
                        @Override
                        public void onDataReceived(Float firstHourEnergy) {
                            // Xử lý dữ liệu khi nhận được
                        }
                    });

                    // Cập nhật tổng năng lượng và tính tiền điện
                    updateTotalEnergyAndBill();
                } else {
                    tvHouseholds.setText("0");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action needed when nothing is selected
            }
        });
    }

    private void fetchHouseholdsData(String userId, DatabaseReference dbRef, ArrayList<String> householdsList, ArrayAdapter<String> adapter) {
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                householdsList.clear();
                householdsList.add("Chọn phòng...");
                // Kiểm tra quyền admin
                String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                if (currentUserEmail != null && adminEmails.contains(currentUserEmail)) {
                    isAdmin = true;
                    householdsList.add("Chọn tất cả"); // Chỉ thêm "Chọn tất cả" nếu là admin
                } else {
                    isAdmin = false;
                }


                deviceCount = 0;  // Reset count before adding rooms

                for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                    String ownerId = roomSnapshot.child("owner").getValue(String.class);
                    String roomName = roomSnapshot.child("name").getValue(String.class);

                    if (ownerId != null && ownerId.equals(userId) && roomName != null) {
                        roomName = roomName.replace("(ad)", "").trim();
                        householdsList.add(roomName);
                        deviceCount++;
                    }
                }

                adapter.notifyDataSetChanged();
                tvHouseholds.setText(String.format("%d phòng", deviceCount));  // Update device count in UI
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Bill_Activity.this, "Lỗi khi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void updateTotalEnergyAndBill() {
        // Lấy giá trị từ TextView
        String totalEnergyStr = tvTotalEnergy.getText().toString().trim();

        if (!totalEnergyStr.isEmpty()) {
            // Thay dấu phẩy thành dấu chấm nếu có (nếu giá trị nhập vào có dấu phẩy)
            totalEnergyStr = totalEnergyStr.replace(",", ".");

            try {
                // Chuyển đổi chuỗi thành double
                double soDien = Double.parseDouble(totalEnergyStr);
                // Gọi phương thức tính tiền điện
                tinhTienDien(soDien);
            } catch (NumberFormatException e) {
                // Xử lý lỗi nếu chuỗi không thể chuyển đổi thành số
                Log.e("Error", "Không thể chuyển đổi chuỗi thành số: " + totalEnergyStr);
            }
        }
    }

    private void setupSpinner(Spinner spinner) {
        ArrayList<String> defaultList = new ArrayList<>();
        defaultList.add("Chọn ngày...");
        ArrayAdapter<String> defaultAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, defaultList);
        defaultAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(defaultAdapter);

        spinner.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                showDatePickerDialog(spinner);
            }
            return true;
        });
    }

    private void showDatePickerDialog(Spinner spinner) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedDateCalendar = Calendar.getInstance();
                    selectedDateCalendar.set(selectedYear, selectedMonth, selectedDay);

                    Calendar currentDate = Calendar.getInstance();
                    String selectedDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;

                    if (spinner.getId() == R.id.sp_day_begin) {
                        if (selectedDateCalendar.after(currentDate)) {
                            Toast.makeText(this, "Ngày bắt đầu không được vượt quá ngày hiện tại!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } else if (spinner.getId() == R.id.sp_day_start) {
                        String beginDateStr = (String) spDayBegin.getSelectedItem();
                        if (!beginDateStr.equals("Chọn ngày...")) {
                            String[] beginDateParts = beginDateStr.split("/");
                            int beginDay = Integer.parseInt(beginDateParts[0]);
                            int beginMonth = Integer.parseInt(beginDateParts[1]) - 1;
                            int beginYear = Integer.parseInt(beginDateParts[2]);

                            Calendar beginDateCalendar = Calendar.getInstance();
                            beginDateCalendar.set(beginYear, beginMonth, beginDay);

                            if (!selectedDateCalendar.before(beginDateCalendar)) {
                                Toast.makeText(this, "Ngày bắt đầu phải nhỏ hơn ngày trong sp_day_begin!", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        } else {
                            Toast.makeText(this, "Vui lòng chọn ngày bắt đầu (sp_day_begin) trước!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    ArrayList<String> list = new ArrayList<>();
                    list.add(selectedDate);
                    ArrayAdapter<String> dateAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list);
                    dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner.setAdapter(dateAdapter);
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    private void calculateEnergyForFirstAndLastHourWithData(String selectedRoom, Calendar startDate, Calendar endDate, DataCallback callback) {
        // Kiểm tra ngày tháng hợp lệ
        Calendar today = Calendar.getInstance();
        if (startDate.after(today) || endDate.after(today)) {
            tvTotalEnergy.setText("Ngày bắt đầu hoặc kết thúc không được vượt quá ngày hiện tại.");
            return;
        }
        if (startDate.after(endDate)) {
            tvTotalEnergy.setText("Ngày bắt đầu phải nhỏ hơn hoặc bằng ngày kết thúc.");
            return;
        }
        // Tính khoảng cách ngày giữa startDate và endDate
        MAX_RETRIES = (int) ((endDate.getTimeInMillis() - startDate.getTimeInMillis()) / (24 * 60 * 60 * 1000));

        MAX_RETRIES = Math.max(MAX_RETRIES, 1);
        Log.d("EnergyCalculation", "Số lần lặp tối đa (MAX_RETRIES): " + MAX_RETRIES);

        String deviceName = selectedRoom;
        String dayPath = generateDatabasePath(deviceName);

        if (dayPath == null) {
            tvTotalEnergy.setText("Tên phòng không hợp lệ.");
            return;
        }

        // Lấy tham chiếu đến Firebase
        DatabaseReference dbDevicesRef = FirebaseDatabase.getInstance().getReference(dayPath);

        // Định dạng ngày và chuẩn bị đường dẫn Firebase
        final String[] startDatePath = {
                startDate.get(Calendar.YEAR) + "/" + (startDate.get(Calendar.MONTH) + 1) + "/" + startDate.get(Calendar.DAY_OF_MONTH)
        };
        final String[] endDatePath = {
                endDate.get(Calendar.YEAR) + "/" + (endDate.get(Calendar.MONTH) + 1) + "/" + endDate.get(Calendar.DAY_OF_MONTH)
        };

        Log.d("EnergyCalculation", "startDatePath: " + startDatePath[0]);
        Log.d("EnergyCalculation", "endDatePath: " + endDatePath[0]);

        // Truy vấn Firebase để lấy dữ liệu cho giờ đầu tiên của ngày bắt đầu (startDate)
        getFirstAvailableHour(dbDevicesRef, startDatePath[0], startDate, true, 0, new DataCallback() {
            @Override
            public void onDataReceived(Float firstHourEnergyStartDate) {
                if (firstHourEnergyStartDate != null && firstHourEnergyStartDate > 0) {
                    Log.d("EnergyCalculation", "Dữ liệu cho startDate: " + startDatePath[0] + " - Energy: " + firstHourEnergyStartDate + " kWh");

                    getFirstAvailableHour(dbDevicesRef, endDatePath[0], endDate, false, 0, new DataCallback() {
                        @Override
                        public void onDataReceived(Float firstHourEnergyEndDate) {
                            if (firstHourEnergyEndDate != null && firstHourEnergyEndDate > 0) {
                                Log.d("EnergyCalculation", "Dữ liệu cho endDate: " + endDatePath[0] + " - Energy: " + firstHourEnergyEndDate + " kWh");

                                // Tính toán năng lượng tiêu thụ giữa startDate và endDate
                                energyConsumed = firstHourEnergyEndDate - firstHourEnergyStartDate;
                                tvTotalEnergy.setText(String.format("%.2f", energyConsumed));
                                tinhTienDien(energyConsumed);
                                if(selectedRoom == "Phòng 1"){
                                    totalEnergyForRooms[0] =energyConsumed ;
                                    Log.d("EnergyCalculation", "Dữ liệu Phòng 1: " + totalEnergyForRooms[0]);
                                    if (totalEnergyForRooms[0] > 0 || totalEnergyForRooms[1] > 0) {
                                        float totalEnergy = totalEnergyForRooms[0] + totalEnergyForRooms[1];
                                        tvTotalEnergy.setText(String.format("%.2f", totalEnergy));
                                        tinhTienDien(totalEnergy);
                                    }else return;
                                }
                                else if (selectedRoom == "Phòng 2"){
                                    totalEnergyForRooms[1] =energyConsumed;
                                    Log.d("EnergyCalculation", "Dữ liệu Phòng 2: " + totalEnergyForRooms[1]);
                                    if (totalEnergyForRooms[0] > 0 || totalEnergyForRooms[1] > 0) {
                                        float totalEnergy = totalEnergyForRooms[0] + totalEnergyForRooms[1];
                                        tvTotalEnergy.setText(String.format("%.2f", totalEnergy));
                                        tinhTienDien(totalEnergy);
                                    }else return;
                                }else return;
                            } else {
                                Log.d("EnergyCalculation", "Không có dữ liệu cho endDate.");
                                energyConsumed = 0;
                            }
                        }
                    });
                } else {
                    Log.d("EnergyCalculation", "Không có dữ liệu cho startDate.");
                    energyConsumed = 0;
                }
            }
        });
    }


    private void calculateEnergyForAllRooms(Calendar startDate, Calendar endDate) {
        Log.d("EnergyCalculation", "Bắt đầu tính toán năng lượng cho các phòng với thời gian: startDate = " + startDate.getTime() + ", endDate = " + endDate.getTime());


        // Tính năng lượng cho Phòng 1
        calculateEnergyForFirstAndLastHourWithData("Phòng 1", startDate, endDate, new DataCallback() {
            @Override
            public void onDataReceived(Float energyForRoom1) {

                Log.d("EnergyCalculation", "Dữ liệu Phòng 1: " + energyForRoom1);
            }
        });

        // Tính năng lượng cho Phòng 2
        calculateEnergyForFirstAndLastHourWithData("Phòng 2", startDate, endDate, new DataCallback() {
            @Override
            public void onDataReceived(Float energyForRoom2) {
                Log.d("EnergyCalculation", "Dữ liệu Phòng 2: " + energyForRoom2);
            }
        });
    }

    // Phương thức để lấy dữ liệu cho giờ đầu tiên của ngày (hoặc di chuyển đến ngày kế tiếp hoặc lùi lại)
    private void getFirstAvailableHour(DatabaseReference dbRef, String datePath, Calendar date, boolean isStartDate, int retryCount, DataCallback callback) {

        if (retryCount > MAX_RETRIES) {
            Log.d("EnergyCalculation", "Vượt quá giới hạn tìm kiếm dữ liệu.");
            callback.onDataReceived(0f);
            energyConsumed = 0;
            return;
        }

        dbRef.child(datePath).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                final Float[] firstHourEnergy = {null};

                // Log dữ liệu nhận được
                Log.d("EnergyCalculation", "Dữ liệu nhận được từ Firebase: " + snapshot.getValue());

                if (isStartDate) {
                    // Kiểm tra từ 0h đến 23h (cho startDate)
                    for (int hour = 0; hour < 24; hour++) {
                        String hourKey = String.format("%02d:00", hour);
                        DataSnapshot hourSnapshot = snapshot.child(hourKey);

                        if (hourSnapshot.exists()) {
                            Float energy = hourSnapshot.child("energy").getValue(Float.class);
                            if (energy != null) {
                                firstHourEnergy[0] = energy;

                                // Log thông tin dữ liệu
                                Log.d("EnergyCalculation", "Ngày: " + datePath + ", Giờ: " + hourKey + ", Energy: " + energy + " kWh");
                                callback.onDataReceived(firstHourEnergy[0]);
                                return;
                            }
                        }
                    }
                    // Nếu không tìm thấy, chuyển sang ngày kế tiếp
                    date.add(Calendar.DAY_OF_YEAR, 1);
                    String nextDatePath = date.get(Calendar.YEAR) + "/" + (date.get(Calendar.MONTH) + 1) + "/" + date.get(Calendar.DAY_OF_MONTH);
                    getFirstAvailableHour(dbRef, nextDatePath, date, isStartDate, retryCount + 1, callback);

                } else {
                    // Kiểm tra từ 23h lùi về 0h (cho endDate)
                    for (int hour = 23; hour >= 0; hour--) {
                        String hourKey = String.format("%02d:00", hour);
                        DataSnapshot hourSnapshot = snapshot.child(hourKey);

                        if (hourSnapshot.exists()) {
                            Float energy = hourSnapshot.child("energy").getValue(Float.class);
                            if (energy != null) {
                                firstHourEnergy[0] = energy;

                                // Log thông tin dữ liệu
                                Log.d("EnergyCalculation", "Ngày: " + datePath + ", Giờ: " + hourKey + ", Energy: " + energy + " kWh");
                                callback.onDataReceived(firstHourEnergy[0]);
                                return;
                            }
                        }
                    }
                    // Nếu không tìm thấy, lùi sang ngày hôm trước
                    date.add(Calendar.DAY_OF_YEAR, -1);
                    String previousDatePath = date.get(Calendar.YEAR) + "/" + (date.get(Calendar.MONTH) + 1) + "/" + date.get(Calendar.DAY_OF_MONTH);
                    getFirstAvailableHour(dbRef, previousDatePath, date, isStartDate, retryCount + 1, callback);
                }
                // Lấy giá trị từ TextView
                String totalEnergyStr = tvTotalEnergy.getText().toString().trim();
                // Kiểm tra xem chuỗi có hợp lệ không
                if (!totalEnergyStr.isEmpty()) {
                    // Thay dấu phẩy thành dấu chấm nếu có (nếu giá trị nhập vào có dấu phẩy)
                    totalEnergyStr = totalEnergyStr.replace(",", ".");

                    try {
                        // Chuyển đổi chuỗi thành double
                        double soDien = Double.parseDouble(totalEnergyStr);

                        // Gọi phương thức tính tiền điện
                        tinhTienDien(soDien);

                    } catch (NumberFormatException e) {
                        // Xử lý lỗi nếu chuỗi không thể chuyển đổi thành số
                        Log.e("Error", "Không thể chuyển đổi chuỗi thành số: " + totalEnergyStr);
                    }
                } else {
                }
                // Nếu không tìm thấy dữ liệu trong toàn bộ vòng lặp, trả về 0
                callback.onDataReceived(0f);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Lỗi khi tải dữ liệu: " + error.getMessage());
                callback.onDataReceived(null);
            }
        });
    }


    interface DataCallback {
        void onDataReceived(Float firstHourEnergy);
    }

    private String generateDatabasePath(String deviceName) {
        if (deviceName == null || deviceName.isEmpty()) {
            Log.e("FirebaseData", "Tên thiết bị null hoặc rỗng.");
            return null;
        }

        if (deviceName.startsWith("Phòng ")) {
            // Lấy số phòng từ tên thiết bị
            String roomNumber = deviceName.replace("Phòng ", "").trim();
            try {
                int number = Integer.parseInt(roomNumber);
                // Trả về đường dẫn Firebase
                return "pzem_data" + (number == 1 ? "" : number);
            } catch (NumberFormatException e) {
                Log.e("FirebaseData", "Lỗi định dạng số từ tên thiết bị: " + roomNumber, e);
            }
        } else {
            Log.e("FirebaseData", "Tên thiết bị không hợp lệ: " + deviceName);
        }
        return null;
    }
    public double tinhTienDien(double soDien) {
        // Các giá trị bậc giá điện
        int[] gioiHanBac = {50, 50, 100, 100, 100, 0}; // Giới hạn số điện cho từng bậc
        double[] giaBac = {1.806, 1.866, 2.167, 2.167, 2.167, 2.167}; // Giá điện từng bậc

        double tongTien = 0;
        double tienChuaThue = 0;
        double tienThue = 0;

        // Tính tiền cho từng bậc giá
        for (int i = 0; i < gioiHanBac.length; i++) {
            if (soDien > 0) {
                // Tính số điện sẽ tính ở bậc hiện tại
                double soDienBac = Math.min(soDien, gioiHanBac[i]);
                // Tính tiền cho bậc này
                tongTien += soDienBac * giaBac[i];
                tienChuaThue += soDienBac * giaBac[i];
                // Giảm số điện còn lại
                soDien -= soDienBac;
            }
        }
        // Tính tiền thuế VAT 8%
        tienThue = tienChuaThue * 0.08;
        // Áp dụng thuế VAT 8%
        tongTien *= 1.08;
        tvtienchuathue.setText(String.format("%.2f", tienChuaThue));
        tvtienthue.setText(String.format("%.2f", tienThue));
        tvtotalPayment.setText(String.format("%.2f", tongTien));
        return tongTien;
    }

}
