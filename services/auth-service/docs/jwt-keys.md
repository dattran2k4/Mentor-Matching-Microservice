# Tạo khóa RSA cho access JWT

Auth Service dùng RS256 để ký access JWT. Private key dùng để ký; public key tương ứng dùng để xác thực chữ ký. Refresh token là chuỗi ngẫu nhiên và không dùng cặp khóa này.

## Vì sao dùng cặp khóa RSA?

RS256 dùng mật mã bất đối xứng: Auth Service ký access JWT bằng **private key**, còn service nhận dùng **public key** tương ứng để kiểm tra chữ ký. JWT được ký chứ không được mã hóa; các claims vẫn có thể đọc được.

| | RS256 | HS256 |
|---|---|---|
| Loại khóa | Cặp RSA: private key và public key | Một secret key đối xứng |
| Ký token | Dùng private key | Dùng secret key |
| Xác thực chữ ký | Dùng public key | Dùng cùng secret key |
| Chia sẻ giữa các service | Chỉ chia sẻ public key cho service cần xác thực | Mỗi service xác thực cần biết secret key |
| Khả năng phát hành | Service chỉ có public key không thể ký token hợp lệ | Service có secret key cũng có thể ký token hợp lệ |

Trong hệ thống microservice, cách dùng RS256 giúp Auth Service giữ quyền ký trong khi các service khác có thể tự xác thực token. HS256 vẫn là lựa chọn hợp lệ khi các bên xác thực đều được tin cậy với cùng secret; việc chọn thuật toán phụ thuộc vào cách quản lý và phân phối khóa của hệ thống.

Public key có thể chia sẻ, nhưng service nhận vẫn phải tin cậy nguồn cung cấp key và kiểm tra issuer, audience, thời hạn cùng các quy tắc phân quyền. Private key phải được giữ bí mật; nếu bị lộ, kẻ có khóa có thể ký token giả. Khi xoay khóa cần phối hợp thời gian giữa bên phát hành và các service xác thực để token còn hạn vẫn kiểm tra được.

## Tạo cặp khóa

Chạy các lệnh sau trong terminal, tại vị trí muốn lưu khóa:

```bash
mkdir -p jwt-keys
cd jwt-keys

openssl genpkey -algorithm RSA \
  -pkeyopt rsa_keygen_bits:3072 \
  -out private.pem

openssl pkey -in private.pem -pubout -out public.pem

chmod 600 private.pem
```

### Ý nghĩa từng lệnh và tham số

| Lệnh hoặc tham số | Ý nghĩa |
|---|---|
| `mkdir -p jwt-keys` | Tạo thư mục `jwt-keys`. Tùy chọn `-p` tạo cả thư mục cha nếu cần và không báo lỗi nếu thư mục đã tồn tại. |
| `cd jwt-keys` | Chuyển vào thư mục vừa tạo; các đường dẫn file tương đối bên dưới được tính từ thư mục này. |
| `openssl genpkey` | Tạo private key mới bằng OpenSSL. |
| `-algorithm RSA` | Chọn thuật toán RSA. |
| `-pkeyopt rsa_keygen_bits:3072` | Đặt độ dài modulus RSA là 3072 bit. Auth Service yêu cầu tối thiểu 2048 bit. |
| `-out private.pem` | Ghi private key vào file PEM `private.pem`. |
| `\` ở cuối dòng | Nối các dòng thành một lệnh shell. |
| `openssl pkey -in private.pem -pubout -out public.pem` | Đọc private key từ `private.pem`, trích xuất public key và ghi vào `public.pem`. Hai file tạo thành một cặp khóa. |
| `chmod 600 private.pem` | Trên hệ thống Unix, chỉ chủ sở hữu được đọc và ghi file; group và người dùng khác không có quyền truy cập theo permission bits. |

Private key tạo bằng lệnh trên là PEM PKCS#8 không mã hóa, có header `-----BEGIN PRIVATE KEY-----`. Giữ file bí mật, không commit hoặc chia sẻ private key. Public key có thể cung cấp cho các service cần xác thực JWT. Đặt thư mục khóa ngoài repository hoặc bảo đảm nó được Git ignore trước khi dùng.

## Cấu hình Auth Service

`TokenConfiguration` đọc nội dung PEM từ đường dẫn trong biến môi trường `AUTH_JWT_PRIVATE_KEY_PATH`. Trong shell, ví dụ khi đang ở thư mục chứa `private.pem`:

```bash
export AUTH_JWT_PRIVATE_KEY_PATH="$(pwd)/private.pem"
```

Sau đó khởi động Auth Service từ cùng shell. Không ghi nội dung private key vào log hay file được commit. `.env.example` chỉ là mẫu; Spring Boot không tự nạp file `.env`.

Service tự trích xuất public key từ private key để tạo bộ ký; file `public.pem` dành cho bên xác thực và hiện chưa được Auth Service công bố qua JWKS. Cần cấu hình `AUTH_JWT_AUDIENCE` trước khi phát hành token. Issuer mặc định là `auth-service`, access token hết hạn sau 15 phút và refresh token sau 30 ngày.

`JwtEncoder` được khởi tạo lazy, nên khóa thiếu hoặc sai có thể chỉ được phát hiện khi chức năng phát hành token được sử dụng.
