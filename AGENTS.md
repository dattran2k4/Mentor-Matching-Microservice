# Mentor Matching Microservice — Agent Guide

## Đọc trước khi làm

1. `README.md`
2. `docs/service-boundaries.md`
3. `docs/architecture.md`
4. Tài liệu nghiệp vụ/giao tiếp/dữ liệu liên quan đến thay đổi.

## Nguyên tắc

- Đây là repo microservices mới; không áp dụng máy móc Clean Architecture của repo Mentor-Matching cũ.
- Dùng feature-first Layered Architecture và command/query separation trong từng service nghiệp vụ.
- Giữ 8 service nghiệp vụ và 2 service hạ tầng theo docs; không thêm core-service, đổi tên auth-service hoặc tách thêm service khi chưa có quyết định cập nhật.
- Tuân thủ `docs/database-strategy.md`: MySQL cho Auth/User/Mentor; PostgreSQL cho Booking/Payment/Review; MongoDB cho Media/Notification. SQL dùng Flyway, MongoDB dùng versioned changesets và replica set khi dùng transaction.
- Mỗi service sở hữu database, entity/document, repository và migration riêng. Không import entity/repository từ service khác, không join hoặc ghi chéo database.
- Controller mỏng; service xử lý nghiệp vụ, phân quyền tài nguyên và transaction cục bộ; repository xử lý persistence.
- DTO API/event tách khỏi JPA entity. Không bắt buộc port/adapter cho mọi dependency.
- Gateway và Discovery không chứa nghiệp vụ; không ép CQRS lên hai project này.
- Không chia sẻ JPA entity, business service hoặc DTO nội bộ bằng một common module. Contract liên service phải có version rõ ràng.
- Các mục Proposed/Open trong docs không được mô tả như tính năng đã có. Giải quyết quyết định chặn trước khi implement phần phụ thuộc.
- Với request chỉ phân tích, không sửa file. Trước khi sửa, đọc file liên quan và nêu ngắn gọn mục đích.
- Giữ nguyên công việc có sẵn của người dùng; không tự commit/push hoặc đổi remote khi chưa được yêu cầu.

## Kiểm chứng

Hiện có skeleton Auth/User/Mentor/Booking/Payment/Review và docs. Chạy `./mvnw clean verify` từ thư mục service thay đổi; chưa có test case nghiệp vụ. Khi thêm test/contracts, chạy các kiểm thử tương ứng. Với docs, kiểm tra link nội bộ và consistency. Xem `docs/service-initialization.md` cho dependency/version và giới hạn hiện tại.
