# Lộ trình triển khai

Không có giai đoạn core-service. Khởi tạo các project độc lập, sau đó triển khai từng luồng xuyên service. Skeleton có health endpoint không đồng nghĩa nghiệp vụ đã hoàn thành.

## Giai đoạn 0 — Chốt thiết kế

Đầu ra: bộ docs hiện tại, service ownership, dependency map, các quyết định còn mở có thứ tự ưu tiên.
Trước skeleton: chốt Java/Spring Boot/Spring Cloud tương thích, build tool, package convention, local ports và cách quản lý version.
Trước business schema/contracts: chốt ID, time/currency, role và API/event cần thiết. Không cần giải quyết ngay mọi lựa chọn production.

## Giai đoạn 1 — Skeleton 10 project

- Tạo 8 service dưới services và Discovery/Gateway dưới platform.
- Health/readiness, cấu hình environment, đăng ký Eureka và explicit gateway routes.
- Build/test độc lập, Dockerfile và Compose local cho phạm vi đã tạo; DB credentials riêng.
- Theo [chiến lược DBMS](database-strategy.md): MySQL (Auth/User/Mentor), PostgreSQL (Booking/Payment/Review), MongoDB replica set (Media/Notification). Pin image/driver version; kiểm chứng connection và transaction theo engine, chọn Mongo migration runner trước schema đầu tiên.
- CI build/test các service; smoke test discovery/routing.
- README có lệnh chạy thực tế; pin version tương thích và ghi lý do chọn.

Hoàn thành khi tất cả ứng dụng khởi động, Gateway route đúng health/test endpoint được chủ đích expose, mất một instance có hành vi quan sát được. Chưa công khai internal/management endpoint nhạy cảm.

## Giai đoạn 2 — Auth, User và thông báo nền

- Chọn event transport, hiện thực outbox/inbox tối thiểu trước AccountRegistered.
- Đăng ký/đăng nhập/refresh/logout, provision profile, cập nhật profile.
- JWT validation, service auth và ownership checks.
- Notification record và delivery flow cho use case thực sự triển khai.

Kiểm chứng: duplicate registration/event, User down rồi phục hồi, token sai/hết hạn, quyền sửa profile, trạng thái account bị khóa theo policy.

## Giai đoạn 3 — Mentor và Media

- Catalog/location, mentor profile, môn dạy/giá, onboarding và approval.
- Upload intent/confirm/attach, private verification delivery.
- Public mentor detail/search và profile projection nếu dùng.

Kiểm chứng: giả mạo uploader/mediaId, file chưa READY, chứng chỉ private, approval/role đồng bộ trễ và event lặp.

## Giai đoạn 4 — Booking và lịch

- Availability cùng Booking DB, timezone policy, lookup snapshots.
- Create/list/cancel/reject/complete theo quyền và state machine.
- Hold expiry và chiến lược concurrency đã chốt.

Kiểm chứng: nhiều request đặt cùng lịch, sửa lịch đồng thời, giá đổi sau booking, invalid mentor subject, hold hết hạn. Không chỉ happy-path unit test.

## Giai đoạn 5 — Payment và xác nhận booking

- Stripe sandbox checkout/webhook, idempotency và paid event.
- Booking confirmation, failure compensation/refund workflow và reconciliation.
- UI/response diễn đạt trạng thái đang xác nhận.

Kiểm chứng: webhook lặp/sai thứ tự, provider timeout sau khi đã tạo session, broker down sau DB commit, paid đến sau cancel/expiry, refund retry. Chỉ coi payment flow hoàn thành khi có cách xử lý ngoại lệ đã thu tiền.

## Giai đoạn 6 — Review, notification đầy đủ và báo cáo

- Review eligibility, unique booking, edit/delete policy, rating projection.
- Thông báo theo booking/payment/review events cần thiết, read/unread, delivery retry.
- Chốt và triển khai dashboard theo owner/query composition hoặc read model.

Kiểm chứng: review của người khác, booking chưa completed, duplicate, update/delete aggregate, replay event, số liệu dashboard bị trễ được thể hiện đúng.

## Giai đoạn 7 — Sẵn sàng vận hành

- Correlation ID, log tập trung, metrics/tracing, cảnh báo outbox lag/consumer failure.
- Contract compatibility và integration tests; backup/restore, secret rotation, timeout/retry limits.
- Chính sách deployment/rollback, DB migration backward compatibility và replay có kiểm soát.

Không mặc định Kubernetes/Config Server/Redis là điều kiện hoàn thành; chọn theo nhu cầu đã được ghi nhận.
