# Service boundaries và quyền sở hữu

## Quy tắc chung

Owner là service duy nhất được ghi dữ liệu nguồn và quyết định quy tắc nghiệp vụ của dữ liệu đó. Service khác dùng API hoặc event để lấy snapshot/projection, không truy cập bảng trực tiếp. Tên bảng dưới đây là nhóm dữ liệu đề xuất, chưa phải DDL cuối cùng.

## DBMS theo owner

Chốt theo [chiến lược DBMS](database-strategy.md): MySQL — Auth/User/Mentor; PostgreSQL — Booking/Payment/Review; MongoDB — Media/Notification. Database và credential riêng cho từng service. Gateway/Discovery không có database nghiệp vụ.

## Danh mục

| Service | Sở hữu | Không sở hữu |
|---|---|---|
| auth-service | accounts, credentials, roles, account status, refresh tokens, email verification/password reset | Hồ sơ học tập, trạng thái duyệt mentor, booking |
| user-service | user profiles, learner profiles, display name, contact info, avatarMediaId | Mật khẩu, email đăng nhập nguồn, khóa account, chuyên môn mentor |
| mentor-service | mentor profiles, approvals/verifications, mentor subjects/prices, achievements, traits, catalog, location | Lịch rảnh, giữ chỗ, thanh toán, file binary |
| booking-service | availability windows, bookings, reservations/holds, historical snapshots, session status | Giá nguồn, trạng thái thanh toán nguồn, đánh giá |
| payment-service | payments/payment attempts theo thiết kế cuối, provider sessions/transactions, webhook receipts, refund/reconciliation tracking | Đổi trạng thái booking trực tiếp, giá do client cung cấp |
| review-service | reviews, review tags/options, rating aggregates | Trạng thái hoàn thành buổi học, hồ sơ mentor nguồn |
| media-service | media assets, upload intent, provider metadata, access type, lifecycle/usage tracking theo contract | Quyết định hồ sơ nào được sửa, bảng mentor/user/review |
| notification-service | notifications, delivery attempts, read/unread state, templates theo phạm vi triển khai | Quyết định booking thành công, quyết định cấp role, token xác thực nguồn |
| discovery-service | Registry của instance và thông tin đăng ký | Nghiệp vụ, DB khách hàng, chuyển tiếp request |
| api-gateway-service | Route, CORS, kiểm soát truy cập cửa vào, rate-limit policy | Repository nghiệp vụ, transaction, tổng hợp nghiệp vụ phức tạp |

Eureka và Spring Cloud Gateway là hướng công nghệ đã chọn. Hai service platform không cần database nghiệp vụ hay cấu trúc CQRS.

## Auth và User

- Auth tạo `userId` ổn định; User dùng cùng định danh làm khóa hồ sơ. Kiểu ID sẽ chốt trước schema.
- Email đăng nhập có một nguồn sự thật tại Auth. Contact email ở User, nếu có, phải được đặt tên riêng và không ngầm thay đổi email đăng nhập.
- Khóa/vô hiệu hóa tài khoản là command của Auth dù API quản trị cũ nằm trong UserController.
- User profile được provision từ AccountRegistered theo cơ chế retry/idempotency. Frontend phải xử lý giai đoạn hồ sơ chưa sẵn sàng; không thông báo mọi resource đã tồn tại chỉ vì Auth đăng ký thành công.
- Hồ sơ học viên chứa mục tiêu, lớp học, sở thích; `gradeId` tham chiếu catalog bằng API/projection, không foreign key sang Mentor DB.

## User và Mentor

User sở hữu tên hiển thị, thông tin cá nhân và avatar chuẩn. Mentor sở hữu bio chuyên môn, chứng chỉ, môn dạy, giá và trạng thái duyệt. Avatar ở API mentor cũ phải chuyển thành tham chiếu/projection từ User hoặc redirect nghiệp vụ rõ ràng, không duy trì hai nguồn avatar không đồng bộ.
`mentorId` là ID hồ sơ mentor, khác `userId`; mỗi request/event phải ghi rõ loại ID. Mentor có thể lưu projection tên/avatar cho tìm kiếm; projection không trở thành nguồn sửa hồ sơ.

MentorApproved là sự kiện nghiệp vụ do Mentor phát. Auth áp dụng chính sách role liên quan một cách idempotent; thời điểm cấp role MENTOR còn mở. Booking vẫn kiểm tra eligibility của hồ sơ, không chỉ token role.

## Mentor và Booking

- Catalog và Location tạm là module nội bộ Mentor, không có catalog-service/location-service riêng.
- Availability chuyển hoàn toàn từ module scheduling sang Booking. Cả endpoint lịch mentor lẫn cập nhật lịch phải được route về Booking.
- Booking xác minh `mentorSubjectId` thuộc mentor, mentor đủ điều kiện nhận booking; lấy giá/tên từ nguồn đáng tin cậy.
- Booking lưu snapshot tên, môn, lớp, giá, tổng tiền và currency. Thay đổi giá/hồ sơ sau đó không sửa booking cũ.
- Kiểm tra và giữ lịch thuộc cùng transaction/chiến lược concurrency của Booking.

## Payment và Booking

Booking sở hữu khoản phải trả của booking; Payment sở hữu khoản thực thu và giao dịch provider. Payment lấy số tiền/currency/payer từ contract Booking, không tin amount phía frontend.
PaymentSucceeded chỉ là sự thật đã thanh toán; Booking tự quyết định có thể xác nhận hay phải phát sinh ngoại lệ. Chi tiết xem [luồng nghiệp vụ](business-flows.md).

## Review và Mentor

Review xác minh booking hoàn thành và tác giả là học viên của booking. Review sở hữu aggregate điểm/count; Mentor chỉ giữ bản projection phục vụ danh sách, không tự ghi review hoặc trở thành nguồn tính điểm thứ hai.
Khi review sửa/xóa/ẩn, aggregate phải được cập nhật và phát lại có version.

## Media và nghiệp vụ

Media xác minh uploader, purpose, trạng thái và provider metadata. User/Mentor/Review kiểm tra quyền gắn file vào resource của mình. Private verification document phải được Mentor cho phép truy cập trước khi Media phát URL ngắn hạn qua contract nội bộ xác thực.
Xóa một attachment không tự động cho phép xóa binary nếu vẫn có tham chiếu khác. Chốt cơ chế attachment lease/reference và cleanup trước chức năng xóa vật lý tự động.

## Ánh xạ từ repo cũ

| Module/luồng cũ | Đích |
|---|---|
| auth + credential/role/account status trong user | Auth |
| profile/learner trong user | User |
| mentor + catalog + location | Mentor |
| booking + scheduling + mentor availability endpoints | Booking |
| payment | Payment |
| review | Review |
| media | Media |
| notification | Notification |
| stats | Query số liệu theo owner; cách tổng hợp dashboard còn mở |
| shared/security | Cấu hình xác minh token ở từng service + contract Auth; không share nghiệp vụ |

Không mặc định giữ nguyên URL/controller cũ nếu ownership đã thay đổi. Các URL giữ tương thích cần gateway route cụ thể, không route wildcard che khuất boundary.
