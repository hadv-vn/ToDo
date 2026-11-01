# API Contract: Ứng dụng Android Quản lý Task/To-Do (CRUD)

| | |
|---|---|
| **Dự án** | Full-Stack Assessment — Task 2: Task/To-Do CRUD Management |
| **Phiên bản** | 1.0 |
| **Ngày tạo** | 2026-10-14 |
| **Trạng thái** | Draft chính thức — chốt contract trước khi code Backend/Android thật |
| **Nguồn căn cứ** | `docs/SRS_ToDo_Android.md` (v1.0, mục 5–6), `docs/HLD_ToDo_Android.md` (v1.0, mục 2–3), `docs/LLD_ToDo_Android.md` (v1.0, mục 1, 3, 9, 10) |
| **Phạm vi** | Backend REST API (Node.js + Express + TypeScript + Prisma + PostgreSQL) cho Auth + Task CRUD, tiêu thụ bởi Android app |

> Dự án greenfield, chưa có code triển khai thật. Nguồn "convention hiện có"
> dùng để thiết kế là **chuỗi tài liệu thiết kế đã thống nhất** (SRS → HLD →
> LLD), không phải code route/controller thật — các field name/status
> code/pseudocode ở LLD mục 1.9–1.11 được dùng làm căn cứ chính, và tài liệu
> này **chốt chính thức** các điểm LLD còn để ở mức giả định.

---

## 0. Hai điểm mở đã được giao quyết định dứt điểm

1. **Bổ sung `GET /tasks/:id`**: ĐÃ THÊM vào contract chính thức (mục 3.3).
   Loại bỏ hoàn toàn rủi ro mất dữ liệu do `SelectedTaskHolder` in-memory khi
   process death (LLD mục 10.3). Android nên đổi `EditTaskScreen` sang gọi
   `GET /tasks/:id` khi mở màn hình (thay vì đọc `SelectedTaskHolder`) —
   **đây là khuyến nghị cập nhật LLD/Android, nằm ngoài phạm vi file này**
   (file này chỉ là API contract; cập nhật class design Android thuộc
   `low-level-designer`).
2. **`confirmPassword` / `confirmNewPassword` là field thật trong request
   body**: ĐÃ CHỐT — có mặt chính thức trong `POST /auth/register` và
   `POST /auth/reset-password`, validate server-side bằng Zod `.refine()`
   phải khớp với `password`/`newPassword` (defense in depth, không chỉ dựa
   vào validate client).

---

## 1. Quy ước chung (áp dụng cho MỌI endpoint)

### 1.1 Base path & Versioning

- Không có prefix version (`/v1/...`) — kế thừa nguyên trạng từ SRS mục 6 và
  HLD/LLD, mọi path dùng trực tiếp `/auth/*`, `/tasks/*`. Đây là quyết định
  đã có sẵn trong 3 tầng tài liệu trước, tài liệu này giữ nguyên, không tự ý
  đổi thêm version.
- Base URL cụ thể (domain/port) không thuộc phạm vi contract này (thuộc
  deployment, xem HLD mục 4).

### 1.2 Authentication

- Header: `Authorization: Bearer <accessToken>`.
- Áp dụng cho **toàn bộ** `/tasks/*` (bao gồm `GET /tasks/:id` mới bổ sung).
  `/auth/*` KHÔNG yêu cầu header này.
- Token là JWT, claim tối thiểu: `sub` (userId), `email`, `iat`, `exp`. TTL
  đề xuất 7 ngày (giả định LLD mục 9.9, chưa có NFR nguồn — xem mục 7).
- Mọi lỗi liên quan token (thiếu header, sai format không phải
  `Bearer <token>`, sai signature, hết hạn) → **đồng nhất `401`**, message
  chung `"Token không hợp lệ hoặc đã hết hạn"`. Không phân biệt lý do cụ thể
  trong message trả về client (tránh lộ chi tiết nội bộ về cơ chế xác thực).

### 1.3 Content-Type

- Request: `Content-Type: application/json` (trừ khi không có body, ví dụ
  `GET`, `DELETE`).
- Response: `Content-Type: application/json` cho mọi response có body.
  `204 No Content` không có body.

### 1.4 Response envelope (thành công)

- **Không dùng envelope** — trả thẳng data ở top-level JSON (object cho 1
  resource, array thuần cho danh sách, `{ message }` cho các thao tác không
  trả resource cụ thể như `forgot-password`/`reset-password`).
- Lý do giữ quy ước này: nhất quán với toàn bộ pseudocode/sequence diagram đã
  thống nhất ở LLD (`res.status(201).json({ accessToken, user })`,
  `res.status(200).json([...tasks])`...) — không có lớp bọc `data`/`meta`
  nào được thiết kế từ trước.

### 1.5 Error Response Schema — CHUẨN DÙNG CHUNG CHO MỌI ENDPOINT

```json
{
  "message": "string — mô tả lỗi, an toàn để hiển thị/log, KHÔNG chứa stack trace hay chi tiết hệ thống nội bộ",
  "details": "object | undefined — CHỈ xuất hiện với lỗi validate cấp field (Zod); dạng { fieldErrors: { [field]: string[] }, formErrors: string[] }. Không có với lỗi business rule/ownership/404."
}
```

**Mapping Exception class (LLD mục 1.4) → HTTP status:**

| Exception class | HTTP status | Khi nào |
|---|---|---|
| `BadRequestError` | `400` | Validate schema (Zod) fail; OTP sai/hết hạn/vượt max attempt; PUT body không có field nào |
| `UnauthorizedError` | `401` | Thiếu/sai/hết hạn JWT; sai email hoặc mật khẩu khi login |
| `ForbiddenError` | `403` | Task tồn tại nhưng không thuộc user hiện tại |
| `NotFoundError` | `404` | Task không tồn tại (đã xóa hoặc chưa từng tồn tại) |
| `ConflictError` | `409` | Email đã tồn tại khi register |
| *(không phải `AppError`)* | `500` | Lỗi hệ thống không lường trước (DB down, lỗi code...). **Production**: message chung `"Đã có lỗi xảy ra, vui lòng thử lại sau"`, KHÔNG trả `details`/stack trace. **Dev**: có thể kèm thêm field debug riêng, không thuộc phạm vi contract public. |

**Lưu ý triển khai quan trọng**: `message` ở response lỗi là nội dung cấp
API (hữu ích cho log/Postman/QA), **không bắt buộc** phải là chuỗi hiển thị
trực tiếp cho người dùng cuối Android — client (theo LLD mục 4.4) map lỗi
theo **status code** (`ApiErrorCode`) sang các chuỗi UI tiếng Việt cố định
của riêng nó trong nhiều trường hợp (ví dụ `404` luôn hiện "Task không còn
tồn tại" ở Android bất kể giá trị `message` backend trả gì). Vì vậy backend
vẫn phải trả `message` rõ ràng, nhất quán, nhưng Android không bị buộc phải
hiển thị y nguyên.

### 1.6 Định dạng dữ liệu dùng chung

| Khái niệm | Định dạng |
|---|---|
| ID (`user.id`, `task.id`) | UUID v4 string, ví dụ `"3f29b6b0-df3e-4a1b-9e21-7f9a2d6c1a44"` |
| Timestamp (`createdAt`, `updatedAt`) | ISO-8601 string, UTC, có hậu tố `Z`, ví dụ `"2026-10-14T08:30:00.000Z"` |
| `Task.status` | string lowercase, enum `"open"` \| `"done"` |
| Email | string, format email chuẩn (RFC 5322 rút gọn theo Zod `.email()`) |
| OTP | string, đúng 6 ký tự số, ví dụ `"483920"` |

### 1.7 Pagination / Filtering / Sorting

- **Filtering**: `GET /tasks` hỗ trợ query param `status` (giá trị
  `open` \| `done` \| `all`; vắng mặt = `all`). Đây là filter duy nhất theo
  đúng SRS mục 6.1 + LLD mục 1.10 (`listTaskQuerySchema`).
- **Sorting**: KHÔNG có trong scope (SRS mục 8.2 loại trừ rõ "sắp xếp" khỏi
  phạm vi).
- **Pagination**: **KHÔNG áp dụng ở v1** cho `GET /tasks` — trả **toàn bộ**
  task của user (không giới hạn `limit`/`offset`/`cursor`).
  - **Đây là một điểm lệch so với nguyên tắc thiết kế API tốt (list endpoint
    nên luôn có pagination)** — ghi nhận rõ, không im lặng bỏ qua. Căn cứ
    giữ nguyên quyết định này cho v1: NFR-1 (SRS mục 4) nêu rõ quy mô dữ
    liệu mục tiêu "vài chục task (~50)" mỗi user, và cả 3 tầng tài liệu
    (SRS/HLD/LLD) đã thiết kế nhất quán list trả về mảng đầy đủ không phân
    trang (ví dụ `GET /tasks` trả `200 [...]` bare array trong mọi sequence
    diagram LLD).
  - **Khuyến nghị**: nếu quy mô thực tế vượt ~50 task/user hoặc NFR-7 (khả
    năng mở rộng, hiện "chưa xác định" theo SRS mục 4) được làm rõ với số
    liệu lớn hơn, cần bổ sung pagination offset-based (`?page=&limit=`) ở
    phiên bản sau — **cần xác nhận lại với stakeholder**, không tự ý thêm ở
    bản v1 này vì sẽ phá vỡ tính nhất quán với 3 tầng tài liệu đã thống nhất
    trước (xem mục 7 — Open Questions).

### 1.8 Rate limiting

- **Chưa triển khai ở v1** (LLD mục 7.1 đã ghi nhận là rủi ro còn lại,
  khuyến nghị dùng `threat-modeler`/`security-auditor`).
- **Khuyến nghị mạnh cho khi triển khai thật**: `POST /auth/login`,
  `POST /auth/forgot-password`, `POST /auth/reset-password` là các endpoint
  nhạy cảm (brute-force credential, brute-force OTP, spam gửi email) — nên
  áp rate limit theo IP và/hoặc theo email (ví dụ tối đa N request/phút).
  Đây là **khuyến nghị thiết kế**, không phải cơ chế đã implement, không
  nằm trong response schema của tài liệu này.

---

## 2. Auth Endpoints

### 2.1 `POST /auth/register`

Đăng ký tài khoản, trả JWT trực tiếp (không cần gọi `/auth/login` riêng —
theo HLD mục 3.1 đã chốt).

**Auth required**: Không.

**Request Body:**

| Field | Type | Required | Validation |
|---|---|---|---|
| `email` | string | Có | Format email hợp lệ (Zod `.email()`); tối đa 255 ký tự *(giả định, không có trong SRS — xem mục 7)* |
| `password` | string | Có | `minLength: 8`, `maxLength: 72` *(giới hạn kỹ thuật bcrypt — bcrypt chỉ xử lý tối đa 72 byte, phần vượt bị cắt âm thầm nếu không chặn — xem mục 7)*, phải chứa ≥1 chữ cái và ≥1 chữ số. Regex minh họa: `^(?=.*[A-Za-z])(?=.*\d).{8,72}$` |
| `confirmPassword` | string | Có | Phải **khớp chính xác** với `password` (Zod `.refine()`) |

**Response thành công (`201 Created`):**

```json
{
  "accessToken": "string (JWT)",
  "user": {
    "id": "string (UUID)",
    "email": "string",
    "createdAt": "string (ISO-8601 UTC)"
  }
}
```

Ví dụ:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhMWIyYzNkNC1lNWY2LTQ3ODktOTBhYi1jZGVmMTIzNDU2NzgiLCJlbWFpbCI6InVzZXJAZXhhbXBsZS5jb20iLCJpYXQiOjE3NjAwMDAwMDAsImV4cCI6MTc2MDYwNDgwMH0.signature",
  "user": {
    "id": "a1b2c3d4-e5f6-4789-90ab-cdef12345678",
    "email": "user@example.com",
    "createdAt": "2026-10-14T08:30:00.000Z"
  }
}
```

**Response lỗi:**

| Status | Tình huống | Body mẫu |
|---|---|---|
| `400` | Email sai format / password không đạt rule / thiếu field | `{ "message": "Dữ liệu không hợp lệ", "details": { "fieldErrors": { "email": ["Email không đúng định dạng"] }, "formErrors": [] } }` |
| `400` | `confirmPassword` không khớp `password` | `{ "message": "Dữ liệu không hợp lệ", "details": { "fieldErrors": { "confirmPassword": ["Xác nhận mật khẩu không khớp"] }, "formErrors": [] } }` |
| `409` | Email đã tồn tại | `{ "message": "Email đã được sử dụng" }` |
| `500` | Lỗi hệ thống | `{ "message": "Đã có lỗi xảy ra, vui lòng thử lại sau" }` |

**Ghi chú thiết kế:**
- **Idempotency**: `POST` tạo resource, **không idempotent** theo thiết kế
  hiện tại (gọi lại với cùng email lần 2 → `409`, không tạo trùng — đây là
  idempotency "tự nhiên" nhờ unique constraint email, không cần
  Idempotency-Key header riêng; rủi ro duplicate thực tế thấp vì resource là
  tài khoản, không phải giao dịch tài chính).
- **Bảo mật**: không yêu cầu auth (hợp lý — đây là endpoint tạo tài khoản).
  Không có IDOR (không có path/query param ID). Không log `password`/
  `confirmPassword` plaintext (NFR-3/NFR-4 SRS).

---

### 2.2 `POST /auth/login`

**Auth required**: Không.

**Request Body:**

| Field | Type | Required | Validation |
|---|---|---|---|
| `email` | string | Có | Không rỗng (không enforce format email ở login — theo LLD mục 1.10, tránh lộ thông tin email có hợp lệ format hay không khác với có tồn tại hay không) |
| `password` | string | Có | Không rỗng |

**Response thành công (`200 OK`):** schema giống `2.1`.

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "a1b2c3d4-e5f6-4789-90ab-cdef12345678",
    "email": "user@example.com",
    "createdAt": "2026-10-10T03:15:00.000Z"
  }
}
```

**Response lỗi:**

| Status | Tình huống | Body mẫu |
|---|---|---|
| `400` | `email`/`password` rỗng hoặc thiếu | `{ "message": "Dữ liệu không hợp lệ", "details": { "fieldErrors": { "password": ["Không được để trống"] }, "formErrors": [] } }` |
| `401` | Sai email HOẶC sai mật khẩu (dùng **chung 1 message**, không tiết lộ trường nào sai) | `{ "message": "Email hoặc mật khẩu không đúng" }` |
| `500` | Lỗi hệ thống | `{ "message": "Đã có lỗi xảy ra, vui lòng thử lại sau" }` |

**Ghi chú thiết kế:**
- **Idempotency**: về bản chất `POST` không ghi thay đổi trạng thái tài
  nguyên (không tạo gì mới), gọi nhiều lần an toàn (chỉ sinh JWT mới mỗi
  lần, không side-effect dữ liệu) — không cần idempotency key.
- **Bảo mật**: message lỗi gộp chung `401` cho sai email/sai password là
  **chủ đích** (chống user-enumeration qua thông báo lỗi). **Khuyến nghị rate
  limiting theo IP/email** (mục 1.8) để chống brute-force — chưa implement,
  cần làm trước production thật.

---

### 2.3 `POST /auth/forgot-password`

Yêu cầu gửi OTP đặt lại mật khẩu qua email. **LUÔN trả `200`** bất kể email
có tồn tại trong hệ thống hay không (chống dò email — đã chốt ở HLD).

**Auth required**: Không.

**Request Body:**

| Field | Type | Required | Validation |
|---|---|---|---|
| `email` | string | Có | Format email hợp lệ (Zod `.email()`) |

**Response thành công (`200 OK`) — LUÔN trả, bất kể email tồn tại hay không:**

```json
{
  "message": "Nếu email tồn tại trong hệ thống, bạn sẽ nhận được hướng dẫn đặt lại mật khẩu"
}
```

**Response lỗi:**

| Status | Tình huống | Body mẫu |
|---|---|---|
| `400` | `email` sai format hoặc rỗng *(lưu ý: đây là lỗi **cú pháp input**, không liên quan tới việc email có tồn tại trong DB hay không — KHÔNG vi phạm nguyên tắc chống user-enumeration, vì không tiết lộ gì về sự tồn tại tài khoản)* | `{ "message": "Dữ liệu không hợp lệ", "details": { "fieldErrors": { "email": ["Email không đúng định dạng"] }, "formErrors": [] } }` |
| `500` | Lỗi hệ thống (không bao gồm lỗi gửi email SMTP — lỗi đó bị nuốt có chủ đích ở Service theo LLD mục 1.9, KHÔNG trả `500` cho client vì sẽ làm response khác với trường hợp "email không tồn tại", lộ thông tin) | `{ "message": "Đã có lỗi xảy ra, vui lòng thử lại sau" }` |

**KHÔNG BAO GIỜ trả `404`** dù email không tồn tại — đây là khác biệt rõ với
bảng gốc ở SRS mục 6.2 (SRS liệt kê `404` là error khả dĩ nhưng đồng thời ghi
nhận mâu thuẫn ở mục 10.2); tài liệu này **chốt dứt điểm theo hướng bảo mật**:
không có `404` cho endpoint này trong bất kỳ trường hợp nào.

**Ghi chú thiết kế:**
- **Idempotency**: gọi nhiều lần với cùng email → mỗi lần sinh OTP mới (ghi
  đè OTP cũ theo LLD mục 1.3) → không hoàn toàn idempotent theo nghĩa "kết
  quả cuối giống nhau", nhưng **response luôn giống nhau** (`200` + message
  cố định) — đáp ứng đúng yêu cầu bảo mật của endpoint này, không cần
  idempotency key.
- **Bảo mật**: đây là trọng tâm anti-enumeration của toàn hệ thống — **test
  bắt buộc** (theo LLD mục 1.12): response với email tồn tại và không tồn
  tại phải **giống nhau 100%** (status + body + thời gian phản hồi tương
  đối — timing attack ngoài phạm vi contract này nhưng nên lưu ý khi
  implement, ví dụ dùng `await` đều cho nhánh có/không gửi email). **Cần
  rate limiting** (mục 1.8) để chống spam gửi OTP liên tục tới 1 email.

---

### 2.4 `POST /auth/reset-password`

**Auth required**: Không.

**Request Body:**

| Field | Type | Required | Validation |
|---|---|---|---|
| `email` | string | Có | Format email hợp lệ |
| `otp` | string | Có | Đúng 6 ký tự số, pattern `^\d{6}$` |
| `newPassword` | string | Có | Cùng rule với `password` ở `2.1` (`minLength 8`, `maxLength 72`, ≥1 chữ + ≥1 số) |
| `confirmNewPassword` | string | Có | Phải khớp chính xác với `newPassword` (Zod `.refine()`) |

**Response thành công (`200 OK`):**

```json
{ "message": "Đặt lại mật khẩu thành công" }
```

**Response lỗi:**

| Status | Tình huống | Body mẫu |
|---|---|---|
| `400` | Lỗi cú pháp input (email sai format, otp không đúng 6 số, password không đạt rule, `confirmNewPassword` không khớp) | `{ "message": "Dữ liệu không hợp lệ", "details": { "fieldErrors": { "otp": ["OTP phải gồm đúng 6 chữ số"] }, "formErrors": [] } }` |
| `400` | OTP sai / đã hết hạn / email không tồn tại / vượt quá `MAX_OTP_ATTEMPTS` — **dùng CHUNG 1 message, KHÔNG có `details`** (chủ đích gộp chung để không tiết lộ nguyên nhân cụ thể, bao gồm cả trường hợp email không tồn tại — nhất quán với nguyên tắc anti-enumeration của `2.3`) | `{ "message": "Mã xác nhận không đúng hoặc đã hết hạn" }` |
| `500` | Lỗi hệ thống | `{ "message": "Đã có lỗi xảy ra, vui lòng thử lại sau" }` |

**Ghi chú thiết kế:**
- **Idempotency**: OTP là one-time-use (bị xóa ngay sau khi dùng thành công
  theo LLD mục 1.3) → gọi lại lần 2 với cùng OTP sau khi đã thành công lần 1
  sẽ nhận `400` (OTP không còn hiệu lực) — hành vi này là **chủ đích bảo
  mật**, không phải lỗi thiết kế idempotency.
- **Bảo mật**: message `400` cho mọi lỗi liên quan OTP/email-không-tồn-tại
  phải **giống nhau tuyệt đối** — đây là điểm dễ bị lộ thông tin nhất nếu
  implement sai (ví dụ vô tình trả message khác nhau giữa "email không tồn
  tại" và "OTP sai"). **Cần rate limiting** theo email/IP để chống
  brute-force không gian 10^6 giá trị OTP (LLD đã giới hạn `MAX_OTP_ATTEMPTS
  = 5` ở mức business logic, nhưng rate limiting ở tầng network vẫn nên có
  thêm).

---

## 3. Task Endpoints

> **Tất cả endpoint dưới đây yêu cầu `Authorization: Bearer <accessToken>`.**
> Thiếu/sai/hết hạn token → `401` theo mục 1.2, áp dụng đồng nhất, không lặp
> lại ở từng endpoint.

**Task object schema (dùng chung cho response thành công của mọi endpoint
Task):**

```json
{
  "id": "string (UUID)",
  "title": "string",
  "description": "string | null",
  "status": "\"open\" | \"done\"",
  "createdAt": "string (ISO-8601 UTC)",
  "updatedAt": "string (ISO-8601 UTC)"
}
```

Lưu ý: **KHÔNG expose `ownerId`** trong response — client không cần biết
field này (ownership được backend enforce nội bộ qua `req.userId`, không
qua field trong payload), giảm over-exposure dữ liệu không cần thiết.

### 3.1 `POST /tasks`

Tạo task mới cho chính user gọi request. `status` mặc định `"open"`, client
không được set `status` khi tạo.

**Request Body:**

| Field | Type | Required | Validation |
|---|---|---|---|
| `title` | string | Có | Server trim (`.trim()`); sau khi trim `minLength: 1` (reject rỗng/chỉ-khoảng-trắng); `maxLength: 200` *(giả định — không có trong SRS, xem mục 7)* |
| `description` | string \| null | Không | Nếu có: `maxLength: 2000` *(giả định — xem mục 7)*. Không gửi hoặc gửi `null` → lưu `null` |

**Response thành công (`201 Created`):** Task object (xem schema chung).

Ví dụ:
```json
{
  "id": "3f29b6b0-df3e-4a1b-9e21-7f9a2d6c1a44",
  "title": "Mua sữa",
  "description": "Mua sữa tươi không đường ở siêu thị",
  "status": "open",
  "createdAt": "2026-10-14T08:30:00.000Z",
  "updatedAt": "2026-10-14T08:30:00.000Z"
}
```

**Response lỗi:**

| Status | Tình huống | Body mẫu |
|---|---|---|
| `400` | `title` rỗng/chỉ-khoảng-trắng/thiếu, hoặc vượt `maxLength` | `{ "message": "Dữ liệu không hợp lệ", "details": { "fieldErrors": { "title": ["Tiêu đề không được để trống"] }, "formErrors": [] } }` |
| `401` | Thiếu/sai/hết hạn token | `{ "message": "Token không hợp lệ hoặc đã hết hạn" }` |
| `500` | Lỗi hệ thống | `{ "message": "Đã có lỗi xảy ra, vui lòng thử lại sau" }` |

**Ghi chú thiết kế:**
- **Idempotency**: `POST` tạo resource, **không idempotent** — gọi 2 lần
  với cùng `title`/`description` sẽ tạo **2 task riêng biệt** (không có
  unique constraint trên title). Rủi ro double-submit khi mất mạng/user bấm
  Lưu 2 lần đã được xử lý ở phía Android (disable nút khi `isSubmitting`,
  theo LLD mục 4.1/6.3), **không** yêu cầu Idempotency-Key header ở backend
  v1 — mức rủi ro (task trùng do user, có thể tự xóa) thấp hơn nhiều so với
  giao dịch tài chính nên không cần cơ chế nặng hơn. Ghi nhận là chấp nhận
  được cho scope hiện tại.
- **Bảo mật**: cần `401` (có). Không cần `403`/IDOR vì không có path param
  ID — resource luôn gắn `ownerId = req.userId` tại thời điểm tạo.

---

### 3.2 `GET /tasks`

List toàn bộ task của chính user gọi request (enforce ownership qua
`WHERE ownerId = req.userId`).

**Query Param:**

| Param | Type | Required | Validation |
|---|---|---|---|
| `status` | string | Không | Enum `"open"` \| `"done"` \| `"all"`. Vắng mặt = coi như `"all"`. Giá trị khác → `400`. |

**Response thành công (`200 OK`):** array of Task object (bare array,
**không pagination** — xem mục 1.7).

Ví dụ (`GET /tasks?status=open`):
```json
[
  {
    "id": "3f29b6b0-df3e-4a1b-9e21-7f9a2d6c1a44",
    "title": "Mua sữa",
    "description": "Mua sữa tươi không đường ở siêu thị",
    "status": "open",
    "createdAt": "2026-10-14T08:30:00.000Z",
    "updatedAt": "2026-10-14T08:30:00.000Z"
  }
]
```

Ví dụ danh sách trống: `[]`

**Response lỗi:**

| Status | Tình huống | Body mẫu |
|---|---|---|
| `400` | `status` có giá trị khác `"open"`/`"done"`/`"all"` | `{ "message": "Dữ liệu không hợp lệ", "details": { "fieldErrors": { "status": ["Giá trị không hợp lệ, phải là open, done hoặc all"] }, "formErrors": [] } }` |
| `401` | Thiếu/sai/hết hạn token | `{ "message": "Token không hợp lệ hoặc đã hết hạn" }` |
| `500` | Lỗi hệ thống | `{ "message": "Đã có lỗi xảy ra, vui lòng thử lại sau" }` |

**Ghi chú thiết kế:**
- **Pagination**: KHÔNG áp dụng ở v1 — xem lý do + rủi ro đã ghi nhận ở mục
  1.7 (không lặp lại).
- **Bảo mật**: `WHERE ownerId = req.userId` ở mọi query — không có cách nào
  từ client lấy task của user khác qua endpoint này (không có IDOR vì không
  có ID trong request, chỉ lọc theo chính identity từ token).

---

### 3.3 `GET /tasks/:id` — **MỚI BỔ SUNG**

Lấy chi tiết 1 task theo ID, enforce ownership. **Bổ sung để loại bỏ rủi ro
mất dữ liệu ở màn hình Sửa Task** khi Android dùng `SelectedTaskHolder`
in-memory (LLD mục 10.3) — Android nên gọi endpoint này khi mở
`EditTaskScreen` thay vì phụ thuộc hoàn toàn vào dữ liệu truyền qua
navigation/in-memory holder.

**Path Param:**

| Param | Type | Required | Validation |
|---|---|---|---|
| `id` | string | Có | Phải là UUID hợp lệ về format; nếu sai format UUID → coi như không tìm thấy, trả `404` (không `400`) — tránh lộ thông tin về việc hệ thống phân biệt "UUID sai format" khác với "UUID đúng format nhưng không tồn tại", giữ hành vi nhất quán đơn giản cho client |

**Response thành công (`200 OK`):** Task object (xem schema chung).

Ví dụ (`GET /tasks/3f29b6b0-df3e-4a1b-9e21-7f9a2d6c1a44`):
```json
{
  "id": "3f29b6b0-df3e-4a1b-9e21-7f9a2d6c1a44",
  "title": "Mua sữa",
  "description": "Mua sữa tươi không đường ở siêu thị",
  "status": "open",
  "createdAt": "2026-10-14T08:30:00.000Z",
  "updatedAt": "2026-10-14T08:30:00.000Z"
}
```

**Response lỗi:**

| Status | Tình huống | Body mẫu |
|---|---|---|
| `401` | Thiếu/sai/hết hạn token | `{ "message": "Token không hợp lệ hoặc đã hết hạn" }` |
| `403` | Task tồn tại nhưng thuộc user khác | `{ "message": "Bạn không có quyền thực hiện thao tác này" }` |
| `404` | Task không tồn tại (hoặc `id` sai format UUID) | `{ "message": "Task không tồn tại" }` |
| `500` | Lỗi hệ thống | `{ "message": "Đã có lỗi xảy ra, vui lòng thử lại sau" }` |

**Ghi chú thiết kế:**
- **Idempotency**: `GET` vốn không có side-effect, idempotent tự nhiên,
  không cần xử lý thêm.
- **Pagination**: không áp dụng (lấy 1 resource, không phải list).
- **Nguy cơ IDOR**: đây là endpoint **path param dùng ID trực tiếp** — rủi
  ro IDOR kinh điển nếu chỉ check "ID hợp lệ là trả dữ liệu". Thiết kế dùng
  **chung logic `assertOwnedTaskOrThrow`** đã có sẵn ở `PUT`/`DELETE`
  `/tasks/:id` (LLD mục 1.9): tách rõ `404` (không tồn tại) vs `403` (tồn
  tại nhưng không phải chủ) — **nhất quán với 2 endpoint kia**, không tạo
  thêm hành vi IDOR mới so với thiết kế đã có. Lưu ý: cách phân biệt
  `403`/`404` này để lộ (một phần) thông tin "ID task đó có tồn tại trong hệ
  thống hay không" cho user không phải chủ — đây là trade-off **đã được
  chấp nhận từ trước** ở `PUT`/`DELETE` (không phải rủi ro mới do endpoint
  này gây ra); nếu muốn triệt tiêu hoàn toàn độ lộ này, cần đổi cả 3 endpoint
  cùng lúc sang luôn trả `404` cho cả 2 trường hợp — **đây là thay đổi ảnh
  hưởng toàn hệ thống, cần xác nhận riêng với stakeholder, không tự đổi ở
  phạm vi bổ sung 1 endpoint này** (xem mục 7).

---

### 3.4 `PUT /tasks/:id`

Update title/description và/hoặc toggle status. Dùng 1 endpoint cho cả 2
hành vi nghiệp vụ Android gọi riêng (sửa nội dung / toggle trạng thái) —
nhất quán với contract gốc SRS mục 6.1 (không tách 2 endpoint ở tầng HTTP).

**Path Param:**

| Param | Type | Required | Validation |
|---|---|---|---|
| `id` | string | Có | UUID. Sai format → `404` (giống 3.3) |

**Request Body** (tất cả field optional, nhưng **phải có ít nhất 1 field**):

| Field | Type | Required | Validation |
|---|---|---|---|
| `title` | string | Không | Nếu có: server trim, `minLength: 1` sau trim, `maxLength: 200` |
| `description` | string \| null | Không | Nếu có: `maxLength: 2000`. Gửi `null` để xóa description hiện tại |
| `status` | string | Không | Nếu có: enum `"open"` \| `"done"` (KHÔNG nhận `"all"` ở đây) |
| *(ràng buộc tổng)* | — | — | Body rỗng `{}` hoặc thiếu cả 3 field trên → `400` (Zod `.refine()`) |

**Response thành công (`200 OK`):** Task object đã cập nhật (xem schema
chung).

Ví dụ request toggle:
```json
{ "status": "done" }
```
Ví dụ response:
```json
{
  "id": "3f29b6b0-df3e-4a1b-9e21-7f9a2d6c1a44",
  "title": "Mua sữa",
  "description": "Mua sữa tươi không đường ở siêu thị",
  "status": "done",
  "createdAt": "2026-10-14T08:30:00.000Z",
  "updatedAt": "2026-10-14T09:05:00.000Z"
}
```

**Response lỗi:**

| Status | Tình huống | Body mẫu |
|---|---|---|
| `400` | Body rỗng/không có field nào | `{ "message": "Dữ liệu không hợp lệ", "details": { "fieldErrors": {}, "formErrors": ["Phải cung cấp ít nhất một trong các trường: title, description, status"] } }` |
| `400` | `title` gửi nhưng rỗng/chỉ-khoảng-trắng; hoặc `status` không thuộc enum | `{ "message": "Dữ liệu không hợp lệ", "details": { "fieldErrors": { "title": ["Tiêu đề không được để trống"] }, "formErrors": [] } }` |
| `401` | Thiếu/sai/hết hạn token | `{ "message": "Token không hợp lệ hoặc đã hết hạn" }` |
| `403` | Task thuộc user khác | `{ "message": "Bạn không có quyền thực hiện thao tác này" }` |
| `404` | Task không tồn tại (đã xóa/ID sai format) | `{ "message": "Task không tồn tại" }` |
| `500` | Lỗi hệ thống | `{ "message": "Đã có lỗi xảy ra, vui lòng thử lại sau" }` |

**Ghi chú thiết kế:**
- **Idempotency**: **CÓ, đúng ngữ nghĩa `PUT`** — gọi lại nhiều lần với cùng
  body cho ra cùng kết quả cuối (ví dụ `{ "status": "done" }` gọi 2 lần vẫn
  kết thúc ở `status: "done"`, chỉ `updatedAt` đổi). Đây là lý do thiết kế
  dùng `PUT` (không dùng `PATCH`) nhất quán với contract gốc SRS — mỗi field
  gửi lên set giá trị tuyệt đối, không có phép toán tương đối (ví dụ không
  có `"toggle": true` kiểu lật ngược trạng thái không xác định trước — client
  Android tự tính `newStatus` trước khi gửi, theo đúng
  `ToggleTaskStatusUseCase` ở LLD mục 4.1).
- **Nguy cơ IDOR**: path param `id` trực tiếp — PHẢI kiểm tra
  `task.ownerId === req.userId` trước khi update (đã có sẵn thiết kế
  `assertOwnedTaskOrThrow`), không chỉ dựa vào "ID hợp lệ tồn tại trong DB".

---

### 3.5 `DELETE /tasks/:id`

**Path Param:**

| Param | Type | Required | Validation |
|---|---|---|---|
| `id` | string | Có | UUID. Sai format → `404` |

**Request Body**: Không có.

**Response thành công (`204 No Content`):** Không có body.

**Response lỗi:**

| Status | Tình huống | Body mẫu |
|---|---|---|
| `401` | Thiếu/sai/hết hạn token | `{ "message": "Token không hợp lệ hoặc đã hết hạn" }` |
| `403` | Task thuộc user khác | `{ "message": "Bạn không có quyền thực hiện thao tác này" }` |
| `404` | Task không tồn tại (đã bị xóa trước đó/ID sai format) | `{ "message": "Task không tồn tại" }` |
| `500` | Lỗi hệ thống | `{ "message": "Đã có lỗi xảy ra, vui lòng thử lại sau" }` |

**Ghi chú thiết kế:**
- **Idempotency**: idempotent theo **end-state của resource** (sau khi gọi,
  task luôn ở trạng thái "không còn tồn tại") nhưng **không idempotent theo
  response code tuyệt đối** — lần gọi đầu thành công `204`, lần gọi lặp lại
  sau đó (task đã bị xóa) trả `404` (không trả `204` lần 2). Đây là lựa chọn
  **có chủ đích, giữ nguyên theo SRS/LLD**: AC FR-14/FR-15 yêu cầu rõ phải
  phân biệt được "xóa lần đầu thành công" vs "đã bị xóa từ trước ở nơi
  khác" để hiển thị thông báo phù hợp ("Task này đã được xóa trước đó")
  thay vì coi như thành công im lặng lần 2 — ưu tiên UX rõ ràng hơn
  idempotency tuyệt đối theo response code. Đây là trade-off đã biết, không
  phải sai sót.
- **Nguy cơ IDOR**: tương tự `3.3`/`3.4` — bắt buộc check ownership trước
  xóa.

---

## 4. Checklist thiết kế API tốt (rà soát toàn bộ)

| Mục | Kết quả |
|---|---|
| Idempotency | `PUT /tasks/:id`: đạt (set giá trị tuyệt đối theo field, không phép toán tương đối). `DELETE /tasks/:id`: idempotent theo end-state, không theo response code (có chủ đích, đã giải thích ở 3.5). `POST /auth/register`, `POST /tasks`: không idempotent nhưng rủi ro duplicate thấp, không cần Idempotency-Key ở v1. |
| Pagination cho list endpoint | `GET /tasks`: **KHÔNG áp dụng ở v1** — lệch khỏi best practice, đã giải thích rõ lý do + rủi ro + khuyến nghị ở mục 1.7, cần xác nhận lại nếu scale thay đổi. |
| N+1 (GraphQL) | Không áp dụng — dự án dùng REST thuần, không có GraphQL resolver. |
| Over-fetching / exposure dữ liệu nhạy cảm | `passwordHash`, `resetOtpHash`, `resetOtpExpiresAt`, `resetOtpAttemptCount` KHÔNG xuất hiện trong bất kỳ response nào (chỉ nội bộ DB). `Task.ownerId` không expose. JWT không chứa field nhạy cảm ngoài `sub`/`email`/`iat`/`exp`. |
| Error case đầy đủ | Có — mỗi endpoint đã liệt kê đủ `400/401/403/404/409/500` phù hợp ngữ cảnh (không endpoint nào chỉ có happy path). |

---

## 5. Bảo mật cần lưu ý khi implement (tổng hợp)

- **Xác thực (`401`)**: bắt buộc cho toàn bộ `/tasks/*` (5 endpoint, bao gồm
  `GET /tasks/:id` mới). `/auth/*` không cần (hợp lý, đây là endpoint công
  khai phục vụ đăng ký/đăng nhập).
- **Phân quyền (`403`) / Ownership**: bắt buộc cho `GET/PUT/DELETE
  /tasks/:id` — mọi thao tác trên 1 task cụ thể phải kiểm tra
  `task.ownerId === req.userId` **trước khi** đọc/sửa/xóa, dùng logic chung
  `assertOwnedTaskOrThrow`.
- **Nguy cơ IDOR**: 3 endpoint dùng `:id` trực tiếp (`GET`, `PUT`, `DELETE
  /tasks/:id`) đều có rủi ro IDOR kinh điển nếu chỉ validate "ID có đúng
  format UUID" mà bỏ qua check ownership — đã thiết kế chặn bằng
  `assertOwnedTaskOrThrow` ở cả 3, nhất quán. Trade-off `403` vs `404` lộ
  một phần thông tin tồn tại ID đã được ghi nhận rõ ở mục 3.3, là quyết định
  kế thừa từ thiết kế gốc, không phải lỗ hổng mới.
- **Rate limiting**: khuyến nghị mạnh cho `POST /auth/login`,
  `POST /auth/forgot-password`, `POST /auth/reset-password` (mục 1.8) —
  CHƯA implement, cần bổ sung trước khi lên production thật.
- **Input validation**: mọi field đều có kiểu dữ liệu/độ dài/định dạng rõ
  ràng (mục 2–3), validate server-side bằng Zod — **không tin tưởng client
  đã validate** dù Android cũng validate inline (defense in depth, đúng theo
  LLD mục 4.1).
- **Không log dữ liệu nhạy cảm**: `password`, `confirmPassword`,
  `newPassword`, `confirmNewPassword`, `otp`, JWT token — không bao giờ log
  plaintext ở bất kỳ tầng nào (NFR-3/NFR-4 SRS, LLD mục 1.4/7.1).
- **Anti-enumeration**: `POST /auth/forgot-password` và nhánh lỗi OTP của
  `POST /auth/reset-password` phải trả response **giống nhau tuyệt đối**
  dù email tồn tại hay không — test bắt buộc trước khi release (LLD mục
  1.12).

---

## 6. Khác biệt so với DTO giả định ở LLD (để cập nhật ngược lại LLD)

| Điểm | LLD giả định (mục 1.9/3.5) | Chốt chính thức ở tài liệu này | Lý do |
|---|---|---|---|
| `GET /tasks/:id` | Không có trong contract (dùng `SelectedTaskHolder` in-memory) | **Thêm mới**, mục 3.3 | Loại bỏ rủi ro mất dữ liệu khi process death (LLD mục 10.3), chi phí thấp |
| `user` object trong `AuthResponseDto`/response register/login | LLD (Android `UserDto`) chỉ có `id`, `email` | Thêm `createdAt` | Field an toàn (không nhạy cảm), hữu ích cho client, không tốn thêm round-trip |
| Error response `details` | LLD `AppError.details: unknown` chưa định hình cấu trúc cụ thể | Chốt cấu trúc `{ fieldErrors: {...}, formErrors: [...] }` (dạng Zod `.flatten()`) | Cần 1 cấu trúc cụ thể, nhất quán để Android parse được; chọn theo convention Zod sẵn có trong stack |
| `title`/`description` `maxLength` | Không có trong LLD/SRS | `title: 200`, `description: 2000` | SRS không ràng buộc, cần giới hạn hợp lý để tránh abuse/DB bloat — **giả định, cần xác nhận lại** |
| `password`/`newPassword` `maxLength` | Không có trong LLD/SRS | `72` | Giới hạn kỹ thuật của bcrypt (72 byte) — bắt buộc về mặt kỹ thuật, không phải lựa chọn tùy ý |
| `email` `maxLength` | Không có | `255` | Giả định theo giới hạn cột DB phổ biến — cần xác nhận khi viết migration Prisma thật |
| `status` query `GET /tasks` | LLD: enum `'open'\|'done'\|'all'` optional | Giữ nguyên, chốt thêm: giá trị khác 3 giá trị này → `400` (LLD chưa nói rõ invalid value xử lý sao) | Làm rõ hành vi lỗi còn thiếu ở LLD |
| `PUT /tasks/:id` field sai format UUID ở path | LLD chưa đề cập | Chốt: sai format UUID → `404` (không `400`) | Nhất quán đơn giản, tránh thêm 1 loại lỗi riêng cho "ID sai format" |

---

## 7. Open Questions / Giả định cần xác nhận lại với stakeholder

1. **Pagination cho `GET /tasks`** (mục 1.7): quyết định v1 là KHÔNG phân
   trang, dựa trên NFR-1 (~50 task/user). Nếu NFR-7 (khả năng mở rộng, SRS
   mục 4 còn "chưa xác định") sau này được làm rõ với số liệu lớn hơn, cần
   quay lại bổ sung pagination — **chưa tự quyết ở đây**.
2. **`403` vs `404` cho task thuộc user khác** (mục 3.3/3.4/3.5): thiết kế
   hiện tại phân biệt rõ 2 mã lỗi (lộ một phần thông tin "ID có tồn tại hay
   không" cho người không phải chủ task). Đây là hành vi **kế thừa từ thiết
   kế gốc** (SRS mục 6.1 cũng liệt kê `403/404` riêng), được giữ nguyên cho
   nhất quán. Nếu muốn triệt tiêu hoàn toàn độ lộ này (luôn trả `404` cho cả
   2 trường hợp), cần xác nhận riêng vì ảnh hưởng cả 3 endpoint cùng lúc.
3. **Chuẩn hóa email (lowercase/trim) trước khi lưu/so sánh**: SRS/HLD/LLD
   đều không đề cập — hiện tại contract này **không chuẩn hóa**, nghĩa là
   `User@Example.com` và `user@example.com` có thể tạo thành 2 tài khoản
   khác nhau (nếu DB unique constraint case-sensitive, mặc định Postgres
   `TEXT`/`VARCHAR` là case-sensitive). **Cần quyết định**: có chuẩn hóa
   `toLowerCase().trim()` trước khi lưu/so sánh hay không — đây là lỗ hổng
   nghiệp vụ tiềm ẩn (duplicate account) nếu không xử lý, chưa được chốt bởi
   3 tầng tài liệu trước và không tự quyết ở đây.
4. **`title`/`description`/`email` `maxLength`** (mục 6): là giả định kỹ
   thuật hợp lý của riêng tài liệu này (không có nguồn từ SRS/HLD/LLD), cần
   stakeholder xác nhận trước khi đưa vào Prisma schema/migration thật.
5. **JWT TTL = 7 ngày, bcrypt `SALT_ROUNDS = 10`, OTP TTL = 10 phút,
   `MAX_OTP_ATTEMPTS = 5`**: giữ nguyên giả định đã có ở LLD mục 9.9/9.1 —
   tài liệu này không đổi, nhưng nhắc lại là **chưa có NFR nguồn** (SRS
   NFR-6 "chưa xác định"), cần xác nhận khi có yêu cầu phi chức năng cụ thể
   hơn.

---

## 8. Bước tiếp theo gợi ý

- Cập nhật `docs/LLD_ToDo_Android.md`: đổi thiết kế `EditTaskScreen` sang
  gọi `GET /tasks/:id` (bỏ/giữ `SelectedTaskHolder` chỉ như cơ chế truyền
  nhanh id khi có sẵn, không còn là nguồn dữ liệu duy nhất) — **việc này
  thuộc `low-level-designer`, không nằm trong phạm vi file này**.
- Dùng `db-schema-reviewer` khi viết `schema.prisma`/migration thật, đối
  chiếu lại các giới hạn `maxLength` giả định ở mục 6/7.
- Dùng `threat-modeler`/`security-auditor` để đánh giá sâu rate limiting
  trước khi lên production (mục 1.8/5).
- Sau khi Backend/Android code thật theo đúng contract này, dùng
  `api-doc-generator` để sinh tài liệu đối chiếu từ code thật (không phải
  phạm vi của tài liệu draft này).
