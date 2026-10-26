# Contract API, event và độ tin cậy

**Trạng thái:** thiết kế ban đầu. Các tên dưới đây là đề xuất, chưa có OpenAPI/AsyncAPI hay endpoint chạy được. Broker/transport chưa chọn; không mặc định Kafka hoặc RabbitMQ.

## API đồng bộ

| Caller | Owner | Mục đích | Khi owner không sẵn sàng |
|---|---|---|---|
| Booking | User | Snapshot học viên | Từ chối tạo mới có lỗi retryable |
| Booking | Mentor | Mentor eligibility, môn dạy, giá/currency | Không tạo booking với dữ liệu đoán |
| Payment | Booking | Payable context, payer, amount, hold/status | Không tạo checkout mới |
| Review | Booking | Booking completion và review eligibility | Không bỏ qua kiểm tra |
| User/Mentor/Review | Media | Validate READY, uploader, purpose | Không attach file chưa xác minh |
| Mentor | Media | Cấp private delivery sau business authorization | Deny/retry, không lộ URL public |
| User | Mentor | Validate grade/location khi dùng catalog | Lỗi rõ ràng hoặc projection có version |

Timeout là bắt buộc; retry có giới hạn và backoff. Chỉ tự retry command khi có idempotency semantics. Không biến dependency down thành NOT_FOUND hoặc dữ liệu rỗng hợp lệ. Circuit breaker là cơ chế có thể thêm theo nhu cầu, không thay thế business validation.

## Public route ownership dự kiến

| Route family | Owner |
|---|---|
| `/api/v1/auth/**`, quản trị account/role/status | Auth |
| `/api/v1/users/**`, learner profile | User |
| `/api/v1/mentors/**`, catalog, location | Mentor, ngoại trừ lịch |
| `/api/v1/bookings/**`, `/api/v1/scheduling/**`, lịch mentor | Booking |
| `/api/v1/payments/**`, Stripe webhook | Payment |
| `/api/v1/reviews/**` | Review |
| `/api/v1/media/**` | Media |
| `/api/v1/notifications/**` | Notification |

Route lịch như `/mentors/{id}/availabilities` nếu giữ URL cũ phải có route cụ thể ưu tiên trước wildcard Mentor. Admin account-status cũ phải route Auth, không tiếp tục ghi từ User. Chốt URL thực tế bằng OpenAPI trước implement controller. `/internal/**` không public qua Gateway.

## Event catalog dự kiến

| Event | Producer | Consumer chính | Dữ liệu tối thiểu/ý nghĩa |
|---|---|---|---|
| AccountRegistered | Auth | User | userId, thời điểm tạo; provision hồ sơ |
| AccountStatusChanged | Auth | Các projection cần thiết | userId, status, version; không thay thế revocation policy |
| UserProfileUpdated | User | Mentor | userId, public display fields, version |
| MentorApproved / MentorRejected | Mentor | Auth, Notification | mentorId, userId, decision/version; Auth tự áp role policy |
| BookingCreated | Booking | Notification | bookingId, participants, thời gian |
| PaymentSucceeded | Payment | Booking | paymentId, bookingId, payerUserId, amount, currency |
| BookingConfirmed | Booking | Notification | bookingId, participants, thời gian |
| BookingConfirmationFailed | Booking | Payment, Notification theo policy | bookingId, paymentId, reason; theo dõi bù trừ |
| BookingCancelled / BookingRejected | Booking | Payment, Notification | bookingId, reason, version; refund theo policy |
| BookingCompleted | Booking | Notification; Review projection nếu bổ sung | bookingId, studentUserId, mentorId |
| MentorRatingUpdated | Review | Mentor | mentorId, count, average, aggregateVersion |
| PaymentRefunded | Payment | Booking, Notification | paymentId, bookingId, refunded amount/currency |

Notification chỉ gửi tới recipient hợp lệ; xác định dữ liệu liên hệ qua contract User/Auth theo mục đích, tránh broadcast PII không cần thiết. Flow email xác minh/reset cần contract riêng có kiểm soát truy cập/retention cho link/token nhạy cảm, không đưa vào event công khai chung.

## Envelope đề xuất

```json
{
  "eventId": "unique-event-id",
  "eventType": "PaymentSucceeded",
  "schemaVersion": 1,
  "occurredAt": "2026-10-25T07:00:00Z",
  "producer": "payment-service",
  "aggregateId": "payment-id",
  "aggregateVersion": 1,
  "correlationId": "request-or-workflow-id",
  "payload": {
    "paymentId": "payment-id",
    "bookingId": "booking-id",
    "payerUserId": "user-id",
    "amount": "250000.00",
    "currency": "VND"
  }
}
```

ID dạng chuỗi trong ví dụ là placeholder, không chốt UUID hay BIGINT. Tiền dùng decimal hoặc minor units có contract rõ; không float. Schema version khác aggregate version. Không coi event delivery là có thứ tự toàn cục.

## Delivery và consistency

- Producer ghi business change + outbox atomically trong DB của mình. Áp dụng [quy định DBMS](database-strategy.md): SQL local transaction; Mongo multi-document transaction trên replica set với transaction manager/session phù hợp khi tách document và outbox/inbox.
- Publisher retry khi broker lỗi; chấp nhận at-least-once delivery.
- Consumer lưu inbox/eventId và thay đổi local state trong cùng transaction; ACK sau commit.
- Inbox dedup không tự bảo đảm email/provider side effect exactly-once. Cần delivery record, provider idempotency nếu có, và xử lý kết quả không chắc chắn.
- Projection dùng aggregateVersion hoặc quy tắc ordering cụ thể để bỏ qua event cũ. Workflow không được bỏ event khác chỉ vì aggregate khác cùng version.
- Có retry giới hạn, dead-letter/quarantine, cảnh báo và quy trình replay. Replay không được tạo lại payment/email vô hạn.
- Outbox/inbox retention phải đủ dài so với cửa sổ replay; thời lượng còn mở.
- Reconciliation job phát hiện paid chưa confirmed, refund pending lâu và projection lệch. Tần suất/SLA chốt trước chạy production.

## Contract evolution và kiểm thử

Thêm field tương thích trước; field bắt buộc hoặc đổi nghĩa cần version mới và kế hoạch rollout. Consumer không import DTO source nội bộ từ producer. Lưu OpenAPI/event schemas vào repo khi implement; kiểm thử consumer/provider, duplicate, out-of-order, timeout và unavailable dependency.
