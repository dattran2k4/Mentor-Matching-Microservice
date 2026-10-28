package com.mentormatching.user.exception;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trả ProblemDetail tại endpoint lỗi dự phòng của servlet (mặc định /error).
 * Xử lý error dispatch tới endpoint này; không chạy tiếp sau mọi MVC exception đã được advice xử lý.
 * Không lấy thông báo exception nội bộ làm nội dung công khai.
 */
@RestController
public class ProblemErrorController implements ErrorController {
    private final ProblemResponses problems;
    /**
     * Khởi tạo ProblemErrorController với các dependency cần thiết.
     *
     * @param problems helper tạo và ghi body lỗi thống nhất
     */
    public ProblemErrorController(ProblemResponses problems) { this.problems = problems; }

    /**
     * Đọc ERROR_STATUS_CODE từ servlet request và trả body lỗi chuẩn.
     * Chỉ chấp nhận số nguyên trong khoảng 400–599; thiếu hoặc sai giá trị thì dùng 500.
     * ProblemResponses giữ URI gốc của request khi có error dispatch.
     *
     * @param request HTTP request hiện tại
     * @return response lỗi với content type ProblemDetail và header requestId
     */
    @RequestMapping("${server.error.path:/error}")
    public ResponseEntity<ProblemDetail> error(HttpServletRequest request) {
        Object value = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = value instanceof Integer number && number >= 400 && number <= 599 ? number : 500;
        HttpStatusCode code = HttpStatusCode.valueOf(status);
        return ResponseEntity.status(code).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .header(ProblemResponses.REQUEST_ID_HEADER, ProblemResponses.requestId(request))
                .body(problems.standard(code, request));
    }
}
