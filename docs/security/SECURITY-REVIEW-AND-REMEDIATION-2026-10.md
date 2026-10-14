# Báo cáo rà soát và khắc phục bảo mật

Ngày rà soát: 15-10-2026  
Phạm vi mã nguồn: backend Spring Boot trong `shopee/` và frontend React trong `frontend/`; frontend được rà cùng luồng thanh toán và các điểm chèn HTML.  
Mục đích: ghi lại lỗi có căn cứ từ mã nguồn, sửa lỗi phù hợp trong phạm vi repository, và nêu rõ các điều kiện còn thiếu trước khi xác nhận hệ thống production.

Các đường dẫn backend trong báo cáo được tính từ thư mục `shopee/`.

## 1. Kết luận phạm vi

Đã sửa các lỗi nghiêm trọng có thể xử lý an toàn trực tiếp trong backend: tràn số lượng giỏ hàng, dữ liệu số lượng tồn lỗi, xác nhận lại giao dịch/đơn đã hủy, thanh toán BANKING không đồng nhất khi hủy, thiếu tài khoản ngân hàng khi chọn chuyển khoản, cấu hình secret mặc định, seeder ngoài dev, giới hạn mật khẩu BCrypt, timing dò email, ánh xạ lỗi request và các điểm rủi ro dữ liệu được xác nhận từ entity. Đã bổ sung kiểm thử luồng trên H2 và đóng gói JAR thành công.

Không thể xác nhận “đã xử lý ít nhất 90% toàn bộ lỗi nghiêm trọng” một cách có căn cứ: danh sách đầu vào gộp lẫn lỗi ứng dụng, rủi ro phụ thuộc hạ tầng, đề xuất nghiệp vụ, vấn đề hiệu năng và CVE có điều kiện khai thác; repository cũng không có production schema/database để hoàn tất chuyển đổi an toàn. Các mục release-blocking còn lại được liệt kê ở mục 4. Vì vậy báo cáo này không xác nhận production đã an toàn hoặc đang chạy ổn định.

## 2. Các lỗi đã sửa

| Mức độ | Phát hiện đã xác minh | Thay đổi | Căn cứ mã nguồn |
|---|---|---|---|
| Critical | Cộng số lượng bằng `int` có thể tràn; checkout có thể chấp nhận dòng giỏ âm từ dữ liệu cũ và tạo tổng âm | Cộng bằng `long`, từ chối số lượng/stock âm hoặc không dương, kiểm tra giá và tổng đơn trước khi trừ kho | `CartService.add/update`, `OrderService.checkout/restoreInventory` |
| Critical | Xác nhận giao dịch chuyển khoản không khóa và có thể đưa đơn đã hủy hoặc đã tiến triển về `CONFIRMED` | Khóa đơn rồi giao dịch; chỉ nhận `PENDING` transaction + `PENDING` order + payment `BANKING`; đồng bộ thời điểm và cập nhật `PAID` trong transaction | `PaymentTransactionService.confirmPaymentByAdmin`, `OrderRepository.findByIdForUpdate`, `PaymentTransactionRepository.findByIdForUpdate` |
| Critical | Hủy đơn không đóng giao dịch pending; payment BANKING vẫn mang PENDING dù order đã CANCELLED | Khi hủy, khóa và chuyển mọi giao dịch `PENDING` sang `REJECTED`; payment BANKING chuyển sang `FAILED`; cập nhật order/trạng thái/hoàn kho cùng transaction | `OrderService.cancelPendingOrder`, `PaymentTransactionRepository.findAllByOrderIdForUpdate` |
| High | Chọn BANKING khi không có tài khoản ngân hàng vẫn tạo giao dịch với số tài khoản giả `N/A` | Từ chối checkout với HTTP 409 nếu không có bank active | `PaymentTransactionService.createTransactionForOrder` |
| High | Mã memo tạo từ `Random`, ngắn và không gắn mã đơn; chuyển `BigDecimal` sang `long` có thể cắt phần lẻ/tràn | Dùng `SecureRandom`, mã 8 ký tự và mã đơn trong memo; chỉ chấp nhận số tiền VND nguyên và chuyển đổi chính xác bằng `longValueExact()` | `PaymentMemoUtil.generateRandomCode`, `PaymentTransactionService.createTransactionForOrder` |
| High | Lưu tài khoản ngân hàng mới vô hiệu hóa bản cũ bên ngoài transaction | Chuyển thao tác vào `BankAccountService.saveActiveAccount()` có `@Transactional`; controller gọi service | `BankAccountService`, `AdminBankController.saveBankAccount` |
| High | Production guard trước chạy sau Hibernate DDL và bắt buộc giữ bootstrap password admin mọi lần khởi động | Chuyển guard sang `EnvironmentPostProcessor` đăng ký qua `spring.factories` để kiểm tra trước khi tạo datasource/EntityManager; bootstrap admin giờ là cấu hình tùy chọn một lần, cặp email/password phải đầy đủ nếu dùng | `config/ProductionConfigurationGuard`, `src/main/resources/META-INF/spring.factories`, `AdminBootstrap` |
| High | Bootstrap tự kích hoạt lại admin hiện hữu bị vô hiệu hóa | Giữ nguyên `active` của tài khoản admin hiện hữu; kiểm tra mật khẩu bootstrap 8–72 byte UTF-8 | `AdminBootstrap` |
| High | Mật khẩu đăng ký quá ngắn hoặc dài hơn giới hạn byte của BCrypt chưa được chặn nhất quán | Bổ sung giới hạn DTO và kiểm tra 8 ký tự tối thiểu, tối đa 72 byte UTF-8 ở service; áp dụng tương tự khi tạo admin | `ApiDtos.RegisterRequest`, `AuthService.validatePassword`, `AdminBootstrap` |
| Medium/High | Login không tồn tại email bỏ qua BCrypt, làm khác thời gian xử lý và hỗ trợ dò email | Thực hiện BCrypt `matches` trên hash dummy trước khi trả lỗi xác thực chung | `AuthService.login` |
| Medium | Lỗi route/phương thức/media type và lỗi tranh chấp DB có thể rơi vào handler tổng quát 500 | Thêm handler trả 404/405/406/415 tương ứng; tranh chấp khóa trả 503 có thông điệp thử lại | `ApiExceptionHandler` |
| Medium | Image Docker chạy root và bỏ qua toàn bộ tests | Chạy `mvn verify` ở build stage và chạy image bằng UID/GID không đặc quyền `10001` | `Dockerfile` |
| Medium | Response đơn hàng giả định chỉ có một payment transaction dù entity cho phép nhiều dòng | Response chọn transaction mới nhất; hủy đơn khóa và từ chối toàn bộ dòng pending. Kịch bản dữ liệu nhiều giao dịch được đưa vào kiểm thử | `PaymentTransactionRepository.findFirstByOrderIdOrderByCreatedAtDesc`, `OrderService.toResponse/cancelPendingOrder` |
| Medium | Login password vượt quá 72 byte có thể làm BCrypt ném exception; lỗi DB ở JWT filter không có JSON response chuẩn | Login password quá dài dùng dummy hash và trả 401; lookup lỗi DB qua JWT filter trả 503 JSON, không bị đổi thành 401 | `AuthService.login`, `JwtAuthenticationFilter` |
| Medium | Dữ liệu có thể vượt precision/length đã map trong DB và gây lỗi persistence | Thêm `@Digits(10,2)` cho giá theo DECIMAL(12,2), thêm `@Size` cho bank fields theo độ dài entity; không đặt cận nghiệp vụ tùy ý | `dto/ApiDtos.java`, `dto/BankDtos.java`, `model/Product.java`, `model/ProductVariant.java`, `model/BankAccount.java` |
| High | Giá có phần lẻ được chấp nhận dù BANKING chỉ chuyển tổng VND nguyên bằng `toBigIntegerExact()` | Thêm `@WholeVndAmount` ở giá product/variant; cho qua dạng `.00`, từ chối phần lẻ khác 0 trước persistence; thêm hồi quy MockMvc | `validation/WholeVndAmountValidator.java`, `dto/ApiDtos.java`, `CriticalBusinessFlowTests` |
| High | Tổng checkout có thể vượt `DECIMAL(14,2)` ở order/subtotal/payment khi giá và quantity lớn | So sánh tổng với `999999999999.99` trước khi tạo order hoặc trừ tồn; thêm hồi quy checkout xác nhận order/stock không đổi | `OrderService.checkout`, `CustomerOrder`, `OrderItem`, `Payment`, `CriticalBusinessFlowTests` |
| Medium | API lỗi validation phụ thuộc câu mặc định tiếng Anh và Size min/max bị diễn đạt sai | Ánh xạ theo mã constraint; Size dùng min/max arguments; thêm nhãn bank fields | `ApiExceptionHandler.validationMessage/sizeConstraint` |
| Medium | Product catalog gây tải thêm category/variants và trang có thứ tự không ổn định khi bằng giá/thời gian/tên | Entity graph cho category, batch fetch variants, thêm id tie-breaker cho mọi sort; escape `\\`, `%`, `_` trong LIKE; chuẩn hóa bằng `Locale.ROOT` | `ProductRepository.findAll`, `ProductService.search`, `ProductSpecifications`, `application.properties` |
| Medium | Lỗi xóa variant bất kỳ bị báo nhầm là lỗi foreign key; các Optional finder có thể yêu cầu một dòng dù entity cho phép nhiều giao dịch/transfer memo | Chỉ đổi `DataIntegrityViolationException` thành 409; bỏ các finder Optional không được dùng, response dùng query transaction mới nhất | `ProductService.deleteVariant`, `PaymentTransactionRepository` |
| Medium | Memo của tên chỉ có ký tự ngoài Latin có thể thiếu tên; BANKING xác nhận thiếu notification | Dùng “AO” khi strip accent cho chuỗi rỗng; tạo notification khi xác nhận BANKING; mốc xác nhận không lùi so createdAt | `PaymentTransactionService`, `PaymentMemoUtil` |
| Medium | Truy vấn top sản phẩm nhóm cả product id và snapshot name, nên cùng một sản phẩm có thể chiếm nhiều dòng sau khi đổi tên | Gộp theo product id, lấy một tên snapshot bằng `MAX(COALESCE(...))`, và sắp xếp phụ theo id để ổn định; thêm hồi quy cho 2 snapshot cùng sản phẩm | `OrderRepository.findTopSellingProducts`, `CriticalBusinessFlowTests` |
| Medium | Runtime timezone phụ thuộc JVM; lock timeout mặc định có thể kéo dài; test database dùng chung tên H2; Surefire agent path không quote | Cấu hình Hibernate UTC, lock timeout 5000 ms, DB test dùng random UUID, quote javaagent path | `application.properties`, `application-test.properties`, `pom.xml` |

## 3. Kiểm thử và kiểm tra đã thực hiện

- Lệnh: `.\mvnw.cmd -o verify` (offline Maven).
- Kết quả cuối: `BUILD SUCCESS`; 8 test chạy, 0 failure, 0 error, 0 skipped; tạo `target/shopee-0.0.1-SNAPSHOT.jar`.
- Luồng MockMvc/H2 kiểm tra: API yêu cầu xác thực; sai route/method/content type; đăng ký và từ chối mật khẩu yếu; login password >72 byte trả 401; từ chối số lượng gây tràn; giá `199000.00` được chấp nhận, phần lẻ `.50` bị từ chối ở product/variant; tổng checkout vượt DECIMAL(14,2) không tạo order hoặc giảm stock; checkout từ chối dòng giỏ cũ âm; BANKING bị từ chối khi chưa có tài khoản nhận; phân quyền endpoint ngân hàng; đổi tài khoản active; hủy đơn chuyển payment BANKING sang FAILED và từ chối mọi transaction pending; từ chối xác nhận giao dịch đã bị hủy; xác nhận giao dịch BANKING còn hiệu lực, đồng bộ `PAID` và tạo notification; luồng COD qua `PENDING → CONFIRMED → SHIPPING → DELIVERED → COMPLETED`; thống kê top sản phẩm không tách cùng id theo tên snapshot.
- `git diff --check` không báo whitespace error; có cảnh báo chuẩn hóa LF/CRLF của Git trên Windows.
- Kiểm thử dùng H2, không phải MySQL. Không có MySQL listener ở cổng 3306 nên chưa xác minh dialect, row lock, deadlock, transaction isolation hoặc migration trên MySQL.
- Docker CLI có nhưng Docker Desktop engine từ chối kết nối named pipe; chưa build/run container.
- Không có quyền truy cập production server/database trong lượt này; không triển khai hoặc chạy migration lên dữ liệu thật.

## 4. Rủi ro còn tồn tại và điều kiện cần trước production

### P0 — Chốt schema hiện hữu trước khi thay đổi migration

Runtime vẫn đặt `spring.flyway.enabled=false` và `spring.jpa.hibernate.ddl-auto=update`. V9 hiện bổ sung `CREATE TABLE IF NOT EXISTS` cho `bank_accounts` và `payment_transactions` theo entity nhưng chưa được áp dụng; bật Flyway/đổi sang `validate` mà không đối chiếu schema thật có thể làm ứng dụng production không khởi động hoặc dữ liệu không tương thích. Rủi ro cập nhật cột `users.active`/`products.category_id` trên DB cũ cũng chưa thể đánh giá nếu không có dump/schema version. Chưa thêm CHECK/UNIQUE mới vì chưa có bản chụp schema production và kiểm kê dữ liệu hiện có. Cần dump schema/dữ liệu đã ẩn thông tin nhạy cảm, kiểm tra V1–V9 theo trạng thái thật, dry-run trên bản sao, rồi mới bật Flyway và `ddl-auto=validate`.

### P0 — Giới hạn đăng nhập/đăng ký/checkout ở lớp dùng chung

Chưa có rate limiting hoặc khóa tạm tài khoản. Chặn đúng giữa nhiều instance cần reverse proxy/API gateway hoặc kho đếm dùng chung (ví dụ Redis); không thêm limiter in-memory đơn giản vì nó có thể bị né qua nhiều instance và tạo giới hạn không nhất quán. Cần quyết định ngưỡng, key theo IP/user và trust proxy trước khi cấu hình.

### P0 — MySQL concurrency test và cấu hình chạy thật

H2 không xác nhận được locking semantics của MySQL. Cần chạy Testcontainers/MySQL hoặc staging riêng để kiểm tra đồng thời hai checkout trên cùng tồn kho, hủy đơn đối đầu với xác nhận payment, và hai lần đổi bank active song song. `BankAccountService` khóa các row hiện có trong transaction; chưa có unique constraint/khóa singleton trong schema để bảo đảm duy nhất một active account ở mọi dialect hoặc khi bảng chưa có row.

### P1 — Đơn BANKING pending có thể giữ tồn vô thời hạn

Không thêm job tự hủy đơn vì source không quy định thời hạn thanh toán. Đây là rủi ro cạn kho/DoS nghiệp vụ. Chủ sản phẩm cần xác nhận thời hạn và cách xử lý giao dịch đến trễ; sau đó thiết kế job có khóa phân tán nếu chạy nhiều instance, hoàn kho đúng một lần và kiểm thử race.

### P1 — JWT 24 giờ, không refresh/revoke; chưa có luồng đổi/khôi phục mật khẩu và xác minh email

Các chức năng này chưa có trong frontend/backend. Không tự đổi TTL vì có thể làm gián đoạn phiên và hiện không có refresh flow; cần quyết định trải nghiệm đăng nhập, thu hồi token và migration lưu token version trước khi triển khai.

### P1 — Dependency lifecycle và CVE scan chưa hoàn tất

Backend đang build với Spring Boot 3.5.11 (theo `pom.xml`/test runtime). `mvn dependency:tree` không chạy offline vì Maven dependency plugin không có trong cache; không có SBOM/OWASP Dependency-Check/Dependabot kết quả để kết luận toàn bộ thư viện có sạch CVE. Một số advisory Spring giai đoạn 2026 có điều kiện kích hoạt riêng; source hiện không có Actuator, `@ProjectedPayload`, native SQL query dùng sort không tin cậy, WebAuthn hoặc OAuth Authorization Server, nên các advisory tương ứng chưa chứng minh là khai thác được ở cấu hình này. Cần chạy dependency/SBOM scan có mạng và review các đường gọi thực tế trước khi nâng version.

Spring công bố Boot 3.5.16 là bản OSS cuối của nhánh 3.5 vào 25-06-2026; vì vậy 3.5.11 đang cũ so với bản vá OSS cuối của nhánh này. Không nâng major hoặc thay version trong lượt này vì dependencies mới không có sẵn offline và chưa thể chạy full verification tương thích. Xem [thông báo Spring Boot 3.5.16](https://spring.io/blog/2026/06/25/spring-boot-3-5-16-available-now/) và [chính sách hỗ trợ Spring](https://spring.io/support-policy).

### P2 — Hiệu năng/tăng trưởng dữ liệu

Catalog đã nạp trước category, batch-fetch variants và thêm thứ tự tie-break cho trang. Chưa sửa phân trang danh sách admin (`allOrders`, `listAll`, `NotificationService.list`, `BankAccountService.listAccounts`, `AdminUserService.list`, transaction dashboard), N+1 khi tải danh sách đơn, index tìm kiếm, cache filter/dashboard, hoặc truy vấn email không tận dụng index do `IgnoreCase`. Phân trang các endpoint list cần giữ tương thích frontend và cần chốt API contract. Đây là rủi ro tải/tăng trưởng cần profile bằng dữ liệu MySQL thật.

### Đã kiểm tra nhưng chưa thấy lỗi trong phần đã quét

- Security config dùng JWT stateless và phân vai; endpoint ngân hàng admin được kiểm tra 403 trong MockMvc.
- Logic kiểm tra quyền sở hữu đơn hàng dùng `findByIdAndUserId`/`requireOwner`; các truy vấn tìm kiếm dùng JPA Criteria/parameter binding, không thấy native SQL nhận sort tùy ý.
- Frontend checkout chỉ gửi phương thức thanh toán, không đọc/chuyển memo; lượt tìm sink trong mã `.js/.jsx/.ts/.tsx` không thấy `dangerouslySetInnerHTML` hoặc `innerHTML`. Đây là rà soát tĩnh giới hạn, không phải kiểm thử XSS đầy đủ.

## 5. Thứ tự triển khai tiếp theo

1. Chụp schema production an toàn, đối chiếu V1–V9 và xác minh V9 cùng các CHECK/UNIQUE cần thiết; sửa migration theo thực trạng và dry-run trên bản sao trước khi bật Flyway.
2. Cấu hình rate limit tại gateway/proxy hoặc Redis dùng chung; sau đó kiểm thử login/register/checkout cả khi chạy nhiều instance.
3. Chạy kiểm thử tích hợp và race trên MySQL, bao gồm tồn kho, hủy/xác nhận, và cập nhật tài khoản nhận tiền đồng thời.
4. Chốt chính sách hết hạn giao dịch, JWT/refresh/revoke, xác minh email và khôi phục mật khẩu; triển khai cùng UI/API.
5. Sinh SBOM, chạy scanner phụ thuộc và cập nhật Spring/dependencies theo advisory tương thích; chạy lại toàn bộ pipeline.
6. Build/run image trên Docker engine và staging có TLS MySQL; kiểm tra health, startup, checkout, payment, hủy/hoàn kho và luồng giao nhận trước khi phát hành.

## 6. Nguồn kiểm chứng

- Spring Boot 3.5.12 advisory/release: https://spring.io/blog/2026/03/19/spring-boot-3-5-12-available-now/
- Spring Data Commons CVE-2026-41721: https://spring.io/security/cve-2026-41721/
- Spring Data JPA CVE-2026-47834: https://spring.io/security/cve-2026-47834/
- Spring Boot 3.5.16: https://spring.io/blog/2026/06/25/spring-boot-3-5-16-available-now/
- Spring support policy: https://spring.io/support-policy
