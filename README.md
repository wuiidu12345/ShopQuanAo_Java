**🛒 Website Shop quần áo Smart Casual (Spring Boot & SQL Server Management Studio)**

Hệ thống website thời trang được phát triển trên nền tảng Java Spring Boot, sử dụng môi trường phát triển IntelliJ IDEA và cơ sở dữ liệu SQL Server.

\-----------------------------------------------------------------------------------------------------------------------------------

**🗄️ BƯỚC 1: CẤU HÌNH CƠ SỞ DỮ LIỆU (SSMS)**

**1. Cấu hình kết nối hệ thống**

Để đảm bảo ứng dụng kết nối thành công đến SQL Server Management Studio, bạn cần cấu hình tài khoản và cổng kết nối như sau:

- **Tài khoản:** Sử dụng quyền xác thực của SQL Server Management Studio với 'Username: sa và Password: 123'.

- **Server name:** Sử dụng dấu chấm . (hoặc localhost).

- **Cấu hình TCP/IP:**
  1. Mở SQL Server Configuration Manager.
  2. Chọn SQL Server Network Configuration → Protocols for MSSQLSERVER → Bật (Enabled) mục TCP/IP.
  3. Click đúp vào TCP/IP → tab IP Addresses → kéo xuống dưới cùng tại mục IPAll:
     - Đặt TCP Port: 1433
     - Để trống TCP Dynamic Ports.
     - Khởi động lại (Restart) dịch vụ SSMS để áp dụng thay đổi.

**2. Khởi tạo dữ liệu**

Trong thư mục ShopQuanAo_SQL_vX, bạn vui lòng chạy lần lượt các file SQL theo thứ tự sau để đảm bảo cấu trúc và dữ liệu:

1. **1_TaoBang.sql**: Tạo cấu trúc các bảng.

2. **2_Trigger.sql**: Thiết lập các Trigger nghiệp vụ.

3. **3_DuLieu.sql**: Thêm dữ liệu mẫu để trải nghiệm.

\-----------------------------------------------------------------------------------------------------------------------------------

**🚀 BƯỚC 2: KHỞI CHẠY DỰ ÁN TRÊN INTELLIJ IDEA**

1. Mở IntelliJ IDEA, chọn Open và trỏ đến thư mục gốc ShopQuanAo_Java (trong ShopQuanAo_Java_vX/...) của dự án để import (hệ thống sẽ tự động tải các dependency từ Maven/Gradle).

2. Kiểm tra cấu hình kết nối database trong file "src/main/resources/application.properties" đảm bảo trùng khớp với thông số ở Bước 1:

- spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=ShopQuanAo;trustServerCertificate=true;
- spring.datasource.username=sa
- spring.datasource.password=123

3. Chọn Run (hoặc nhấn Shift + F10) để khởi chạy dự án. Mở trình duyệt và truy cập theo đường dẫn: http://localhost:8080.

\-----------------------------------------------------------------------------------------------------------------------------------

**🔑 THÔNG TIN TÀI KHOẢN TRẢI NGHIỆM**

Hệ thống sử dụng **giao diện đăng nhập chung** cho cả khách hàng và nhân viên quản lý. Tùy vào tài khoản đăng nhập mà hệ thống sẽ điều hướng đến trang bán hàng hoặc trang quản trị tương ứng.

| **Vai trò**         | **Tên đăng nhập** | **Mật khẩu** |
| ------------------- | ----------------- | ------------ |
| **Quản lý (Admin)** | admin             | Admin@123    |
| **Khách hàng Demo** | demo1@gmail.com   | Demo@123     |
|                     | demo2@gmail.com   | User#456     |
|                     | demo3@gmail.com   | Pass$789     |

(Dữ liệu chi tiết nằm trong bảng NHAN_VIEN và KHACH_HANG)

\-----------------------------------------------------------------------------------------------------------------------------------

**🎟️ HỆ THỐNG MÃ GIẢM GIÁ**

Để trải nghiệm tính năng tính toán khuyến mãi trong giỏ hàng, bạn có thể sử dụng các mã sau:

- **SAVE10**: Giảm 10% tổng hóa đơn.
- **CASH5**: Giảm trực tiếp 5.000đ.
- **CASH20**: Giảm trực tiếp 20.000đ.
- **FREESHIP**: Miễn phí vận chuyển.
- **NEW25**: Giảm 25% cho khách hàng mới.

\-----------------------------------------------------------------------------------------------------------------------------------

**Cảm ơn bạn đã quan tâm đến dự án!**
