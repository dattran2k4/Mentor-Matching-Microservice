# user-service

Project Spring Boot khởi tạo, chưa có controller, entity, repository, API nghiệp vụ, JWT issuer/decoder configuration hay migration SQL.

- Java 21; Spring Boot 4.1.2; Spring Cloud 2025.1.4.
- Package: `com.mentormatching.user`.
- Port mặc định: `8082`; MySQL database: `mm_user` (phải tạo riêng).
- Dependency và trạng thái: [service initialization](../../docs/service-initialization.md).

## Build

Chạy từ thư mục service:

```bash
./mvnw clean verify
```

Hiện chưa có test case; lệnh trên kiểm tra dependency resolution, compile và đóng gói executable JAR. Dependency test đã sẵn sàng cho bước triển khai sau.

## Chạy khi đã chuẩn bị MySQL

Tạo database/user riêng và cấp quyền phù hợp. Khai báo `DB_PASSWORD` qua shell hoặc IDE; dùng `DB_URL`/`DB_USERNAME` nếu khác giá trị mặc định. `.env.example` là mẫu tham khảo, Spring Boot không tự nạp file `.env`.

```bash
./mvnw spring-boot:run
```

Datasource/Flyway cần MySQL sẵn sàng; build không cần MySQL. Chưa có migration tạo bảng. Eureka mặc định tắt vì Discovery chưa được tạo. Chỉ bật `EUREKA_ENABLED=true` khi đã có server.

Spring Security giữ auto-configuration mặc định, chưa có cấu hình JWT hay API login nghiệp vụ; không coi service đã có chức năng auth chỉ vì đã thêm dependency. OpenAPI mặc định tắt; bật `API_DOCS_ENABLED=true` khi cần và thiết kế quyền truy cập trong bước security sau.
