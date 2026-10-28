# Mentor-Matching-Microservice

Hệ thống kết nối học viên và mentor, được xây lại theo microservices trong repository độc lập với Mentor-Matching hiện tại.

**Trạng thái:** đã khởi tạo sáu project Auth, User, Mentor, Booking, Payment và Review dưới `services/` với dependency/cấu hình nền, chưa có nghiệp vụ. Các service còn lại là cấu trúc đích; chưa có Docker Compose hoặc pipeline. Xem [hướng dẫn initialization](docs/service-initialization.md).

## Hướng kiến trúc đã thống nhất

- Monorepo gồm **8 service nghiệp vụ và 2 service hạ tầng**, build/deploy độc lập.
- Tên service xác thực là **auth-service**, không có identity-service song song.
- Bên trong service nghiệp vụ: **feature-first Layered Architecture + CQRS ở mức command/query**.
- Mỗi service nghiệp vụ sở hữu dữ liệu và migration riêng (Flyway cho SQL; versioned changesets cho MongoDB). Ban đầu command/query dùng chung database của service.
- Không có core-service trung gian; khởi tạo trực tiếp các service đã chia boundary.
- Discovery dùng hướng Spring Cloud Netflix Eureka; Gateway dùng hướng Spring Cloud Gateway. Hai project đầu pin Java 21, Spring Boot 4.1.2 và Spring Cloud 2025.1.4 theo Spring Initializr và compatibility matrix.

DBMS đã chọn: **MySQL** cho Auth/User/Mentor; **PostgreSQL** cho Booking/Payment/Review; **MongoDB** cho Media/Notification. Mỗi service có database riêng; Discovery/Gateway không có DB nghiệp vụ. Xem [quy định DBMS](docs/database-strategy.md).

## Đọc tài liệu

1. [Tổng quan dự án](docs/project-overview.md): mục tiêu, vai trò, phạm vi.
2. [Danh mục service và boundary](docs/service-boundaries.md): ai sở hữu nghiệp vụ và dữ liệu nào.
3. [Kiến trúc code](docs/architecture.md): layered, CQRS, repository layout.
4. [Luồng nghiệp vụ](docs/business-flows.md): đăng ký, onboarding, booking, payment, review, media.
5. [API và event](docs/integration-contracts.md): giao tiếp, retry, outbox, idempotency.
6. [Dữ liệu và bảo mật](docs/data-and-security.md): ownership, JWT, quyền tài nguyên, migration.
7. [Lộ trình triển khai](docs/implementation-plan.md): các giai đoạn và tiêu chí hoàn thành.
8. [Quyết định còn mở](docs/decisions.md): những điểm cần bàn tiếp trước khi triển khai phần liên quan.

Agent bắt đầu từ [AGENTS.md](AGENTS.md). Các đề xuất API, event, schema và state mới trong docs là thiết kế dự kiến, không phải tính năng đã implement.

## Cấu trúc đích

```text
services/
  auth-service/
  user-service/
  mentor-service/
  booking-service/
  payment-service/
  review-service/
  media-service/
  notification-service/
platform/
  discovery-service/
  api-gateway-service/
infrastructure/
docs/
```

Mỗi service build độc lập bằng `./mvnw clean verify` từ thư mục tương ứng. Chưa có root aggregator; hiện chưa có test case, chỉ kiểm chứng build/package. Chạy ứng dụng cần database MySQL hoặc PostgreSQL riêng theo service và biến môi trường như README từng service. Không sao chép nguyên `.git`, secrets, database production hoặc migration cũ vào project mới.
