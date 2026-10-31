package com.mentormatching.auth.dto.response;

import lombok.Builder;

/**
 * Cấu trúc response thành công của API.
 *
 * @param status mã HTTP
 * @param code mã kết quả
 * @param success trạng thái thành công
 * @param message thông báo kết quả
 * @param data dữ liệu trả về
 * @param <T> kiểu dữ liệu trả về
 */
@Builder
public record ApiResponse<T>(int status, String code, boolean success, String message, T data) {
}
