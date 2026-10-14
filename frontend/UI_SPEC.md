# Đặc tả giao diện BLUE THREAD

## 1. Mục tiêu và quy ước

- Cửa hàng thời trang tiếng Việt, màu xanh dương làm màu hành động và nhận diện; nền trắng/xám xanh để ưu tiên ảnh, tên và giá sản phẩm.
- Desktop dùng nội dung căn giữa tối đa 1.200px; mobile thu gọn điều hướng, lưới 2 cột và đưa bộ lọc vào panel trượt.
- Mọi danh sách sản phẩm, tồn kho, giỏ hàng, đơn hàng và thông báo đều lấy từ API. Giao diện có trạng thái đang tải, lỗi API và danh sách rỗng; không tự tạo sản phẩm demo.
- Tiền hiển thị theo VND. Không cho chọn số lượng lớn hơn tồn kho trả về. API vẫn là nơi xác thực cuối cùng.
- JWT lưu ở localStorage và gắn vào request Authorization. Điều hướng theo role; backend mới là nơi kiểm tra quyền thực.

## 2. Khung giao diện dùng chung

### Announcement bar và Header
- Dải thông tin giao hàng/đổi trả ở đầu trang.
- Logo về trang chủ; menu cửa hàng, hàng mới, hàng có sẵn; ô tìm kiếm desktop và mobile.
- Tài khoản, thông báo khi đã đăng nhập, giỏ hàng kèm số lượng dòng hàng. Link quản trị chỉ xuất hiện với role ADMIN.
- Menu mobile gọn, đóng sau khi chuyển trang.

### Footer
- Link cửa hàng, thông tin hỗ trợ, chính sách cơ bản; không hứa các dịch vụ thanh toán/giao hàng ngoài API.

### Components
- ProductCard: ảnh, danh mục, tên, giá, tình trạng còn hàng; link tới chi tiết.
- Pill: nhãn trạng thái đơn có màu nhất quán.
- Empty state: biểu tượng, mô tả và hành động tiếp theo.
- Toast: xác nhận thao tác hoặc hiển thị lỗi API.
- Tiền tệ/ngày tháng định dạng theo locale Việt Nam.

## 3. Luồng khách hàng

### Trang chủ — /
- Hero giới thiệu bộ sưu tập và CTA sang cửa hàng.
- Danh mục lấy từ API categories; nếu cửa hàng chưa có danh mục, hiển thị nhãn mặc định để vẫn dẫn tới bộ lọc tương ứng.
- Sản phẩm mới gọi GET /api/products?page=0&pageSize=8&sort=newest.
- Nếu API lỗi: hiển thị nội dung nhắc khởi động backend; nếu chưa có hàng: trạng thái rỗng.

### Bộ sưu tập — /san-pham
- Tìm kiếm bằng keyword; lọc category, size, color, minPrice, maxPrice, inStock.
- Giá trị filter khả dụng lấy GET /api/products/filters.
- Sort hỗ trợ đúng backend: newest, price_asc, price_desc, name_asc.
- GET /api/products trả trang gồm items, totalElements, totalPages, first, last; phân trang dùng page/pageSize.
- Desktop hiển thị sidebar lọc; mobile dùng panel. Xóa lọc giữ lại từ khóa tìm kiếm.

### Chi tiết — /san-pham/:id
- Hiển thị ảnh, danh mục, tên, giá biến thể, mô tả và tổng tồn từ các biến thể.
- Chọn tổ hợp size/màu có trong variants; chặn tăng số lượng vượt stockQuantity.
- Thêm giỏ gửi POST /api/cart/items với variantId và quantity. Chưa đăng nhập thì chuyển đến đăng nhập rồi quay lại.
- Sản phẩm liên quan gọi GET /api/products/{id}/related?limit=4.
- Hướng dẫn size, yêu thích và album ảnh hiện chỉ là phần trình bày; backend chưa có API cho các nghiệp vụ này.

### Đăng nhập/đăng ký — /dang-nhap
- Đăng nhập POST /api/auth/login; lưu accessToken, userId, role; redirect về trang cần truy cập.
- Đăng ký POST /api/auth/register; tạo xong yêu cầu đăng nhập.
- Thông báo lỗi validation/API hiển thị qua toast. Không có chức năng quên/đặt lại mật khẩu do backend chưa có API.

### Giỏ hàng — /gio-hang
- GET /api/cart; hiển thị sản phẩm/biến thể, giá, số lượng, tồn khả dụng, tạm tính.
- PUT /api/cart/items/{id} đổi quantity; DELETE /api/cart/items/{id} xóa dòng.
- Người chưa đăng nhập được yêu cầu đăng nhập. Giỏ rỗng có CTA về cửa hàng.
- Backend hiện lưu giỏ theo tài khoản; không có giỏ ẩn danh đồng bộ.

### Checkout — /thanh-toan
- Địa chỉ giao hàng, số điện thoại; kiểm tra trường bắt buộc.
- Hai phương thức backend hỗ trợ: COD và BANKING. Chuyển khoản chỉ ghi nhận lựa chọn, không tích hợp cổng thanh toán.
- POST /api/orders với shippingAddress, phone, paymentMethod; khi thành công đi tới chi tiết đơn hàng.
- Tồn kho và giỏ do backend kiểm tra/cập nhật trong giao dịch.

### Đơn hàng — /don-hang và /don-hang/:id
- Danh sách GET /api/orders; mỗi đơn hiển thị mã, ngày tạo, số mặt hàng, tổng tiền, trạng thái.
- Chi tiết GET /api/orders/{id}; dòng hàng, địa chỉ, điện thoại, phương thức, tổng tiền.
- Tiến trình chỉ phản ánh trạng thái backend hiện có: PENDING, CONFIRMED, CANCELLED. Chưa có trạng thái giao hàng/hoàn tất hoặc API vận chuyển.

### Thông báo — /thong-bao
- GET /api/notifications; tiêu đề, nội dung, ngày, liên kết đơn.
- PUT /api/notifications/{id}/read đánh dấu đã đọc; làm mới danh sách sau thao tác.

### Tài khoản — /tai-khoan
- Lối tắt tới đơn hàng, thông báo, cửa hàng và đăng xuất.
- Backend chưa có endpoint hồ sơ cá nhân nên trang chưa cho cập nhật tên/email/điện thoại.

## 4. Khu vực ADMIN

### Bố cục
- Thanh điều hướng riêng: Tổng quan, Sản phẩm, Đơn hàng.
- Chỉ hiển thị khi session có role ADMIN; request vẫn do backend bảo vệ bằng role.

### Tổng quan — /admin
- Gọi GET /api/admin/products và GET /api/admin/orders.
- Tổng số sản phẩm, đơn hàng, đơn PENDING và tổng giá trị đơn chưa hủy.
- Danh sách đơn gần đây.
- Đây là số tổng hợp phía giao diện, không phải báo cáo doanh thu chuẩn kế toán; chưa có phân tích theo ngày, tồn kho thấp hoặc biểu đồ.

### Sản phẩm — /admin/san-pham
- GET /api/admin/products hiển thị danh mục, giá, trạng thái, size/màu/tồn mỗi biến thể.
- Tạo: POST /api/admin/products. Sửa: PUT /api/admin/products/{id}.
- Ẩn: DELETE /api/admin/products/{id}; sản phẩm được deactivate thay vì xóa lịch sử.
- Thêm biến thể: POST /api/admin/products/{productId}/variants.
- Sửa biến thể/tồn: PUT /api/admin/products/{productId}/variants/{variantId}.
- Biểu mẫu ảnh nhận image URL; chưa có upload ảnh. Form biến thể hiện dùng hộp thoại nhập nhanh, nên cần API/danh mục quản lý nâng cấp nếu vận hành quy mô lớn.

### Đơn hàng — /admin/don-hang
- GET /api/admin/orders; lọc trạng thái ở giao diện.
- PENDING có thể xác nhận qua PUT /api/admin/orders/{id}/confirm hoặc hủy qua PUT /api/admin/orders/{id}/cancel.
- Xác nhận hủy yêu cầu thao tác rõ ràng; backend hoàn tồn kho theo nghiệp vụ đã chọn.
- ADMIN xem được địa chỉ, số điện thoại, dòng hàng và tổng tiền; không có cập nhật vận đơn hoặc thanh toán trực tuyến.

## 5. API và cấu hình chạy

- Vite proxy /api tới http://localhost:8080 để chạy local; không cần CORS khi cùng dùng proxy.
- Khi triển khai frontend/backend khác origin, cấu hình CORS và VITE_API_BASE_URL theo domain môi trường.
- Chạy backend trước, sau đó pnpm install và pnpm dev; mở http://localhost:5173.
- Backend/API contract là nguồn dữ liệu và quyền truy cập chính thức.
