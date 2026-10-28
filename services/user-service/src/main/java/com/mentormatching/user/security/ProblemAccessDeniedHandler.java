package com.mentormatching.user.security;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import com.mentormatching.user.exception.ProblemResponses;

/**
 * Trả lỗi từ chối truy cập 403 dạng ProblemDetail từ Spring Security filter chain,
 * bao gồm các trường hợp CSRF được chuyển tới handler này.
 */
@Component
public class ProblemAccessDeniedHandler implements AccessDeniedHandler {
    private final ProblemResponses problems;
    /**
     * Khởi tạo ProblemAccessDeniedHandler với các dependency cần thiết.
     *
     * @param problems helper tạo và ghi body lỗi thống nhất
     */
    public ProblemAccessDeniedHandler(ProblemResponses problems) { this.problems = problems; }

    /**
     * Trả body 403 chuẩn cho lỗi truy cập do security chain chuyển tới.
     * Không đưa chi tiết quyền hoặc nội dung exception nội bộ ra client.
     *
     * @param request HTTP request hiện tại
     * @param response HTTP response cần ghi
     * @param ex exception cần xử lý
     * @throws IOException nếu thao tác I/O khi xử lý request/response thất bại
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        problems.write(HttpStatus.FORBIDDEN, request, response);
    }
}
