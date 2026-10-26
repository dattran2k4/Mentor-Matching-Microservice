# Tổng quan dự án

## Mục tiêu

Xây nền tảng giúp học viên tìm mentor theo môn học/lớp học, xem hồ sơ và lịch, đặt buổi học, thanh toán, nhận thông báo và đánh giá sau buổi học. Mentor quản lý hồ sơ chuyên môn, lịch rảnh và các buổi đã đặt. Quản trị viên quản lý tài khoản, nội dung tham chiếu và duyệt mentor.

Dự án mới là bản thiết kế lại triển khai độc lập từ repo Mentor-Matching, không phải đổi package của modular monolith thành nhiều process nhưng vẫn dùng chung bảng.

## Vai trò

| Vai trò | Nghiệp vụ |
|---|---|
| Khách | Xem danh sách/hồ sơ mentor được công khai, catalog, đăng ký |
| Học viên | Quản lý hồ sơ, đặt lịch, trả tiền, xem lịch sử, đánh giá |
| Mentor | Quản lý chuyên môn, gửi hồ sơ duyệt, khai báo lịch, xử lý buổi học |
| ADMIN/MANAGER | Duyệt hồ sơ, quản trị theo phạm vi được cấp, xử lý ngoại lệ |

Tên role/claim cụ thể và khác biệt ADMIN/MANAGER phải chốt trong security contract. Không cho client tự cấp vai trò đặc quyền. Vai trò MENTOR không tự chứng minh hồ sơ đã được duyệt hoặc sở hữu một booking.

## Phạm vi nghiệp vụ

- Account/auth và hồ sơ cá nhân/học viên.
- Mentor onboarding, xác minh, môn dạy, giá, traits, thành tích.
- Catalog: category, subject, grade, subject-grade. Location: city/district.
- Availability, booking, lịch sử, hoàn thành/hủy/từ chối buổi học.
- Stripe Checkout, webhook và xử lý nhất quán Payment–Booking.
- Review, sửa/xóa đánh giá, tổng hợp điểm mentor.
- Media upload trực tiếp lên provider và kiểm soát truy cập.
- Notification trong ứng dụng; kênh gửi email triển khai theo từng use case.
- Báo cáo quản trị: mỗi service cung cấp số liệu của mình; thiết kế tổng hợp xuyên service còn mở, không bỏ tính năng hoặc join DB để giải quyết nhanh.

## Nguồn tham chiếu và giới hạn

Đã đối chiếu docs và source repo cũ tại `../Mentor-Matching/backend` ngày 2026-10-25: module map, architecture, database, payment, media, các controller, Booking domain và ReviewService.
Docs cũ có phần chậm hơn code: Review đã có create/update/delete và Payment đã gọi xác nhận Booking; không coi chúng chỉ là skeleton.

Bản mới giữ ý nghĩa nghiệp vụ và phân định lại quyền sở hữu. Các khả năng như hoàn tiền tự động, hết hạn giữ chỗ, private media delivery và đối soát là yêu cầu thiết kế cần triển khai, không phải cam kết repo cũ đã có.

## Phạm vi hiện tại của repo mới

Đã tạo bộ tài liệu nền và skeleton Auth/User/Mentor/Booking/Payment/Review với Java 21, Boot 4.1.2, Cloud 2025.1.4; chưa triển khai nghiệp vụ. Chưa chọn broker, chưa tạo bốn project Media/Notification/Discovery/Gateway và chưa di chuyển dữ liệu thật. Chi tiết ở [service initialization](service-initialization.md). Xem [quyết định còn mở](decisions.md).
