# Software Requirements Specification (SRS)
## Ứng dụng Android Mobile Quản lý Task/To-Do (CRUD)

| | |
|---|---|
| **Dự án** | Full-Stack Assessment — Task 2: Task/To-Do CRUD Management |
| **Phiên bản** | 1.0 |
| **Ngày tạo** | 2026-10-08 |
| **Trạng thái** | Draft — chờ review stakeholder |
| **Nguồn yêu cầu** | Worksheet gốc `Task2_Task_ToDo_CRUD.pdf` + làm rõ qua `requirements-analyst` và xác nhận trực tiếp với stakeholder |

---

## Mục lục

1. [Giới thiệu](#1-giới-thiệu)
2. [Mô tả tổng quan](#2-mô-tả-tổng-quan)
3. [Yêu cầu chức năng](#3-yêu-cầu-chức-năng)
4. [Yêu cầu phi chức năng](#4-yêu-cầu-phi-chức-năng)
5. [Data Model](#5-data-model)
6. [API Contract](#6-api-contract)
7. [Edge Cases & Test Scenarios](#7-edge-cases--test-scenarios)
8. [Phạm vi](#8-phạm-vi)
9. [Giao diện ngoài](#9-giao-diện-ngoài)
10. [Phụ lục](#10-phụ-lục)

---

## 1. Giới thiệu

### 1.1 Mục đích tài liệu

Tài liệu này mô tả đầy đủ các yêu cầu chức năng và phi chức năng cho việc xây
dựng một **ứng dụng Android native** quản lý Task/To-Do (CRUD) cùng với
**Backend REST API** phục vụ ứng dụng đó, theo đúng tinh thần IEEE 830 (rút
gọn). Tài liệu dùng làm căn cứ thống nhất giữa các bên liên quan (product
owner, lập trình viên backend, lập trình viên Android, QA) trong suốt quá
trình thiết kế, phát triển và kiểm thử.

### 1.2 Phạm vi tài liệu

Tài liệu bao trùm toàn bộ scope **full-stack** đã được chốt:

- Backend REST API cho quản lý Task (theo đúng contract gốc trong worksheet,
  không thay đổi) và API Auth (đăng ký/đăng nhập/đăng xuất/quên mật khẩu —
  phần **mở rộng** so với worksheet gốc, do scope full-stack + đăng
  nhập/đăng xuất đã được xác nhận).
- Ứng dụng Android native tiêu thụ các API trên, hoạt động theo mô hình
  **online-only** (không cache offline, không đồng bộ nền).

Tài liệu KHÔNG bao gồm: thiết kế kỹ thuật chi tiết (kiến trúc code, lựa chọn
framework/thư viện cụ thể, cơ chế lưu trữ token trên thiết bị, nhà cung cấp
dịch vụ gửi email...), ước lượng effort/story point. Các nội dung này thuộc
phạm vi của tài liệu thiết kế kỹ thuật/estimation riêng.

### 1.3 Đối tượng sử dụng

- Product owner / stakeholder: đối chiếu yêu cầu đã chốt.
- Lập trình viên Backend: triển khai API theo đúng contract và business
  rule.
- Lập trình viên Android: triển khai UI/UX và luồng tương tác theo đúng
  Acceptance Criteria.
- QA/Tester: dùng Acceptance Criteria và Edge Cases làm căn cứ viết test
  case.

### 1.4 Định nghĩa / Thuật ngữ viết tắt

| Thuật ngữ | Giải thích |
|---|---|
| SRS | Software Requirements Specification |
| CRUD | Create, Read, Update, Delete |
| JWT | JSON Web Token — token xác thực dùng trong header `Authorization: Bearer <token>` |
| OTP | One-Time Password — mã xác nhận dùng một lần, dùng trong luồng đặt lại mật khẩu |
| API | Application Programming Interface |
| REST | Representational State Transfer |
| FR | Functional Requirement — Yêu cầu chức năng |
| NFR | Non-Functional Requirement — Yêu cầu phi chức năng |
| AC | Acceptance Criteria — Tiêu chí chấp nhận |
| UUID | Universally Unique Identifier |
| Toast/Snackbar | Thành phần UI Android hiển thị thông báo ngắn, tự biến mất |
| FilterChip | Thành phần UI dạng "chip" dùng để chọn bộ lọc (Material Design) |
| Swipe-to-delete | Thao tác vuốt ngang trên một dòng danh sách để lộ hành động xóa |
| Pull-to-refresh | Thao tác vuốt kéo xuống từ đầu danh sách để làm mới dữ liệu |
| Online-only | Mô hình hoạt động yêu cầu kết nối mạng cho mọi thao tác, không có cache/offline mode |

### 1.5 Tài liệu tham chiếu

- `Task2_Task_ToDo_CRUD.pdf` — Worksheet gốc của bài assessment (Overview,
  User Story gốc, Data Model, API Contract Task, Back-End/Front-End
  Requirements, Edge Cases, Unit Test Scenarios gốc).
- Output làm rõ yêu cầu từ `requirements-analyst` + xác nhận trực tiếp với
  stakeholder (nội dung Epic A–G, mục 7 của tài liệu này).

---

## 2. Mô tả tổng quan

### 2.1 Bối cảnh nghiệp vụ

Đây là bài tập đánh giá kỹ năng full-stack: xây dựng một ứng dụng quản lý
công việc cá nhân (to-do) cho từng người dùng, với yêu cầu nghiệp vụ cốt lõi
là CRUD task có **enforce ownership nghiêm ngặt** (người dùng chỉ thấy và
sửa được task của chính mình). Phạm vi đã được mở rộng so với worksheet gốc
để bao gồm một ứng dụng Android hoàn chỉnh (có đăng ký/đăng nhập/đăng
xuất/quên mật khẩu dùng JWT) thay vì chỉ một "front-end list UI" chung
chung như bản gốc.

Mô hình hoạt động đã chốt là **online-only**: ứng dụng không lưu cache task
offline, không đồng bộ nền. Khi mất kết nối mạng, ứng dụng phải hiển thị lỗi
rõ ràng cho người dùng, không được crash.

### 2.2 Đối tượng dùng / Actor

- **Người dùng (End User)**: cá nhân sử dụng ứng dụng Android để đăng
  ký/đăng nhập và quản lý danh sách task cá nhân của riêng mình (tạo, xem,
  sửa, đánh dấu hoàn thành, xóa). Mỗi người dùng chỉ thấy và thao tác được
  trên task do chính mình sở hữu (`ownerId`).
- **Hệ thống Backend**: đóng vai trò cung cấp API, lưu trữ dữ liệu User và
  Task, enforce ownership và validation.

### 2.3 Giả định & ràng buộc

Các giả định/ràng buộc sau đã được nêu rõ trong input và/hoặc xác nhận trực
tiếp với stakeholder (xem thêm mục 10 — Phụ lục):

- Kiến trúc: **full-stack** — xây cả Backend REST API lẫn ứng dụng Android
  native.
- Xác thực: đăng ký/đăng nhập/đăng xuất đầy đủ, dùng **JWT** lưu trên thiết
  bị; cơ chế lưu trữ token cụ thể (ví dụ EncryptedSharedPreferences,
  Keystore...) thuộc về thiết kế kỹ thuật, không đi sâu ở SRS này.
- Mô hình hoạt động: **online-only** — không cache offline, không đồng bộ
  nền.
- Backend framework-agnostic — không chỉ định công nghệ backend cụ thể,
  nhưng phải tuân thủ đúng API Contract (mục 6).
- Màn hình Tạo task và Sửa task là **màn hình riêng (full-screen)**, không
  phải inline-edit (khác với gợi ý "inline-edit" trong worksheet gốc — đã
  điều chỉnh cho phù hợp UX mobile, xem Phụ lục mục 10.1).
- Xóa task dùng cơ chế **swipe-to-delete** kèm dialog xác nhận.
- Mật khẩu tối thiểu 8 ký tự, gồm ít nhất 1 chữ và 1 số.
- Mỗi task đều thuộc về đúng một chủ sở hữu (`ownerId`); không có tính năng
  chia sẻ/cộng tác task giữa nhiều người dùng.

---

## 3. Yêu cầu chức năng

Mỗi yêu cầu chức năng (FR) tương ứng với một User Story đã làm rõ, được tổ
chức theo Epic. Acceptance Criteria (AC) được giữ nguyên nội dung từ input,
chỉ định dạng lại cho nhất quán.

### 3.1 Epic A — Xác thực (Authentication)

#### FR-01 — Đăng ký tài khoản (US-01)

**User Story:** As a người dùng mới chưa có tài khoản, I want đăng ký tài
khoản bằng email và mật khẩu trên ứng dụng Android, so that tôi có thể bắt
đầu tạo và quản lý danh sách công việc cá nhân của riêng mình.

**Acceptance Criteria:**
- Nhập email + mật khẩu hợp lệ (≥8 ký tự, có ít nhất 1 chữ và 1 số) + xác
  nhận mật khẩu khớp → nhấn Đăng ký → tạo tài khoản thành công → tự động
  đăng nhập → vào thẳng màn hình Danh sách Task kèm thông báo chào mừng.
- Để trống email hoặc mật khẩu hoặc xác nhận mật khẩu → hiển thị lỗi inline
  dưới từng ô, KHÔNG gọi API.
- Email sai định dạng → lỗi inline "Email không hợp lệ", KHÔNG gọi API.
- Mật khẩu không đạt yêu cầu tối thiểu → lỗi inline tương ứng, KHÔNG gọi
  API.
- Xác nhận mật khẩu không khớp mật khẩu → lỗi inline "Mật khẩu xác nhận
  không khớp", KHÔNG gọi API.
- Server trả lỗi email đã tồn tại (`409`) → hiển thị "Email đã được sử
  dụng", giữ nguyên dữ liệu đã nhập.
- Đang gửi request → nút Đăng ký ở trạng thái loading/disable, chống nhấn
  trùng.
- Mất mạng/server không phản hồi → thông báo lỗi mạng rõ ràng, không crash,
  giữ dữ liệu đã nhập.

#### FR-02 — Đăng nhập (US-02)

**User Story:** As a người dùng đã có tài khoản, I want đăng nhập bằng
email và mật khẩu, so that tôi truy cập được vào danh sách task cá nhân của
mình.

**Acceptance Criteria:**
- Email/mật khẩu đúng → xác thực thành công, lưu JWT token trên thiết bị,
  vào màn hình Danh sách Task.
- Để trống email hoặc mật khẩu → lỗi inline, KHÔNG gọi API.
- Sai email hoặc sai mật khẩu → thông báo lỗi chung "Email hoặc mật khẩu
  không đúng" (không tiết lộ trường nào sai), KHÔNG điều hướng.
- Đang xử lý → nút Đăng nhập loading/disable, chống gửi trùng.
- Mất mạng/server lỗi → thông báo lỗi mạng, không crash.
- **Giữ phiên đăng nhập**: mở lại app khi token còn hiệu lực → tự động vào
  thẳng Danh sách Task, không bắt đăng nhập lại.

#### FR-03 — Đăng xuất (US-03)

**User Story:** As a người dùng đã đăng nhập, I want đăng xuất khỏi ứng
dụng, so that tôi bảo vệ được dữ liệu task của mình khi không còn sử dụng
thiết bị hoặc muốn đổi tài khoản.

**Acceptance Criteria:**
- Chọn Đăng xuất → hiển thị dialog xác nhận "Bạn có chắc muốn đăng xuất?"
  (Hủy/Đăng xuất).
- Xác nhận Đăng xuất → xóa token khỏi thiết bị, điều hướng về màn hình
  Đăng nhập.
- Từ màn hình Đăng nhập sau khi đăng xuất, nhấn Back → KHÔNG quay lại được
  Danh sách Task trước đó.
- Mở lại app sau khi đăng xuất → phải đăng nhập lại.

#### FR-04 — Tự động xử lý khi phiên hết hạn (US-04)

**User Story:** As a người dùng đang sử dụng app, I want được thông báo và
tự động đưa về màn hình Đăng nhập khi phiên hết hạn, so that tôi không bị
kẹt ở trạng thái thao tác thất bại mà không hiểu lý do.

**Acceptance Criteria:**
- Server trả `401` ở bất kỳ API task nào → hiển thị "Phiên đăng nhập đã hết
  hạn, vui lòng đăng nhập lại", xóa token cũ, tự động điều hướng về Đăng
  nhập.
- Nếu đang nhập dở dữ liệu ở form Tạo/Sửa task khi phiên hết hạn → hiển thị
  thông báo kèm cảnh báo dữ liệu chưa lưu sẽ mất, sau đó điều hướng về Đăng
  nhập (chấp nhận mất dữ liệu — tình huống bất thường, không cần cơ chế
  khôi phục).

#### FR-05 — Quên mật khẩu (US-05)

> Ghi chú: đây là phần **mở rộng** so với worksheet gốc, bổ sung do quyết
> định mở rộng phạm vi sang full-stack + đăng ký/đăng nhập/đăng xuất đầy
> đủ (xem Phụ lục mục 10.1).

**User Story:** As a người dùng đã đăng ký nhưng quên mật khẩu, I want yêu
cầu đặt lại mật khẩu qua email, so that tôi khôi phục được quyền truy cập
tài khoản mà không cần tạo tài khoản mới.

**Acceptance Criteria:**
- Từ màn hình Đăng nhập, chọn "Quên mật khẩu" → mở màn hình nhập email.
- Nhập email hợp lệ → nhấn Gửi → gọi API `forgot-password` → hiển thị
  thông báo "Nếu email tồn tại trong hệ thống, bạn sẽ nhận được hướng dẫn
  đặt lại mật khẩu" (không tiết lộ email có tồn tại hay không, vì lý do bảo
  mật) → điều hướng về màn hình nhập mã xác nhận + mật khẩu mới.
- Email để trống hoặc sai định dạng → lỗi inline, KHÔNG gọi API.
- Màn hình đặt mật khẩu mới: nhập mã xác nhận (OTP) + mật khẩu mới + xác
  nhận mật khẩu mới → nhấn Xác nhận → gọi API `reset-password` → thành công
  → thông báo "Đặt lại mật khẩu thành công" → điều hướng về màn hình Đăng
  nhập.
- Mã xác nhận sai/hết hạn → lỗi inline "Mã xác nhận không đúng hoặc đã hết
  hạn".
- Mật khẩu mới không đạt yêu cầu tối thiểu hoặc xác nhận không khớp → lỗi
  inline, KHÔNG gọi API.
- Mất mạng/server lỗi ở bất kỳ bước nào → thông báo lỗi mạng rõ ràng, không
  crash.

### 3.2 Epic B — Xem danh sách Task

#### FR-06 — Xem danh sách task của chính mình (US-06)

**Acceptance Criteria:**
- Đã đăng nhập, có task → mở Danh sách Task → hiển thị toàn bộ task thuộc
  về mình, không hiển thị task người khác.
- Đang tải → hiển thị loading state.
- Mỗi task hiển thị title + trạng thái open/done bằng dấu hiệu trực quan
  (gạch ngang cho task done).
- Description hiển thị rút gọn 1 dòng (truncate + ellipsis) ngay trong danh
  sách.

#### FR-07 — Lọc task theo trạng thái All/Open/Done (US-07)

**Acceptance Criteria:**
- UI dùng FilterChip hàng ngang phía trên danh sách (All/Open/Done).
- Chọn "Open" → chỉ hiển thị task open. Chọn "All" → hiển thị lại toàn bộ.
- Đang lọc mà không có task thỏa điều kiện → empty-state riêng theo ngữ
  cảnh bộ lọc (khác với trường hợp hoàn toàn chưa có task nào).
- Đang lọc Open/Done, một task bị toggle khiến không còn thỏa điều kiện lọc
  hiện tại → task đó biến mất khỏi danh sách ngay lập tức.

#### FR-08 — Pull-to-refresh danh sách task (US-08)

**Acceptance Criteria:**
- Vuốt kéo xuống từ đầu danh sách → hiển thị chỉ báo refresh, gọi lại API,
  giữ nguyên bộ lọc đang áp dụng.
- Refresh thành công → danh sách cập nhật theo dữ liệu mới.
- Refresh lỗi → chỉ báo biến mất, danh sách cũ giữ nguyên, hiển thị thông
  báo lỗi.
- App quay lại foreground từ background → KHÔNG tự động refresh, giữ dữ
  liệu đã có (chỉ refresh khi người dùng chủ động pull-to-refresh).

#### FR-09 — Empty-state khi không có task (US-09)

**Acceptance Criteria:**
- Chưa từng tạo task nào → hiển thị "Bạn chưa có task nào. Nhấn + để tạo
  task đầu tiên." kèm gợi ý hành động.
- Đang lọc Open/Done không có task thỏa điều kiện nhưng tổng thể vẫn có
  task khác → empty-state phù hợp ngữ cảnh bộ lọc, khác thông điệp với
  trường hợp trên.

#### FR-10 — Xử lý lỗi mạng khi tải danh sách (US-10)

**Acceptance Criteria:**
- Không có mạng, mở lần đầu chưa có dữ liệu cũ → hiển thị lỗi rõ ràng
  ("Không có kết nối mạng") kèm nút "Thử lại".
- Server trả `401` → xử lý theo FR-04, không hiển thị như lỗi mạng thông
  thường.
- Server trả lỗi khác (`500`, timeout) → thông báo lỗi chung kèm nút Thử
  lại, không crash.

### 3.3 Epic C — Tạo Task

#### FR-11 — Tạo task mới (US-11)

**Acceptance Criteria:**
- Từ Danh sách Task, nhấn nút Thêm (+) → mở màn hình riêng (full-screen)
  "Tạo Task" với 2 ô: Title, Description.
- Nhập Title hợp lệ (không rỗng/không chỉ khoảng trắng) → nhấn Lưu →
  validate client OK → gửi request tạo → thành công → đóng màn hình, quay
  lại danh sách, task mới hiển thị (trạng thái open mặc định) kèm thông báo
  thành công.
- Title để trống hoặc chỉ khoảng trắng → lỗi inline "Tiêu đề không được để
  trống" ngay trên client, KHÔNG gọi API.
- Description trống → vẫn tạo task bình thường (tùy chọn).
- Đang gửi → nút Lưu loading/disable, chống tạo trùng.
- Server trả `400` dù client đã validate → hiển thị lỗi tương ứng trên
  form, giữ dữ liệu đã nhập.
- Mất mạng/server lỗi → thông báo lỗi mạng, giữ dữ liệu đã nhập, màn hình
  vẫn mở để thử lại.
- Xoay màn hình khi đang nhập → dữ liệu Title/Description không bị mất.

### 3.4 Epic D — Sửa & Toggle Task

#### FR-12 — Sửa title/description của task (US-12)

**Acceptance Criteria:**
- Chạm vào dòng task (không phải vào checkbox trạng thái) → mở màn hình
  riêng (full-screen) "Sửa Task" với Title/Description điền sẵn.
- Sửa Title hợp lệ → Lưu → cập nhật thành công → đóng màn hình, danh sách
  hiển thị thông tin mới kèm thông báo thành công.
- Xóa hết Title hoặc chỉ khoảng trắng → lỗi inline, KHÔNG gọi API.
- Task đã bị xóa ở thiết bị/phiên khác trước khi Lưu → server trả `404` →
  hiển thị "Task này không còn tồn tại" → quay lại danh sách đã làm mới
  (loại bỏ task đó).
- Mất mạng/server lỗi khi lưu → thông báo lỗi mạng, giữ nguyên màn hình và
  dữ liệu đã sửa.
- Xoay màn hình khi đang sửa → dữ liệu không bị mất.

#### FR-13 — Toggle trạng thái open/done (US-13)

**Acceptance Criteria:**
- Task đang open, chạm vào checkbox trạng thái → chuyển thành done, UI cập
  nhật ngay, gửi request lên server.
- Task đang done, chạm lại → chuyển về open.
- Chạm nhanh liên tục (double-tap) vào cùng checkbox khi request đầu chưa
  xong → KHÔNG gửi thêm request trùng lặp (disable/ignore input cho tới khi
  có phản hồi).
- Server trả lỗi (mạng/`404`/khác) → UI rollback về đúng trạng thái trước
  khi toggle, hiển thị thông báo lỗi.
- Task đã bị xóa ở thiết bị khác trước khi toggle → server trả `404` →
  hiển thị "Task không còn tồn tại", loại bỏ task khỏi danh sách.

### 3.5 Epic E — Xóa Task

#### FR-14 — Xóa task (US-14)

**Acceptance Criteria:**
- Trên mỗi dòng task, vuốt ngang (swipe-to-delete) để lộ hành động xóa →
  hiển thị dialog xác nhận "Bạn có chắc muốn xóa task này? Hành động không
  thể hoàn tác." (Hủy/Xóa).
- Chọn Hủy → đóng dialog, task vẫn còn nguyên trong danh sách (vị trí/trạng
  thái swipe reset lại).
- Chọn Xóa → gửi request xóa → thành công → task biến mất khỏi danh sách
  ngay lập tức kèm thông báo thành công.
- Server trả `404` (task đã bị xóa từ trước ở nơi khác) → thông báo "Task
  này đã được xóa trước đó", đảm bảo task biến mất khỏi danh sách hiện tại.
- Mất mạng/server lỗi khi xóa → thông báo lỗi mạng, task VẪN còn trong danh
  sách cho tới khi server xác nhận thành công (không xóa UI trước rồi
  rollback, đúng tinh thần online-only).

### 3.6 Epic F — Xử lý ngoại lệ ownership/dữ liệu lỗi thời

#### FR-15 — Xử lý khi thao tác với task không hợp lệ (US-15)

**Acceptance Criteria:**
- Dữ liệu trên thiết bị có task A (tải trước đó) nhưng đã bị xóa ở thiết bị
  khác → Sửa/Toggle/Xóa task A → server trả `404` → hiển thị "Task này
  không còn tồn tại" → tự động loại bỏ task A khỏi danh sách hiển thị.
- Server trả `403` cho một thao tác (trường hợp bất thường) → hiển thị
  "Bạn không có quyền thực hiện thao tác này", không thay đổi UI.
- Lỗi `403`/`404` → KHÔNG hiển thị màn hình lỗi toàn trang gây gián đoạn,
  chỉ thông báo cục bộ (toast/snackbar/dialog) rồi quay lại danh sách.

**Ghi chú:** Vì `GET /tasks` chỉ trả task của chính chủ sở hữu và app không
có deep-link/nhập ID thủ công, lỗi `403`/`404` thực tế chủ yếu phát sinh khi
dữ liệu trên thiết bị đã lỗi thời (task bị xóa ở phiên/thiết bị khác), không
phải do người dùng cố ý truy cập trái phép qua UI.

### 3.7 Epic G — Phản hồi & hiệu năng khi thao tác (traceability với NFR)

> Hai mục dưới đây được mô tả dưới dạng User Story/AC trong input gốc, có
> nội dung thuộc bản chất phi chức năng. Được giữ ở đây để truy vết đầy đủ
> theo đúng cấu trúc Epic đã làm rõ; nội dung tương ứng cũng được phản ánh
> lại dưới dạng NFR ở mục 4.

#### FR-16 — Phản hồi tức thời cho mọi thao tác ghi (US-16)

**Acceptance Criteria:**
- Mọi thao tác ghi (tạo/sửa/toggle/xóa) đang gửi → UI thể hiện trạng thái
  đang xử lý (loading cục bộ trên nút/item).
- Thành công → phản hồi ngắn gọn (toast/snackbar), tự biến mất sau vài
  giây.
- Thất bại → thông báo lỗi tương ứng theo story liên quan.

#### FR-17 — Hiệu năng cuộn danh sách (US-17)

**Acceptance Criteria:**
- Danh sách vài chục task (ví dụ 50) → cuộn không giật/khựng ở mức người
  dùng thông thường nhận thấy.
- App chuyển từ background ra foreground khi danh sách đang hiển thị → vẫn
  hiển thị đúng dữ liệu đã có, không crash/trắng màn hình.

---

## 4. Yêu cầu phi chức năng

| Mã | Nội dung | Nguồn | Trạng thái |
|---|---|---|---|
| NFR-1 | List rendering phải mượt với danh sách vài chục task (ví dụ ~50 task); không có số liệu đo (fps/ms) cụ thể trong input. | Mục 6 input; FR-17/US-17 | Đã xác định (mức thô) |
| NFR-2 | Mọi thao tác ghi (tạo/sửa/toggle/xóa) phải có phản hồi rõ ràng cho người dùng (toast/snackbar thành công hoặc lỗi). | Mục 6 input; FR-16/US-16 | Đã xác định |
| NFR-3 | Bảo mật: email/mật khẩu không được log dạng plaintext; không hiển thị mật khẩu rõ trên UI; JWT token phải được lưu an toàn trên thiết bị (cơ chế lưu trữ cụ thể thuộc về thiết kế kỹ thuật, không đi sâu ở SRS). | Mục 6 input | Đã xác định (mức nguyên tắc, chưa có cơ chế kỹ thuật cụ thể) |
| NFR-4 | Bảo mật mật khẩu backend: mật khẩu phải được hash (không lưu plaintext); validate email format và độ mạnh mật khẩu tối thiểu 8 ký tự gồm ít nhất 1 chữ và 1 số. | Mục 5 input | Đã xác định |
| NFR-5 | Ngôn ngữ/giao diện: không hỗ trợ đa ngôn ngữ, không yêu cầu dark mode, không yêu cầu accessibility đặc biệt (TalkBack...) ở v1 — ghi nhận là ngoài phạm vi v1, có thể bổ sung sau. | Mục 6 input | Đã xác định (ở dạng loại trừ phạm vi) |
| NFR-6 | Thời gian phản hồi API cụ thể (response time SLA, ví dụ p95 bao nhiêu ms) | — | **Chưa xác định — cần bổ sung** |
| NFR-7 | Khả năng mở rộng / số lượng người dùng đồng thời hệ thống backend cần chịu tải | — | **Chưa xác định — cần bổ sung** |
| NFR-8 | Tuân thủ dữ liệu / quy định bảo mật dữ liệu cá nhân (ví dụ GDPR hoặc quy định tương đương) | — | **Chưa xác định — cần bổ sung** |
| NFR-9 | Phiên bản Android tối thiểu hỗ trợ (minSdkVersion), độ phân giải/kích thước màn hình tối thiểu | — | **Chưa xác định — cần bổ sung** |
| NFR-10 | Tính khả dụng (uptime/availability) của backend | — | **Chưa xác định — cần bổ sung** |

---

## 5. Data Model

Giữ nguyên từ đặc tả gốc, không thay đổi.

### 5.1 Task

| Field | Type | Constraints |
|---|---|---|
| `id` | UUID/int | Primary key |
| `ownerId` | reference | Indexed; liên kết tới user |
| `title` | string | Required, non-empty, trimmed |
| `description` | string | Optional |
| `status` | enum | `open` \| `done` (mặc định `open`) |
| `createdAt` / `updatedAt` | timestamp | Quản lý tự động |

### 5.2 User (bổ sung do scope Auth)

| Field | Type | Constraints |
|---|---|---|
| `id` | UUID/int | Primary key |
| `email` | string | Unique |
| `passwordHash` | string | Không lưu plaintext (xem NFR-4) |
| `createdAt` | timestamp | Quản lý tự động |

---

## 6. API Contract

### 6.1 Task (giữ nguyên nguyên văn từ đặc tả gốc)

| Method | Endpoint | Purpose | Success | Errors |
|---|---|---|---|---|
| POST | `/tasks` | Tạo task | `201` | `400`, `401` |
| GET | `/tasks` | List task của chính mình (tùy chọn `?status=`) | `200` | `401` |
| PUT | `/tasks/:id` | Update/toggle task | `200` | `400`, `403/404` |
| DELETE | `/tasks/:id` | Xóa task | `204` | `403/404` |

### 6.2 Auth (bổ sung mới — mở rộng ngoài worksheet gốc)

> Phần này được bổ sung do phạm vi full-stack + login/register/forgot
> password đã chốt; ghi rõ đây là phần mở rộng ngoài worksheet gốc
> `Task2_Task_ToDo_CRUD.pdf`.

| Method | Endpoint | Purpose | Success | Errors |
|---|---|---|---|---|
| POST | `/auth/register` | Đăng ký tài khoản (email, password) | `201` | `400` (validate), `409` (email đã tồn tại) |
| POST | `/auth/login` | Đăng nhập, trả về JWT token | `200` | `400`, `401` (sai email/mật khẩu) |
| POST | `/auth/forgot-password` | Yêu cầu đặt lại mật khẩu (gửi email chứa mã/link) | `200` | `400`, `404` (email không tồn tại — **khuyến nghị bảo mật**: cân nhắc trả `200` chung để tránh lộ thông tin tài khoản tồn tại, xem FR-05/AC) |
| POST | `/auth/reset-password` | Đặt mật khẩu mới bằng mã xác nhận | `200` | `400` (mã sai/hết hạn, mật khẩu không hợp lệ) |

Mọi API `/tasks/*` đều yêu cầu header `Authorization: Bearer <token>`; token
không hợp lệ/hết hạn → `401` (xem FR-04).

### 6.3 Yêu cầu backend liên quan (giữ nguyên nguyên văn từ đặc tả gốc)

- Enforce ownership trên mọi read/write — user không được truy cập task
  người khác.
- Validate field bắt buộc; reject title rỗng/chỉ-khoảng-trắng.
- Trả `404` cho ID không tồn tại, `403/404` cho truy cập chéo owner.
- Hỗ trợ filter theo status trên list endpoint.
- (Bổ sung do scope Auth) Mật khẩu phải được hash; validate email format và
  độ mạnh mật khẩu tối thiểu 8 ký tự gồm ít nhất 1 chữ và 1 số; có ô "Xác
  nhận mật khẩu" khi đăng ký và khi đặt lại mật khẩu.

---

## 7. Edge Cases & Test Scenarios

### 7.1 Unit Test Scenarios gốc (Given/When/Then — giữ nguyên từ worksheet gốc)

| # | Given | When | Then |
|---|---|---|---|
| 1 | Title rỗng/khoảng-trắng | Create được gọi | Validation fail, trả `400` |
| 2 | Task của user A | User B update/delete | Bị từ chối, `403/404` |
| 3 | Task đang `open` | Toggle được gọi | Chuyển thành `done` (và ngược lại) |
| 4 | Task ID không tồn tại | Delete được gọi | Trả `404` |
| 5 | Nhiều task mixed status | List với `?status=open` | Chỉ trả task `open` |

### 7.2 Edge Cases bổ sung đặc thù mobile (từ làm rõ yêu cầu)

| # | Mô tả | Liên quan FR |
|---|---|---|
| 6 | Chạm nhanh liên tục (double-tap) vào checkbox toggle → chỉ 1 request được gửi | FR-13 |
| 7 | Xoay màn hình khi đang nhập form Tạo/Sửa → dữ liệu không mất | FR-11, FR-12 |
| 8 | Mất mạng giữa chừng khi đang submit thao tác ghi → hiển thị lỗi, không mất dữ liệu đã nhập (Tạo/Sửa) hoặc không xóa task khỏi UI trước khi server xác nhận (Xóa) | FR-11, FR-12, FR-14 |
| 9 | Token hết hạn (`401`) trong khi đang ở màn hình bất kỳ → tự động điều hướng về Đăng nhập | FR-04 |
| 10 | Xóa task đã bị xóa ở thiết bị/phiên khác (`404` khi xóa) → thông báo phù hợp, không phải lỗi chung chung | FR-14, FR-15 |

---

## 8. Phạm vi

### 8.1 Trong phạm vi

- Đăng ký, Đăng nhập, Đăng xuất, Quên mật khẩu (reset qua email/OTP) bằng
  email/mật khẩu; JWT lưu trên thiết bị; giữ phiên đăng nhập; tự động điều
  hướng về Đăng nhập khi token hết hạn.
- CRUD đầy đủ cho task: tạo, xem danh sách (của chính mình), sửa
  title/description, toggle open/done, xóa (swipe-to-delete).
- Lọc danh sách theo trạng thái All/Open/Done (FilterChip).
- Pull-to-refresh, empty-state, loading-state, error-state cho danh sách và
  các màn hình tạo/sửa.
- Ownership enforcement phía UI (`403`/`404`) và xử lý dữ liệu lỗi thời.
- Chống double-tap/double-submit cho toggle và các thao tác ghi khác.
- Giữ dữ liệu form khi xoay màn hình.
- Backend REST API cho Task (theo contract gốc) và Auth (bổ sung, mục 6.2).

### 8.2 Ngoài phạm vi

- Cache/lưu trữ task offline, đồng bộ nền (đã chốt online-only).
- Đăng nhập mạng xã hội/OAuth.
- Tìm kiếm, sắp xếp, due date/reminder, nhãn/category, chia sẻ/cộng tác
  task.
- Deep-link tới task cụ thể từ ngoài app.
- Quản lý hồ sơ người dùng (đổi email, avatar) sau đăng ký.
- Đa ngôn ngữ, dark mode, accessibility đặc biệt (TalkBack...) ở v1.
- Inline-edit kiểu web — đã thay bằng màn hình Sửa riêng phù hợp mobile.

---

## 9. Giao diện ngoài

- **API REST giữa Android app và Backend**: xem chi tiết đầy đủ ở mục 6
  (API Contract) — đây là giao diện ngoài chính của hệ thống, giao tiếp qua
  HTTP/JSON, xác thực bằng JWT trong header `Authorization: Bearer <token>`.
- **Dịch vụ gửi email** (phục vụ FR-05 — Quên mật khẩu): Backend cần tích
  hợp với một dịch vụ gửi email để gửi mã/link đặt lại mật khẩu. Input
  không nêu cụ thể nhà cung cấp dịch vụ email hay cơ chế kỹ thuật (SMTP,
  API bên thứ ba...) — nội dung này thuộc phạm vi thiết kế kỹ thuật, không
  đi sâu ở SRS.
- Không có tích hợp hệ thống ngoài nào khác được input đề cập (không có
  đăng nhập mạng xã hội/OAuth, không có push notification, không có tích
  hợp thanh toán...).

---

## 10. Phụ lục

### 10.1 Quyết định đã chốt (xác nhận trực tiếp bởi stakeholder)

Các điểm dưới đây là **quyết định đã được stakeholder xác nhận trực tiếp**
trong quá trình làm rõ yêu cầu (qua `requirements-analyst`), không phải suy
đoán của người viết SRS:

- Phạm vi kiến trúc là **full-stack** (backend + Android app), mở rộng so
  với worksheet gốc vốn chỉ nói chung chung "Front-End Requirements".
- Có đầy đủ Đăng ký/Đăng nhập/Đăng xuất dùng JWT lưu trên thiết bị — worksheet
  gốc chỉ giả định "logged-in user" mà không mô tả luồng xác thực.
- Bổ sung thêm luồng **Quên mật khẩu** (FR-05, API `/auth/forgot-password`
  và `/auth/reset-password`) — hoàn toàn không có trong worksheet gốc.
- Mô hình hoạt động là **online-only**, không cache offline — là một quyết
  định rõ ràng cắt giảm phạm vi so với các ứng dụng to-do thông thường
  (thường có offline-first).
- Màn hình Tạo/Sửa task dùng **màn hình riêng (full-screen)**, không dùng
  "inline-edit" như gợi ý trong mục Front-End Requirements của worksheet
  gốc — điều chỉnh này đã được xác nhận là phù hợp hơn với UX mobile.
- Cơ chế xóa task dùng **swipe-to-delete** kèm dialog xác nhận — cụ thể hóa
  hơn so với "delete controls" chung chung trong worksheet gốc.
- Về nguyên tắc optimistic UI: worksheet gốc yêu cầu chung "Optimistic UI
  update (or refresh) after each operation, with rollback on error" cho
  mọi thao tác. Bản làm rõ đã cụ thể hóa khác nhau theo từng thao tác:
  - **Toggle** (FR-13): áp dụng optimistic update — UI đổi trạng thái ngay,
    rollback nếu server lỗi.
  - **Create/Edit/Delete** (FR-11, FR-12, FR-14): KHÔNG áp dụng optimistic
    update — chờ xác nhận thành công từ server rồi mới cập nhật UI (đặc
    biệt rõ với Delete: task vẫn còn trong danh sách cho tới khi có xác
    nhận xóa thành công). Đây là lựa chọn nhất quán với nguyên tắc
    online-only đã chốt, đã được xác nhận bởi stakeholder — không phải mâu
    thuẫn chưa giải quyết.
- Forgot-password trả về thông báo chung chung "Nếu email tồn tại trong hệ
  thống..." dù email có tồn tại hay không, theo khuyến nghị bảo mật tránh
  lộ thông tin tài khoản tồn tại — đã được ghi nhận là khuyến nghị, cần xác
  nhận lại với bên thiết kế bảo mật khi triển khai thực tế mã trả về
  (`200` chung hay `404` như bảng mục 6.2 liệt kê — xem câu hỏi mở 10.2).

### 10.2 Câu hỏi còn mở / cần làm rõ thêm

- Bảng API Contract — Auth (mục 6.2) liệt kê `404` là một trong các error
  code khả dĩ của `/auth/forgot-password`, nhưng ngay trong cùng mục lại
  khuyến nghị bảo mật là "trả `200` chung để tránh lộ thông tin tài khoản
  tồn tại". Đây là điểm cần làm rõ lại với bên thiết kế API/bảo mật: liệu
  `404` có thực sự được dùng khi email không tồn tại (rủi ro lộ thông tin),
  hay toàn bộ trường hợp đều trả `200` kèm thông điệp trung lập như AC của
  FR-05 mô tả. SRS này ghi nhận cả hai cách hiểu, không tự chọn phương án.
- Các mục NFR-6 đến NFR-10 (mục 4) chưa có số liệu/ràng buộc cụ thể từ
  input — cần bổ sung làm rõ (gợi ý dùng `requirements-analyst`) trước khi
  đưa vào thiết kế kỹ thuật/estimation.
- Cơ chế lưu trữ JWT token cụ thể trên thiết bị Android (ví dụ
  EncryptedSharedPreferences, Android Keystore...) chưa được chốt trong
  input — thuộc phạm vi thiết kế kỹ thuật, không phải SRS, nhưng cần được
  quyết định trước khi triển khai NFR-3.
- Nhà cung cấp/dịch vụ gửi email cho luồng Quên mật khẩu (FR-05) chưa được
  chỉ định — thuộc phạm vi thiết kế kỹ thuật.

### 10.3 Gợi ý bước tiếp theo

- Dùng `requirements-analyst` để làm rõ thêm các mục NFR còn thiếu (NFR-6
  đến NFR-10) và câu hỏi mở về mã lỗi `forgot-password` (mục 10.2) trước
  khi chốt thiết kế kỹ thuật.
- Sau khi SRS này được review và chốt, dùng `project-estimator` để ước
  lượng effort/story point cho từng FR (SRS này không gán effort/ưu tiên).
