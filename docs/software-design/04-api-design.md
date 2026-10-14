# 04 Thiết kế API và giao diện giữa các module

## Quy ước chung

Các đường dẫn backend trong tài liệu này được tính từ thư mục `shopee/`; đường dẫn frontend bắt đầu bằng `frontend/`.

Backend dùng REST controllers dưới `/api`, trao đổi JSON. Endpoint được xác minh từ annotation trong `src/main/java/com/project_shopping/shopee/controller`. Trạng thái thành công dưới đây theo `@ResponseStatus`; khi method không ghi status, Spring mặc định trả 200 cho response body. Những endpoint `void` có `NO_CONTENT` trả 204.

### Xác thực và phân quyền

- Login trả `accessToken`; client gửi `Authorization: Bearer <token>`.
- JWT filter xác minh signature, trích subject và tải lại tài khoản active cùng role từ DB trên từng request.
- OPTIONS và register/login public. `GET /api/products/**`, `GET /api/categories/**`, `GET /api/bank-accounts/active` public.
- `/api/admin/**` yêu cầu ADMIN.
- `/api/cart/**`, `/api/orders/**`, `/api/notifications/**`, `/api/wishlist/**` yêu cầu USER.
- `/api/auth/me` yêu cầu USER hoặc ADMIN.
- Các API còn lại yêu cầu authenticated; `POST /api/products/{productId}/reviews` còn kiểm role USER trong service.
- CORS chỉ đăng ký `/api/**`; origins từ `APP_CORS_ALLOWED_ORIGINS` (mặc định localhost:5173), methods GET/POST/PUT/PATCH/DELETE/OPTIONS, headers Authorization/Content-Type/Accept, max-age 3600.

### Lỗi chung

`ErrorResponse = { timestamp: Instant, status: int, error: String, message: String }`.

| Nguyên nhân | HTTP | Nguồn ánh xạ |
|---|---:|---|
| DTO validation, thiếu query parameter, sai kiểu query/path, JSON/enum không parse | 400 | ApiExceptionHandler |
| ResponseStatusException | status được service chỉ định: thường 400/401/403/404/409 | ApiExceptionHandler |
| Trùng/ràng buộc persistence | 409 | DataIntegrityViolationException handler |
| Không đăng nhập | 401 | Security authentication entry point |
| Sai role | 403 | Security access denied handler |
| Database tạm thời không truy cập được trong JWT user lookup | 503 JSON | JwtAuthenticationFilter |
| Lỗi chưa xử lý | 500, message tổng quát; log exception | ApiExceptionHandler |

Validation response chọn lỗi field đầu tiên. Nội dung được ánh xạ theo mã constraint (không theo default message locale của Bean Validation), `Size` đọc min/max; field labels được localize trong `ApiExceptionHandler`. Không có schema OpenAPI/Swagger trong dependency hiện tại.

## Authentication và hồ sơ

| Method / endpoint | Request | Thành công | Controller → xử lý | Bảo vệ / lỗi chính |
|---|---|---|---|---|
| POST `/api/auth/register` | RegisterRequest: fullName, email, password bắt buộc; phone tùy chọn | 201 UserResponse | AuthController.register → AuthService.register | Public; validation 400, email trùng 409 |
| POST `/api/auth/login` | LoginRequest: email/password bắt buộc | 200 AuthResponse(accessToken,userId,role) | AuthController.login → AuthService.login | Public; sai thông tin hoặc password >72 byte UTF-8 → 401; user inactive → 403 |
| GET `/api/auth/me` | Bearer token | 200 UserResponse | AuthController.me → AuthService.profile | USER/ADMIN; token/account không hợp lệ thường 401 |
| PUT `/api/auth/me` | ProfileUpdateRequest: fullName bắt buộc ≤120, phone tùy chọn ≤30 | 200 UserResponse | AuthController.updateProfile → AuthService.updateProfile | USER/ADMIN; validation 400 |

RegisterRequest còn yêu cầu email đúng định dạng và ≤190; password DTO giới hạn 8–72 ký tự, service bổ sung giới hạn 72 byte UTF-8 để tương thích BCrypt. Role không nhận từ client, service luôn tạo USER.

## Catalog và sản phẩm

| Method / endpoint | Query/path/body | Thành công | Controller → service | Phân quyền / nhánh lỗi |
|---|---|---|---|---|
| GET `/api/products` | keyword, category, type, size, color, minPrice, maxPrice, inStock; page=0, pageSize=12, sort=newest mặc định | 200 ProductPageResponse | ProductController.getProducts → ProductService.search | Public; 400 page ngoài 0–10000, pageSize ngoài 1–100, giá âm/min>max, sort không hỗ trợ, query type mismatch |
| GET `/api/products/filters` | Không có | 200 ProductFilterOptions(categories,sizes,colors,minPrice,maxPrice) | getFilterOptions → filterOptions | Public |
| GET `/api/products/categories` | Không có | 200 List<String> | getCategories → categories | Public |
| GET `/api/products/{id}` | id path | 200 ProductResponse | getProductDetail → getActive | Public; product/category không active hoặc không có → 404 |
| GET `/api/products/{id}/related` | limit=8 mặc định, phạm vi service 1–20 | 200 List<ProductResponse> | getRelatedProducts → related | Public; product nguồn không active/category inactive → 404; limit sai → 400 |
| GET `/api/products/{productId}/reviews` | productId path | 200 List<ReviewResponse> | ReviewController.list → ReviewService.list | Public; product không tồn tại → 404 |
| POST `/api/products/{productId}/reviews` | ReviewRequest: rating 1–5, comment tùy chọn ≤1000 | 201 ReviewResponse | ReviewController.create → ReviewService.create | Authenticated; chỉ USER và order COMPLETED đã mua product; 401/403/404/409 |

Trong list endpoint, nếu `category` khác null và không blank thì dùng giá trị này; nếu không thì controller dùng `type` làm alias. Sort được hỗ trợ: newest, price_asc, price_desc, name_asc, name_desc. Filter `minPrice/maxPrice` so với giá product; size/color/stock so với cùng một variant.

## Quản trị danh mục và sản phẩm

| Method / endpoint | Request | Thành công | Controller → service | Lỗi/hiệu ứng |
|---|---|---|---|---|
| GET `/api/admin/categories` | — | 200 List<CategoryResponse> | AdminCategoryController.list → listAdmin | ADMIN |
| POST `/api/admin/categories` | CategoryRequest: name bắt buộc ≤80, description ≤500 tùy chọn, active tùy chọn | 201 CategoryResponse | create → CategoryService.create | Duplicate name 409; active null mặc định true |
| PUT `/api/admin/categories/{id}` | CategoryRequest | 200 CategoryResponse | update → update | Không có 404; duplicate tên 409; active null giữ nguyên |
| DELETE `/api/admin/categories/{id}` | id | 204 | delete → delete | Có product ref thì 409; không có thì xóa |
| GET `/api/admin/products` | — | 200 List<ProductResponse> | AdminProductController.getAllProducts → listAll | ADMIN; gồm trạng thái inactive |
| POST `/api/admin/products` | ProductRequest: name ≤180, categoryId positive, price ≥0.01, DECIMAL(12,2), số nguyên VND (`199000.00` hợp lệ; `199000.50` không hợp lệ), imageUrl ≤1000; status tùy chọn | 201 ProductResponse | createProduct → ProductService.create | Category phải tồn tại/active; DTO lỗi 400 |
| PUT `/api/admin/products/{id}` | ProductRequest | 200 ProductResponse | updateProduct → update | Product không tồn tại 404; category không hợp lệ 400 |
| DELETE `/api/admin/products/{id}` | id | 204 | deactivateProduct → deactivate | Soft delete: status INACTIVE; không xóa entity |
| POST `/api/admin/products/{productId}/variants` | VariantRequest: size ≤40/color ≤60 bắt buộc, stock ≥0, price ≥0.01, DECIMAL(12,2), số nguyên VND (`.00` hợp lệ; phần lẻ khác 0 không hợp lệ) | 201 VariantResponse | addVariant → addVariant | Product thiếu 404; validation giá 400; duplicate (product,size,color) DB conflict 409 |
| PUT `/api/admin/products/{productId}/variants/{variantId}` | VariantRequest như trên | 200 VariantResponse | updateVariant → updateVariant | Variant thiếu/không thuộc product 404; DB conflict 409 |
| DELETE `/api/admin/products/{productId}/variants/{variantId}` | ids | 204 | deleteVariant → deleteVariant | Variant sai cha/thiếu 404; lỗi xóa persistence đổi thành 409 |

## Giỏ hàng và đơn

| Method / endpoint | Request | Thành công | Controller → service | Quy tắc / lỗi chính |
|---|---|---|---|---|
| GET `/api/cart` | Bearer USER | 200 CartResponse | CartController.getCart → CartService.get | User không tìm thấy → 401; chưa có cart trả id null / items rỗng / total 0 |
| POST `/api/cart/items` | CartItemRequest: variantId và quantity positive | 201 CartResponse | addItem → CartService.add | Product inactive/quantity vượt stock → 400; variant thiếu 404 |
| PUT `/api/cart/items/{id}` | CartQuantityRequest: quantity positive | 200 CartResponse | updateItem → update | Item không thuộc cart/variant thiếu → 404; inactive/vượt stock → 400 |
| DELETE `/api/cart/items/{id}` | line id | 204 | deleteItem → delete | Item/cart không tồn tại → 404 |
| POST `/api/orders` | CheckoutRequest: shippingAddress ≤500, phone ≤30 bắt buộc; paymentMethod COD/BANKING | 201 OrderResponse | OrderController.checkout → OrderService.checkout | USER; giỏ rỗng, product inactive, variant thiếu, tồn thiếu, tổng vượt DECIMAL(14,2) → 400; BANKING không có tài khoản active → 409 |
| GET `/api/orders` | Bearer USER | 200 List<OrderResponse> | getMyOrders → myOrders | Chỉ đơn của user hiện tại |
| GET `/api/orders/{id}` | id | 200 OrderResponse | getOrderDetail → myOrder | Không có hoặc không sở hữu → 404 |
| PUT `/api/orders/{id}/cancel` | id | 200 OrderResponse | cancelOrder → cancelByCustomer | Owner và PENDING mới được hủy; khác trạng thái 409; hủy trả stock |
| PUT `/api/orders/{id}/confirm-receipt` | id | 200 OrderResponse | confirmReceipt → confirmReceipt | Owner và DELIVERED; sai owner 404, status sai 409 |
| GET `/api/admin/orders` | — | 200 List<OrderResponse> | AdminOrderController.listOrders → allOrders | ADMIN |
| GET `/api/admin/orders/{id}` | id | 200 OrderResponse | getOrderDetail → adminOrder | ADMIN; thiếu → 404 |
| PUT `/api/admin/orders/{id}/confirm` | id | 200 OrderResponse | confirmOrder → confirm | Chỉ PENDING → CONFIRMED; sai state 409 |
| PUT `/api/admin/orders/{id}/start-delivery` | id | 200 OrderResponse | startDelivery | Chỉ CONFIRMED → SHIPPING; sai state 409 |
| PUT `/api/admin/orders/{id}/mark-delivered` | id | 200 OrderResponse | markDelivered | Chỉ SHIPPING → DELIVERED; sai state 409 |
| PUT `/api/admin/orders/{id}/cancel` | id | 200 OrderResponse | cancelOrder → cancel | Chỉ PENDING; hoàn tồn; sai state 409 |

## Notifications, wishlist, ngân hàng, thống kê và user admin

| Method / endpoint | Request | Thành công | Controller → xử lý | Bảo vệ / lỗi |
|---|---|---|---|---|
| GET `/api/notifications` | Bearer USER | 200 List<NotificationResponse> | NotificationController.getNotifications → list | User thiếu → 401 |
| PUT `/api/notifications/{id}/read` | id | 200 NotificationResponse | markAsRead → markRead | Chỉ owner; không có/không sở hữu → 404 |
| GET `/api/wishlist` | Bearer USER | 200 List<ProductResponse> | WishlistController.getWishlist → get | Chỉ product/category active được liệt kê |
| POST `/api/wishlist/{productId}` | product id | 201 List<ProductResponse> | add → add | Add idempotent nếu đã có; product không active/category inactive → 404 |
| DELETE `/api/wishlist/{productId}` | product id | 204 | remove → remove | Không có row thì no-op |
| GET `/api/bank-accounts/active` | — | 200 BankAccountResponse | PublicBankController.getActiveBank | Public; chưa có active → 404 |
| POST `/api/admin/bank-accounts` | BankAccountRequest: bankCode ≤20, bankName ≤100, accountNo ≤50, accountName ≤120; không blank; qrTemplate nullable ≤20 | 201 BankAccountResponse | AdminBankController.saveBankAccount → BankAccountService.saveActiveAccount | ADMIN; trim và uppercase theo Locale.ROOT một số field; khóa các row hiện có rồi lưu row mới active |
| GET `/api/admin/bank-accounts` | — | 200 List<BankAccountResponse> | listBankAccounts | ADMIN |
| GET `/api/admin/payment-transactions` | — | 200 List<PaymentTransactionResponse> | listTransactionsForUniver → getAllTransactionsForUniver | ADMIN; mới nhất trước |
| PUT `/api/admin/payment-transactions/{id}/confirm` | id | 200 PaymentTransactionResponse | confirmPayment → confirmPaymentByAdmin | ADMIN; transaction/order thiếu 404; trạng thái không PENDING hoặc payment không phải BANKING → 409; thành công đặt transaction CONFIRMED, order CONFIRMED, payment PAID và tạo notification |
| GET `/api/admin/statistics` | — | 200 AdminStatisticsResponse | AdminStatisticsController.dashboard → dashboard | ADMIN; threshold low stock là 5; top 10 completed products |
| GET `/api/admin/users` | — | 200 List<AdminUserResponse> | AdminUserController.list → list | ADMIN |
| PUT `/api/admin/users/{id}/active` | ActiveRequest `{active: boolean}` bắt buộc | 200 AdminUserResponse | setActive → AdminUserService.setActive | ADMIN; chỉ USER; thiếu 404, target role khác USER 403 |

## DTO và validation dùng chung

Request DTO là Java records trong `dto/ApiDtos.java` và `dto/BankDtos.java`. Jakarta Validation chỉ được kích hoạt ở controller có `@Valid`. Một số rule chỉ nằm trong service (sort/pagination/stock/state/ownership) chứ không nằm ở Bean Validation. Enum không đúng chuỗi thường gây unreadable JSON 400. `ActiveRequest` ở controller riêng cũng có `@NotNull` và `@Valid`.

Response DTO tránh trả entity/password. Order response có status timeline, order lines snapshot và payment; transfer memo được lookup theo order nếu phương thức BANKING.

## Giao diện frontend ↔ backend

`frontend/src/api.js` ghép base URL (mặc định rỗng) với `/api`, gắn Authorization nếu có token và parse JSON. Frontend services gọi các module gồm hồ sơ, reviews, wishlist, order lifecycle và admin stats. Vite proxy local tại `frontend/vite.config.js` forward `/api` sang cổng 8082. `frontend/UI_SPEC.md` còn một số mô tả cũ về hồ sơ/review API và vòng đời order rút gọn; source hiện hành là bằng chứng mới hơn.

**Chưa xác định:** OpenAPI contract, versioning, pagination cho admin lists, giới hạn tốc độ, idempotency key cho checkout, API refund, xử lý callback thanh toán và hợp đồng chính xác với Univer/Sheet.

