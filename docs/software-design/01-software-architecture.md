# 01 Kiến trúc phần mềm

## Mục đích và phạm vi

Tài liệu mô tả kiến trúc đang có của BLUE THREAD, một cửa hàng thời trang trực tuyến. Repo hiện tại chứa backend Spring Boot tại thư mục gốc; frontend React được tài liệu backend dẫn tới và đang có source tại thư mục sibling `C:\DAAAN\shopee\frontend`. Sơ đồ và mô tả phía frontend dựa trên source sibling đó. Các kết luận được đánh dấu là **đã xác minh trong mã**, **được tài liệu dự án xác nhận**, **suy ra từ cách triển khai**, hoặc **chưa xác định**.

## Tóm tắt kiến trúc

Ứng dụng dùng kiến trúc client–server, backend phân lớp. Trình duyệt chạy React/Vite và gọi REST API JSON dưới tiền tố `/api`. Backend Spring MVC tiếp nhận request, Spring Security xử lý JWT và role, controller chuyển tới service nghiệp vụ, service dùng Spring Data JPA repository để đọc/ghi cơ sở dữ liệu quan hệ.

**Đã xác minh trong mã:** backend là Java 21 / Spring Boot 3.5.11 với Spring Web, Data JPA, Security, Validation, MySQL Connector/J và JJWT; dependency được khai báo trong `pom.xml`. Frontend khai báo React 19, React Router 7 và Vite 6 trong `frontend/package.json`. `frontend/vite.config.js` chạy frontend ở cổng 5199 và proxy `/api` tới `http://localhost:8082`.

**Được tài liệu dự án xác nhận:** `src/BACKEND_SETUP.md` gọi backend là e-commerce mini backend, mô tả MySQL 8 và frontend là project riêng. Tài liệu đó nói Flyway áp dụng migration khi khởi động, nhưng cấu hình hiện tại trong `src/main/resources/application.properties` đặt `spring.flyway.enabled=false`; khi runtime dùng cấu hình này, Hibernate đặt `ddl-auto=update`. Vì vậy mặc định đã cấu hình trong source không chạy migration Flyway. Schema thực tế của môi trường triển khai chưa thể xác minh từ repo.

## Sơ đồ kiến trúc

```mermaid
flowchart LR
  U[Người dùng trên trình duyệt]
  FE[React 19 / React Router 7 / Vite]
  API[api.js: fetch JSON, localStorage JWT]
  SEC[Spring Security: JWT filter, quyền USER / ADMIN]
  CTRL[Spring MVC controllers]
  SVC[Services: nghiệp vụ và transaction]
  REPO[Spring Data JPA repositories]
  DB[(MySQL-compatible relational database)]
  RUN[ApplicationRunner / CommandLineRunner]

  U --> FE --> API -->|HTTP /api, JSON| SEC
  SEC --> CTRL --> SVC --> REPO --> DB
  RUN --> REPO
  SVC -->|Order, payment, notification changes| REPO
```

Sơ đồ thể hiện quan hệ đã thấy trong source. Việc triển khai cụ thể có thể đặt frontend và backend cùng hoặc khác origin; origin CORS cấu hình được đọc từ `APP_CORS_ALLOWED_ORIGINS`.

## Công nghệ và cấu hình xác minh được

| Lớp | Công nghệ / cấu hình | Bằng chứng |
|---|---|---|
| Frontend | JavaScript ES modules, React 19, React DOM, React Router 7, Vite 6, Tailwind CSS 4 qua Vite plugin, Lucide React | `frontend/package.json`, `frontend/vite.config.js` |
| Backend | Java 21, Spring Boot 3.5.11, Spring Web MVC, Spring Data JPA, Spring Security, Jakarta Validation, Lombok | `pom.xml`, `src/main/java/com/project_shopping/shopee/` |
| Xác thực | JWT qua JJWT 0.12.6, mật khẩu qua BCryptPasswordEncoder | `pom.xml`, `config/SecurityConfig.java`, `security/JwtService.java` |
| Database | MySQL JDBC driver; URL local mặc định trỏ `localhost:3306/clotherwebsite`; test profile dùng H2 in-memory | `application.properties`, `src/test/resources/application-test.properties` |
| Schema | JPA entity mapping; có Flyway scripts V1–V8 nhưng Flyway bị tắt trong application properties | `application.properties`, `db/migration/` |
| Build backend | Maven Wrapper; Dockerfile build bằng Maven 3.9.9 / Eclipse Temurin 21, runtime JRE 21 | `pom.xml`, `mvnw.cmd`, `Dockerfile` |
| Build frontend | npm scripts: `dev`, `build`, `preview`, `clean`; lockfile pnpm có trong sibling frontend | `frontend/package.json`, `frontend/pnpm-lock.yaml` |

**Chưa xác định:** cơ sở dữ liệu thực của môi trường đã triển khai, biến môi trường production, pipeline CI/CD, host frontend, và việc Flyway có được bật bằng cấu hình ngoài `application.properties` hay không. Source chỉ chứng minh cấu hình mặc định đã commit.

## Module và trách nhiệm

| Module | Trách nhiệm | Giao tiếp chính |
|---|---|---|
| Giao diện khách hàng | Trang chủ, danh mục, chi tiết và tìm kiếm sản phẩm, giỏ hàng, checkout, tài khoản, yêu thích, đơn hàng, thông báo | React pages gọi `frontend/src/services/*` |
| Giao diện quản trị | Quản lý danh mục, sản phẩm/biến thể, đơn hàng, tài khoản và tổng quan | Frontend admin services gọi `/api/admin/**` |
| REST controllers | Ánh xạ HTTP method/path, lấy principal, bind request DTO, áp dụng `@Valid`, gọi service | Các lớp `controller/*Controller.java` |
| Tài khoản và bảo mật | Đăng ký, đăng nhập, hồ sơ, ký/đọc JWT, nạp tài khoản và role hiện tại từ DB, chặn theo role | `AuthService`, `JwtService`, `JwtAuthenticationFilter`, `SecurityConfig` |
| Danh mục và sản phẩm | Tìm kiếm/ lọc / phân trang sản phẩm; quản lý sản phẩm, biến thể, danh mục | `ProductService`, `CategoryService`, `ProductSpecifications` |
| Giỏ hàng và đặt hàng | Giỏ theo user, kiểm tồn, khóa hàng, tạo order/item/payment và trừ tồn | `CartService`, `OrderService`, repositories |
| Vòng đời đơn hàng | Chuyển trạng thái, ghi mốc thời gian, hoàn tồn khi hủy, tạo thông báo | `OrderService` |
| Thanh toán chuyển khoản | Lưu tài khoản nhận tiền, tạo nội dung chuyển khoản, admin xác nhận thủ công | `PaymentTransactionService`, bank controllers/repositories |
| Đánh giá và yêu thích | Đánh giá sản phẩm sau đơn COMPLETED; danh sách yêu thích của USER | `ReviewService`, `WishlistService` |
| Thông báo | Liệt kê và đánh dấu đã đọc thông báo của user | `NotificationService` |
| Báo cáo quản trị | Đếm sản phẩm, tồn kho, trạng thái đơn, doanh thu completed và sản phẩm bán chạy | `AdminStatisticsService`, repositories |
| Khởi tạo dữ liệu | Tạo admin mặc định, tài khoản ngân hàng mẫu và dữ liệu demo khi cấu hình cho phép | `AdminBootstrap`, `BankDataInitializer`, `DemoDataSeeder` |
| Persistence | Entity, truy vấn Spring Data, Specification động và transaction database | `model/*`, `repository/*`, `@Transactional` |

## Luồng dữ liệu chính

1. **Duyệt catalog:** React `productService` gọi `GET /api/products`; controller nhận filter/query parameters; `ProductService.search` kiểm tra pagination/giá/sort; `ProductSpecifications.catalog` sinh predicate; `ProductRepository` trả page; service chuyển entity thành response DTO.
2. **Đăng nhập:** frontend gửi email/mật khẩu; `AuthService` tìm tài khoản và so mật khẩu BCrypt, từ chối tài khoản inactive, sau đó `JwtService` ký JWT. Với request có bearer token, filter kiểm chữ ký, lấy subject, tải user còn active từ DB và dựng principal với role đọc từ DB.
3. **Thêm giỏ:** `CartService` khóa user, giỏ và biến thể; kiểm sản phẩm ACTIVE, cộng quantity với dòng hiện tại và so với tồn; lưu giỏ và trả line subtotals cùng tổng.
4. **Checkout:** `OrderService.checkout` khóa giỏ và các biến thể theo thứ tự id; kiểm giỏ còn hàng, product ACTIVE và tồn; tính tổng theo giá biến thể; tạo order PENDING cùng item có snapshot, payment PENDING; trừ tồn; nếu BANKING thì tạo payment transaction; xóa các dòng giỏ. Các thao tác nằm trong một transaction service.
5. **Cập nhật trạng thái:** admin hoặc khách hàng sở hữu đơn gọi API tương ứng; service khóa order, kiểm owner/status, cập nhật timestamp, tạo notification. Hủy PENDING khóa lại biến thể và hoàn quantity.

## Ranh giới và điểm cần lưu ý cho kiểm thử

- Không thấy frontend server-side rendering; source vào ứng dụng qua `src/main.jsx` và `BrowserRouter`.
- Không có service gọi cổng thanh toán trong dependencies hoặc backend source. BANKING hiện tạo hướng dẫn/nội dung chuyển khoản và chờ admin xác nhận; comment nhắc Univer Dashboard Sheet nhưng không có connector sheet trong code.
- Kiểm tra role ở giao diện (`AppRoutes.jsx`) giúp điều hướng, còn quyền API được enforce ở backend `SecurityConfig`.
- Tài liệu `frontend/UI_SPEC.md` mô tả một số luồng cũ: chưa nêu API hồ sơ, reviews, wishlist, trạng thái giao hàng đầy đủ và API thống kê, trong khi các phần này có trong source hiện tại. Khi có sai khác, tài liệu này ưu tiên source và ghi nhận lệch ở mục thông tin chưa xác định.
- `BACKEND_SETUP.md` nói Flyway chạy lúc startup; `application.properties` tắt Flyway. V8 migration cũng có thể không phản ánh schema hiện hành nếu chỉ dùng cấu hình mặc định. Cần chốt chiến lược schema/migration trước khi thiết kế test repository dựa trên migration.

## Thông tin còn thiếu

- Chưa có cấu hình/ảnh chụp database của production để khẳng định schema triển khai.
- Chưa có hợp đồng ngoài hệ thống với Univer/Google Sheets; endpoint chỉ trả transaction rows và nhận thao tác xác nhận.
- Chưa có test frontend hoặc test logic backend ngoài `ShopeeApplicationTests.contextLoads`; chưa xác định coverage hiện hữu ngoài source test được liệt kê.
- Repo backend và source frontend là hai thư mục riêng; quan hệ version/commit giữa hai thư mục chưa xác minh.

