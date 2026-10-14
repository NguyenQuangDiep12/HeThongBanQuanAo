# 05 Danh mục đơn vị cần kiểm thử

## Mục tiêu và phạm vi

Danh mục ưu tiên các nhánh có nghiệp vụ, dữ liệu, quyền hoặc side effect. Mã nguồn test hiện có chỉ là `ShopeeApplicationTests.contextLoads()` chạy Spring context với profile test; chưa thấy test unit cho service, controller, filter/query hoặc frontend trong các thư mục source đã rà soát. `pom.xml` có Spring Boot Test và H2; frontend `package.json` không khai báo test script/test runner. Theo yêu cầu, tài liệu này chỉ đề xuất test, không tạo hay chạy test code.

Mỗi mục ghi đơn vị, đầu vào/đầu ra, nhánh chính, mock/fixture, case bất thường và ưu tiên. **P0** là logic có thể làm sai tiền/tồn/trạng thái đơn; **P1** là xác thực/quyền/dữ liệu catalog và luồng mua; **P2** là logic hỗ trợ, báo cáo và xử lý lỗi; **P3** là khởi tạo hoặc giao diện ít rủi ro hơn. Mức ưu tiên là đề xuất dựa trên tác động nếu sai, không phải phân loại do source khai báo.

## P0 — Checkout, tồn kho, vòng đời đơn và xác nhận thanh toán

### UT-ORD-01 — OrderService.checkout

- **Module / vị trí:** đặt hàng; `src/main/java/com/project_shopping/shopee/service/OrderService.java:41`.
- **Đầu vào / đầu ra:** email user + `CheckoutRequest(shippingAddress, phone, paymentMethod)` → `OrderResponse`.
- **Logic cần bao phủ:** cart vắng hoặc rỗng; cart có nhiều variant; variant lock thiếu; product INACTIVE; stock vừa đủ / thiếu; total dùng variant price; địa chỉ/phone trim; order PENDING; snapshot line; trừ tồn; payment COD/BANKING; BANKING tạo transaction; cart clear và updatedAt.
- **Mock/fixture:** UserRepository, CartRepository, VariantRepository, OrderRepository, PaymentTransactionService, PaymentTransactionRepository, NotificationRepository; fixture với nhiều variant và order item.
- **Case bất thường:** lỗi khi tạo transaction/lưu order/variant xảy ra thì kiểm tra rollback ở integration test; variant/cart bị xóa; tồn đổi trước checkout; tổng BigDecimal scale/giá lớn; cart line quantity <=0 chỉ kiểm qua DTO/persistence vì service nhận được object bất kỳ.
- **Ưu tiên / lý do:** P0 — một lỗi có thể ghi sai tổng tiền, trừ sai stock hoặc xóa giỏ mà không tạo đơn.

### UT-ORD-02 — OrderService.confirm, startDelivery, markDelivered, confirmReceipt

- **Vị trí:** `OrderService.java:113, 124, 135, 146`.
- **Đầu vào / đầu ra:** order id, thêm email cho confirmReceipt → OrderResponse.
- **Logic cần bao phủ:** từng trạng thái hợp lệ: PENDING→CONFIRMED→SHIPPING→DELIVERED→COMPLETED; ghi timestamp; mỗi chuyển trạng thái tạo đúng một notification; transition từ status khác trả 409; order thiếu 404; confirmReceipt kiểm owner trước status.
- **Mock/fixture:** OrderRepository với `findByIdForUpdate`/`findDetailedById`, UserRepository, VariantRepository khi liên quan, NotificationRepository; dùng OrderStatus enum fixture.
- **Case bất thường:** order khác owner phải biểu hiện 404; `timestampAfter` khi clock sớm hơn previous → 409; previous null; timestamp bằng previous được chấp nhận; lỗi notification phải rollback state khi chạy integration test.
- **Ưu tiên / lý do:** P0 — trạng thái điều khiển khả năng hủy, giao hàng và hoàn tất đơn.

### UT-ORD-03 — OrderService.cancelByCustomer, cancelPendingOrder, restoreInventory

- **Vị trí:** `OrderService.java:159, 165, 172, 182`.
- **Đầu vào / đầu ra:** email/id hoặc CustomerOrder → OrderResponse.
- **Logic cần bao phủ:** chỉ owner + PENDING; admin cancel cũng chỉ PENDING; stock được cộng đúng theo từng order line; lock variant theo id tăng; notification và cancelledAt/status; các status khác → 409.
- **Mock/fixture:** UserRepository, OrderRepository, VariantRepository, NotificationRepository; order nhiều variant với tồn hiện tại khác nhau.
- **Case bất thường:** order không thuộc user → 404; variant đã mất → 409; `Math.addExact` tràn số; order line quantity sai; lỗi ở một variant sau khi variant trước đã được cập nhật cần transaction rollback.
- **Ưu tiên / lý do:** P0 — hoàn stock sai gây bán vượt kho hoặc thất thoát tồn.

### UT-PAY-01 — PaymentTransactionService.confirmPaymentByAdmin

- **Vị trí:** `src/main/java/com/project_shopping/shopee/service/PaymentTransactionService.java:76`.
- **Đầu vào / đầu ra:** transaction id → `PaymentTransactionResponse`.
- **Logic cần bao phủ:** không tìm thấy → 404; CONFIRMED → 400; PENDING → transaction CONFIRMED, order CONFIRMED và payment PAID; không có payment; kiểm tra order/payment/transaction được lưu và timestamp.
- **Mock/fixture:** PaymentTransactionRepository, BankAccountRepository, OrderRepository; CustomerOrder với status/payment tương ứng.
- **Case bất thường / hành vi cần chốt:** source cho phép xác nhận transaction REJECTED và không guard order status trước khi set CONFIRMED; không tạo notification và gọi Instant.now nhiều lần. Viết test characterization cho hành vi hiện tại, sau đó chủ sở hữu nghiệp vụ cần quyết định có giới hạn transition hay không.
- **Ưu tiên / lý do:** P0 — xác nhận sai có thể đánh dấu đơn chưa trả tiền thành đã trả hoặc đưa order status lùi.

## P1 — Danh tính, phân quyền, giỏ hàng và catalog

### UT-AUTH-01 — AuthService.register và login

- **Vị trí:** `service/AuthService.java:33, 66`.
- **Đầu vào / đầu ra:** RegisterRequest/LoginRequest → UserResponse/AuthResponse.
- **Logic cần bao phủ:** chuẩn hóa tên/email; kiểm tra email trùng ignore-case; role cố định USER; hash password; sai email/sai password cùng trả 401; inactive đúng password trả 403; active trả token và role.
- **Mock:** UserRepository, PasswordEncoder, JwtService.
- **Case bất thường:** email blank/format sai do DTO validation; email có whitespace/case; race duplicate DB unique violation; password không được lưu plain text; repository trả empty.
- **Ưu tiên / lý do:** P1 — sai logic mở tài khoản hoặc cho inactive user đăng nhập ảnh hưởng truy cập toàn ứng dụng.

### UT-SEC-01 — JwtService và JwtAuthenticationFilter

- **Vị trí:** `security/JwtService.java:28, 51, 70, 84`; `security/JwtAuthenticationFilter.java:34, 44, 61, 75, 91`.
- **Đầu vào / đầu ra:** cấu hình secret/expiry; bearer header + request → SecurityContext được set hoặc để trống.
- **Logic cần bao phủ:** secret <32 byte và expiry ≤0 bị từ chối; secret rỗng tạo khóa tạm; token subject/expiration/signature; bỏ qua route đã liệt kê; header thiếu/sai prefix; token hợp lệ nạp role từ DB; user inactive/không tồn tại không authenticate; JWT lỗi clear context.
- **Mock:** UserRepository, JwtService; request/response/filter chain và SecurityContext.
- **Case bất thường:** token hết hạn, sửa chữ ký, bearer rỗng, header viết thường `bearer ` (source yêu cầu prefix đúng `Bearer `), context đã có auth; token role claim khác role DB.
- **Ưu tiên / lý do:** P1 — nền tảng cho mọi quyền API; test route matcher thêm qua MockMvc.

### UT-SEC-02 — SecurityConfig.securityFilterChain

- **Vị trí:** `config/SecurityConfig.java:42, 54, 63, 127`.
- **Đầu vào / đầu ra:** HttpSecurity, filter, ObjectMapper → filter chain.
- **Logic cần bao phủ:** OPTIONS/public routes, route USER, route ADMIN, `/api/auth/me`, anyRequest authenticated; 401/403 JSON ErrorResponse; CORS + stateless + CSRF disabled.
- **Mock / test seam:** MockMvc + Spring Security test helpers; JWT filter mock/injected test configuration.
- **Case bất thường:** anonymous review POST, ADMIN vào API USER, USER vào admin, inactive account token, unknown `/api/**` route.
- **Ưu tiên / lý do:** P1 — controller/service unit test không xác minh authorization filter chain.

### UT-CART-01 — CartService.add/update/delete

- **Vị trí:** `service/CartService.java:48, 106, 147`.
- **Đầu vào / đầu ra:** email, CartItemRequest hoặc line id/CartQuantityRequest → CartResponse/void.
- **Logic cần bao phủ:** add tạo cart; variant line mới; variant lặp cộng quantity; tồn vừa đủ/vượt; product ACTIVE/INACTIVE; update theo đúng user; delete line; cart total từ variant price.
- **Mock:** UserRepository, CartRepository, VariantRepository.
- **Case bất thường:** user/variant/cart/line không có; line id thuộc user khác; quantity null/0 bị DTO chặn; cạnh tranh update tồn; unique cart-user/cart-variant.
- **Ưu tiên / lý do:** P1 — tác động tới trải nghiệm giỏ và là input trực tiếp cho checkout.

### UT-CAT-01 — ProductService.search và validatePagination

- **Vị trí:** `service/ProductService.java:51, 160`.
- **Đầu vào / đầu ra:** filter scalars + page/pageSize/sort → ProductPageResponse.
- **Logic cần bao phủ:** biên page 0/10000; size 1/100; giá null/0/âm; min=max/min>max; năm loại sort; sort case-insensitive; mặc định newest khi null; mapping metadata Page.
- **Mock:** ProductRepository SpecificationExecutor trả Page<Product>; ProductSpecifications có thể assert được truyền filter tương ứng.
- **Case bất thường:** page/size ngoài biên, unknown sort, BigDecimal scale, giá min lớn hơn max; category blank/type alias do controller.
- **Ưu tiên / lý do:** P1 — lỗi lọc có thể làm sai danh mục và có thể gây truy vấn quá lớn.

### UT-CAT-02 — ProductSpecifications.catalog

- **Vị trí:** `repository/ProductSpecifications.java:19`, helper từ dòng 83 trở đi.
- **Đầu vào / đầu ra:** các filter nullable → JPA Specification predicate.
- **Logic cần bao phủ:** luôn ACTIVE product/category; keyword trên name/description/category; escape `%` và `_`; category/size/color ignore-case; inclusive price; size/color/stock phải cùng một variant; false-only nghĩa “không có variant stock dương”; false cộng size/color nghĩa match variant stock 0.
- **Mock / test seam:** ưu tiên `@DataJpaTest` / repository integration dùng entity fixture; Criteria API mock thường khó kiểm semantic.
- **Case bất thường:** keyword blank, category blank, product không variants, nhiều variant khác nhau khớp từng filter riêng, stock = 0/1.
- **Ưu tiên / lý do:** P1 — query động là nơi dễ sai AND/EXISTS và trả sản phẩm không thỏa tổ hợp lọc.

### UT-PROD-01 — ProductService CRUD và soft delete

- **Vị trí:** `service/ProductService.java:237, 255, 265, 286, 328`.
- **Đầu vào / đầu ra:** id/ProductRequest → ProductResponse/void.
- **Logic cần bao phủ:** public detail chỉ product/category active; admin list gồm mọi status; create category active và default ACTIVE; update status null giữ trạng thái; deactivate thành INACTIVE.
- **Mock:** ProductRepository, CategoryService.
- **Case bất thường:** product thiếu, category inactive/thiếu, trim tên, status null vs ACTIVE/INACTIVE, giá/description/image truyền qua.
- **Ưu tiên / lý do:** P1 — dữ liệu catalog là cơ sở của filter, cart và checkout.

### UT-PROD-02 — ProductService variant mutations

- **Vị trí:** `service/ProductService.java:345, 373, 415`.
- **Đầu vào / đầu ra:** product/variant id + VariantRequest → VariantResponse/void.
- **Logic cần bao phủ:** create variant, trim size/color, update phải đúng parent product, stock/price cập nhật, delete thành công.
- **Mock:** ProductRepository, VariantRepository.
- **Case bất thường:** product/variant thiếu, mismatched parent 404, duplicate size/color, delete variant đã FK reference → 409.
- **Ưu tiên / lý do:** P1 — biến thể xác định giá và stock thực khi mua.

### UT-CAT-03 — CategoryService.create/update/delete/requireActive

- **Vị trí:** `service/CategoryService.java:43, 57, 79, 93, 116`.
- **Đầu vào / đầu ra:** CategoryRequest/id → CategoryResponse/Category/void.
- **Logic cần bao phủ:** trim name/description; description blank→null; duplicate name ignore-case; create active null→true; update active null giữ cũ; delete category có product bị chặn; requireActive thiếu/inactive → 400.
- **Mock:** CategoryRepository, ProductRepository.
- **Case bất thường:** case-only duplicate; không sản phẩm; tên trùng chính nó khi update; count product trả về.
- **Ưu tiên / lý do:** P1 — category inactive làm sản phẩm ẩn khỏi catalog và chặn cập nhật product.

### UT-REV-01 — ReviewService.create

- **Vị trí:** `service/ReviewService.java:40`.
- **Đầu vào / đầu ra:** email/product id/ReviewRequest → ReviewResponse.
- **Logic cần bao phủ:** user tồn tại và role USER; product tồn tại; có order COMPLETED với product; chưa review cũ; lưu đúng rating/comment.
- **Mock:** UserRepository, ProductRepository, OrderRepository, ProductReviewRepository.
- **Case bất thường:** role ADMIN → 403; chưa mua/đơn chưa COMPLETED → 403; product/user không tồn tại; duplicate → 409; unique race DB conflict; product INACTIVE hiện không bị chặn trong service.
- **Ưu tiên / lý do:** P1 — quyền tạo nội dung và ràng buộc mua hàng.

### UT-ADMIN-01 — AdminUserService.setActive

- **Vị trí:** `service/AdminUserService.java:25`.
- **Đầu vào / đầu ra:** user id/active/adminEmail → AdminUserResponse.
- **Logic cần bao phủ:** chỉ role USER; chặn tự vô hiệu hóa theo email ignore-case; bật/tắt thành công; trả trạng thái mới.
- **Mock:** UserRepository.
- **Case bất thường:** thiếu id→404; ADMIN/role null→403 (role enum không null theo mapping); admin casing/whitespace; admin khác tài khoản hiện tại vẫn không thể bị đổi do chỉ USER.
- **Ưu tiên / lý do:** P1 — thay đổi active ảnh hưởng JWT filter và khả năng truy cập.

### UT-BANK-01 — AdminBankController.saveBankAccount

- **Vị trí:** `controller/AdminBankController.java:36`.
- **Đầu vào / đầu ra:** BankAccountRequest → BankAccountResponse 201.
- **Logic cần bao phủ:** trim/uppercase bank code, uppercase account name; qrTemplate null thành “compact”; tạo row mới active; list/public query chọn active row cao id.
- **Mock:** BankAccountRepository.
- **Case bất thường / hành vi cần chốt:** existing rows bị setActive(false) nhưng không được save rõ ràng; controller không `@Transactional`. Test persistence để xác minh có nhiều row active hay không; tránh chỉ kiểm object mock.
- **Ưu tiên / lý do:** P1 — checkout chuyển khoản phụ thuộc account được chọn; hành vi single-active chưa được đảm bảo bởi source persistence.

### UT-PAY-02 — createTransactionForOrder và PaymentMemoUtil

- **Vị trí:** `service/PaymentTransactionService.java:43`; `util/PaymentMemoUtil.java:16, 30`.
- **Đầu vào / đầu ra:** CustomerOrder → PaymentTransaction; tên/mã/amount → string.
- **Logic cần bao phủ:** không item fallback AO; lấy snapshot hoặc product current name; strip dấu/ký tự; cắt 12; nhiều item append PLUS; random code length 4; amount longValue; account active mới nhất hoặc N/A; order code format; transaction status default PENDING.
- **Mock:** BankAccountRepository, PaymentTransactionRepository; util random không cần mock nếu assert pattern/length.
- **Case bất thường:** tên null/blank/emoji/tiếng Việt/độ dài biên; amount có lẻ (memo bỏ phần thập phân nhưng transaction giữ BigDecimal); length âm/0; collision code; không có bank.
- **Ưu tiên / lý do:** P1 — lỗi memo hoặc account gây đối soát chuyển khoản nhầm.

## P2 — Wishlist, notifications, thống kê, mapping lỗi và frontend

### UT-WISH-01 — WishlistService.add/get/remove

- **Vị trí:** `service/WishlistService.java:32, 39, 51`.
- **Đầu vào / đầu ra:** email/product id → danh sách ProductResponse/void.
- **Logic cần bao phủ:** add idempotent; chỉ product ACTIVE/category ACTIVE; get newest first; remove row hiện có; remove thiếu no-op.
- **Mock:** WishlistItemRepository, UserRepository, ProductRepository.
- **Case bất thường:** user/product thiếu, inactive product/category, race unique constraint, khác chữ hoa email.
- **Ưu tiên / lý do:** P2 — dữ liệu cá nhân và unique constraint nhưng ít tác động tài chính.

### UT-NOT-01 — NotificationService.list/markRead

- **Vị trí:** `service/NotificationService.java:29, 51`.
- **Đầu vào / đầu ra:** email/id → list/NotificationResponse.
- **Logic cần bao phủ:** sort repository newest first; mapping read/type/order; markRead true.
- **Mock:** UserRepository, NotificationRepository.
- **Case bất thường:** user thiếu→401; notification thiếu/không thuộc user→404; markRead lặp là idempotent.
- **Ưu tiên / lý do:** P2 — bảo vệ dữ liệu theo user.

### UT-STAT-01 — AdminStatisticsService.dashboard

- **Vị trí:** `service/AdminStatisticsService.java:36`.
- **Đầu vào / đầu ra:** không tham số → AdminStatisticsResponse.
- **Logic cần bao phủ:** count statuses; total inventory; low stock threshold = 5 (1..5), out-of-stock active; revenue COMPLETED; category/status projections; top 10 completed products.
- **Mock:** ProductRepository, OrderRepository, UserRepository; projection Object[] có đúng vị trí và kiểu.
- **Case bất thường:** query rỗng/null sum; Number trả Long/Integer/BigDecimal; malformed projection/type; completed revenue 0.
- **Ưu tiên / lý do:** P2 — báo cáo sai ảnh hưởng vận hành nhưng không trực tiếp thay đổi order.

### UT-ERR-01 — ApiExceptionHandler

- **Vị trí:** `controller/ApiExceptionHandler.java:25–188`.
- **Đầu vào / đầu ra:** các framework exceptions/status → ResponseEntity<ErrorResponse>.
- **Logic cần bao phủ:** ResponseStatus reason null/có; missing param/type mismatch; validation chọn field đầu; unreadable JSON; integrity→409; unknown exception→500; status localization; field labels/size translation.
- **Mock:** exception objects / BindingResult; kiểm status và DTO.
- **Case bất thường:** validation list rỗng, defaultMessage null, status ngoài switch; không lộ exception message ở generic 500.
- **Ưu tiên / lý do:** P2 — nhất quán API và thông tin lỗi.

### UT-BOOT-01 — AdminBootstrap.run

- **Vị trí:** `config/AdminBootstrap.java:36`.
- **Đầu vào / đầu ra:** ApplicationArguments + config → void hoặc startup exception.
- **Logic cần bao phủ:** cả email/password blank thì skip; thiếu một trường fail; existing non-admin fail; existing admin inactive tự bật; admin mới encode và lưu.
- **Mock:** UserRepository, PasswordEncoder.
- **Case bất thường:** normalize email; user role null; encode throw; giá trị whitespace.
- **Ưu tiên / lý do:** P2 — startup access admin phụ thuộc bootstrap.

### UT-SEED-01 — DemoDataSeeder.run và idempotency

- **Vị trí:** `config/DemoDataSeeder.java:195, 218, 279, 359, 385`.
- **Đầu vào / đầu ra:** ApplicationArguments/config → các record seed.
- **Logic cần bao phủ:** enabled=false không ghi; mật khẩu/email invalid; sample email lặp; existing user role sai; category/product/variant missing được bổ sung; existing order/cart không duplicate; user sample không nhận role khác USER; order demo trừ stock và notification.
- **Mock:** các repositories được inject, PasswordEncoder; kiểm call set.
- **Case bất thường:** không có sản phẩm/category active; stock cạn; partial seed; chạy 2 lần; dữ liệu category inactive.
- **Ưu tiên / lý do:** P3 — có side effect DB ở startup và mặc định enabled; test tính idempotent giúp tránh phình data.

### UT-FE-API-01 — frontend api wrapper

- **Vị trí:** `C:\DAAAN\shopee\frontend\src\api.js:4–66`.
- **Đầu vào / đầu ra:** path/options → Promise payload hoặc Error; localStorage/session event side effects.
- **Logic cần bao phủ:** getUser parse/role normalize/fallback malformed JSON; body JSON header trừ FormData; token Authorization; 204 null; response JSON; 401 clear session + event; network failure; AbortError; payload message/error fallback; money format.
- **Mock:** localStorage, fetch, FormData, window Event/dispatch.
- **Case bất thường:** empty/invalid JSON response, status 204 body, no token, existing Authorization override, network error object without name.
- **Ưu tiên / lý do:** P2 — wrapper ảnh hưởng mọi giao diện backend; frontend hiện không khai test runner.

### UT-FE-ROUTE-01 — AppRoutes access gates và login redirect

- **Vị trí:** `C:\DAAAN\shopee\frontend\src\AppRoutes.jsx:23, 51, 79`; `features/auth/AuthPage.jsx` (submit redirect).
- **Đầu vào / đầu ra:** user/role/path → protected page hoặc Empty state/redirect.
- **Logic cần bao phủ:** anonymous vào customer/admin route; USER vào admin; ADMIN vào customer; redirect bắt đầu `/` nhưng không bắt đầu `//`; role ADMIN chỉ redirect admin path, USER không redirect admin.
- **Mock:** MemoryRouter, location/query và navigation.
- **Case bất thường:** redirect URL ngoài site, `//evil`, `/adminx`, thiếu redirect.
- **Ưu tiên / lý do:** P1 — kiểm soát điều hướng và tránh redirect ngoài ý muốn; API vẫn phải được test độc lập.

### UT-FE-CHECKOUT-01 — Checkout.submit và useCart

- **Vị trí:** `C:\DAAAN\shopee\frontend\src\features\cart\CheckoutPage.jsx:11`; `useCart.js:5`.
- **Đầu vào / đầu ra:** form → createOrder call/navigation/toast; token/cart loading state.
- **Logic cần bao phủ:** no token Empty/login link; submit prevention; busy true/finally false; success refreshCart + navigate order detail; failure toast; empty cart button disabled; useCart no token vs fetch/error.
- **Mock:** createOrder, useCart, useNavigate, getToken, callbacks.
- **Case bất thường:** double submit/busy; fetchCart reject; checkout server rejects stale stock; no cart.
- **Ưu tiên / lý do:** P2 — frontend phải phản ánh backend rejection và tránh gửi thao tác lặp.

### UT-FE-ORD-01 — AdminOrders.actionsFor

- **Vị trí:** `C:\DAAAN\shopee\frontend\src\features\admin\AdminOrdersPage.jsx:19`.
- **Đầu vào / đầu ra:** order.status → danh sách action.
- **Logic cần bao phủ:** PENDING confirm/cancel; CONFIRMED start-delivery; SHIPPING mark-delivered; DELIVERED/COMPLETED/CANCELLED không có action.
- **Mock:** không cần cho pure function; component test mock API/window.confirm.
- **Case bất thường:** status null/unknown không tạo action; cancel confirmation false không gọi API; busyId chặn thao tác đồng thời.
- **Ưu tiên / lý do:** P2 — đảm bảo UI action tương ứng với transition backend.

### UT-FE-WISH-01 — WishlistProvider.toggle/useWishlist

- **Vị trí:** `C:\DAAAN\shopee\frontend\src\features\account\WishlistContext.jsx:7, 42`.
- **Đầu vào / đầu ra:** user/product id → navigation hoặc API mutation/context ids/toast.
- **Logic cần bao phủ:** load ids chỉ role USER; reset khi đổi session; guest chuyển login và giữ location; role khác USER chỉ toast; add/remove theo membership; API failure không cập nhật id.
- **Mock:** wishlist service, router hooks, toast callback.
- **Case bất thường:** in-flight load sau unmount không set state; ids String conversion; concurrent toggle.
- **Ưu tiên / lý do:** P2 — context được product page dùng xuyên suốt.

### UT-FE-ACCOUNT-01 — Account load/save profile

- **Vị trí:** `C:\DAAAN\shopee\frontend\src\features\account\AccountPage.jsx:9`.
- **Đầu vào / đầu ra:** session profile/form → profile API, localStorage, user state, status text.
- **Logic cần bao phủ:** profile load đồng bộ state/session; save prevents default, busy, clears message, updates name/phone; failure message; email read-only.
- **Mock:** fetchMyProfile/updateMyProfile, localStorage wrappers, callbacks.
- **Case bất thường:** token null; API failure during initial load; phone null; repeated save.
- **Ưu tiên / lý do:** P2 — đồng bộ thông tin hiển thị với account backend.

## P3 — Format, seed và phần thuần

### UT-UTIL-01 — PaymentMemoUtil.stripAccents/generateRandomCode

- **Vị trí:** `util/PaymentMemoUtil.java:16, 30`.
- **Đầu vào / đầu ra:** String/int → String.
- **Logic cần bao phủ:** tiếng Việt có dấu, đ/Đ, dấu câu/khoảng trắng, null/blank; mã có đúng length và thuộc alphabet.
- **Case bất thường:** length 0/âm, chỉ ký tự bị loại, supplementary Unicode.
- **Mock:** không cần.
- **Ưu tiên / lý do:** P1 cho strip/memo, P3 cho code random thuần; test này được thực hiện cùng UT-PAY-02.

## Thứ tự triển khai đề xuất

1. Bắt đầu với OrderService checkout/cancel/state transitions và PaymentTransactionService vì liên quan tồn kho, order state và paid state.
2. Kiểm thử SecurityConfig/JWT, login/register và AdminUserService để xác minh mọi route và account-active branch.
3. Kiểm thử CartService, ProductService.search, ProductSpecifications và category/variant rules.
4. Kiểm thử review gate, payment memo/bank-account selection, wishlist và notifications.
5. Kiểm thử thống kê, exception mapping, bootstrap/seeder và frontend wrapper/access actions.
6. Bổ sung persistence integration cho unique/FK/cascade/schema/locking, đặc biệt concurrency checkout và rollback. Unit mocks một mình không chứng minh các đặc tính DB này.

## Lưu ý trước khi viết test

- Profile `test` dùng H2 và Flyway off; các migrations MySQL không chạy. Chốt schema boot strategy trước test repository.
- Timestamps dùng `Instant.now()` trực tiếp; assertion nên so trạng thái/mốc có mặt và thứ tự, hoặc sau này inject `Clock` nếu cần test mốc chính xác.
- Test `AdminBankController.saveBankAccount` với DB thật để xác minh inactive row persistence; test Mockito đơn thuần sẽ không bắt vấn đề persistence context.
- `OrderService.checkout` gọi conversion DTO và persistence trong cùng service; test mock nên kiểm cả `PaymentTransactionService` call conditional COD/BANKING.
- Trạng thái REJECTED của payment transaction hiện không có API để gán; behavior confirm từ REJECTED nên được characterization trước khi quyết định yêu cầu mới.
- Không chạy test và không thay đổi ứng dụng trong bước lập tài liệu này.

