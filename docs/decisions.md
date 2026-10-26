# Quyết định kiến trúc và câu hỏi còn mở

Cập nhật ngày 2026-10-27. “Đã thống nhất” là hướng người dùng đã chọn; “Đề xuất” cần kiểm chứng trước triển khai phần phụ thuộc.

## Đã thống nhất

| Quyết định | Nội dung |
|---|---|
| Repository | Project mới Mentor-Matching-Microservice, remote/history độc lập |
| Decomposition | 8 service nghiệp vụ + Discovery + Gateway; không core-service |
| Naming | auth-service, không identity-service song song |
| Internal architecture | Feature-first Layered Architecture + CQRS command/query |
| Data boundary | Owner database/migration riêng, không đọc/ghi trực tiếp dữ liệu service khác |
| DBMS baseline | MySQL: Auth/User/Mentor; PostgreSQL: Booking/Payment/Review; MongoDB: Media/Notification. [Chi tiết và trade-off](database-strategy.md), quyết định 2026-10-26 theo yêu cầu đa DBMS |
| Platform direction | Eureka Discovery và Spring Cloud Gateway |
| Delivery sequence | Thiết kế boundary/contracts, skeleton service, triển khai từng luồng |

## Baseline initialization ngày 2026-10-26

Theo yêu cầu chỉ init Auth/User: hai Maven project độc lập, Java 21, Spring Boot 4.1.2, Spring Cloud 2025.1.4, Maven Wrapper 3.10.0 và springdoc 3.1.1. Đây là lựa chọn kỹ thuật cho skeleton, chưa chốt JWT issuance protocol, broker hoặc schema. Xem [service initialization](service-initialization.md).

## Mở rộng skeleton ngày 2026-10-27

Init Mentor (MySQL), Booking/Payment/Review (PostgreSQL) cùng baseline Auth/User. Mỗi project có driver, Flyway module, Testcontainers đúng DBMS; Payment thêm Stripe Java 34.0.0. Chưa tạo schema, API hoặc nghiệp vụ; chưa cấu hình broker/provider secrets.

## Đề xuất trong bộ docs

- Auth provision User qua AccountRegistered + outbox/inbox.
- JWT bất đối xứng/JWKS; mỗi resource service kiểm tra token và quyền nghiệp vụ.
- Catalog/location nằm trong Mentor; availability nằm trong Booking.
- Avatar chuẩn thuộc User; Mentor giữ projection khi cần.
- PaymentSucceeded truyền bất đồng bộ, Booking quyết định confirmation.
- Review dùng Booking API kiểm tra eligibility ban đầu; rating projection cập nhật qua event.
- Media hướng Cloudinary direct upload, verify metadata phía server.

Các đề xuất không đồng nghĩa đã implement hoặc đã chốt schema chi tiết.

## Cần chốt theo thời điểm

| Câu hỏi | Chặn giai đoạn nào? | Hướng đánh giá |
|---|---|---|
| Baseline cho service còn lại | Khi mở rộng skeleton | Sáu service hiện dùng Java 21, Maven Wrapper 3.10.0, Boot 4.1.2, Cloud 2025.1.4; kiểm tra tương thích khi đổi |
| DBMS version/image tag/driver và Mongo migration runner? | Skeleton/persistence implementation | Engine đã chốt; chọn release tương thích, pin tag và runner có version/coordination |
| Root Maven aggregator và đồng bộ version cho service tiếp theo? | Khi mở rộng skeleton | Sáu project hiện build độc lập, mỗi project dùng Boot parent/BOM và Maven Wrapper |
| Broker/transport gì? | AccountRegistered và các flow event | Retry, ordering, local operation, kỹ năng đội |
| ID BIGINT/UUID/khác? | Schema và public contracts | Sinh ID, khả năng import dữ liệu, API stability |
| Currency, rounding, timezone? | Giá, availability, booking schema | Dùng decimal/minor units có contract, lịch có timezone |
| Role MENTOR cấp lúc nào? ADMIN/MANAGER khác nhau ra sao? | Onboarding và authorization | Tách account role khỏi mentor approval eligibility |
| Token TTL, account lock hiệu lực khi nào, service auth? | Security implementation | Cân bằng revocation và chi phí lookup; không hứa JWT revoke tức thời |
| PENDING giữ lịch bao lâu, expiry state và checkout expiry? | Booking/Payment | Chống giữ chỗ vô hạn, xử lý payment đến muộn |
| Chốt giá có hiệu lực bao lâu, mentor bị đình chỉ sau lookup xử lý sao? | Booking contract | Snapshot/version, thời điểm áp dụng quy tắc rõ ràng |
| Refund cutoff/phí/toàn phần-một phần/approval? | Payment compensation | Không mất dấu tiền đã thu; có đối soát và manual resolution |
| Một payment hay nhiều attempt cho booking? | Payment schema | Checkout retry, phiên cũ, unique provider transaction |
| Media reference/lease và cleanup? | Attach/delete lifecycle | Ngăn xóa file đang attach, policy private delivery |
| Review sửa trong 30 ngày, xóa/ẩn và moderation? | Review | Giữ hay đổi rule cũ có quyết định rõ |
| Dashboard tổng hợp ở đâu? | Stats migration | Query composition hoặc read model; không đặt nghiệp vụ vào Gateway |
| Config Server, Redis, metrics/tracing stack? | Theo nhu cầu vận hành | Chưa thêm project chỉ để đủ danh sách công nghệ |
| Xóa account và retention dữ liệu? | Delete/account lifecycle | Không cascade mất booking/payment history |
| Có di chuyển dữ liệu từ repo cũ không? | Data cutover | Mapping IDs, backfill, validation và rollback riêng |

## Cách ghi quyết định tiếp theo

Khi chốt một mục: ghi ngày, lựa chọn, lý do, trade-off và tài liệu/contracts bị ảnh hưởng. Quyết định lớn có thể tách ADR riêng. Không đổi boundary âm thầm chỉ để giảm số lời gọi trong một tính năng.
