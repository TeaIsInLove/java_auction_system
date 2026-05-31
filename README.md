# Hệ Thống Đấu Giá Trực Tuyến

Ứng dụng desktop mô phỏng sàn đấu giá trực tuyến thời gian thực, xây dựng bằng **JavaFX + SQLite + TCP Socket**, môn **Lập Trình Nâng Cao 2026** — Trường Đại học Công nghệ, ĐHQGHN.

**Phạm vi hệ thống:** Hỗ trợ 2 vai trò (Admin / User), 3 loại vật phẩm (Electronics, Art, Vehicle), nhiều client kết nối đồng thời qua TCP Socket, cập nhật giá theo thời gian thực.

---

## Danh sách chức năng đã hoàn thành

### Bắt buộc
| # | Chức năng | Trạng thái |
|---|-----------|-----------|
| 1 | Đăng ký / Đăng nhập với mã hóa mật khẩu SHA-256 | ✅ |
| 2 | Phân quyền Admin / User | ✅ |
| 3 | Tạo, duyệt, bắt đầu, kết thúc phiên đấu giá | ✅ |
| 4 | Đặt giá thời gian thực, an toàn đa luồng (ReentrantLock) | ✅ |
| 5 | Lịch sử đặt giá theo phiên | ✅ |
| 6 | Thanh toán tự động (PAID / FINISHED / CANCELED) | ✅ |
| 7 | 3 loại vật phẩm: Electronics, Art, Vehicle (Factory Pattern) | ✅ |
| 8 | Lưu trữ bền vững bằng SQLite qua JDBC | ✅ |
| 9 | Unit tests JUnit 5 + CI tự động GitHub Actions | ✅ |
| 10 | Admin quản lý người dùng (xem, đổi vai trò, xóa) | ✅ |

### Tùy chọn (Bonus)
| Chức năng | Trạng thái |
|-----------|-----------|
| **Anti-snipe** — tự động gia hạn 5 phút khi có bid trong 5 phút cuối | ✅ |
| **Auto-bidding** — đăng ký giá tối đa, hệ thống tự đặt giá thay | ✅ |
| **Biểu đồ xu hướng giá** — LineChart cập nhật thời gian thực trong phòng đấu giá | ✅ |

---

## Công nghệ sử dụng

| Thành phần | Công nghệ |
|-----------|-----------|
| Giao diện | JavaFX 21 + FXML + CSS |
| Cơ sở dữ liệu | SQLite (sqlite-jdbc 3.45) |
| Mạng | Java TCP Socket (cổng 9999) |
| Công cụ build | Apache Maven 3.9+ |
| Kiểm thử | JUnit 5 (Jupiter) |
| CI/CD | GitHub Actions |
| Phiên bản Java | JDK 21 LTS (khuyến nghị) hoặc JDK 25 |
| Hệ điều hành | Windows / macOS / Linux |

---

## Yêu cầu cài đặt

- **JDK 21+** — tải tại https://adoptium.net
- **Maven 3.9+** — hoặc dùng wrapper `mvnw` / `mvnw.cmd` đi kèm (không cần cài thêm)
- Không cần cài SQLite riêng — driver đã nhúng sẵn trong file jar

---

## Cấu trúc thư mục

```
java_auction_system/
├── src/
│   ├── main/
│   │   ├── java/com/example/bai_tap_lon/
│   │   │   ├── Controllers/     JavaFX controllers + AuctionWorkspace (Facade) + AutoBidManager
│   │   │   ├── auth/            Repository (JDBC), AuthService, PasswordUtil, DatabaseManager
│   │   │   ├── exception/       Phân cấp ngoại lệ tùy chỉnh (AuctionException...)
│   │   │   ├── model/           Domain model: Auction, Item, User, BidTransaction
│   │   │   ├── network/         AuctionServer, AuctionClient, NetworkMessage (đồng bộ TCP)
│   │   │   └── session/         SessionManager
│   │   └── resources/
│   │       └── .../Views/
│   │           ├── fxml/        10 màn hình FXML
│   │           └── css/         Stylesheet
│   └── test/                    Unit tests (JUnit 5)
├── .github/workflows/ci.yml     Cấu hình GitHub Actions CI
└── pom.xml
```

---

## Cách build chương trình

```bash
# Clone repository
git clone https://github.com/TeaIsInLove/java_auction_system.git
cd java_auction_system

# Windows
mvnw.cmd clean package -DskipTests

# macOS / Linux (cấp quyền thực thi lần đầu)
chmod +x mvnw
./mvnw clean package -DskipTests
```

---

## Cách chạy chương trình

### Chạy đơn (1 cửa sổ)

```bash
# Windows
mvnw.cmd javafx:run

# macOS / Linux
./mvnw javafx:run
```

Lần đầu chạy, database SQLite được tạo tự động tại `~/.auctionsystem/app.db`.

---

### Chạy Server + nhiều Client (demo đa máy khách)

> Mở **nhiều cửa sổ terminal** và chạy theo thứ tự sau:

**Bước 1 — Cửa sổ 1 (Server):** Chạy instance đầu tiên

```bash
# Windows
mvnw.cmd javafx:run

# macOS / Linux
./mvnw javafx:run
```

Instance đầu tiên tự động khởi động **AuctionServer TCP trên cổng 9999**.

**Bước 2 — Cửa sổ 2, 3, … (Client):** Mở thêm terminal, chạy cùng lệnh

```bash
# Windows
mvnw.cmd javafx:run

# macOS / Linux
./mvnw javafx:run
```

Các instance sau tự động kết nối vào server đang chạy.  
Khi một client đặt giá → **tất cả client còn lại nhận cập nhật ngay lập tức**.

---

**Chạy trên 2 máy khác nhau (cùng mạng LAN):**

Trên **Máy B**, tạo file `~/.auctionsystem/server.properties` với nội dung:

```properties
server.host=<địa chỉ IP của Máy A trên mạng LAN>
```

Sau đó chạy bình thường — Máy B sẽ tự kết nối vào server của Máy A.

---

### Tài khoản Admin mặc định

| Trường | Giá trị |
|--------|---------|
| Email | `admin@local` |
| Mật khẩu | `admin123` |

---

## Chạy kiểm thử

```bash
# Windows
mvnw.cmd clean test

# macOS / Linux
./mvnw clean test
```

Kiểm thử chạy ở chế độ headless (không cần màn hình) nhờ cấu hình `useModulePath=false` trong surefire plugin.

---

## CI / CD

GitHub Actions (`.github/workflows/ci.yml`) tự động chạy trên mỗi lần push hoặc pull request vào nhánh `main` và `dev`:

1. Checkout mã nguồn
2. Cài đặt JDK 21 (Temurin)
3. Chạy `mvn clean test`
4. Upload kết quả Surefire làm artifact

---

## Design Patterns sử dụng

| Pattern | Nơi áp dụng |
|---------|------------|
| **Singleton** | `AuctionManager`, `AuctionWorkspace`, `SessionManager`, `AuctionClient`, `AutoBidManager` |
| **Facade** | `AuctionWorkspace` — tập trung toàn bộ business logic, controller không gọi trực tiếp xuống DB |
| **Observer** | `AuctionSubject` / `AuctionObserver` — view tự cập nhật khi có bid mới |
| **Factory** | `ItemFactory.createItem(type, ...)` — tạo Electronics / Art / Vehicle |
| **MVC** | JavaFX FXML (View) + Controller + Repository (Model) |

---

## Kiến trúc cập nhật thời gian thực

- Instance đầu tiên khởi động **AuctionServer** (TCP, cổng 9999) trong daemon thread
- Các instance sau kết nối như **AuctionClient**
- Khi bid được đặt → broadcast `NetworkMessage` tới tất cả client
- Mỗi client đồng bộ SQLite local trước → `reloadAuctionFromDb()` → `Platform.runLater()` cập nhật giao diện

---

## Tài liệu

- **Báo cáo PDF:** [Điền link sau khi có]
- **Video demo:** https://youtu.be/mtPrMOAMlX4?si=P8Dcv9RbvmRNx82D
