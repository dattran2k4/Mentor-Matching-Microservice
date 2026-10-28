package com.mentormatching.user.exception;

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
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Phiên bản tự đăng ký từng exception bằng @ExceptionHandler, không kế thừa lớp handler của Spring.
 * Auth-service giữ phiên bản kế thừa để so sánh; cả hai dùng cùng error contract.
 * Khi bổ sung exception mới, cần chủ động khai báo handler và giữ đúng status/header.
 * Các lỗi trong security filter chain dùng entry point/denied handler riêng.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ProblemResponses problems;

    /**
     * Khởi tạo GlobalExceptionHandler với các dependency cần thiết.
     *
     * @param problems helper tạo và ghi body lỗi thống nhất
     */
    public GlobalExceptionHandler(ProblemResponses problems) { this.problems = problems; }

    /**
     * Một lỗi validation công khai, không chứa giá trị đầu vào bị từ chối.
     *
     * @param field tên field/tham số hoặc request cho lỗi cấp object
     * @param message thông báo an toàn cho client
     */
    public record FieldViolation(String field, String message) {}

    /**
     * Xử lý validation thất bại trên một object đầu vào của controller.
     * Ví dụ: {@code register(@Valid @RequestBody Registration request)} khi field
     * {@code email} của DTO vi phạm {@code @NotBlank} hoặc {@code @Email}.
     * Cơ chế này cũng áp dụng cho object {@code @ModelAttribute} và {@code @RequestPart}.
     *
     * <p>Spring chọn method này qua {@code @ExceptionHandler(MethodArgumentNotValidException.class)}.
     * Tên method có thể thay đổi vì không override lớp cha.
     * Nếu controller nhận {@code BindingResult} ngay sau argument, controller có thể
     * tự xử lý lỗi. Nếu chữ ký method yêu cầu method validation, Spring dùng
     * {@link HandlerMethodValidationException} thay cho cơ chế validation từng object.
     *
     * <p>Trả lỗi 400 với code {@code VALIDATION_ERROR}, danh sách field/message an toàn,
     * không chứa giá trị bị từ chối. Lỗi cấp object dùng tên {@code request}.
     * Body được chuyển đến {@code finishResponse} để xử lý phần chung.
     *
     * @param ex exception chứa các lỗi field/object trong BindingResult
     * @param webRequest request hiện tại, dùng để tạo instance và requestId
     * @return response ProblemDetail, hoặc null nếu response đã được gửi
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, WebRequest webRequest) {
        HttpHeaders headers = ex.getHeaders();
        HttpStatusCode status = ex.getStatusCode();
        ProblemDetail problem = problems.create(status, "VALIDATION_ERROR", "One or more fields are invalid.",
                ((ServletWebRequest) webRequest).getRequest());
        List<FieldViolation> errors = ex.getBindingResult().getAllErrors().stream()
                .map(error -> new FieldViolation(
                        error instanceof org.springframework.validation.FieldError field ? field.getField() : "request",
                        safeValidationMessage(error.getCode())))
                .distinct().toList();
        problem.setProperty("errors", errors);
        return finishResponse(ex, problem, headers, status, webRequest);
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
     * <p>Method tự đăng ký {@code @ExceptionHandler(HandlerMethodValidationException.class)}.
     * Chỉ {@code @Valid} không tự kích hoạt method validation.
     * Controller có {@code @Validated} ở cấp class dùng validation qua AOP proxy,
     * nên không nên mặc định rằng lỗi của cơ chế đó sẽ đi qua hook này.
     *
     * @param ex exception chứa kết quả validation theo tham số hoặc giá trị trả về
     * @param webRequest request hiện tại, dùng để tạo instance và requestId
     * @return response ProblemDetail, hoặc null nếu response đã được gửi
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex, WebRequest webRequest) {
        HttpHeaders headers = ex.getHeaders();
        HttpStatusCode status = ex.getStatusCode();
        // Return-value validation is a server error, never a client validation failure.
        if (!status.is4xxClientError()) {
            return finishResponse(ex, null, headers, status, webRequest);
        }
        ProblemDetail problem = problems.create(status, "VALIDATION_ERROR", "One or more request parameters are invalid.",
                ((ServletWebRequest) webRequest).getRequest());
        problem.setProperty("errors", ex.getParameterValidationResults().stream()
                .map(result -> {
                    String name = result.getMethodParameter().getParameterName();
                    return new FieldViolation(name != null ? name : "arg" + result.getMethodParameter().getParameterIndex(),
                            "Invalid value.");
                }).toList());
        return finishResponse(ex, problem, headers, status, webRequest);
    }

    /**
     * Lỗi HTTP method; giữ header Allow.
     * @param ex exception do Spring MVC phát sinh
     * @param request ngữ cảnh request hiện tại
     * @return response lỗi chuẩn, hoặc null nếu response đã committed
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Object> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, WebRequest request) {
        return finishResponse(ex, null, ex.getHeaders(), ex.getStatusCode(), request);
    }

    /**
     * Lỗi Content-Type không hỗ trợ; giữ header gợi ý media type.
     * @param ex exception do Spring MVC phát sinh
     * @param request ngữ cảnh request hiện tại
     * @return response lỗi chuẩn, hoặc null nếu response đã committed
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Object> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex, WebRequest request) {
        return finishResponse(ex, null, ex.getHeaders(), ex.getStatusCode(), request);
    }

    /**
     * Lỗi không đáp ứng được định dạng Accept.
     * @param ex exception do Spring MVC phát sinh
     * @param request ngữ cảnh request hiện tại
     * @return response lỗi chuẩn, hoặc null nếu response đã committed
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Object> handleNotAcceptable(HttpMediaTypeNotAcceptableException ex, WebRequest request) {
        return finishResponse(ex, null, ex.getHeaders(), ex.getStatusCode(), request);
    }

    /**
     * Lỗi binding request, gồm thiếu parameter/header/path variable; giữ status của exception.
     * @param ex exception do Spring MVC phát sinh
     * @param request ngữ cảnh request hiện tại
     * @return response lỗi chuẩn, hoặc null nếu response đã committed
     */
    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<Object> handleRequestBinding(ServletRequestBindingException ex, WebRequest request) {
        return finishResponse(ex, null, ex.getHeaders(), ex.getStatusCode(), request);
    }

    /**
     * Lỗi thiếu multipart part bắt buộc.
     * @param ex exception do Spring MVC phát sinh
     * @param request ngữ cảnh request hiện tại
     * @return response lỗi chuẩn, hoặc null nếu response đã committed
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Object> handleMissingPart(MissingServletRequestPartException ex, WebRequest request) {
        return finishResponse(ex, null, ex.getHeaders(), ex.getStatusCode(), request);
    }

    /**
     * Lỗi không tìm thấy MVC handler.
     * @param ex exception do Spring MVC phát sinh
     * @param request ngữ cảnh request hiện tại
     * @return response lỗi chuẩn, hoặc null nếu response đã committed
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Object> handleNoHandler(NoHandlerFoundException ex, WebRequest request) {
        return finishResponse(ex, null, ex.getHeaders(), ex.getStatusCode(), request);
    }

    /**
     * Lỗi không tìm thấy resource.
     * @param ex exception do Spring MVC phát sinh
     * @param request ngữ cảnh request hiện tại
     * @return response lỗi chuẩn, hoặc null nếu response đã committed
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Object> handleNoResource(NoResourceFoundException ex, WebRequest request) {
        return finishResponse(ex, null, ex.getHeaders(), ex.getStatusCode(), request);
    }

    /**
     * Lỗi hết thời gian xử lý bất đồng bộ.
     * @param ex exception do Spring MVC phát sinh
     * @param request ngữ cảnh request hiện tại
     * @return response lỗi chuẩn, hoặc null nếu response đã committed
     */
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<Object> handleAsyncTimeout(AsyncRequestTimeoutException ex, WebRequest request) {
        return finishResponse(ex, null, ex.getHeaders(), ex.getStatusCode(), request);
    }

    /**
     * Lỗi vượt giới hạn upload.
     * @param ex exception do Spring MVC phát sinh
     * @param request ngữ cảnh request hiện tại
     * @return response lỗi chuẩn, hoặc null nếu response đã committed
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Object> handleUploadTooLarge(MaxUploadSizeExceededException ex, WebRequest request) {
        return finishResponse(ex, null, ex.getHeaders(), ex.getStatusCode(), request);
    }

    /**
     * Lỗi có HTTP status/header do Spring cung cấp; dùng thông báo công khai thay body gốc.
     * @param ex exception do Spring MVC phát sinh
     * @param request ngữ cảnh request hiện tại
     * @return response lỗi chuẩn, hoặc null nếu response đã committed
     */
    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<Object> handleErrorResponse(ErrorResponseException ex, WebRequest request) {
        return finishResponse(ex, null, ex.getHeaders(), ex.getStatusCode(), request);
    }

    /**
     * Lỗi sai kiểu dữ liệu đầu vào; không công khai giá trị bị từ chối.
     * @param ex exception cần xử lý
     * @param request ngữ cảnh request hiện tại
     * @return response lỗi chuẩn, hoặc null nếu response đã committed
     */
    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, WebRequest request) {
        return finishResponse(ex, null, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Lỗi cấu hình chuyển đổi phía server; không quy thành lỗi đầu vào 400.
     * @param ex exception cần xử lý
     * @param request ngữ cảnh request hiện tại
     * @return response lỗi chuẩn, hoặc null nếu response đã committed
     */
    @ExceptionHandler(ConversionNotSupportedException.class)
    public ResponseEntity<Object> handleConversionFailure(ConversionNotSupportedException ex, WebRequest request) {
        return finishResponse(ex, null, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /**
     * Lỗi không đọc được request body, ví dụ JSON sai cú pháp.
     * @param ex exception cần xử lý
     * @param request ngữ cảnh request hiện tại
     * @return response lỗi chuẩn, hoặc null nếu response đã committed
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleUnreadableMessage(HttpMessageNotReadableException ex, WebRequest request) {
        return finishResponse(ex, null, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Lỗi ghi hoặc serialize response body.
     * @param ex exception cần xử lý
     * @param request ngữ cảnh request hiện tại
     * @return response lỗi chuẩn, hoặc null nếu response đã committed
     */
    @ExceptionHandler(HttpMessageNotWritableException.class)
    public ResponseEntity<Object> handleUnwritableMessage(HttpMessageNotWritableException ex, WebRequest request) {
        return finishResponse(ex, null, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /**
     * Lỗi method validation ngoài loại MVC chuyên biệt; trả lỗi server an toàn.
     * @param ex exception cần xử lý
     * @param request ngữ cảnh request hiện tại
     * @return response lỗi chuẩn, hoặc null nếu response đã committed
     */
    @ExceptionHandler(MethodValidationException.class)
    public ResponseEntity<Object> handleMethodValidation(MethodValidationException ex, WebRequest request) {
        return finishResponse(ex, null, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /**
     * Response bất đồng bộ không còn dùng được, ví dụ client đã ngắt kết nối.
     * Không cố ghi thêm body lên kết nối bị hỏng.
     * @param ex exception báo response không thể sử dụng
     * @return null vì không còn response để ghi
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public ResponseEntity<Object> handleUnusableAsyncResponse(AsyncRequestNotUsableException ex) {
        return null;
    }

    /**
     * Helper nội bộ dùng chung cho các handler MVC đã đăng ký: dừng nếu response đã committed, giữ body
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
    private ResponseEntity<Object> finishResponse(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest webRequest) {
        ServletWebRequest servlet = (ServletWebRequest) webRequest;
        if (servlet.getResponse() != null && servlet.getResponse().isCommitted()) { return null; }
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
        if (constraint == null) { return "Invalid value."; }
        return switch (constraint) {
            case "NotBlank", "NotEmpty", "NotNull" -> "This field is required.";
            case "Email" -> "Must be a valid email address.";
            case "Size", "Length" -> "The length is outside the allowed range.";
            case "Min", "Max", "DecimalMin", "DecimalMax", "Positive", "PositiveOrZero", "Negative", "NegativeOrZero" -> "The value is outside the allowed range.";
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
