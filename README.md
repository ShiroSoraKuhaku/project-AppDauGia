# 🔨 Auction System (Hệ Thống Đấu Giá Trực Tuyến)

## 1. Mô tả ngắn gọn bài toán và phạm vi hệ thống
Hệ thống Bidding (đấu giá trực tuyến) là nền tảng phần mềm cho phép nhiều người dùng cùng tham gia cạnh tranh giá để mua một sản phẩm trong một khoảng thời gian xác định. Người bán đưa sản phẩm lên hệ thống và giá bán cuối cùng được xác định thông qua quá trình đấu giá giữa các người mua (Tham khảo mô hình eBay Auctions).

Phạm vi hệ thống bao gồm: Quản lý tài khoản người dùng, Quản lý danh sách sản phẩm đấu giá, Thực hiện đặt giá theo thời gian thực (Real-time bidding), Tự động kết thúc phiên và xử lý thanh toán, lưu trữ dữ liệu an toàn.

## 2. Công nghệ sử dụng, môi trường chạy và yêu cầu cài đặt
### Công nghệ sử dụng
- **Backend:** Java 25, Socket, Đa luồng (Thread Pool, Concurrency).
- **Frontend:** JavaFX (Sử dụng CSS để tuỳ chỉnh giao diện).
- **Database:** MySQL, JDBC.
- **DevOps/Tools:** Docker, Docker Compose, Maven, Git/GitHub.

### Yêu cầu cài đặt (Setup Prerequisites)
1. **Java JDK 25** (hoặc mới hơn, đã được thêm vào biến môi trường `JAVA_HOME`).
2. **Apache Maven** (đã được thêm vào biến môi trường `PATH`).
3. **Docker / Docker Compose** (Windows/macOS có thể dùng Docker Desktop; trên Linux nên cài Docker Engine + Docker Compose plugin để khởi tạo MySQL database và chạy Server nếu dùng Docker).

## 3. Cấu trúc thư mục hoặc các module chính
Cấu trúc cơ bản của dự án được tổ chức theo mô hình Multi-Module kết hợp Client-Server:
```text
DauGiaProject/
├── docker-compose.yml          # Cấu hình Docker container cho MySQL và Backend Server
├── Dockerfile                  # Cấu hình build Docker cho Server
├── init.sql                    # Script khởi tạo bảng tự động cho MySQL
├── pom.xml                     # Cấu hình Maven gốc (Parent POM)
├── window/                     # Script chạy/dừng cho Windows
│   ├── run.bat
│   ├── client.bat
│   └── stop.bat
├── mac-linux/                  # Script chạy/dừng cho macOS/Linux
│   ├── run.sh
│   ├── client.sh
│   ├── stop.sh
├── client-module/              # 🎨 FRONTEND: Giao diện người dùng JavaFX
│   ├── pom.xml
│   └── src/main/java/com/group15/daugia/client/
│       ├── Launcher.java       # Lớp trung gian khởi chạy ứng dụng JavaFX (Khắc phục lỗi Module)
│       ├── controller/         # Các Controller xử lý sự kiện UI
│       └── view/               # Các file thiết kế giao diện FXML
├── server-module/              # 🖥️ BACKEND: Socket Server, Đa luồng, kết nối Database
│   ├── pom.xml
│   └── src/main/java/com/group15/daugia/server/
│       ├── Main.java           # Lớp khởi chạy Server
│       ├── handler/            # Các Handler xử lý Request từ Client
│       └── repository/         # Tương tác với Database (JDBC)
├── shared-module/              # 📦 DỮ LIỆU CHUNG (Dùng cho cả Client và Server)
│   ├── pom.xml
│   └── src/main/java/com/group15/daugia/shared/
│       ├── model/              # Thực thể OOP (Entity)
│       └── message/            # Đóng gói dữ liệu truyền tải (Request, Response)
└── mysql-db/                   # Thư mục lưu trữ dữ liệu của MySQL khi chạy bằng Docker
```

## 4. Vị trí các file .jar
Sau khi biên dịch và đóng gói thành công bằng lệnh Maven tại thư mục gốc (VD: `mvn clean install`), các file `.jar` độc lập (Fat JAR) đã bao gồm thư viện sẽ được tạo ra tại:
- **Client:** `client-module/target/client-1.0-SNAPSHOT.jar`
- **Server:** `server-module/target/server-1.0-SNAPSHOT.jar`
- **Shared:** `shared-module/target/share-1.0-SNAPSHOT.jar` (Chỉ chứa các file .class chung dùng làm thư viện).

## 5. Hướng dẫn chạy hệ thống (How to run)

**🚨 YÊU CẦU BẮT BUỘC CHUNG:** Bạn **phải đảm bảo Docker Engine/Docker Desktop đang chạy** trước. Chờ cho trạng thái Docker chuyển sang **Engine running** rồi mới tiến hành chạy Database hoặc Server.

### Cách 1: Chạy tự động theo hệ điều hành
Cách này sẽ tự động:
1. Khởi động MySQL + Server bằng Docker Compose.
2. Chờ Server sẵn sàng trên `localhost:8080`.
3. Sẵn sàng để mở một hoặc nhiều Client riêng biệt.

**Lưu ý**: Phải chạy backend lên trước thì mới có thể chạy client.

#### Windows
Chạy backend:
```bat
window\run.bat
```
Hoặc double click trực tiếp `window\run.bat`.

Mở thêm một Client:
```bat
window\client.bat
```
Hoặc double click trực tiếp `window\client.bat`.

Khi muốn dừng toàn bộ hệ thống:
```bat
window\stop.bat
```
Hoặc double click trực tiếp `window\stop.bat`.

#### macOS/Linux
Chạy backend:
```bash
bash ./mac-linux/run.sh
```

Mở thêm một Client:
```bash
bash ./mac-linux/client.sh
```

Khi muốn dừng toàn bộ hệ thống:
```bash
bash ./mac-linux/stop.sh
```

Lưu ý:
- Script tự động ưu tiên `docker compose`, nếu máy bạn chỉ có bản cũ thì sẽ tự fallback sang `docker-compose`.
- `run` chỉ khởi động backend. `client` là launcher riêng và có thể gọi nhiều lần để mở nhiều client song song.
- `client` sẽ tự build JAR nếu file chưa tồn tại.
- Nếu bạn vừa sửa code client, hãy build lại một lần bằng `mvn -q -pl client-module -am package -DskipTests` trước khi mở các client mới để chắc chắn dùng bản cập nhật.

#### Linux chạy Server bằng Docker
Nếu bạn muốn chạy backend trực tiếp bằng Docker trên Linux, dùng `docker compose` trong thư mục gốc của dự án:

Khởi động MySQL + Server:
```bash
docker compose up -d --build server
```

Nếu máy bạn chỉ có bản Docker Compose cũ:
```bash
docker-compose up -d --build server
```

Kiểm tra trạng thái dịch vụ:
```bash
docker compose ps
```

Xem log của Server:
```bash
docker compose logs -f server
```

Dừng toàn bộ hệ thống:
```bash
docker compose down
```

Lưu ý:
- Service `server` có `depends_on` nên Docker sẽ tự khởi động cả `db` trước khi chạy Server.
- MySQL được map ra host tại cổng `3307`, còn Server chạy ở `localhost:8080`.
- Nếu bạn muốn build lại image sau khi sửa code, hãy chạy lại lệnh `docker compose up -d --build server`.

### Cách 2: Chạy thủ công (Dành cho Developer)
Sử dụng cách này để có thể Debug mã nguồn trực tiếp trên máy host. Server và Client sẽ được chạy thủ công thông qua Maven.

**Bước 1: Khởi động Database (MySQL)**
Mở Terminal, di chuyển vào thư mục gốc `DauGiaProject` và kích hoạt riêng database:
```bash
docker-compose up -d db
```

**Bước 2: Biên dịch toàn bộ dự án**
```bash
mvn clean install -DskipTests
```

**Bước 3: Khởi động Server**
Mở Terminal mới, vào thư mục `server-module` và chạy:
```bash
cd server-module
java -jar target/server-1.0-SNAPSHOT.jar
```
*(Hoặc chạy Class `com.group15.daugia.server.Main` trực tiếp trên IDE).* 

**Bước 4: Khởi động Client**
Mở Terminal mới, vào thư mục `client-module` và chạy một lần cho mỗi client:
```bash
cd client-module
mvn exec:java
```

Nếu muốn build lại trước khi chạy:
```bash
mvn compile exec:java
```

Bạn có thể mở nhiều Terminal và chạy lại lệnh trên nhiều lần để có nhiều client cùng lúc. Nếu muốn dùng script, chạy `window\client.bat` hoặc `mac-linux/client.sh` nhiều lần.

## 6. Danh sách các chức năng đã hoàn thành
### 6.1. Nhóm chức năng nâng cao
- **Trực quan hóa dữ liệu:** Vẽ biểu đồ Line Chart theo dõi biến động giá và các mốc ngày.
- **Cơ chế đóng băng tài khoản (Frozen Balance):** Tạm giữ và hoàn trả số dư để đảm bảo khả năng thanh toán.
- **Chống đấu giá phút cuối (Anti-sniping):** Tự động gia hạn thời gian kết thúc khi có lượt bid mới.
- **Cập nhật thời gian thực (Real-time Updates):** Đồng bộ hóa dữ liệu và hiển thị thay đổi ngay lập tức trên giao diện mà không cần tải lại trang.

### 6.2. Nhóm chức năng cơ bản
- **Đăng ký tài khoản.**
- **Đăng nhập hệ thống.**
- **Quản lý số dư tài khoản (Nạp/Trừ tiền).**
- **Phân quyền người dùng (Bidder, Seller, Admin).**
- **Tạo phiên đấu giá mới.**
- **Quyết toán đấu giá (Chuyển tiền cho người bán).**
- **Giám sát trạng thái phiên tự động.**
- **Hiển thị danh mục sản phẩm.**
- **Đặt giá thầu nhanh, thủ công.**
- **Giao tiếp qua Socket Server/Client.**
- **Phát thông báo Broadcast (Thông tin giá mới, trạng thái mới).**
- **Nạp dữ liệu từ MySQL khi khởi động.**
- **Thiết kế giao diện người dùng JavaFX.**

## 7. Link báo cáo PDF và Video Demo
- **Báo cáo Bài tập lớn (PDF):** [Link tải báo cáo tại đây](#)
- **Video Demo hệ thống:** [Link xem video tại đây](#)

## 🛠️ Hướng Dẫn Tải & Cấu Hình Môi Trường (Setup Prerequisites)

Nếu máy tính của bạn chưa cài đặt các công cụ cần thiết, hãy thực hiện nhanh theo hướng dẫn sau:

### 1. Cài đặt Java JDK 25 (hoặc mới hơn)
* **Tải xuống**: Truy cập trang chủ [Oracle JDK](https://oracle.com) hoặc [Amazon Corretto](https://amazon.com) để tải bản cài đặt phù hợp (.exe cho Windows, .pkg cho Mac).
* **Thiết lập**: Chạy file vừa tải để cài đặt. Đảm bảo bạn đã thêm đường dẫn JDK vào biến môi trường `JAVA_HOME`.
* **Kiểm tra**: Mở Terminal gõ `java -version` nếu hiển thị đúng phiên bản là thành công.

### 2. Cài đặt Apache Maven
* **Tải xuống**: Truy cập [Maven Download](https://apache.org), tải file Zip (Binary zip archive).
* **Thiết lập**: Giải nén vào một thư mục cố định (ví dụ: `C:\maven`). Thêm đường dẫn thư mục `bin` vào biến môi trường `PATH` của hệ thống.
* **Kiểm tra**: Mở Terminal gõ `mvn -v` để xác nhận hệ thống đã nhận diện được lệnh Maven.

### 3. Cài đặt & Khởi động Docker Desktop
* **Tải xuống**: Truy cập trang chủ [Docker Desktop](https://docker.com) để tải bộ cài đặt phù hợp với hệ điều hành.
* **Cài đặt**: Tiến hành nhấn Next theo trình duyệt cài đặt mặc định (đối với Windows nên tích chọn cài đặt WSL 2 nếu hệ thống yêu cầu).
* **Mở Docker**: Sau khi cài xong, tìm và khởi chạy phần mềm **Docker Desktop**. Chờ vài phút cho đến khi biểu tượng cá voi ở góc dưới màn hình sáng xanh (Engine running). Luôn giữ Docker chạy ngầm khi chạy dự án này để cung cấp Database MySQL.
