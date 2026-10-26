# payment-service

Skeleton độc lập, chỉ có class khởi động Spring Boot. Chưa có API, entity, repository, nghiệp vụ, security configuration, schema SQL hay test case.

- Java 21; Spring Boot 4.1.2; Spring Cloud 2025.1.4; Maven Wrapper riêng.
- Package `com.mentormatching.payment`; port mặc định `8085`.
- DBMS **PostgreSQL**, database riêng `mm_payment` và user runtime riêng.
- JDBC driver, Flyway module và Testcontainers theo đúng PostgreSQL.
- Dependency chung và phiên bản: [service initialization](../../docs/service-initialization.md).

## Build

Từ thư mục service:

```bash
./mvnw clean verify
```

Build kiểm tra dependency resolution, compile và executable JAR; hiện chưa có test case. Chưa cần database/Docker cho build này. Integration tests sau phải dùng PostgreSQL thực, không thay bằng H2.

## Chạy sau khi chuẩn bị database

Tạo database/user riêng; đặt `DB_PASSWORD` qua shell hoặc IDE. `DB_URL`, `DB_USERNAME`, `SERVER_PORT` có giá trị mặc định trong application.yml. `.env.example` là mẫu tham khảo, Spring Boot không tự nạp `.env`.

```bash
./mvnw spring-boot:run
```

Flyway bật nhưng thư mục migration chưa có SQL; JPA chỉ validate, không tự tạo schema. Startup cần database và credentials hợp lệ. Eureka mặc định tắt đến khi có Discovery. Swagger mặc định tắt; bật API_DOCS_ENABLED khi cần và cấu hình quyền truy cập sau. Actuator expose health/info, ẩn health details. Spring Security vẫn auto-configuration mặc định; chưa có JWT decoder/issuer configuration của dự án.

## Stripe

Đã thêm `com.stripe:stripe-java:34.0.0`. Chưa tạo Stripe client, cấu hình secret, checkout hoặc webhook. Khi implement cần chốt API version/webhook version phù hợp SDK; không lấy secret từ repo cũ.
