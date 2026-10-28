package com.mentormatching.auth.security;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import com.mentormatching.auth.exception.ProblemResponses;

/**
 * Trả lỗi xác thực 401 dạng ProblemDetail từ Spring Security filter chain.
 * Cấu hình hiện tại dùng HTTP Basic; challenge cần đổi khi triển khai cơ chế Bearer/JWT.
 */
@Component
public class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ProblemResponses problems;
    /**
     * Khởi tạo ProblemAuthenticationEntryPoint với các dependency cần thiết.
     *
     * @param problems helper tạo và ghi body lỗi thống nhất
     */
    public ProblemAuthenticationEntryPoint(ProblemResponses problems) { this.problems = problems; }

    /**
     * Thiết lập HTTP Basic challenge và trả body 401 chuẩn, không dùng nội dung exception làm detail.
     * Được gọi khi Spring Security yêu cầu xác thực hoặc xử lý xác thực thất bại.
     *
     * @param request HTTP request hiện tại
     * @param response HTTP response cần ghi
     * @param ex exception cần xử lý
     * @throws IOException nếu thao tác I/O khi xử lý request/response thất bại
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Mentor Matching\"");
        problems.write(HttpStatus.UNAUTHORIZED, request, response);
    }
}
