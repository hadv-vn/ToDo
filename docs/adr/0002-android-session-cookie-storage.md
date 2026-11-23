# 0002. Lưu trữ Session Cookie trên Android — CookieJar tự viết, mã hóa bằng AndroidKeyStore

## Status

Accepted (đã triển khai ở commit `53d6d66`, 2026-11-02; code tại
`android/app/src/main/java/com/example/todo/data/local/SessionCookieStore.kt`,
`data/remote/interceptor/CsrfHeaderInterceptor.kt`, `di/NetworkModule.kt`).

> Ghi lại sau khi code đã chạy (retroactive) — ADR thứ 2 của repo ToDo,
> tiếp theo [0001](0001-task-manual-reorder-sort-key.md), để tiếp tục lấp
> finding `c6 Decision Coverage` trong `health-check/index.html`
> (còn 3/5 thay đổi `feat:` lớn chưa có ADR sau ADR này).

## Context

Android app ban đầu được viết nhắm vào 1 backend Express giả định, xác
thực bằng **JWT Bearer token** (lưu qua `TokenLocalDataSource`, mã hóa
AES/GCM với key từ `AndroidKeyStore`). Khi submodule `auth-core` (NestJS)
được chọn làm backend chung (`4eacf29`), lộ ra một ràng buộc đã chốt sẵn
ở phía backend: `auth-core` dùng **server-side session lưu Postgres +
cookie httpOnly `sid`**, không phải JWT — quyết định đó đã được ghi ADR
riêng ở `auth-core/docs/architecture/adr/0001-session-management-strategy.md`,
dựa trên yêu cầu "khóa tài khoản phải chấm dứt session ngay lập tức" mà
JWT stateless không đáp ứng được.

Vì cơ chế xác thực đổi từ token (app tự gắn header `Authorization:
Bearer ...`) sang cookie (server tự `Set-Cookie`, client chỉ cần gửi lại
đúng cookie mỗi request) + CSRF double-submit (`csrfToken` cookie +
header `x-csrf-token` cho request thay đổi dữ liệu), tầng network/auth
của Android phải viết lại từ đầu. Câu hỏi thiết kế cần chốt ở ADR này:
**lưu cookie session ở đâu, bằng cách nào, trên thiết bị Android?**
(Phần "đổi sang cơ chế cookie" là ràng buộc đã có sẵn từ backend, không
phải lựa chọn của Android — phần Android **thực sự phải quyết định** là
cách triển khai client-side bên dưới đây.)

**Các phương án đã cân nhắc:**

- **A — `CookieJar` mặc định của OkHttp (in-memory, không lưu đĩa)**:
  đơn giản nhất, không cần code thêm. Bị loại ngay vì mất toàn bộ session
  mỗi khi process Android bị kill (rất thường xảy ra, không phải chỉ lúc
  tắt app) — người dùng phải đăng nhập lại liên tục, vi phạm kỳ vọng
  "giữ phiên" ngầm định của 1 app mobile thông thường.
- **B — Thư viện cookie-jar bên thứ ba có sẵn** (ví dụ dạng
  `okhttp3-persistent-cookiejar`): tiết kiệm code, đã xử lý sẵn việc
  parse/match cookie theo chuẩn OkHttp. Bị loại vì 2 lý do: (1) vi phạm
  nguyên tắc #3/#9 (`CLAUDE.md`) — không thêm dependency ngoài khi cơ chế
  sẵn có (`AndroidKeyStore` + `DataStore<Preferences>`) đã đủ; (2) các
  thư viện phổ biến loại này lưu cookie ở dạng gần-plaintext
  (SharedPreferences/SQLite không mã hóa) — cookie `sid` có mức nhạy cảm
  tương đương token truy cập tài khoản trước đây, không nên lưu kém an
  toàn hơn so với JWT đã bị thay thế.
- **C — Tự viết `CookieJar`, mã hóa toàn bộ bằng AES/GCM với key từ
  `AndroidKeyStore`, persist qua `DataStore<Preferences>`** — tái dùng
  **đúng** kỹ thuật đã áp dụng cho `TokenLocalDataSource` (JWT) trước khi
  bị xóa ở cùng commit này.

Chọn **C**.

## Decision

Thay `AuthHeaderInterceptor` (gắn `Authorization: Bearer <JWT>`, đã xóa)
bằng 2 thành phần mới:

- **`SessionCookieStore : CookieJar`** (`SessionCookieStoreImpl`) — OkHttp
  tự gọi `saveFromResponse`/`loadForRequest` nên cookie server set qua
  `Set-Cookie` được lưu/gắn lại tự động, không cần code gọi tay ở từng
  request. Cài đặt:
  - Serialize toàn bộ cookie đang có thành 1 chuỗi (delimiter là ký tự
    control U+0001/U+0002, không trùng với bất kỳ giá trị cookie thật
    base64url nào), mã hóa 1 lần bằng `AES/GCM/NoPadding` với
    `SecretKey` lấy từ `AndroidKeyStore` qua `MasterKey` (`androidx.
    security.crypto`), lưu chuỗi đã mã hóa vào **1 key** duy nhất trong
    `DataStore<Preferences>`.
  - `loadForRequest`/`saveFromResponse` là API đồng bộ của interface
    `CookieJar` (không phải `suspend`) và chạy trên thread dispatcher
    của OkHttp — dùng `runBlocking(Dispatchers.IO)` để đọc/ghi
    `DataStore` lần đầu, có cache trong bộ nhớ (`@Volatile var cache`)
    cho các lần gọi sau.
  - Chỉ xử lý host-only, 1 `BASE_URL` duy nhất trong toàn app — không
    cần logic multi-domain/subdomain đầy đủ mà `Cookie`/`CookieJar` của
    OkHttp hỗ trợ.
  - Giải mã lỗi (ví dụ key `AndroidKeyStore` không còn hợp lệ) → trả
    `null`/coi như không có cookie, **không throw** — buộc đăng nhập lại
    là xử lý an toàn, hợp lý hơn crash.
  - Expose thêm `hasActiveSessionCookie()` (cho UX "giữ phiên" khi mở
    app) và `clear()` (cho logout / 401 toàn cục) — 2 method không có
    trong interface `CookieJar` gốc, đặc thù cho nhu cầu của app này.
- **`CsrfHeaderInterceptor`** — đọc cookie `csrfToken` (non-httpOnly) từ
  chính `SessionCookieStore`, gắn header `x-csrf-token` cho request
  `POST/PUT/PATCH/DELETE`, **trừ** nhóm `/api/v1/auth/` (chưa có session
  nên chưa có CSRF token để gắn).

`NetworkModule` gắn `SessionCookieStore` làm `cookieJar` của OkHttp
`Client`; `TokenLocalDataSource`/`AuthHeaderInterceptor` (JWT) bị xóa
hoàn toàn — không giữ song song 2 cơ chế xác thực.

## Consequences

**Tích cực:**

- Tái dùng đúng 1 kỹ thuật mã hóa (`AndroidKeyStore` + AES/GCM) cho cả
  2 thế hệ xác thực (JWT trước đây, cookie hiện tại) — không có 2 cách
  lưu dữ liệu nhạy cảm khác nhau cùng tồn tại trong app (nguyên tắc #9
  `CLAUDE.md`).
- Không thêm dependency ngoài — đúng nguyên tắc #3/#9.
- Tự viết `CookieJar` cho phép thêm đúng 2 method nghiệp vụ cần
  (`hasActiveSessionCookie`, `clear`) mà một thư viện generic không có
  sẵn, không phải "uốn" API thư viện ngoài cho vừa nhu cầu riêng.
- OkHttp tự gọi `saveFromResponse`/`loadForRequest` nên không có chỗ nào
  trong tầng network phải nhớ "đọc/gắn cookie tay" — khác hẳn JWT cũ
  (phải có interceptor riêng chủ động thêm header ở mọi request).

**Đánh đổi / rủi ro chấp nhận:**

- `runBlocking` trong `loadCache()`/`persist()` chặn thread dispatcher
  của OkHttp trong lúc đọc/ghi `DataStore` — chấp nhận được vì bản chất
  network call đã là I/O blocking, nhưng là điểm cần nhớ nếu sau này đổi
  sang dispatcher có thread pool giới hạn chặt.
- Chỉ hỗ trợ host-only, 1 domain — nếu `BASE_URL` sau này tách sang nhiều
  domain/CDN khác nhau cho các phần API khác nhau, cơ chế này không tự
  đúng, cần mở rộng logic match cookie.
- Serialize bằng delimiter ký tự điều khiển tự chế (không phải JSON
  chuẩn) — nhẹ hơn nhưng khó mở rộng thêm field cookie mới mà không sửa
  tay cả `serialize`/`deserialize`; không có schema version.
- Giải mã lỗi (đổi máy, khôi phục backup Android sai key) âm thầm coi
  như mất session, không ghi log/metric riêng để phân biệt với trường
  hợp "chưa từng đăng nhập" — khó chẩn đoán nếu người dùng report "tự bị
  đăng xuất" mà không rõ nguyên nhân.
- `CsrfHeaderInterceptor` hiện chưa có unit test riêng
  (`SessionCookieStoreTest.kt` ở `androidTest` có test cho
  `SessionCookieStore`, nhưng chưa thấy test cho logic loại trừ
  `/api/v1/auth/` hoặc việc gắn đúng header) — nợ kỹ thuật nhỏ, nên bổ
  sung cùng lúc với lần tới chạm vào file này.

**Ảnh hưởng tới các bước tiếp theo:**

- Mọi cookie mới mà `auth-core` thêm sau này tự động được lưu/gửi đúng
  qua `SessionCookieStore` mà không cần sửa code, trừ khi Android cần
  **đọc** giá trị cụ thể của cookie đó (như đã làm riêng cho `sid` ở
  `hasActiveSessionCookie()` và `csrfToken` ở `CsrfHeaderInterceptor`).
- Nếu `auth-core` đổi chiến lược session (ví dụ thêm Redis cache theo
  hướng nâng cấp đã ghi ở ADR 0001 của `auth-core`), ADR này **không**
  cần đổi — phía Android chỉ phụ thuộc vào hợp đồng "cookie `sid` +
  `csrfToken`", không phụ thuộc cách backend lưu session phía sau.

## Liên quan

- ADR gốc phía backend (lý do buộc phải đổi):
  `auth-core/docs/architecture/adr/0001-session-management-strategy.md`.
- ADR trước trong repo ToDo: [0001-task-manual-reorder-sort-key.md](0001-task-manual-reorder-sort-key.md).
- Code: `SessionCookieStore.kt`, `CsrfHeaderInterceptor.kt`,
  `di/NetworkModule.kt`; lịch sử bị thay thế: `TokenLocalDataSource`,
  `AuthHeaderInterceptor` (xóa ở `53d6d66`).
- Test: `android/app/src/androidTest/java/com/example/todo/data/local/SessionCookieStoreTest.kt`.
- Commit triển khai: `53d6d66` (feat: cập nhật Android cho khớp
  auth-core, bump submodule lên commit có Task module).
- Dashboard tham chiếu: `health-check/index.html` — chỉ số `c6 Decision
  Coverage`.
