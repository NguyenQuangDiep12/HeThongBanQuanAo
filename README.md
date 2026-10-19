# BLUE THREAD

Ứng dụng bán quần áo gồm frontend React và backend Spring Boot. Frontend gọi REST API qua `/api`; backend lưu dữ liệu trong MySQL.

## Cấu trúc

```text
C:\DAAAN\shopee
├── frontend/       # React, Vite, giao diện khách hàng và quản trị
├── shopee/         # Spring Boot API, cấu hình và cơ sở dữ liệu
└── docs/           # Tài liệu thiết kế, sơ đồ và đánh giá bảo mật
```

## Chạy dự án

Cần Java 21, Docker với MySQL 8, Node.js và pnpm. Khởi động MySQL container (nếu chưa chạy):

```powershell
docker run -d --name mysql-db -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root123 -e MYSQL_DATABASE=shopbanquanao -e MYSQL_USER=app_user -e MYSQL_PASSWORD=app123 mysql:8.0
```

Sau khi MySQL khởi động, mở PowerShell thứ nhất để chạy backend. Cấu hình dev mặc định đã trỏ tới database `shopbanquanao` và user `app_user`; đặt JWT secret và mật khẩu admin:

```powershell
cd C:\DAAAN\shopee\shopee
$env:JWT_SECRET = "<chuỗi bí mật ngẫu nhiên tối thiểu 32 ký tự>"
$env:ADMIN_PASSWORD = "<mật khẩu quản trị>"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Mở PowerShell thứ hai để chạy frontend:

```powershell
cd C:\DAAAN\shopee\frontend
pnpm install
pnpm dev
```

Frontend chạy tại [http://localhost:5173](http://localhost:5173) và chuyển tiếp `/api` đến backend tại [http://localhost:8082](http://localhost:8082). Backend dùng cấu hình profile `dev` trong `shopee/src/main/resources/application-dev.properties`; chỉ dùng profile này với cơ sở dữ liệu phát triển. Xem [hướng dẫn backend](shopee/src/BACKEND_SETUP.md) để biết cấu hình, seed dữ liệu và các endpoint.

## Build

```powershell
cd C:\DAAAN\shopee\shopee
.\mvnw.cmd clean package -DskipTests

cd C:\DAAAN\shopee\frontend
pnpm run build
```

## Chức năng

- Tìm kiếm, lọc và xem sản phẩm, biến thể, tồn kho và đánh giá.
- Đăng ký, đăng nhập, giỏ hàng, thanh toán COD/chuyển khoản, đơn hàng và thông báo.
- Danh sách yêu thích của khách hàng.
- Quản trị sản phẩm, danh mục, biến thể, đơn hàng, người dùng, tài khoản ngân hàng và thống kê.

## Tài liệu

- [Tổng quan kiến trúc](docs/software-design/01-software-architecture.md)
- [Thiết kế cơ sở dữ liệu](docs/software-design/03-database-design.md)
- [Thiết kế API](docs/software-design/04-api-design.md)
- [Danh mục kiểm thử](docs/software-design/05-unit-test-inventory.md)
- [Tài liệu thiết kế và kiểm thử cập nhật](docs/software-design/Shopee-Software-Design-Unit-Testing-Updated-2026-10-15.docx)
- [Đánh giá bảo mật](docs/security/SECURITY-REVIEW-AND-REMEDIATION-2026-10.md)
