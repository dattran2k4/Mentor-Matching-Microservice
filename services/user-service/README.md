# user-service

Java 21, Spring Boot 4.1.2, Spring Cloud 2025.1.4; package `com.mentormatching.user`.
MySQL database riêng `mm_user`, port mặc định 8082.

Đã có class khởi động và error handling RFC 9457; chưa có API nghiệp vụ, entity, repository, migration SQL hoặc JWT implementation.

## Build và test

```bash
./mvnw clean verify
```

Web MVC slice tests kiểm chứng ProblemDetail, validation, security errors, request IDs và fallback. Không cần database/Docker cho các test này; chưa kiểm thử persistence.

## Chạy

Tạo MySQL database và user riêng, đặt DB_PASSWORD qua shell/IDE. Có thể override DB_URL/DB_USERNAME/SERVER_PORT; `.env.example` chỉ là mẫu, Spring Boot không tự nạp `.env`.

```bash
./mvnw spring-boot:run
```

Startup cần MySQL và credentials hợp lệ. Flyway bật nhưng chưa có SQL migration; JPA chỉ validate. Eureka và Swagger mặc định tắt như cấu hình skeleton.

## ProblemDetail

Handler dùng `@ExceptionHandler` trực tiếp, không kế thừa `ResponseEntityExceptionHandler`; xem JavaDoc trong [GlobalExceptionHandler](src/main/java/com/mentormatching/user/exception/GlobalExceptionHandler.java). Errors trả trực tiếp application/problem+json và X-Request-ID. HTTP Basic là baseline kỹ thuật; form login/redirect tắt, CSRF giữ bật, health public nhưng ẩn chi tiết. Chưa có user store, login/register hoặc JWT. Khi thêm resource server JWT phải cấu hình handler/challenge Bearer phù hợp.
