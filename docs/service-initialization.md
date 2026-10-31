# Khởi tạo các service nghiệp vụ

Cập nhật ngày 2026-10-27. Phạm vi: **chỉ init sáu project Auth, User, Mentor, Booking, Payment và Review**, không triển khai nghiệp vụ, entity, controller, repository, SQL schema hoặc security configuration.

## Project đã tạo

| Project | Package | Port mặc định | Database |
|---|---|---|---|
| [auth-service](../services/auth-service/README.md) | com.mentormatching.auth | 8081 | mm_auth / MySQL |
| [user-service](../services/user-service/README.md) | com.mentormatching.user | 8082 | mm_user / MySQL |
| [mentor-service](../services/mentor-service/README.md) | com.mentormatching.mentor | 8083 | mm_mentor / MySQL |
| [booking-service](../services/booking-service/README.md) | com.mentormatching.booking | 8084 | mm_booking / PostgreSQL |
| [payment-service](../services/payment-service/README.md) | com.mentormatching.payment | 8085 | mm_payment / PostgreSQL |
| [review-service](../services/review-service/README.md) | com.mentormatching.review | 8086 | mm_review / PostgreSQL |

Mỗi project có `pom.xml`, Maven Wrapper, `.env.example`, README và class Spring Boot application. Chưa tạo các layer rỗng hay implementation CQRS trước khi có feature. Các project không phụ thuộc nhau và không có root parent/aggregator riêng; mỗi POM kế thừa Spring Boot parent trực tiếp.

## Phiên bản

- Java 21.
- Spring Boot 4.1.2 (stable, không SNAPSHOT/RC).
- Spring Cloud BOM 2025.1.4.
- Maven Wrapper 3.3.4, Maven distribution 3.10.0 theo Initializr.
- springdoc OpenAPI 3.1.1.
- MySQL/PostgreSQL JDBC, Flyway, Lombok, Spring Security và Testcontainers theo dependency management của Boot; không pin bản độc lập gây lệch BOM.

Initializr metadata trả ID `4.1.2.RELEASE`; Maven parent dùng tọa độ release `4.1.2`. Cặp Cloud/Boot được kiểm tra qua compatibility matrix, không copy version từ monolith cũ.

## Dependency nền cho cả sáu project

| Nhóm | Dependency/mục đích |
|---|---|
| HTTP | spring-boot-starter-webmvc |
| Validation | spring-boot-starter-validation |
| Persistence | spring-boot-starter-data-jpa; mysql-connector-j hoặc org.postgresql:postgresql (runtime) theo owner |
| Migration | spring-boot-starter-flyway; flyway-mysql hoặc flyway-database-postgresql theo DBMS |
| Security | spring-boot-starter-security; spring-boot-starter-security-oauth2-resource-server, gồm thư viện JOSE/JWT theo starter |
| Monitoring | spring-boot-starter-actuator |
| Discovery | spring-cloud-starter-netflix-eureka-client |
| API docs | springdoc-openapi-starter-webmvc-ui |
| Build tooling | Lombok optional + annotation processor; Spring configuration processor |
| Test | Boot test starters cho Web MVC/JPA/Security/OAuth2/Validation/Flyway/Actuator; Spring Boot Testcontainers, Testcontainers JUnit Jupiter và module MySQL/PostgreSQL đúng DBMS |

Security dependency chưa cung cấp login/register của dự án. Chưa chọn OAuth2 Authorization Server/full OIDC hay custom token issuance, nên không thêm Authorization Server starter hoặc thư viện JWT thứ hai trùng JOSE. JWT issuer/audience/JWKS, filter chain và authorization sẽ làm ở bước nghiệp vụ.

Không thêm broker (chưa chọn), Redis, MongoDB driver, SDK provider ngoài Stripe Payment, mail sender hoặc OpenFeign chỉ để đủ số dependency. Auth sẽ yêu cầu Notification gửi email theo contract, không tự sở hữu email delivery. HTTP client cụ thể sẽ chọn khi có lời gọi liên service đầu tiên.

## Dependency riêng theo service

- Auth/User/Mentor: `com.mysql:mysql-connector-j`, `org.flywaydb:flyway-mysql`, `org.testcontainers:testcontainers-mysql`.
- Booking/Payment/Review: `org.postgresql:postgresql`, `org.flywaydb:flyway-database-postgresql`, `org.testcontainers:testcontainers-postgresql`; không giữ driver/module MySQL thừa.
- Payment: thêm `com.stripe:stripe-java:34.0.0` (stable, pin riêng vì không thuộc Boot BOM). Chưa có Stripe client, secret hoặc webhook. Kiểm tra API version khi implement, không sao chép integration cũ mặc định tương thích.
- Booking: chưa tạo extension btree_gist hoặc range constraint; triển khai bằng migration khi chốt schema.
- Mentor dùng Media API sau này, không thêm Cloudinary SDK vào Mentor.

## Cấu hình và trạng thái chạy

- Datasource dùng database theo bảng trên và user riêng; DB_PASSWORD bắt buộc từ môi trường, không commit secret.
- Flyway bật, migration folder rỗng có `.gitkeep`; JPA `ddl-auto=validate`, open-in-view tắt.
- Chưa tạo database/user hay container. Startup cần MySQL/PostgreSQL thực theo service và credential hợp lệ.
- Eureka mặc định tắt vì chưa có Discovery; bật bằng EUREKA_ENABLED khi server sẵn sàng.
- Actuator chỉ expose health/info; không hiển thị chi tiết health.
- OpenAPI/Swagger UI mặc định tắt; API_DOCS_ENABLED bật khi cần, quyền truy cập còn theo Spring Security mặc định.
- Chưa cấu hình JWT, chưa có authentication flow của dự án. Không coi auto-configuration mặc định là security production.
- `.env.example` chỉ là mẫu; Spring Boot không tự nạp `.env`. Dùng biến môi trường shell/IDE.

## Build

Từ từng thư mục service:

```bash
./mvnw clean verify
```

Build không cần database/Docker khi chưa có test dùng chúng. Hiện không có test case; lệnh verify kiểm chứng dependency resolution, compile và executable JAR packaging. Dependency Testcontainers đã chuẩn bị cho integration tests sau; không dùng H2 thay MySQL/PostgreSQL.

Khi đã chuẩn bị DBMS tương ứng và biến môi trường:

```bash
./mvnw spring-boot:run
```

Chưa có Docker Compose, CI, Gateway, Discovery hoặc database migrations trong lượt init này.

## Nguồn version/dependency

- [Spring Initializr metadata](https://start.spring.io/metadata/client).
- [Spring Cloud compatibility](https://spring.io/projects/spring-cloud/).
- [Spring Boot releases](https://docs.spring.io/spring-boot/).
- [springdoc 3.1.1 release](https://github.com/springdoc/springdoc-openapi/releases/tag/v3.1.1).

Nguồn bổ sung: [PostgreSQL Flyway module](https://documentation.red-gate.com/flyway/reference/database-driver-reference/postgresql-database), [Stripe Java SDK](https://github.com/stripe/stripe-java).
