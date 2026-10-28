package com.mentormatching.user.exception;

import org.springframework.http.HttpStatus;

/**
 * Biểu diễn lỗi nghiệp vụ dự kiến thuộc nhóm HTTP 4xx.
 * Thông báo được trả trực tiếp cho client: caller phải cung cấp nội dung an toàn,
 * không chứa SQL, stack trace, token hoặc dữ liệu nhạy cảm.
 */
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    /**
     * Tạo lỗi nghiệp vụ với status 4xx và mã lỗi ổn định.
     * Không tự kiểm tra nội dung thông báo có chứa dữ liệu nhạy cảm hay không.
     *
     * @param status HTTP status của lỗi
     * @param code mã lỗi ổn định, ví dụ EMAIL_ALREADY_EXISTS
     * @param publicMessage thông báo nghiệp vụ an toàn được trả trực tiếp cho client
     * @throws IllegalArgumentException nếu status không phải 4xx hoặc code sai định dạng
     */
    public ApiException(HttpStatus status, String code, String publicMessage) {
        super(publicMessage);
        if (status == null || !status.is4xxClientError()) {
            throw new IllegalArgumentException("ApiException requires a 4xx status");
        }
        if (code == null || !code.matches("[A-Z][A-Z0-9_]*")) {
            throw new IllegalArgumentException("A stable uppercase error code is required");
        }
        this.status = status;
        this.code = code;
    }

    /**
     * Lấy HTTP status đã được kiểm tra khi tạo exception.
     *
     * @return HTTP status thuộc nhóm 4xx
     */
    public HttpStatus getStatus() { return status; }
    /**
     * Lấy mã lỗi ổn định để client xử lý mà không phụ thuộc thông báo hiển thị.
     *
     * @return mã lỗi dạng chữ hoa và dấu gạch dưới
     */
    public String getCode() { return code; }
}
