#include <WiFi.h>
#include <FirebaseESP32.h>
#include <PZEM004Tv30.h>
#include <time.h>
#include <Adafruit_SSD1306.h>
#include <Adafruit_GFX.h>

// Cấu hình OLED
#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64
#define OLED_RESET -1
#define SSD1306_I2C_ADDRESS 0x3C

Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, OLED_RESET);

// Firebase
#define FIREBASE_HOST "https://congtodien-dd8dc-default-rtdb.firebaseio.com/"
#define FIREBASE_AUTH "k5Bcm9r3pfnUwhf6OPLcyMBnwsNGxogqYNpUg059"

FirebaseConfig config;
FirebaseAuth auth;
FirebaseData firebaseData;

// PZEM
PZEM004Tv30 pzem(Serial2, 16, 17);

// Giá bậc thang
float bac1 = 1.678;

// Cấu hình múi giờ Việt Nam
#define TIMEZONE_OFFSET 25200 // +7 giờ
#define DAYLIGHT_OFFSET 0

// Pin cấu hình Wi-Fi
#define WIFI_CONFIG_BUTTON 5
bool wifiConfigMode = false;

// Hàm hiển thị thông tin lên OLED
void displayMessage(String message) {
  display.clearDisplay();
  display.setCursor(0, 0);
  display.setTextSize(1);
  display.setTextColor(SSD1306_WHITE);
  display.println(message);
  display.display();
}

// Hàm hiển thị dữ liệu cảm biến lên OLED
void displaySensorData(float voltage, float current, float power, float energy) {
  display.clearDisplay();
  display.setCursor(0, 0);
  display.setTextSize(1);
  display.setTextColor(SSD1306_WHITE);

  display.printf("Voltage: %.2f V\n", voltage);
  display.printf("Current: %.2f A\n", current);
  display.printf("Power: %.2f W\n", power);
  display.printf("Energy: %.2f kWh\n", energy);
  display.display();
}

// Hàm cấu hình Wi-Fi
void configWiFi() {
  Serial.println("Wi-Fi Config Mode");
  displayMessage("Configuring Wi-Fi...");
  
  // Nhập thông tin Wi-Fi qua Serial
  Serial.println("Enter Wi-Fi SSID:");
  String ssid = Serial.readStringUntil('\n');
  ssid.trim();
  
  Serial.println("Enter Wi-Fi Password:");
  String password = Serial.readStringUntil('\n');
  password.trim();

  WiFi.begin(ssid.c_str(), password.c_str());
  displayMessage("Connecting...");
  
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    display.print(".");
    display.display();
  }
  
  displayMessage("Wi-Fi Connected!");
  delay(2000);
}

// Hàm kết nối Wi-Fi
void connectWiFi() {
  if (digitalRead(WIFI_CONFIG_BUTTON) == LOW) {
    wifiConfigMode = true;
  }

  if (wifiConfigMode) {
    configWiFi();
    wifiConfigMode = false;
  } else {
    displayMessage("Connecting Wi-Fi...");
    WiFi.begin();
    
    int retryCount = 0;
    while (WiFi.status() != WL_CONNECTED && retryCount < 20) { // Thử kết nối tối đa 20 lần
      delay(500);
      retryCount++;
      display.print(".");
      display.display();
    }

    if (WiFi.status() == WL_CONNECTED) {
      displayMessage("Wi-Fi Connected!");
      delay(2000);
    } else {
      displayMessage("No Wi-Fi! Displaying data only.");
      delay(2000);
    }
  }
}

// Hàm gửi dữ liệu lên Firebase
void sendToFirebase(String path, float voltage, float current, float power, float energy) {
  if (WiFi.status() == WL_CONNECTED) {
    if (!Firebase.setFloat(firebaseData, path + "/voltage", voltage)) {
      Serial.println("Failed to send voltage: " + firebaseData.errorReason());
    }
    Firebase.setFloat(firebaseData, path + "/current", current);
    Firebase.setFloat(firebaseData, path + "/power", power);
    Firebase.setFloat(firebaseData, path + "/energy", energy);
  } else {
    Serial.println("No Wi-Fi. Data not sent to Firebase.");
  }
}

// Hàm lấy thời gian định dạng
String getFormattedTime(const char* format) {
  time_t now = time(nullptr);
  struct tm* timeInfo = localtime(&now);
  char buffer[50];
  strftime(buffer, sizeof(buffer), format, timeInfo);
  return String(buffer);
}

void setup() {
  Serial.begin(115200);

  // Khởi tạo OLED
  if (!display.begin(SSD1306_I2C_ADDRESS, OLED_RESET)) {
    Serial.println(F("OLED không hoạt động!"));
    while (true);
  }

  displayMessage("Starting...");
  
  // Thiết lập nút cấu hình Wi-Fi
  pinMode(WIFI_CONFIG_BUTTON, INPUT_PULLUP);

  // Kết nối Wi-Fi
  connectWiFi();

  // Cấu hình Firebase
  config.database_url = FIREBASE_HOST;
  config.signer.tokens.legacy_token = FIREBASE_AUTH;
  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  // Cấu hình NTP
  configTime(TIMEZONE_OFFSET, DAYLIGHT_OFFSET, "pool.ntp.org", "time.nist.gov");
  displayMessage("Syncing time...");
  
  while (time(nullptr) < 8 * 3600 * 2) {
    delay(1000);
  }

  displayMessage("Time synced!");
  delay(1000);
}

void loop() {
  // Kiểm tra nút nhấn để vào chế độ cấu hình Wi-Fi
  if (digitalRead(WIFI_CONFIG_BUTTON) == LOW) {
    wifiConfigMode = true;
    connectWiFi();
  }

  // Lấy thời gian hiện tại
  time_t now = time(nullptr);
  struct tm* timeInfo = localtime(&now);

  // Kiểm tra dữ liệu cảm biến mỗi giờ
  static int lastHour = -1;
  if (timeInfo->tm_hour != lastHour) {
    lastHour = timeInfo->tm_hour;

    // Đọc dữ liệu PZEM
    float voltage = pzem.voltage();
    float current = pzem.current();
    float power = pzem.power();
    float energy = pzem.energy();

    if (!isnan(voltage) && !isnan(current) && !isnan(power) && !isnan(energy)) {
      // Hiển thị dữ liệu cảm biến lên OLED
      displaySensorData(voltage, current, power, energy);

      // Nếu có Wi-Fi, gửi lên Firebase
      if (WiFi.status() == WL_CONNECTED) {
        String year = getFormattedTime("%Y");
        String month = getFormattedTime("%m");
        String day = getFormattedTime("%d");
        String hour = getFormattedTime("%H:%M");
        String path = "/pzem_data/" + year + "/" + month + "/" + day + "/" + hour;
        sendToFirebase(path, voltage, current, power, energy);
      } else {
        Serial.println("Wi-Fi disconnected. Data displayed only.");
      }
    } else {
      displayMessage("Error reading sensor!");
    }
  }

  delay(1000); // Chờ lặp lại mỗi giây
}
