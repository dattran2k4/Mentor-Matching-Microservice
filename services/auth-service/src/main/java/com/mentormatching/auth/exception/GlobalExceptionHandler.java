package com.mentormatching.auth.exception;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Tùy chỉnh lỗi Spring MVC qua các hook của ResponseEntityExceptionHandler
 * và đăng ký handler riêng cho lỗi nghiệp vụ hoặc lỗi chưa được xử lý.
 * Các lỗi trong security filter chain dùng entry point/denied handler riêng.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ProblemResponses problems;

    /**
     * Khởi tạo GlobalExceptionHandler với các dependency cần thiết.
     *
     * @param problems helper tạo và ghi body lỗi thống nhất
     */
    public GlobalExceptionHandler(ProblemResponses problems) {
        this.problems = problems;
    }

    /**
     * Một lỗi validation công khai, không chứa giá trị đầu vào bị từ chối.
     *
     * @param field tên field/tham số hoặc request cho lỗi cấp object
     * @param message thông báo an toàn cho client
     */
    public record FieldViolation(String field, String message) {
    }

    /**
     * Xử lý validation thất bại trên một object đầu vào của controller.
     * Ví dụ: {@code register(@Valid @RequestBody Registration request)} khi field
     * {@code email} của DTO vi phạm {@code @NotBlank} hoặc {@code @Email}.
     * Cơ chế này cũng áp dụng cho object {@code @ModelAttribute} và {@code @RequestPart}.
     *
     * <p>Spring gọi hook này khi validation từng argument phát sinh
     * {@link MethodArgumentNotValidException}; lớp cha đã đăng ký exception nên
     * không thêm {@code @ExceptionHandler} vào method override này.
     * Nếu controller nhận {@code BindingResult} ngay sau argument, controller có thể
     * tự xử lý lỗi. Nếu chữ ký method yêu cầu method validation, Spring dùng
     * {@link HandlerMethodValidationException} thay cho cơ chế validation từng object.
     *
     * <p>Trả lỗi 400 với code {@code VALIDATION_ERROR}, danh sách field/message an toàn,
     * không chứa giá trị bị từ chối. Lỗi cấp object dùng tên {@code request}.
     * Body được chuyển đến {@code handleExceptionInternal} để xử lý phần chung.
     *
     * @param ex         exception chứa các lỗi field/object trong BindingResult
     * @param headers    các HTTP header do Spring cung cấp, cần được giữ lại
     * @param status     HTTP status do Spring xác định (400 cho exception này)
     * @param webRequest request hiện tại, dùng để tạo instance và requestId
     * @return response ProblemDetail, hoặc null nếu response đã được gửi
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status, WebRequest webRequest) {
        ProblemDetail problem = problems.create(status, "VALIDATION_ERROR", "One or more fields are invalid.",
                ((ServletWebRequest) webRequest).getRequest());
        List<FieldViolation> errors = ex.getBindingResult().getAllErrors().stream()
                .map(error -> new FieldViolation(
                        error instanceof FieldError field ? field.getField() : "request",
                        safeValidationMessage(error.getCode())))
                .distinct().toList();
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, webRequest);
    }

    /**
     * Xử lý validation ở cấp method của controller bằng cơ chế tích hợp của Spring MVC.
     * Ví dụ đầu vào: {@code list(@RequestParam @Min(1) int page)} nhận page bằng 0.
     * Constraint trực tiếp trên tham số kích hoạt method validation; cơ chế này cũng
     * kiểm tra constraint lồng trong object có {@code @Valid}.
     * Vì vậy exception này không chỉ dành cho query/path parameter.
     *
     * <p>Đầu vào không hợp lệ trả 400 với code {@code VALIDATION_ERROR} và lỗi theo
     * tên tham số. Giá trị trả về vi phạm constraint (ví dụ method có {@code @Min(1)}
     * nhưng trả 0) là lỗi server 500: giữ thông báo chung, không đưa chi tiết validation
     * nội bộ ra client và không đổi thành lỗi đầu vào 400.
     *
     * <p>Lớp cha đã đăng ký exception và gọi hook này; không cần thêm
     * {@code @ExceptionHandler}. Chỉ {@code @Valid} không tự kích hoạt method validation.
     * Controller có {@code @Validated} ở cấp class dùng validation qua AOP proxy,
     * nên không nên mặc định rằng lỗi của cơ chế đó sẽ đi qua hook này.
     *
     * @param ex         exception chứa kết quả validation theo tham số hoặc giá trị trả về
     * @param headers    các HTTP header do Spring cung cấp, cần được giữ lại
     * @param status     HTTP status do Spring xác định: 400 cho đầu vào, 500 cho đầu ra
     * @param webRequest request hiện tại, dùng để tạo instance và requestId
     * @return response ProblemDetail, hoặc null nếu response đã được gửi
     */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers, HttpStatusCode status, WebRequest webRequest) {
        // Return-value validation is a server error, never a client validation failure.
        if (!status.is4xxClientError()) {
            return handleExceptionInternal(ex, null, headers, status, webRequest);
        }
        ProblemDetail problem = problems.create(status, "VALIDATION_ERROR", "One or more request parameters are invalid.",
                ((ServletWebRequest) webRequest).getRequest());
        problem.setProperty("errors", ex.getParameterValidationResults().stream()
                .map(result -> {
                    String name = result.getMethodParameter().getParameterName();
                    return new FieldViolation(name != null ? name : "arg" + result.getMethodParameter().getParameterIndex(),
                            "Invalid value.");
                }).toList());
        return handleExceptionInternal(ex, problem, headers, status, webRequest);
    }

    /**
     * Chuẩn hóa body do các hook của lớp cha yêu cầu tạo.
     * Chủ động bỏ qua defaultDetail và các tham số message để dùng thông báo công khai cố định,
     * tránh lộ chi tiết parser hoặc dữ liệu đầu vào; metadata do ProblemResponses bổ sung.
     *
     * @param ex exception cần xử lý
     * @param status HTTP status của lỗi
     * @param defaultDetail thông báo mặc định của framework, không sử dụng ở đây
     * @param detailMessageCode mã tra cứu thông báo của framework, không sử dụng ở đây
     * @param detailMessageArguments tham số thông báo của framework, không sử dụng ở đây
     * @param webRequest ngữ cảnh request MVC hiện tại
     * @return body lỗi chuẩn theo status
     */
    @Override
    protected ProblemDetail createProblemDetail(Exception ex, HttpStatusCode status, String defaultDetail,
                                                String detailMessageCode, Object[] detailMessageArguments, WebRequest webRequest) {
        // Framework-generated bodies use the same safe messages and metadata as our custom handlers.
        return problems.standard(status, ((ServletWebRequest) webRequest).getRequest());
    }

    /**
     * Điểm xử lý chung cho các hook MVC: dừng nếu response đã committed, giữ body
     * ProblemDetail đã tùy chỉnh hoặc tạo body chuẩn nếu chưa có.
     * Ghi log lỗi 5xx kèm requestId rồi dựng response; không phân loại chi tiết lỗi validation.
     *
     * @param ex exception cần xử lý
     * @param body body đã tùy chỉnh, có thể null
     * @param headers header do Spring cung cấp, cần giữ lại
     * @param status HTTP status của lỗi
     * @param webRequest ngữ cảnh request MVC hiện tại
     * @return response lỗi, hoặc null nếu response đã committed
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode status, WebRequest webRequest) {
        ServletWebRequest servlet = (ServletWebRequest) webRequest;
        if (servlet.getResponse() != null && servlet.getResponse().isCommitted()) {
            return null;
        }
        HttpServletRequest request = servlet.getRequest();
        ProblemDetail problem = body instanceof ProblemDetail detail ? detail : problems.standard(status, request);
        if (status.is5xxServerError()) {
            log.error("Request failed requestId={}", ProblemResponses.requestId(request), ex);
        }
        return response(problem, headers, status);
    }

    /**
     * Trả status, code và thông báo công khai từ ApiException.
     * Không che lại thông báo; trách nhiệm bảo đảm thông báo an toàn thuộc nơi tạo exception.
     *
     * @param ex exception cần xử lý
     * @param request HTTP request hiện tại
     * @return response lỗi nghiệp vụ 4xx
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Object> handleBusiness(ApiException ex, HttpServletRequest request) {
        return response(problems.create(ex.getStatus(), ex.getCode(), ex.getMessage(), request),
                new HttpHeaders(), ex.getStatus());
    }

    /**
     * Xử lý AuthenticationException đi tới MVC advice bằng response 401 và Basic challenge.
     * Lỗi phát sinh trong security filter chain dùng ProblemAuthenticationEntryPoint riêng.
     *
     * @param ex exception cần xử lý
     * @param request HTTP request hiện tại
     * @return response yêu cầu xác thực
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Mentor Matching\"");
        return response(problems.standard(HttpStatus.UNAUTHORIZED, request), headers, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Xử lý AccessDeniedException đi tới MVC advice bằng response 403 an toàn.
     * Không thay thế ProblemAccessDeniedHandler của security filter chain.
     *
     * @param ex exception cần xử lý
     * @param request HTTP request hiện tại
     * @return response từ chối truy cập
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return response(problems.standard(HttpStatus.FORBIDDEN, request), new HttpHeaders(), HttpStatus.FORBIDDEN);
    }

    /**
     * Fallback cho exception chưa có handler phù hợp trong MVC.
     * Ghi exception đầy đủ vào log cùng requestId, chỉ trả thông báo 500 chung cho client.
     *
     * @param ex exception cần xử lý
     * @param request HTTP request hiện tại
     * @return response lỗi server đã ẩn chi tiết nội bộ
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected failure requestId={}", ProblemResponses.requestId(request), ex);
        return response(problems.standard(HttpStatus.INTERNAL_SERVER_ERROR, request),
                new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Ánh xạ tên constraint sang thông báo công khai cố định.
     * Không dùng rejected value hoặc message tùy ý; constraint null/chưa hỗ trợ dùng thông báo chung.
     *
     * @param constraint tên constraint validation, có thể null
     * @return thông báo validation an toàn
     */
    private String safeValidationMessage(String constraint) {
        if (constraint == null) {
            return "Invalid value.";
        }
        return switch (constraint) {
            case "NotBlank", "NotEmpty", "NotNull" -> "This field is required.";
            case "Email" -> "Must be a valid email address.";
            case "Size", "Length" -> "The length is outside the allowed range.";
            case "Min", "Max", "DecimalMin", "DecimalMax", "Positive", "PositiveOrZero", "Negative", "NegativeOrZero" ->
                    "The value is outside the allowed range.";
            default -> "Invalid value.";
        };
    }

    /**
     * Dựng ResponseEntity từ ProblemDetail đã được tạo bởi các helper của service.
     * Sao chép header gốc để giữ Allow/Retry-After, rồi đặt content type và X-Request-ID.
     * Body cần có property requestId do ProblemResponses cung cấp.
     *
     * @param problem body ProblemDetail có metadata của service
     * @param original header gốc cần sao chép
     * @param status HTTP status của lỗi
     * @return response có body, status và header thống nhất
     */
    private ResponseEntity<Object> response(ProblemDetail problem, HttpHeaders original, HttpStatusCode status) {
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(original); // Keep framework headers such as Allow and Retry-After.
        headers.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        headers.set(ProblemResponses.REQUEST_ID_HEADER, (String) problem.getProperties().get("requestId"));
        return new ResponseEntity<>(problem, headers, status);
    }
}
