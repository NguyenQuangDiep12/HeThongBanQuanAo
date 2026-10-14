package com.project_shopping.shopee.controller;

import com.project_shopping.shopee.dto.ApiDtos.ErrorResponse;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final System.Logger LOGGER =
            System.getLogger(ApiExceptionHandler.class.getName());

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
            ResponseStatusException exception) {

        HttpStatus status = HttpStatus.valueOf(
                exception.getStatusCode().value()
        );

        String message = exception.getReason();

        if (message == null) {
            message = "Đã xảy ra lỗi. Vui lòng thử lại.";
        }

        return createErrorResponse(status, message, localizedStatus(status));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException exception) {
        return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Thiếu tham số bắt buộc: " + fieldLabel(exception.getParameterName()) + "."
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception) {
        return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Giá trị của " + fieldLabel(exception.getName()) + " không đúng định dạng."
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::validationMessage)
                .findFirst()
                .orElse("Dữ liệu gửi lên chưa hợp lệ. Vui lòng kiểm tra lại.");

        return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableRequest(
            HttpMessageNotReadableException exception) {

        return createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Dữ liệu gửi lên không hợp lệ. Vui lòng kiểm tra các trường bắt buộc và giá trị đã chọn."
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException exception) {

        return createErrorResponse(
                HttpStatus.CONFLICT,
                "Dữ liệu không thể lưu do bị trùng hoặc vi phạm ràng buộc. Vui lòng kiểm tra lại."
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException exception) {
        return createErrorResponse(HttpStatus.NOT_FOUND, "Không tìm thấy đường dẫn yêu cầu.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception) {
        return createErrorResponse(HttpStatus.METHOD_NOT_ALLOWED,
                "Phương thức HTTP không được hỗ trợ cho đường dẫn này.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception) {
        return createErrorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Định dạng nội dung gửi lên không được hỗ trợ.");
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotAcceptable(
            HttpMediaTypeNotAcceptableException exception) {
        return createErrorResponse(HttpStatus.NOT_ACCEPTABLE,
                "Định dạng phản hồi được yêu cầu không được hỗ trợ.");
    }

    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ErrorResponse> handleConcurrencyFailure(
            ConcurrencyFailureException exception) {
        LOGGER.log(System.Logger.Level.WARNING,
                "Yêu cầu không thể hoàn tất do tranh chấp khóa cơ sở dữ liệu.", exception);
        return createErrorResponse(HttpStatus.SERVICE_UNAVAILABLE,
                "Dữ liệu đang được cập nhật bởi yêu cầu khác. Vui lòng thử lại.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception exception) {
        LOGGER.log(
                System.Logger.Level.ERROR,
                "Đã xảy ra lỗi không mong muốn khi xử lý yêu cầu API.",
                exception
        );
        return createErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau."
        );
    }

    private ResponseEntity<ErrorResponse> createErrorResponse(
            HttpStatus status,
            String message) {

        return createErrorResponse(status, message, localizedStatus(status));
    }

    private ResponseEntity<ErrorResponse> createErrorResponse(
            HttpStatus status,
            String message,
            String error) {

        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now(),
                status.value(),
                error,
                message
        );

        return ResponseEntity
                .status(status)
                .body(errorResponse);
    }

    private String validationMessage(FieldError error) {
        String label = fieldLabel(error.getField());
        String constraint = switch (error.getCode() == null ? "" : error.getCode()) {
            case "NotBlank", "NotNull" -> "không được để trống";
            case "Positive" -> "phải lớn hơn 0";
            case "PositiveOrZero" -> "phải bằng hoặc lớn hơn 0";
            case "DecimalMin" -> "phải đạt giá trị tối thiểu cho phép";
            case "DecimalMax", "Max" -> "vượt quá giá trị tối đa cho phép";
            case "Email" -> "không đúng định dạng email";
            case "WholeVndAmount" -> "phải là số nguyên VND";
            case "Size" -> sizeConstraint(error.getArguments());
            case "Digits" -> "không đúng số chữ số cho phép";
            default -> "chưa hợp lệ";
        };
        return label + " " + constraint + ".";
    }

    private String sizeConstraint(Object[] arguments) {
        if (arguments == null || arguments.length < 3
                || !(arguments[1] instanceof Number maxValue)
                || !(arguments[2] instanceof Number minValue)) {
            return "có độ dài không hợp lệ";
        }
        int min = minValue.intValue();
        int max = maxValue.intValue();
        if (min > 0 && max < Integer.MAX_VALUE) {
            return "phải có từ " + min + " đến " + max + " ký tự";
        }
        if (min > 0) {
            return "phải có ít nhất " + min + " ký tự";
        }
        if (max < Integer.MAX_VALUE) {
            return "không được vượt quá " + max + " ký tự";
        }
        return "có độ dài không hợp lệ";
    }

    private String fieldLabel(String field) {
        Map<String, String> fieldNames = Map.ofEntries(
                Map.entry("fullName", "họ tên"),
                Map.entry("email", "email"),
                Map.entry("password", "mật khẩu"),
                Map.entry("phone", "số điện thoại"),
                Map.entry("name", "tên sản phẩm"),
                Map.entry("categoryId", "danh mục"),
                Map.entry("price", "giá"),
                Map.entry("imageUrl", "đường dẫn hình ảnh"),
                Map.entry("size", "kích cỡ"),
                Map.entry("color", "màu sắc"),
                Map.entry("stockQuantity", "số lượng tồn kho"),
                Map.entry("quantity", "số lượng"),
                Map.entry("variantId", "phân loại sản phẩm"),
                Map.entry("shippingAddress", "địa chỉ giao hàng"),
                Map.entry("paymentMethod", "phương thức thanh toán"),
                Map.entry("description", "mô tả"),
                Map.entry("bankCode", "mã ngân hàng"),
                Map.entry("bankName", "tên ngân hàng"),
                Map.entry("accountNo", "số tài khoản"),
                Map.entry("accountName", "tên chủ tài khoản"),
                Map.entry("qrTemplate", "mẫu QR")
        );
        Map<String, String> parameterNames = Map.of(
                "page", "số trang",
                "pageSize", "số sản phẩm mỗi trang",
                "limit", "số lượng sản phẩm",
                "keyword", "từ khóa tìm kiếm",
                "category", "danh mục",
                "minPrice", "giá thấp nhất",
                "maxPrice", "giá cao nhất",
                "inStock", "trạng thái còn hàng"
        );
        return fieldNames.getOrDefault(
                field,
                parameterNames.getOrDefault(field, "trường " + field)
        );
    }

    public static String localizedStatus(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Yêu cầu không hợp lệ";
            case UNAUTHORIZED -> "Chưa đăng nhập";
            case FORBIDDEN -> "Không có quyền truy cập";
            case NOT_FOUND -> "Không tìm thấy dữ liệu";
            case METHOD_NOT_ALLOWED -> "Phương thức không được hỗ trợ";
            case NOT_ACCEPTABLE -> "Định dạng phản hồi không được hỗ trợ";
            case UNSUPPORTED_MEDIA_TYPE -> "Định dạng nội dung không được hỗ trợ";
            case CONFLICT -> "Dữ liệu bị xung đột";
            case SERVICE_UNAVAILABLE -> "Dịch vụ tạm thời không khả dụng";
            case INTERNAL_SERVER_ERROR -> "Lỗi hệ thống";
            default -> "Yêu cầu không thể xử lý";
        };
    }
}
