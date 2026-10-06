package com.project_shopping.shopee.controller;

import com.project_shopping.shopee.dto.ApiDtos.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

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
                .map(error -> validationMessage(error.getField(), error.getDefaultMessage()))
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

    private String validationMessage(String field, String defaultMessage) {
        String label = fieldLabel(field);
        String constraint = switch (defaultMessage == null ? "" : defaultMessage) {
            case "must not be blank", "must not be null" -> "không được để trống";
            case "must be greater than 0" -> "phải lớn hơn 0";
            case "must be greater than or equal to 0" -> "phải bằng hoặc lớn hơn 0";
            case "must be greater than or equal to 0.01" -> "phải lớn hơn 0";
            case "must be a well-formed email address" -> "không đúng định dạng email";
            default -> translateSizeMessage(defaultMessage);
        };
        return label + " " + constraint + ".";
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
                Map.entry("description", "mô tả")
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

    private String translateSizeMessage(String defaultMessage) {
        if (defaultMessage != null && defaultMessage.startsWith("size must be between ")) {
            return "vượt quá độ dài cho phép";
        }
        return "chưa hợp lệ";
    }

    public static String localizedStatus(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Yêu cầu không hợp lệ";
            case UNAUTHORIZED -> "Chưa đăng nhập";
            case FORBIDDEN -> "Không có quyền truy cập";
            case NOT_FOUND -> "Không tìm thấy dữ liệu";
            case CONFLICT -> "Dữ liệu bị xung đột";
            case INTERNAL_SERVER_ERROR -> "Lỗi hệ thống";
            default -> "Yêu cầu không thể xử lý";
        };
    }
}
