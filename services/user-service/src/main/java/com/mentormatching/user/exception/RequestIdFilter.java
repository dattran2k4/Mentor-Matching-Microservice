package com.mentormatching.user.exception;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gắn requestId do server tạo vào response header và MDC để đối chiếu log.
 * Chạy sớm trong filter chain; tái sử dụng ID trong request attribute qua các dispatch.
 * MDC là ngữ cảnh theo thread, không tự truyền sang mọi tác vụ bất đồng bộ.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {
    /**
     * Cho phép filter chạy lại trong async dispatch để gắn MDC trên thread xử lý dispatch.
     *
     * @return false để không bỏ qua async dispatch
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() { return false; }
    /**
     * Cho phép filter tham gia error dispatch theo vòng đời của OncePerRequestFilter.
     *
     * @return false để không bỏ qua error dispatch
     */
    @Override
    protected boolean shouldNotFilterErrorDispatch() { return false; }

    /**
     * Gắn cùng requestId vào MDC và response trước khi chuyển sang filter tiếp theo.
     * Khối finally khôi phục MDC cũ hoặc xóa giá trị vừa tạo, tránh rò ngữ cảnh sang request khác
     * khi servlet container tái sử dụng thread; vẫn dọn dẹp khi filter phía sau ném exception.
     *
     * @param request HTTP request hiện tại
     * @param response HTTP response cần ghi
     * @param chain filter chain tiếp tục xử lý request
     * @throws IOException nếu thao tác I/O khi xử lý request/response thất bại
     * @throws ServletException nếu filter chain xử lý request thất bại
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String previous = MDC.get("requestId");
        String id = ProblemResponses.requestId(request);
        MDC.put("requestId", id);
        response.setHeader(ProblemResponses.REQUEST_ID_HEADER, id);
        try {
            chain.doFilter(request, response);
        } finally {
            if (previous == null) { MDC.remove("requestId"); }
            else { MDC.put("requestId", previous); }
        }
    }
}
