# Chiến lược DBMS theo service

Quyết định ngày **2026-10-26** theo yêu cầu sử dụng ít nhất ba DBMS. Baseline của dự án là **MySQL + PostgreSQL + MongoDB**; chưa triển khai database hay pin phiên bản. Bảng dưới là nguồn chuẩn cho lựa chọn DBMS khi tạo skeleton, schema, migration và test.

## Phân bổ đã chọn

| Service | DBMS | Database riêng | Lý do chọn |
|---|---|---|---|
| auth-service | MySQL / InnoDB | `mm_auth` | Account, role, credential và refresh token có quan hệ rõ; cần unique constraint và transaction |
| user-service | MySQL / InnoDB | `mm_user` | Profile, learner profile là dữ liệu có cấu trúc; truy cập chủ yếu theo userId, ít cần document model |
| mentor-service | MySQL / InnoDB | `mm_mentor` | Hồ sơ, môn dạy, giá, catalog, location, verification có nhiều quan hệ và truy vấn lọc có cấu trúc |
| booking-service | PostgreSQL | `mm_booking` | Giao dịch giữ lịch/đặt lịch; range và exclusion constraint hỗ trợ chống khoảng thời gian chồng nhau |
| payment-service | PostgreSQL | `mm_payment` | Transaction, numeric chính xác, uniqueness và audit/đối soát phù hợp mô hình quan hệ |
| review-service | PostgreSQL | `mm_review` | Unique theo booking, cập nhật review/aggregate/outbox atomically, thống kê theo mentor |
| media-service | MongoDB | `mm_media` | Metadata khác nhau theo provider/resource type, đọc/cập nhật theo asset document |
| notification-service | MongoDB | `mm_notification` | Payload theo loại thông báo/kênh gửi khác nhau; truy vấn theo recipient và thời gian |
| discovery-service | Không có DB nghiệp vụ | — | Registry Eureka, không lưu dữ liệu domain |
| api-gateway-service | Không có DB nghiệp vụ | — | Routing và policy; không chứa persistence nghiệp vụ |

Tổng cộng **3 service MySQL + 3 service PostgreSQL + 2 service MongoDB**. Đây là lựa chọn thiết kế cho phạm vi hiện tại, không phải kết quả benchmark. MySQL cũng làm tốt Payment/Review, PostgreSQL cũng làm tốt Auth/Mentor; không có quy tắc “thanh toán bắt buộc PostgreSQL”. Phân bổ này đáp ứng yêu cầu đa DBMS đồng thời tận dụng khả năng cụ thể ở Booking và document model ở Media/Notification.

“Cùng DBMS” khác “cùng database”: ba service dùng MySQL vẫn có ba database và ba tài khoản runtime độc lập. Chỉ đổi loại DBMS không tự loại bỏ coupling nếu service vẫn đọc/ghi dữ liệu của nhau.

## MySQL: Auth, User, Mentor

Dùng InnoDB, foreign key trong database cùng service, unique index cho invariant và transaction cục bộ.

- Auth: unique login identifier sau normalization; unique token hash khi phù hợp; index theo account/session/expiry. Rotation và account state phải có concurrency control, không dựa vào check-then-write đơn thuần.
- User: unique userId; learner profile gắn với profile trong cùng DB. Grade/location ID của Mentor là external reference, không cross-DB foreign key.
- Mentor: quan hệ subject/grade/mentor_subject, verification, traits và achievements vẫn chuẩn hóa. Profile có một số field linh hoạt không đủ lý do chuyển cả catalog nhiều quan hệ sang MongoDB. Unique userId cho một hồ sơ mentor nếu giữ quy tắc này.
- Case sensitivity/collation cho email, ID và token hash phải chọn có chủ đích; không áp cùng một collation cho mọi field. Không dùng float cho giá.

## PostgreSQL: Booking, Payment, Review

### Booking

Thiết kế khoảng thời gian thực tế dùng `tstzrange` với khoảng nửa mở `[start, end)` để hai buổi liên tiếp có thể sát nhau. Dùng exclusion constraint theo mentor + khoảng thời gian cho các reservation còn chiếm lịch; `btree_gist` là hướng extension để kết hợp equality trên mentorId với overlap trên range.

Đây là định hướng ràng buộc, chưa là DDL cuối: phải chốt kiểu ID, state giữ chỗ và predicate active trước khi viết migration. Không dùng `now()` trong predicate để giả định hold tự hết hiệu lực. Worker/command phải chuyển trạng thái hold hết hạn và giải phóng lịch atomically; xử lý cạnh tranh với payment confirmation.

Exclusion constraint chặn overlap nhưng không tự xác minh giờ nằm trong availability hoặc mentor còn được phép dạy. Việc sửa availability đồng thời vẫn cần chiến lược lock/version; recurring windows giữ timezone riêng để xử lý DST. Kiểm thử hai transaction cạnh tranh trên PostgreSQL thật.

### Payment

Dùng `numeric` hoặc minor units theo contract đã chốt, luôn lưu currency. Unique provider event/transaction/idempotency key theo phạm vi provider để chống ghi lặp. Ghi state và outbox trong một transaction. Không đặt unique bookingId máy móc khi mô hình payment attempts chưa chốt.

Payment và Booking cùng PostgreSQL nhưng **khác database, credentials và transaction**. Không join, foreign data wrapper, dblink, cross-service SQL hay transaction chung. Workflow vẫn dùng API/event, compensation và reconciliation.

### Review

Unique bookingId theo chính sách một review/booking; FK review–tags chỉ trong Review DB. Update review, aggregate điểm và outbox trong transaction cục bộ; concurrency control khi nhiều review cùng mentor. Có thể lưu sum/count để tính average ổn định. Quy tắc soft delete/re-review phải chốt trước unique constraint cuối cùng.

## MongoDB: Media và Notification

Dùng Spring Data MongoDB, document model và DTO tách biệt. Không thêm JPA datasource hoặc Flyway SQL chỉ để bắt chước cấu trúc service quan hệ.

MongoDB vẫn phải có **schema validation, index và schemaVersion**. Các field quan trọng như owner/recipient, purpose/type, status, timestamps có kiểu xác định; chỉ metadata/payload được linh hoạt có giới hạn.

### Media

Một asset document chứa identity, uploader, purpose, access type, status và metadata theo provider. File binary nằm ở Cloudinary/object storage; **không lưu base64/file binary trong document**, không thêm GridFS vào baseline.

Unique index `(provider, providerAccount, objectKey)` theo phạm vi key thực tế của provider; index phục vụ owner/status/createdAt. Cập nhật READY/DELETED có điều kiện/version để tránh ghi đè race. Reference/lease với tài nguyên bên ngoài cần contract rõ, không suy ra bằng join.

Không dùng TTL index xóa asset document để thay cho quy trình xóa file provider: TTL không thực thi API provider hoặc kiểm tra usage. Cleanup phải theo workflow có audit và retry.

### Notification

Document thông báo cho recipient; payload theo loại/kênh và version template. Index `(recipientUserId, createdAt)` và query unread theo nhu cầu. Dùng unique key `(eventId, recipientUserId, channel)` hoặc delivery key tương đương chống duplicate; không unique eventId đơn lẻ vì một event có nhiều người nhận.

Delivery attempts tăng không giới hạn phải tách collection hoặc giới hạn lịch sử, không nhồi array vô hạn vào document. TTL chỉ dùng cho dữ liệu có policy retention đã chốt; không xóa inbox dedup trước cửa sổ replay. Không coi TTL là scheduler gửi đúng giờ.

### Atomicity và replica set

Một document có atomic update; khi cần ghi business document + outbox/inbox/delivery record ở nhiều document, dùng MongoDB transaction với transaction manager/session đúng cấu hình. Không coi annotation `@Transactional` tự đủ nếu thiếu cấu hình Mongo transaction.

**Local/CI MongoDB phải chạy replica set có hỗ trợ transaction**, không chạy standalone cho các test atomicity. Topology và hostname phải truy cập được từ client host/container. Local một node replica set có thể dùng cho phát triển; không coi đó là cấu hình HA production. Production topology, write concern và backup phải được thiết kế riêng.

Giữ transaction ngắn; dùng driver-supported retry cho transient/unknown commit outcomes đúng semantics. Không gọi email/storage provider bên trong transaction rồi giả định rollback được side effect.

## Migration và persistence conventions

| DBMS | Persistence | Migration/change management |
|---|---|---|
| MySQL | Spring Data JPA hoặc SQL cục bộ | Flyway, SQL theo MySQL |
| PostgreSQL | Spring Data JPA và SQL native khi cần range/constraint | Flyway, SQL theo PostgreSQL, extension trong quy trình migration |
| MongoDB | Spring Data MongoDB | Versioned changesets quản lý collections, validators, indexes và backfill; công cụ runner chốt trước implementation |

Mongo migration runner phải có lịch sử version/checksum, coordination tránh chạy trùng giữa instance và cách resume/retry an toàn. Không chỉ gọi `createIndex` tùy tiện mỗi lần startup rồi coi là migration hoàn chỉnh. Flyway không được áp đặt làm SQL migration cho Mongo trong baseline.

Không dùng Hibernate `ddl-auto=update` làm cơ chế schema production. Không copy migration MySQL cũ vào PostgreSQL: cần chuyển dialect, enum/check, timestamp, numeric, index và concurrency rules.

## Quy định xuyên service

1. Mỗi service một primary DBMS theo bảng; không thêm database thứ hai chỉ vì vài field linh hoạt.
2. Mỗi owner có database, runtime credential và migration role riêng; runtime không dùng root/superuser. Mongo migration cần quyền validator/index phù hợp, tách khỏi runtime nếu có thể.
3. Không chia sẻ repository/entity/document model hay truy vấn chéo DB, dù cùng engine hoặc instance.
4. CQRS vẫn command/query dùng cùng DB của service; chưa thêm read DB thứ hai, Elasticsearch hoặc Redis để tăng số công nghệ.
5. API/event thống nhất ID, currency, timestamps độc lập kiểu DB. Không để ObjectId/driver type lọt ra contract hoặc tự đổi userId khi sang MongoDB.
6. Producer outbox và consumer inbox nằm trong database của chính service; đảm bảo atomicity đúng engine.
7. Thay DBMS, thêm primary store hoặc đổi owner phải cập nhật quyết định kiến trúc và kế hoạch migrate/test trước code.

## Môi trường local, CI và vận hành

Local Compose dự kiến gồm một MySQL instance (3 database), một PostgreSQL instance (3 database), MongoDB replica set (2 database). Chia sẻ instance local tiết kiệm tài nguyên nhưng vẫn chung failure domain; không phải isolation production.

Chốt version/image tag cụ thể và driver tương thích khi tạo skeleton, không dùng `latest`. Chưa tạo Compose hoặc DB ở lượt quyết định này.

Integration tests dùng đúng engine/version tương thích, ưu tiên container test: MySQL cho uniqueness/transactions; PostgreSQL cho range constraints, migrations và race; Mongo replica set cho validators, unique indexes, inbox/outbox và transaction rollback. H2/in-memory mock không thay thế các kiểm chứng này.

Ba DBMS làm tăng công việc backup/restore, upgrades, metrics và xử lý sự cố. Chuẩn bị runbook và thử restore từng loại; không coi backup riêng từng database là snapshot nhất quán toàn workflow. Khôi phục cần reconcile booking/payment và replay event có kiểm soát. Không hứa throughput tốt hơn khi chưa benchmark.

## Nguồn kỹ thuật

Lựa chọn service là quyết định dự án; các khả năng kỹ thuật được đối chiếu ngày 2026-10-26:

- [MySQL InnoDB transaction practices](https://dev.mysql.com/doc/refman/26.7/en/innodb-best-practices.html).
- [PostgreSQL range types và exclusion constraints](https://www.postgresql.org/docs/18/rangetypes.html).
- [MongoDB schema validation](https://www.mongodb.com/docs/manual/core/schema-validation/).
- [MongoDB transactions và topology](https://www.mongodb.com/docs/manual/core/transactions-production-consideration/).

Phiên bản trong URL nguồn không phải phiên bản đã pin của dự án.
