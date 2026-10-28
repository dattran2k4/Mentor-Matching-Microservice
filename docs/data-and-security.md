# Dữ liệu, bảo mật và migration

## Database ownership

Mỗi business service có database/schema riêng và tài khoản DB chỉ được truy cập dữ liệu của mình. Theo [chiến lược DBMS](database-strategy.md), local dùng MySQL cho Auth/User/Mentor, PostgreSQL cho Booking/Payment/Review, MongoDB replica set cho Media/Notification; mỗi service có database và credentials riêng dù cùng instance. Discovery/Gateway không có database nghiệp vụ.

- Foreign key chỉ dùng bên trong một service. ID liên service là reference, validate qua API/projection.
- Không cross-database JOIN, không reuse JPA entity, không migration chỉnh bảng của service khác.
- Migration thuộc service sở hữu schema: Flyway cho MySQL/PostgreSQL, versioned changesets cho MongoDB (validator/index/backfill). Không chỉnh migration đã áp dụng dùng chung; thêm migration mới.
- Read projection có thể sao chép subset dữ liệu, ghi rõ owner, source version và cách rebuild.
- Booking snapshot giữ lịch sử tại thời điểm giao dịch, không cập nhật theo profile/price mới.
- Ghi amount cùng currency; thời gian chuẩn hóa instant/offset và lưu timezone lịch khi cần. Recurring availability cần timezone/DST policy rõ, không mặc định mọi người luôn cùng múi giờ.
- Type ID, precision tiền, currency hỗ trợ và chính sách timezone phải chốt trước schema/API.

## Khác biệt khi chuyển source cũ

- `users` cũ chứa nhiều trách nhiệm: tách credentials/role/status sang Auth, profile/learner sang User.
- Availability sang Booking cùng logic chống trùng lịch.
- Bỏ quan hệ PaymentJpaEntity -> BookingJpaEntity và FK xuyên service; dùng bookingId + snapshot/API/event.
- Các FK profile/catalog/location xuyên owner phải thay bằng reference có contract.
- Không copy V1 cũ vào từng service rồi giữ nguyên toàn bộ bảng.
- Repo mới bắt đầu migration riêng. Di chuyển dữ liệu thật là một dự án riêng: mapping ID, backfill, kiểm tra counts/tổng tiền, cutover và rollback; chưa được thực hiện trong giai đoạn docs.

## Authentication và authorization

Auth sở hữu credentials và vòng đời token. Hướng thiết kế: access JWT ký bất đối xứng, public keys qua JWKS, private keys chỉ ở Auth; phiên bản/thư viện sẽ chọn lúc implementation.

Gateway kiểm tra token ở cửa vào. Mỗi service vẫn xác minh token hợp lệ (signature, issuer, audience, expiry) hoặc service credential theo loại request và tự kiểm tra quyền tài nguyên. Không tin `X-User-Id` hay `X-Role` do client tự gửi; Gateway phải loại bỏ header giả mạo nếu dùng header nội bộ.

- User chỉ sửa profile được phép.
- Mentor chỉ sửa hồ sơ chuyên môn mình sở hữu; reviewer admin có quyền duyệt riêng.
- Booking xác minh student/mentor participant từ dữ liệu của mình, không chỉ role.
- Payment xác minh payer đúng booking; webhook dùng provider signature thay cho user JWT.
- Review xác minh tác giả và booking eligibility.
- Media private delivery yêu cầu cả danh tính caller và quyết định quyền của owner resource.

Giao tiếp nội bộ phải có service authentication và giới hạn quyền; mạng nội bộ/Discovery không tự tạo trust. Cơ chế service token hoặc mTLS còn mở trước khi triển khai contract nội bộ.

## Khóa tài khoản và token còn hạn

Khóa account/thu hồi refresh token không tự vô hiệu JWT đã phát. Cần chốt policy: access-token TTL, cơ chế kiểm tra trạng thái cho thao tác nhạy cảm và yêu cầu hiệu lực tức thời. Không chỉ dựa vào event AccountStatusChanged để hứa thu hồi tức thời trên mọi instance.

Refresh token lưu hash, rotation/reuse policy được thiết kế rõ; password dùng bộ mã hóa thích hợp, không mã hóa có thể giải ngược. Không log token, mật khẩu, Stripe secret, chữ ký upload hoặc private delivery URL.

## Secrets và private media

Secrets đi qua env/secret store, không commit `.env` thực hoặc key. `.env.example` chỉ placeholder khi tạo skeleton.
Media xác minh metadata bằng chữ ký provider hoặc server-side lookup, không tin metadata client. Mentor authorize chứng chỉ private rồi Media phát URL hết hạn; không cho bất cứ service/caller nào yêu cầu arbitrary asset URL không có scope.
Chính sách xóa account, giữ dữ liệu giao dịch, xóa media và audit còn mở; không cascade delete xuyên service theo suy đoán.

## Kiểm chứng persistence

Kiểm thử migration, indexes và concurrency trên DBMS của từng service: MySQL, PostgreSQL hoặc MongoDB replica set. H2/mock không đủ chứng minh dialect, PostgreSQL exclusion constraint hoặc MongoDB multi-document transaction đúng. Dữ liệu seed local phải là dữ liệu giả và thuộc từng owner.
