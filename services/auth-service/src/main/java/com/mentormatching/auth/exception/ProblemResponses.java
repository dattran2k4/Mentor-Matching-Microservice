package com.mentormatching.auth.exception;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.UUID;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Tạo ProblemDetail thống nhất cho MVC, servlet error dispatch và Spring Security.
 * Không tự bắt exception; caller quyết định HTTP status và thời điểm trả response.
 * Metadata gồm mã lỗi ổn định, URI của request và requestId do server tạo.
 */
@Component
public class ProblemResponses {
    public static final String REQUEST_ID_ATTRIBUTE = ProblemResponses.class.getName() + ".requestId";
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    private final ObjectMapper mapper;

    /**
     * Khởi tạo ProblemResponses với các dependency cần thiết.
     *
     * @param mapper ObjectMapper do Spring quản lý, dùng để ghi JSON
     */
    public ProblemResponses(ObjectMapper mapper) { this.mapper = mapper; }

    /**
     * Lấy ID đã lưu trong request hoặc tạo UUID và lưu lại để các thành phần dùng chung.
     * Không đọc header X-Request-ID từ client; ID này thuộc request của service, chưa phải distributed trace ID.
     *
     * @param request HTTP request hiện tại
     * @return ID do server tạo cho request
     */
    public static String requestId(HttpServletRequest request) {
        Object existing = request.getAttribute(REQUEST_ID_ATTRIBUTE);
        if (existing instanceof String id) { return id; }
        String id = UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ID_ATTRIBUTE, id);
        return id;
    }

    /**
     * Tạo body lỗi với type dạng URN, code và requestId.
     * Ưu tiên URI gốc của servlet error dispatch; nếu đường dẫn không tạo được URI,
     * dùng URN chứa requestId để chính bộ xử lý lỗi không thất bại.
     * Caller phải truyền code phù hợp để tạo URI và detail an toàn; method không lọc dữ liệu nhạy cảm.
     *
     * @param status HTTP status của lỗi
     * @param code mã lỗi ổn định, ví dụ EMAIL_ALREADY_EXISTS
     * @param detail thông báo an toàn để công khai cho client
     * @param request HTTP request hiện tại
     * @return ProblemDetail mới, chưa được ghi ra response
     */
    public ProblemDetail create(HttpStatusCode status, ErrorCode code, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("urn:mentor-matching:problem:" + code.name().toLowerCase(Locale.ROOT).replace('_', '-')));
        Object original = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String path = original instanceof String value ? value : request.getRequestURI();
        try {
            problem.setInstance(URI.create(path));
        } catch (IllegalArgumentException ignored) {
            // A malformed request path must not break the error handler itself.
            problem.setInstance(URI.create("urn:request:" + requestId(request)));
        }
        problem.setProperty("code", code.name());
        problem.setProperty("requestId", requestId(request));
        return problem;
    }

    /**
     * Ánh xạ HTTP status sang mã lỗi và thông báo công khai cố định.
     * Status chưa có ánh xạ dùng INTERNAL_SERVER_ERROR cho 5xx, REQUEST_REJECTED cho nhóm còn lại.
     *
     * @param status HTTP status của lỗi
     * @param request HTTP request hiện tại
     * @return body lỗi chuẩn có metadata của request
     */
    public ProblemDetail standard(HttpStatusCode status, HttpServletRequest request) {
        ErrorCode code = switch (status.value()) {
            case 400 -> ErrorCode.INVALID_REQUEST;
            case 401 -> ErrorCode.AUTHENTICATION_REQUIRED;
            case 403 -> ErrorCode.ACCESS_DENIED;
            case 404 -> ErrorCode.RESOURCE_NOT_FOUND;
            case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
            case 406 -> ErrorCode.NOT_ACCEPTABLE;
            case 409 -> ErrorCode.CONFLICT;
            case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
            case 429 -> ErrorCode.TOO_MANY_REQUESTS;
            case 503 -> ErrorCode.SERVICE_UNAVAILABLE;
            default -> status.is5xxServerError() ? ErrorCode.INTERNAL_SERVER_ERROR : ErrorCode.REQUEST_REJECTED;
        };
        String detail = switch (status.value()) {
            case 400 -> "The request is invalid.";
            case 401 -> "Authentication is required to access this resource.";
            case 403 -> "You do not have permission to perform this request.";
            case 404 -> "The requested resource was not found.";
            case 405 -> "The HTTP method is not supported for this resource.";
            case 406 -> "The requested response format is not supported.";
            case 409 -> "The request conflicts with the current resource state.";
            case 415 -> "The request content type is not supported.";
            case 429 -> "Too many requests. Please try again later.";
            case 503 -> "The service is temporarily unavailable.";
            default -> status.is5xxServerError() ? "An unexpected error occurred." : "The request could not be processed.";
        };
        return create(status, code, detail, request);
    }

    /**
     * Ghi trực tiếp JSON lỗi vào servlet response, thường dùng trong security filter chain.
     * Đặt status, application/problem+json và X-Request-ID; bỏ qua nếu response đã committed.
     * Giữ các header khác mà caller đã thiết lập, ví dụ WWW-Authenticate.
     *
     * @param status HTTP status của lỗi
     * @param request HTTP request hiện tại
     * @param response HTTP response cần ghi
     * @throws IOException nếu thao tác I/O khi xử lý request/response thất bại
     */
    public void write(HttpStatusCode status, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (response.isCommitted()) { return; }
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader(REQUEST_ID_HEADER, requestId(request));
        mapper.writeValue(response.getOutputStream(), standard(status, request));
    }
}
