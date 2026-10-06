package com.project_shopping.shopee.util;

import java.text.Normalizer;
import java.util.Random;

public final class PaymentMemoUtil {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Random RANDOM = new Random();

    private PaymentMemoUtil() {}

    /**
     * Bỏ dấu Tiếng Việt & loại bỏ ký tự đặc biệt
     */
    public static String stripAccents(String input) {
        if (input == null || input.isBlank()) return "";
        String nfdNormalizedString = Normalizer.normalize(input, Normalizer.Form.NFD);
        String withoutAccents = nfdNormalizedString.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return withoutAccents
                .replace("đ", "d")
                .replace("Đ", "D")
                .replaceAll("[^a-zA-Z0-9]", "")
                .toUpperCase();
    }

    /**
     * Sinh mã ngẫu nhiên duy nhất (VD: K92F)
     */
    public static String generateRandomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
