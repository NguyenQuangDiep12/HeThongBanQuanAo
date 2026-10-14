# Hệ thống bán quần áo BLUE THREAD

Ứng dụng thương mại điện tử bán quần áo, gồm backend cung cấp REST API và frontend React phục vụ khách hàng cùng nhân viên quản trị. Hai phần được phát triển và chạy độc lập; frontend lấy dữ liệu từ backend qua API.

## Mục tiêu

- Cho phép khách hàng tìm, lọc và xem thông tin sản phẩm quần áo.
- Hỗ trợ chọn kích cỡ, màu sắc, quản lý giỏ hàng và tạo đơn hàng.
- Cung cấp giao diện quản trị để cập nhật sản phẩm, biến thể, tồn kho và xử lý đơn.
- Lưu dữ liệu bằng MySQL; quản lý thay đổi cấu trúc cơ sở dữ liệu bằng Flyway.

## Cấu trúc dự án

```text
C:\DAAAN\shopee
├── shopee/                         # Backend Spring Boot
│   ├── src/main/java/              # Controller, service, model, repository
│   ├── src/main/resources/
│   │   ├── application.properties  # Cấu hình chạy backend và MySQL
│   │   └── db/migration/           # Flyway migrations
│   ├── run-backend.ps1             # Chạy backend riêng
│   ├── clean-backend.ps1           # Clean backend
│   └── build-backend.ps1           # Clean và build backend
└── frontend/                       # Frontend React + Tailwind CSS
    ├── src/                        # Giao diện, routes và API client
    ├── public/                     # Tài nguyên tĩnh như favicon
    └── package.json                # Lệnh dev, clean, build frontend
```

## Công nghệ

| Thành phần | Công nghệ |
| --- | --- |
| Backend | Java 21, Spring Boot, Spring Security, Spring Data JPA |
| Cơ sở dữ liệu | MySQL 8, Flyway |
| Xác thực | JWT; phân quyền USER và ADMIN |
| Frontend | React, Vite, Tailwind CSS, React Router |
| Giao tiếp | REST API, dữ liệu JSON; API prefix `/api` |

## Chức năng khách hàng

- Trang chủ và danh sách sản phẩm lấy dữ liệu động từ backend.
- Tìm kiếm theo từ khóa; lọc theo danh mục, kích cỡ, màu sắc, giá và tình trạng còn hàng.
- Sắp xếp và phân trang danh sách sản phẩm.
- Xem chi tiết, mô tả, giá và tồn kho của từng biến thể sản phẩm.
- Đăng ký, đăng nhập và xác thực bằng JWT.
- Thêm sản phẩm vào giỏ, điều chỉnh số lượng hoặc xóa khỏi giỏ.
- Đặt hàng với địa chỉ, số điện thoại và phương thức COD hoặc chuyển khoản.
- Xem đơn hàng, trạng thái xử lý và thông báo liên quan.

## Chức năng quản trị

- Xem tổng quan sản phẩm và đơn hàng từ các API hiện có.
- Tạo, cập nhật hoặc ẩn sản phẩm.
- Thêm biến thể theo kích cỡ/màu sắc; cập nhật giá và tồn kho.
- Xem đơn hàng đang chờ; xác nhận hoặc hủy đơn.
- Khi ADMIN hủy đơn PENDING, backend hoàn lại lượng tồn kho đã giữ cho đơn đó.

## Cấu hình và dữ liệu

Backend đọc cấu hình từ [application.properties](</C:/DAAAN/shopee/shopee/src/main/resources/application.properties>). Cấu hình hiện tại sử dụng MySQL tại `localhost:3306`, database `clotherwebsite`. Hãy khởi động MySQL và bảo đảm database tồn tại trước khi chạy backend. Flyway sẽ tạo/cập nhật bảng khi ứng dụng khởi động.

Frontend chạy Vite tại `localhost:5173` và proxy đường dẫn `/api` tới backend `localhost:8080`. Vì vậy, có thể khởi động frontend riêng, nhưng các chức năng tải dữ liệu từ API cần backend hoạt động.

## Chạy hai phần riêng biệt

Mở PowerShell thứ nhất cho backend:

```powershell
cd C:\DAAAN\shopee\shopee
.\run-backend.ps1
```

Mở PowerShell thứ hai cho frontend:

```powershell
cd C:\DAAAN\shopee\frontend
npm install    # chỉ cần ở lần đầu hoặc khi dependencies thay đổi
npm run dev
```

Mở giao diện tại [http://localhost:5173](http://localhost:5173). Backend API ở [http://localhost:8080](http://localhost:8080). Dừng mỗi ứng dụng bằng `Ctrl+C` trong terminal tương ứng.

### Clean và build độc lập

Trong thư mục backend:

Trong thư mục frontend:

```powershell
npm run clean
npm run build
```

Build backend hiện bỏ qua test bằng `-DskipTests`; test có thể chạy riêng bằng Maven nếu cần.

## Tài liệu chi tiết

- [Hướng dẫn backend](</C:/DAAAN/shopee/shopee/BACKEND_RUN.md>)
- [Hướng dẫn frontend](</C:/DAAAN/shopee/frontend/README.md>)

## Phạm vi hiện tại

Ứng dụng chưa tích hợp cổng thanh toán trực tuyến, upload ảnh trực tiếp, quên mật khẩu, danh sách yêu thích, vận chuyển/tracking hoặc báo cáo doanh thu chuyên sâu. Các phần này cần endpoint và nghiệp vụ backend bổ sung trước khi sử dụng.
