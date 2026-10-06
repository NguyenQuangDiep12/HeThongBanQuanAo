package com.project_shopping.shopee.config;

import com.project_shopping.shopee.model.Category;
import com.project_shopping.shopee.model.Cart;
import com.project_shopping.shopee.model.CartItem;
import com.project_shopping.shopee.model.CustomerOrder;
import com.project_shopping.shopee.model.Notification;
import com.project_shopping.shopee.model.OrderItem;
import com.project_shopping.shopee.model.Payment;
import com.project_shopping.shopee.model.Product;
import com.project_shopping.shopee.model.ProductVariant;
import com.project_shopping.shopee.model.User;
import com.project_shopping.shopee.model.enums.OrderStatus;
import com.project_shopping.shopee.model.enums.PaymentMethod;
import com.project_shopping.shopee.model.enums.PaymentStatus;
import com.project_shopping.shopee.model.enums.ProductStatus;
import com.project_shopping.shopee.model.enums.Role;
import com.project_shopping.shopee.repository.CartRepository;
import com.project_shopping.shopee.repository.CategoryRepository;
import com.project_shopping.shopee.repository.NotificationRepository;
import com.project_shopping.shopee.repository.OrderRepository;
import com.project_shopping.shopee.repository.ProductRepository;
import com.project_shopping.shopee.repository.UserRepository;
import com.project_shopping.shopee.repository.VariantRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class DemoDataSeeder implements ApplicationRunner {

    private static final int DEMO_RECORD_COUNT = 20;
    private static final List<String> SIZES = List.of("S", "M", "L");
    private static final List<String> COLORS = List.of("Đen", "Trắng");

    private static final List<ProductSeed> PRODUCT_SEEDS = List.of(
            new ProductSeed(
                    "Áo thun basic cotton",
                    "Thời trang",
                    "Áo thun cotton mềm, dễ phối đồ hằng ngày.",
                    "199000"),
            new ProductSeed(
                    "Áo polo cổ bẻ",
                    "Áo polo nam",
                    "Polo dệt pique thoáng mát, phom dáng gọn gàng.",
                    "329000"),
            new ProductSeed(
                    "Áo sơ mi Oxford",
                    "Áo sơ mi",
                    "Sơ mi Oxford phù hợp đi làm và dạo phố.",
                    "429000"),
            new ProductSeed(
                    "Quần jeans straight fit",
                    "Quần jeans",
                    "Quần jeans dáng straight, chất denim bền và thoải mái.",
                    "549000"),
            new ProductSeed(
                    "Áo khoác nhẹ daily",
                    "Áo khoác",
                    "Áo khoác nhẹ cho những ngày thời tiết mát.",
                    "699000"),
            new ProductSeed(
                    "Áo thun nữ ribbed",
                    "Áo thun nữ",
                    "Áo thun gân co giãn với phom dáng nữ tính.",
                    "259000"),
            new ProductSeed(
                    "Đầm midi tối giản",
                    "Đầm nữ",
                    "Đầm midi thanh lịch, phù hợp nhiều dịp.",
                    "589000"),
            new ProductSeed(
                    "Chân váy chữ A",
                    "Chân váy",
                    "Chân váy chữ A dễ phối cùng áo thun hoặc sơ mi.",
                    "379000"),
            new ProductSeed(
                    "Hoodie nỉ unisex",
                    "Hoodie",
                    "Hoodie nỉ mềm, phom rộng unisex.",
                    "629000"),
            new ProductSeed(
                    "Túi tote canvas",
                    "Phụ kiện",
                    "Túi tote canvas dày dặn, tiện dụng hằng ngày.",
                    "159000"),
            new ProductSeed(
                    "Quần kaki slim fit",
                    "Quần kaki",
                    "Quần kaki co giãn nhẹ, phù hợp đi làm và đi chơi.",
                    "459000"),
            new ProductSeed(
                    "Áo cardigan dệt kim",
                    "Áo len",
                    "Cardigan dệt kim mềm nhẹ, dễ phối nhiều lớp.",
                    "519000"),
            new ProductSeed(
                    "Váy maxi hoa nhí",
                    "Đầm nữ",
                    "Váy maxi họa tiết hoa nhí, chất vải nhẹ và thoáng.",
                    "629000"),
            new ProductSeed(
                    "Quần short linen",
                    "Quần short",
                    "Quần short linen thoáng mát cho ngày hè năng động.",
                    "289000"),
            new ProductSeed(
                    "Sơ mi linen tay dài",
                    "Áo sơ mi",
                    "Sơ mi linen đứng phom, thoáng mát và ít kén dáng.",
                    "489000"),
            new ProductSeed(
                    "Áo croptop cotton",
                    "Áo thun nữ",
                    "Áo croptop cotton co giãn, thiết kế tối giản.",
                    "229000"),
            new ProductSeed(
                    "Áo khoác denim classic",
                    "Áo khoác",
                    "Áo khoác denim cổ điển, dễ kết hợp với trang phục thường ngày.",
                    "759000"),
            new ProductSeed(
                    "Quần jogger nỉ basic",
                    "Quần jogger",
                    "Quần jogger nỉ mềm, cạp chun thoải mái khi vận động.",
                    "399000"),
            new ProductSeed(
                    "Mũ lưỡi trai cotton",
                    "Phụ kiện",
                    "Mũ lưỡi trai cotton có khóa điều chỉnh kích thước.",
                    "189000"),
            new ProductSeed(
                    "Áo polo nữ basic",
                    "Áo polo nữ",
                    "Áo polo nữ chất cotton thoáng khí, phom dáng gọn gàng.",
                    "319000")
    );

    private static final System.Logger LOGGER =
            System.getLogger(DemoDataSeeder.class.getName());

    private final CategoryRepository categories;
    private final ProductRepository products;
    private final VariantRepository variants;
    private final CartRepository carts;
    private final OrderRepository orders;
    private final NotificationRepository notifications;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String demoUserEmail;
    private final String demoUserPassword;

    public DemoDataSeeder(
            CategoryRepository categories,
            ProductRepository products,
            VariantRepository variants,
            CartRepository carts,
            OrderRepository orders,
            NotificationRepository notifications,
            UserRepository users,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.demo-data.enabled:true}") boolean enabled,
            @Value("${app.bootstrap-demo-user.email:user@local}") String demoUserEmail,
            @Value("${app.bootstrap-demo-user.password:123456}") String demoUserPassword) {

        this.categories = categories;
        this.products = products;
        this.variants = variants;
        this.carts = carts;
        this.orders = orders;
        this.notifications = notifications;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.demoUserEmail = demoUserEmail;
        this.demoUserPassword = demoUserPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        List<User> demoUsers = seedDemoCustomers();
        Map<String, Category> categoryIndex = loadCategories();
        List<Product> demoProducts = seedProducts(categoryIndex);
        int seededCarts = seedCarts(demoUsers, demoProducts);
        int seededOrders = seedOrders(demoUsers, demoProducts);

        LOGGER.log(
                System.Logger.Level.INFO,
                "Đã chuẩn bị dữ liệu mẫu: {0} tài khoản mẫu, {1} danh mục, {2} sản phẩm, {3} phân loại, {4} giỏ hàng mới và {5} đơn hàng mới.",
                demoUsers.size(),
                categoryIndex.size(),
                demoProducts.size(),
                countSeedVariants(),
                seededCarts,
                seededOrders
        );
    }

    private List<User> seedDemoCustomers() {
        if (demoUserPassword == null || demoUserPassword.isBlank()) {
            throw new IllegalStateException(
                    "Cần cấu hình app.bootstrap-demo-user.password với mật khẩu mẫu hợp lệ."
            );
        }

        List<User> demoUsers = new ArrayList<>();
        HashSet<String> seededEmails = new HashSet<>();
        for (int index = 1; index <= DEMO_RECORD_COUNT; index++) {
            String email = index == 1
                    ? normalizeEmail(demoUserEmail)
                    : "demo-user-%02d@local".formatted(index);
            if (!seededEmails.add(email)) {
                throw new IllegalStateException(
                        "Email tài khoản mẫu bị trùng với một tài khoản mẫu khác."
                );
            }
            demoUsers.add(ensureDemoCustomer(email, index));
        }
        return demoUsers;
    }

    private User ensureDemoCustomer(String email, int index) {
        var existing = users.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            if (existing.get().getRole() != Role.USER) {
                throw new IllegalStateException(
                        "Email khách hàng mẫu đã được tài khoản không có quyền khách hàng sử dụng."
                );
            }
            return existing.get();
        }

        return users.save(new User(
                "Khách hàng mẫu %02d".formatted(index),
                email,
                passwordEncoder.encode(demoUserPassword),
                Role.USER,
                null
        ));
    }

    private Map<String, Category> loadCategories() {
        Map<String, Category> categoryIndex = new HashMap<>();
        for (Category category : categories.findAllByOrderByNameAsc()) {
            categoryIndex.put(normalizeName(category.getName()), category);
        }

        for (ProductSeed productSeed : PRODUCT_SEEDS) {
            String key = normalizeName(productSeed.categoryName());
            categoryIndex.computeIfAbsent(key, ignored -> categories
                    .findByNameIgnoreCase(productSeed.categoryName())
                    .orElseGet(() -> categories.save(new Category(
                            productSeed.categoryName(),
                            "Danh mục sản phẩm thời trang"))));
        }

        return categoryIndex;
    }

    private List<Product> seedProducts(Map<String, Category> categoryIndex) {
        List<Product> seededProducts = new ArrayList<>();
        for (ProductSeed seed : PRODUCT_SEEDS) {
            Category category = categoryIndex.get(normalizeName(seed.categoryName()));
            if (!category.isActive()) {
                continue;
            }

            Product product = products
                    .findWithVariantsByNameIgnoreCaseAndCategory_Id(
                            seed.name(),
                            category.getId())
                    .orElseGet(() -> createProduct(seed, category));
            if (product.getStatus() != ProductStatus.ACTIVE) {
                continue;
            }

            List<ProductVariant> missingVariants = createVariants(product, seed)
                    .stream()
                    .filter(variant -> !variants
                            .existsByProduct_IdAndSizeIgnoreCaseAndColorIgnoreCase(
                                    product.getId(),
                                    variant.getSize(),
                                    variant.getColor()))
                    .toList();
            if (!missingVariants.isEmpty()) {
                variants.saveAll(missingVariants);
            }
            if (!variants.findAllByProduct_IdOrderBySizeAscColorAsc(product.getId()).isEmpty()) {
                seededProducts.add(product);
            }
        }
        return seededProducts;
    }

    private Product createProduct(ProductSeed seed, Category category) {
        Product product = new Product(
                seed.name(),
                seed.description(),
                new BigDecimal(seed.price()),
                null
        );
        product.setCategory(category);
        product.setStatus(ProductStatus.ACTIVE);
        return products.save(product);
    }

    private List<ProductVariant> createVariants(
            Product product,
            ProductSeed seed) {

        List<ProductVariant> productVariants = new ArrayList<>();
        List<String> sizes = "Phụ kiện".equals(seed.categoryName())
                ? List.of("Free size")
                : SIZES;
        BigDecimal price = new BigDecimal(seed.price());

        for (String color : COLORS) {
            for (String size : sizes) {
                productVariants.add(new ProductVariant(
                        product,
                        size,
                        color,
                        15,
                        price
                ));
            }
        }

        return productVariants;
    }

    private int countSeedVariants() {
        return PRODUCT_SEEDS.stream()
                .mapToInt(seed -> "Phụ kiện".equals(seed.categoryName())
                        ? COLORS.size()
                        : SIZES.size() * COLORS.size())
                .sum();
    }

    private int seedCarts(List<User> demoUsers, List<Product> demoProducts) {
        if (demoProducts.isEmpty()) {
            return 0;
        }

        int createdCarts = 0;
        for (int index = 0; index < demoUsers.size(); index++) {
            User user = demoUsers.get(index);
            if (carts.findByUserId(user.getId()).isPresent()) {
                continue;
            }

            Product product = demoProducts.get(index % demoProducts.size());
            ProductVariant variant = firstAvailableVariant(product);
            if (variant == null) {
                continue;
            }

            Cart cart = new Cart(user);
            cart.getItems().add(new CartItem(cart, variant, 1));
            carts.save(cart);
            createdCarts++;
        }
        return createdCarts;
    }

    private int seedOrders(List<User> demoUsers, List<Product> demoProducts) {
        if (demoProducts.isEmpty()) {
            return 0;
        }

        int orderCount = 0;
        for (int index = 0; index < demoUsers.size(); index++) {
            User user = demoUsers.get(index);
            if (!orders.findAllByUserIdOrderByCreatedAtDesc(user.getId()).isEmpty()) {
                continue;
            }

            Product product = demoProducts.get(index % demoProducts.size());
            ProductVariant variant = firstAvailableVariant(product);
            if (variant == null) {
                continue;
            }

            OrderStatus status = demoOrderStatus(index);
            CustomerOrder order = createDemoOrder(
                    user,
                    variant,
                    index,
                    status
            );
            orders.save(order);
            variant.setStockQuantity(variant.getStockQuantity() - 1);
            notifications.save(createDemoNotification(order, status));
            orderCount++;
        }
        return orderCount;
    }

    private ProductVariant firstAvailableVariant(Product product) {
        return variants.findAllByProduct_IdOrderBySizeAscColorAsc(product.getId())
                .stream()
                .filter(variant -> variant.getStockQuantity() > 0)
                .findFirst()
                .orElse(null);
    }

    private CustomerOrder createDemoOrder(
            User user,
            ProductVariant variant,
            int index,
            OrderStatus status) {

        Instant createdAt = Instant.now().minus(Duration.ofDays(25L - index));
        CustomerOrder order = new CustomerOrder(
                user,
                "Địa chỉ mẫu khách hàng %02d, TP. Hồ Chí Minh".formatted(index + 1),
                "09000000%02d".formatted(index + 1),
                variant.getPrice()
        );
        order.setCreatedAt(createdAt);
        order.setStatus(status);
        order.getItems().add(new OrderItem(order, variant, 1, variant.getPrice()));
        setOrderTimeline(order, status, createdAt);

        Payment payment = new Payment(order, PaymentMethod.COD, order.getTotalAmount());
        if (status == OrderStatus.DELIVERED || status == OrderStatus.COMPLETED) {
            payment.setPaymentStatus(PaymentStatus.PAID);
            payment.setPaidAt(status == OrderStatus.COMPLETED
                    ? order.getCustomerConfirmedAt()
                    : order.getDeliveredAt());
        } else {
            payment.setPaymentStatus(PaymentStatus.PENDING);
        }
        order.setPayment(payment);
        return order;
    }

    private void setOrderTimeline(
            CustomerOrder order,
            OrderStatus status,
            Instant createdAt) {

        if (status == OrderStatus.CONFIRMED
                || status == OrderStatus.SHIPPING
                || status == OrderStatus.DELIVERED
                || status == OrderStatus.COMPLETED) {
            order.setConfirmedAt(createdAt.plus(Duration.ofHours(2)));
        }
        if (status == OrderStatus.SHIPPING
                || status == OrderStatus.DELIVERED
                || status == OrderStatus.COMPLETED) {
            order.setShippingAt(createdAt.plus(Duration.ofDays(1)));
        }
        if (status == OrderStatus.DELIVERED || status == OrderStatus.COMPLETED) {
            order.setDeliveredAt(createdAt.plus(Duration.ofDays(2)));
        }
        if (status == OrderStatus.COMPLETED) {
            order.setCustomerConfirmedAt(createdAt.plus(Duration.ofDays(3)));
        }
    }

    private OrderStatus demoOrderStatus(int index) {
        return switch (index / (DEMO_RECORD_COUNT / 5)) {
            case 0 -> OrderStatus.PENDING;
            case 1 -> OrderStatus.CONFIRMED;
            case 2 -> OrderStatus.SHIPPING;
            case 3 -> OrderStatus.DELIVERED;
            default -> OrderStatus.COMPLETED;
        };
    }

    private Notification createDemoNotification(
            CustomerOrder order,
            OrderStatus status) {

        String message = switch (status) {
            case PENDING -> "Đơn hàng mẫu đang chờ quản trị viên xác nhận.";
            case CONFIRMED -> "Đơn hàng mẫu đã được xác nhận.";
            case SHIPPING -> "Đơn hàng mẫu đang được giao.";
            case DELIVERED -> "Đơn hàng mẫu đã được giao thành công.";
            case COMPLETED -> "Khách hàng mẫu đã xác nhận nhận hàng.";
            case CANCELLED -> "Đơn hàng mẫu đã bị hủy.";
        };
        Notification notification = new Notification(
                order.getUser(),
                order,
                "Cập nhật đơn hàng mẫu",
                message
        );
        notification.setRead(status == OrderStatus.COMPLETED);
        return notification;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalStateException(
                    "Cần cấu hình app.bootstrap-demo-user.email bằng địa chỉ email hợp lệ."
            );
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeName(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private record ProductSeed(
            String name,
            String categoryName,
            String description,
            String price) {
    }
}
