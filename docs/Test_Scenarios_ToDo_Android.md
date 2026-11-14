# Kịch bản test chức năng — ToDo Android

| | |
|---|---|
| **Phạm vi** | App Android `com.example.todo` (bản debug) + backend `auth-core` (cổng 8081) |
| **Loại test** | Test chức năng thủ công trên emulator (black-box, end-to-end) |
| **Căn cứ** | [SRS_ToDo_Android.md](SRS_ToDo_Android.md) (FR-01 → FR-17) và code hiện tại trong `android/app/src/main/` |
| **Ngày tạo** | 2026-11-14 |

Kịch bản viết theo **hành vi thật của app hiện tại**. Ở chỗ app khác SRS, kết quả mong đợi theo code và được đánh dấu ⚠️. Danh sách đầy đủ các điểm lệch nằm ở [mục 4](#4-điểm-lệch-giữa-app-và-srs--cần-xác-nhận).

---

## 1. Chuẩn bị môi trường

### 1.1 Tiền điều kiện chung

1. Backend `auth-core` đang chạy ở `http://localhost:8081` (từ emulator: `http://10.0.2.2:8081`).
2. Emulator đang chạy, APK debug đã cài:
   ```powershell
   .\android\scripts\run-android.ps1
   ```
3. Đọc được email xác thực / email reset mật khẩu. Nếu chưa cấu hình SMTP, nội dung email nằm trong console log của backend.

### 1.2 Dữ liệu test

| Ký hiệu | Mô tả |
|---|---|
| `U1` | Tài khoản đã xác thực email, mật khẩu `Passw0rd1` |
| `U2` | Tài khoản thứ hai đã xác thực email (dùng để test ownership) |
| `U3` | Tài khoản đã đăng ký nhưng **chưa** xác thực email |
| `NEW` | Email chưa từng đăng ký, ví dụ `qa+<timestamp>@example.com` |

### 1.3 Cách giả lập tình huống đặc biệt

| Tình huống | Cách làm |
|---|---|
| Mất mạng | `adb shell svc wifi disable; adb shell svc data disable`. Bật lại bằng `enable`. Hoặc bật chế độ máy bay trên emulator |
| Server không phản hồi | Tạm dừng backend (`auth-core/scripts/stop-all.sh` hoặc dừng process) |
| Phiên hết hạn (401) | Thu hồi session của user trên web admin `auth-core`, hoặc xóa bản ghi session trong DB, rồi thao tác tiếp trên app |
| Task bị xóa ở nơi khác (404) | Đăng nhập cùng user trên web/curl/thiết bị khác, xóa task đó, rồi thao tác trên app với dữ liệu cũ (chưa refresh) |
| Xoay màn hình | Nút xoay trên thanh công cụ emulator, hoặc `adb shell settings put system user_rotation 1` (`0` để xoay lại) |
| Mạng chậm (để thấy loading) | Extended controls của emulator → Cellular → Network speed: GSM/EDGE |

### 1.4 Quy ước

- **Ưu tiên:** P1 = luồng chính / chặn release · P2 = validate & xử lý lỗi · P3 = edge case.
- **Kết quả:** ghi `Pass` / `Fail` / `Blocked` vào cột *KQ* khi thực thi. `✅ Pass (tự động)` nghĩa là
  case đã có test code (unit JVM hoặc instrumented Compose UI) chạy thật trên emulator và pass —
  xem [mục 5](#5-phạm-vi-test-tự-động-đã-triển-khai-2026-11-14). Các case còn lại vẫn cần thực thi
  thủ công như mô tả ở trên.

---

## 2. Tổng hợp

| Nhóm | Mã test | Số case |
|---|---|---|
| Đăng ký | TC-REG-01 → 10 | 10 |
| Đăng nhập | TC-LOGIN-01 → 09 | 9 |
| Đăng xuất | TC-LOGOUT-01 → 04 | 4 |
| Phiên hết hạn | TC-SESS-01 → 03 | 3 |
| Quên / đặt lại mật khẩu | TC-PWD-01 → 11 | 11 |
| Danh sách task | TC-LIST-01 → 08 | 8 |
| Lọc | TC-FILTER-01 → 05 | 5 |
| Pull-to-refresh | TC-REFRESH-01 → 04 | 4 |
| Tạo task | TC-CREATE-01 → 09 | 9 |
| Sửa task | TC-EDIT-01 → 08 | 8 |
| Toggle trạng thái | TC-TOGGLE-01 → 06 | 6 |
| Xóa task | TC-DEL-01 → 06 | 6 |
| Ownership / dữ liệu cũ | TC-OWN-01 → 03 | 3 |
| Hiệu năng & vòng đời app | TC-PERF-01 → 03 | 3 |
| **Tổng** | | **89** |

---

## 3. Kịch bản chi tiết

### 3.1 Đăng ký — FR-01

| ID | Ưu tiên | Kịch bản | Các bước | Kết quả mong đợi | KQ |
|---|---|---|---|---|---|
| TC-REG-01 | P1 | Đăng ký thành công | 1. Login → "Chưa có tài khoản? Đăng ký" <br>2. Nhập email `NEW`, mật khẩu `Passw0rd1`, xác nhận `Passw0rd1` <br>3. Nhấn **Đăng ký** | ⚠️ Hiện thông báo "Đăng ký thành công. Vui lòng kiểm tra email để xác thực tài khoản trước khi đăng nhập." Form bị khóa, **không** tự vào danh sách task. Backend gửi email xác thực | ✅ Pass (thật, 2026-11-14) |
| TC-REG-02 | P1 | Xác thực email rồi đăng nhập | 1. Làm TC-REG-01 <br>2. Mở link xác thực trong email/log <br>3. Quay lại Login, đăng nhập bằng tài khoản vừa tạo | Đăng nhập thành công, vào "Task của tôi" với empty-state | ✅ Pass (thật, 2026-11-14) — ban đầu **Fail** vì bug backend `markEmailVerified()` không chuyển `status` sang `ACTIVE`, đã sửa & verify lại (auth-core commit `44c3be8`) |
| TC-REG-03 | P2 | Bỏ trống các ô | Để trống cả 3 ô → nhấn **Đăng ký** | Lỗi inline dưới từng ô, không gọi API (không có loading) | |
| TC-REG-04 | P2 | Email sai định dạng | Email `abc@`, mật khẩu hợp lệ → **Đăng ký** | Lỗi inline "Email không đúng định dạng", không gọi API | |
| TC-REG-05 | P2 | Mật khẩu < 8 ký tự | Mật khẩu `Ab1` | Lỗi inline "Mật khẩu phải tối thiểu 8 ký tự, có ít nhất 1 chữ và 1 số" | |
| TC-REG-06 | P2 | Mật khẩu chỉ có chữ / chỉ có số | Lần lượt dùng `abcdefgh` và `12345678` | Cả 2 lần đều có lỗi inline như TC-REG-05 | |
| TC-REG-07 | P2 | Xác nhận mật khẩu không khớp | Mật khẩu `Passw0rd1`, xác nhận `Passw0rd2` | Lỗi inline "Xác nhận mật khẩu không khớp" | |
| TC-REG-08 | P2 | Email đã tồn tại (409) | Đăng ký bằng email của `U1` | "Email đã được sử dụng". Dữ liệu đã nhập còn nguyên | |
| TC-REG-09 | P3 | Chống nhấn trùng | Bật mạng chậm → nhấn **Đăng ký** 2–3 lần liên tục | Nút chuyển sang loading/disable, backend chỉ nhận 1 request | |
| TC-REG-10 | P2 | Mất mạng | Tắt mạng → nhập dữ liệu hợp lệ → **Đăng ký** | "Không có kết nối mạng, vui lòng thử lại", app không crash, giữ dữ liệu đã nhập | |

### 3.2 Đăng nhập — FR-02

| ID | Ưu tiên | Kịch bản | Các bước | Kết quả mong đợi | KQ |
|---|---|---|---|---|---|
| TC-LOGIN-01 | P1 | Đăng nhập thành công | Nhập `U1` + mật khẩu đúng → **Đăng nhập** | Vào màn "Task của tôi", danh sách task của `U1` được tải | ✅ Pass (thật, 2026-11-14) |
| TC-LOGIN-02 | P2 | Bỏ trống email/mật khẩu | Để trống từng ô → **Đăng nhập** | "Email không được để trống" / "Mật khẩu không được để trống", không gọi API | |
| TC-LOGIN-03 | P2 | Sai mật khẩu | `U1` + `Wrong1234` | "Email hoặc mật khẩu không đúng", vẫn ở màn Login | |
| TC-LOGIN-04 | P2 | Email không tồn tại | `nobody@example.com` + mật khẩu bất kỳ | Cùng thông báo như TC-LOGIN-03 (không lộ ô nào sai) | |
| TC-LOGIN-05 | P2 | Email chưa xác thực | Đăng nhập bằng `U3` | Thông báo email chưa xác thực, hiện nút **Gửi lại email xác thực** | |
| TC-LOGIN-06 | P3 | Gửi lại email xác thực | Từ TC-LOGIN-05 → nhấn **Gửi lại email xác thực** | "Nếu email tồn tại và chưa xác thực, một email xác thực mới đã được gửi.", nút ẩn đi, backend gửi email mới | |
| TC-LOGIN-07 | P3 | Chống nhấn trùng | Mạng chậm → nhấn **Đăng nhập** liên tục | Nút loading/disable, chỉ 1 request | |
| TC-LOGIN-08 | P2 | Mất mạng / server tắt | Tắt mạng (hoặc dừng backend) → **Đăng nhập** | "Không có kết nối mạng, vui lòng thử lại", không crash | |
| TC-LOGIN-09 | P1 | Giữ phiên khi mở lại app | 1. Đăng nhập `U1` <br>2. Vuốt tắt app khỏi Recents <br>3. Mở lại app | SRS: vào thẳng danh sách task. ⚠️ Code hiện tại luôn bắt đầu ở màn Login → **dự kiến Fail**, xem mục 4 | |

### 3.3 Đăng xuất — FR-03

| ID | Ưu tiên | Kịch bản | Các bước | Kết quả mong đợi | KQ |
|---|---|---|---|---|---|
| TC-LOGOUT-01 | P1 | Đăng xuất | Ở "Task của tôi" → nhấn **Đăng xuất** | ⚠️ Về màn Login ngay, **không có** dialog xác nhận (SRS yêu cầu có, xem mục 4) | ✅ Pass (tự động, thật) |
| TC-LOGOUT-02 | P1 | Back sau khi đăng xuất | Sau TC-LOGOUT-01 nhấn nút Back hệ thống | Thoát app, **không** quay lại danh sách task | ✅ Pass (thật, 2026-11-14) |
| TC-LOGOUT-03 | P2 | Phiên đã bị hủy | Sau khi đăng xuất, dùng cookie cũ gọi `GET /api/v1/tasks` (nếu lấy được), hoặc đăng nhập user khác | Backend trả 401. Đăng nhập lại chỉ thấy task của user mới | |
| TC-LOGOUT-04 | P3 | Đăng xuất khi mất mạng | Tắt mạng → **Đăng xuất** | Vẫn về màn Login (cookie cục bộ luôn bị xóa), không crash | |

### 3.4 Phiên hết hạn — FR-04

| ID | Ưu tiên | Kịch bản | Các bước | Kết quả mong đợi | KQ |
|---|---|---|---|---|---|
| TC-SESS-01 | P1 | 401 khi ở danh sách | 1. Đăng nhập `U1` <br>2. Thu hồi session trên server <br>3. Pull-to-refresh | Về màn Login, snackbar "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại". Back không quay lại danh sách | |
| TC-SESS-02 | P2 | 401 khi đang ở form Tạo/Sửa | 1. Mở **Tạo task**, nhập dở <br>2. Thu hồi session <br>3. Nhấn **Lưu** | Về Login, snackbar "Phiên đăng nhập đã hết hạn. Dữ liệu bạn đang nhập sẽ không được lưu, vui lòng đăng nhập lại." | |
| TC-SESS-03 | P2 | 401 khi toggle / xóa | Thu hồi session → toggle hoặc xóa một task | Về Login kèm snackbar như TC-SESS-01, không hiện lỗi chung | |

### 3.5 Quên / đặt lại mật khẩu — FR-05

| ID | Ưu tiên | Kịch bản | Các bước | Kết quả mong đợi | KQ |
|---|---|---|---|---|---|
| TC-PWD-01 | P1 | Mở màn Quên mật khẩu | Login → **Quên mật khẩu?** | Màn "Quên mật khẩu" với ô Email và nút **Gửi token xác nhận** | |
| TC-PWD-02 | P1 | Gửi yêu cầu với email hợp lệ | Nhập email `U1` → **Gửi token xác nhận** | Chuyển sang "Đặt lại mật khẩu" với dòng "Một email chứa token đặt lại mật khẩu đã được gửi tới <email>". Backend gửi email có token | |
| TC-PWD-03 | P2 | Email không tồn tại | Nhập `nobody@example.com` → gửi | Hành vi **giống hệt** TC-PWD-02 (không lộ việc email có tồn tại hay không) | |
| TC-PWD-04 | P2 | Email trống / sai định dạng | Để trống, rồi nhập `abc` → gửi | "Email không đúng định dạng", không gọi API | |
| TC-PWD-05 | P2 | Quay lại đăng nhập | Nhấn **Quay lại đăng nhập** | Về màn Login | |
| TC-PWD-06 | P1 | Đặt lại mật khẩu thành công | 1. Từ TC-PWD-02 nhập token từ email <br>2. Mật khẩu mới `NewPassw0rd1`, xác nhận khớp <br>3. **Xác nhận** <br>4. Đăng nhập bằng mật khẩu mới | Bước 3: về màn Login. Bước 4: đăng nhập được. Mật khẩu cũ không còn dùng được | |
| TC-PWD-07 | P2 | Token trống | Để trống token → **Xác nhận** | "Vui lòng nhập token xác nhận từ email", không gọi API | |
| TC-PWD-08 | P2 | Token sai / hết hạn | Nhập token `invalid-token` | "Token không đúng hoặc đã hết hạn" (hoặc thông báo cụ thể từ server) | |
| TC-PWD-09 | P2 | Mật khẩu mới không hợp lệ | Mật khẩu mới `abc` | Lỗi inline độ mạnh mật khẩu, không gọi API | |
| TC-PWD-10 | P2 | Xác nhận không khớp | Mật khẩu mới và xác nhận khác nhau | "Xác nhận mật khẩu không khớp", không gọi API | |
| TC-PWD-11 | P2 | Mất mạng | Tắt mạng ở màn Quên mật khẩu và ở màn Đặt lại → gửi | "Không có kết nối mạng, vui lòng thử lại", không crash | |

### 3.6 Danh sách task — FR-06, FR-09, FR-10

| ID | Ưu tiên | Kịch bản | Các bước | Kết quả mong đợi | KQ |
|---|---|---|---|---|---|
| TC-LIST-01 | P1 | Hiển thị task của mình | `U1` có vài task → đăng nhập | Hiển thị đủ task của `U1`, mỗi dòng có checkbox + tiêu đề | |
| TC-LIST-02 | P1 | Không thấy task người khác | `U2` tạo task "Task của U2" → đăng nhập `U1` | Danh sách của `U1` **không** có "Task của U2" | |
| TC-LIST-03 | P2 | Loading state | Bật mạng chậm → đăng nhập | Hiện spinner giữa màn hình khi đang tải lần đầu | |
| TC-LIST-04 | P2 | Hiển thị task done | Có task đã done | Tiêu đề gạch ngang, checkbox được tick | |
| TC-LIST-05 | P3 | Mô tả dài bị rút gọn | Tạo task có mô tả dài hơn 1 dòng | Mô tả chỉ hiện 1 dòng, có dấu "…" | |
| TC-LIST-06 | P1 | Empty-state khi chưa có task | Đăng nhập user chưa có task | "Bạn chưa có task nào. Nhấn + để tạo task đầu tiên." | ✅ Pass (thật, 2026-11-14) |
| TC-LIST-07 | P2 | Lỗi mạng ở lần tải đầu | Tắt mạng → đăng nhập bằng phiên còn hiệu lực (hoặc tắt mạng ngay sau login) | "Không có kết nối mạng" + nút **Thử lại**. Bật mạng → **Thử lại** → danh sách tải được | |
| TC-LIST-08 | P2 | Lỗi server (500) | Làm backend trả lỗi (ví dụ tắt DB) → tải danh sách | Thông báo lỗi chung + nút **Thử lại**, không crash | |

### 3.7 Lọc theo trạng thái — FR-07, FR-09

Dữ liệu: `U1` có ít nhất 2 task open và 2 task done.

| ID | Ưu tiên | Kịch bản | Các bước | Kết quả mong đợi | KQ |
|---|---|---|---|---|---|
| TC-FILTER-01 | P1 | Lọc Đang mở | Nhấn chip **Đang mở** | Chỉ hiện task open, chip được chọn | ✅ Pass (tự động, thật) |
| TC-FILTER-02 | P1 | Lọc Đã xong | Nhấn chip **Đã xong** | Chỉ hiện task done | ✅ Pass (tự động, thật) |
| TC-FILTER-03 | P1 | Về Tất cả | Nhấn **Tất cả** | Hiện lại toàn bộ task | ✅ Pass (tự động) |
| TC-FILTER-04 | P2 | Empty-state theo bộ lọc | Đánh dấu done hết task → chọn **Đang mở** | "Không có task nào đang mở." (khác thông điệp của TC-LIST-06). Tương tự với **Đã xong**: "Không có task nào đã xong." | ✅ Pass (thật, 2026-11-14, cả 2 vế) — ban đầu **Fail** khi phát hiện qua xóa task cuối cùng khớp filter (xem mục 5), đã sửa & verify lại |
| TC-FILTER-05 | P2 | Task biến mất khi hết khớp bộ lọc | Đang lọc **Đang mở** → tick 1 task | Task đó biến mất ngay khỏi danh sách | |

### 3.8 Pull-to-refresh — FR-08

| ID | Ưu tiên | Kịch bản | Các bước | Kết quả mong đợi | KQ |
|---|---|---|---|---|---|
| TC-REFRESH-01 | P1 | Refresh lấy dữ liệu mới | Tạo task mới cho `U1` từ web/curl → trên app kéo danh sách xuống | Hiện chỉ báo refresh, task mới xuất hiện | |
| TC-REFRESH-02 | P2 | Giữ bộ lọc khi refresh | Chọn **Đã xong** → kéo refresh | Vẫn ở bộ lọc Đã xong, chỉ hiện task done | |
| TC-REFRESH-03 | P2 | Refresh lỗi | Tắt mạng → kéo refresh | Chỉ báo biến mất, danh sách cũ giữ nguyên, có snackbar lỗi | |
| TC-REFRESH-04 | P3 | Không tự refresh khi quay lại app | Thêm task từ web → app vào background → mở lại app | Danh sách **không** tự cập nhật, chỉ có khi kéo refresh | |

### 3.9 Tạo task — FR-11

| ID | Ưu tiên | Kịch bản | Các bước | Kết quả mong đợi | KQ |
|---|---|---|---|---|---|
| TC-CREATE-01 | P1 | Tạo task đủ tiêu đề + mô tả | Nhấn **+** → nhập Tiêu đề "Mua sữa", Mô tả "2 hộp" → **Lưu** | Đóng form, quay lại danh sách, "Mua sữa" xuất hiện ở trạng thái open | ✅ Pass (thật, 2026-11-14) |
| TC-CREATE-02 | P1 | Tạo task không có mô tả | Chỉ nhập Tiêu đề → **Lưu** | Tạo thành công | |
| TC-CREATE-03 | P2 | Tiêu đề trống | Mở form, không nhập Tiêu đề | ⚠️ Nút **Lưu** bị disable, không gọi API được | ✅ Pass (thật, 2026-11-14) |
| TC-CREATE-04 | P2 | Tiêu đề chỉ có khoảng trắng | Nhập "   " | Nút **Lưu** vẫn disable | |
| TC-CREATE-05 | P3 | Tiêu đề > 255 ký tự (400 từ server) | Dán chuỗi 300 ký tự → **Lưu** | Hiện lỗi trên form (thông báo từ server hoặc "Dữ liệu không hợp lệ"), giữ dữ liệu, vẫn ở form | |
| TC-CREATE-06 | P3 | Chống tạo trùng | Mạng chậm → nhấn **Lưu** liên tục | Nút loading/disable, chỉ tạo **1** task | |
| TC-CREATE-07 | P2 | Mất mạng khi lưu | Tắt mạng → **Lưu** | "Không có kết nối mạng, vui lòng thử lại", vẫn ở form, giữ dữ liệu. Bật mạng → **Lưu** lại được | |
| TC-CREATE-08 | P2 | Xoay màn hình khi đang nhập | Nhập Tiêu đề + Mô tả → xoay ngang → xoay dọc | Dữ liệu còn nguyên | |
| TC-CREATE-09 | P3 | Đóng form không lưu | Nhập dở → **Đóng** (hoặc Back) | Về danh sách, không tạo task | |

### 3.10 Sửa task — FR-12

| ID | Ưu tiên | Kịch bản | Các bước | Kết quả mong đợi | KQ |
|---|---|---|---|---|---|
| TC-EDIT-01 | P1 | Mở form Sửa | Chạm vào dòng task (không chạm checkbox) | Mở "Sửa task", Tiêu đề/Mô tả điền sẵn đúng | ✅ Pass (thật, 2026-11-14) |
| TC-EDIT-02 | P1 | Sửa thành công | Đổi Tiêu đề, Mô tả → **Lưu** | Về danh sách, thông tin mới hiển thị | ✅ Pass (thật, 2026-11-14) |
| TC-EDIT-03 | P2 | Xóa hết tiêu đề | Xóa hết Tiêu đề (hoặc chỉ để khoảng trắng) | Nút **Lưu** bị disable | |
| TC-EDIT-04 | P2 | Task bị xóa trước khi mở form | Xóa task trên web → trên app (chưa refresh) chạm vào task đó | Hiện "Task này không còn tồn tại" | |
| TC-EDIT-05 | P2 | Task bị xóa trước khi lưu (404) | Mở form Sửa → xóa task trên web → **Lưu** | Quay lại danh sách đã làm mới, task đó không còn | |
| TC-EDIT-06 | P2 | Mất mạng khi lưu | Tắt mạng → **Lưu** | Lỗi mạng, vẫn ở form, giữ dữ liệu đã sửa | |
| TC-EDIT-07 | P2 | Xoay màn hình khi sửa | Sửa dở → xoay màn hình | Dữ liệu đã sửa không mất | |
| TC-EDIT-08 | P3 | Đóng không lưu | Sửa dở → **Đóng** | Về danh sách, task giữ dữ liệu cũ | |

### 3.11 Toggle trạng thái — FR-13

| ID | Ưu tiên | Kịch bản | Các bước | Kết quả mong đợi | KQ |
|---|---|---|---|---|---|
| TC-TOGGLE-01 | P1 | Open → Done | Tick checkbox của task open | Đổi ngay sang done (gạch ngang). Kéo refresh vẫn là done | ✅ Pass (thật, 2026-11-14) |
| TC-TOGGLE-02 | P1 | Done → Open | Bỏ tick task done | Đổi về open, được lưu trên server | |
| TC-TOGGLE-03 | P3 | Chạm liên tục | Mạng chậm → chạm checkbox nhanh 3–4 lần | Chỉ 1 request được gửi, trạng thái cuối khớp server | |
| TC-TOGGLE-04 | P2 | Rollback khi mất mạng | Tắt mạng → tick task open | UI đổi tạm rồi **quay lại open**, có snackbar "Không có kết nối mạng, vui lòng thử lại" | |
| TC-TOGGLE-05 | P2 | Task đã bị xóa (404) | Xóa task trên web → tick task đó trên app | Snackbar "Task không còn tồn tại", task biến mất khỏi danh sách | |
| TC-TOGGLE-06 | P3 | Chạm checkbox không mở form Sửa | Chạm đúng vào checkbox | Chỉ toggle, **không** mở màn Sửa | ✅ Pass (tự động) |

### 3.12 Xóa task — FR-14

| ID | Ưu tiên | Kịch bản | Các bước | Kết quả mong đợi | KQ |
|---|---|---|---|---|---|
| TC-DEL-01 | P1 | Hiện dialog khi vuốt | Vuốt ngang 1 dòng task | Nền đỏ có chữ "Xóa", dialog "Xóa task" với nội dung "Bạn có chắc muốn xóa task này? Hành động không thể hoàn tác." | ✅ Pass (tự động, thật) |
| TC-DEL-02 | P1 | Hủy xóa | Trong dialog nhấn **Hủy** | Dialog đóng, dòng trở về vị trí cũ, task vẫn còn | ✅ Pass (tự động, thật) |
| TC-DEL-03 | P1 | Xác nhận xóa | Nhấn **Xóa** | Task biến mất khỏi danh sách. Refresh lại không thấy | ✅ Pass (tự động, thật, 2026-11-14) — khi xóa task cuối cùng khớp filter từng làm màn hình trắng (bug đã sửa, xem mục 5) |
| TC-DEL-04 | P2 | Task đã bị xóa trước đó (404) | Xóa task trên web → vuốt xóa task đó trên app → **Xóa** | Snackbar "Task này đã được xóa trước đó", task biến mất | |
| TC-DEL-05 | P2 | Mất mạng khi xóa | Tắt mạng → vuốt → **Xóa** | Snackbar lỗi mạng, task **vẫn còn** trong danh sách | |
| TC-DEL-06 | P3 | Vuốt theo cả 2 hướng | Vuốt trái → phải và phải → trái | Cả 2 hướng đều mở dialog xác nhận | |

### 3.13 Ownership / dữ liệu cũ — FR-15

| ID | Ưu tiên | Kịch bản | Các bước | Kết quả mong đợi | KQ |
|---|---|---|---|---|---|
| TC-OWN-01 | P1 | API chặn truy cập chéo | Lấy `id` một task của `U2`. Bằng phiên của `U1` gọi `PUT` và `DELETE /api/v1/tasks/<id>` (curl/Postman) | Backend trả 403 hoặc 404, task của `U2` không bị thay đổi | |
| TC-OWN-02 | P2 | Lỗi 403/404 chỉ báo cục bộ | Tái hiện 404 bằng TC-TOGGLE-05 / TC-DEL-04 | Chỉ có snackbar, không có màn lỗi toàn trang, vẫn ở danh sách | |
| TC-OWN-03 | P3 | Thông báo 403 | Nếu tái hiện được 403 (ví dụ chỉnh response bằng proxy) | Snackbar "Bạn không có quyền thực hiện thao tác này", UI không thay đổi | |

### 3.14 Hiệu năng & vòng đời app — FR-16, FR-17

| ID | Ưu tiên | Kịch bản | Các bước | Kết quả mong đợi | KQ |
|---|---|---|---|---|---|
| TC-PERF-01 | P2 | Cuộn 50 task | Tạo khoảng 50 task (script gọi API) → cuộn nhanh lên/xuống | Cuộn mượt, không giật rõ rệt | |
| TC-PERF-02 | P2 | Background → foreground | Ở danh sách → nhấn Home → đợi 1 phút → mở lại app | Danh sách vẫn đúng, không trắng màn hình, không crash | |
| TC-PERF-03 | P2 | Phản hồi khi đang ghi | Mạng chậm → tạo / sửa / toggle / xóa | Mỗi thao tác đều có trạng thái đang xử lý (nút loading hoặc item bị khóa) | |

---

## 4. Điểm lệch giữa app và SRS — cần xác nhận

Đây là những chỗ code hiện tại làm khác SRS. Cần chốt xem nên **sửa app** hay **cập nhật SRS** trước khi đánh giá Pass/Fail cho các case liên quan.

| # | SRS yêu cầu | App hiện tại | Case liên quan |
|---|---|---|---|
| 1 | FR-01: đăng ký xong tự đăng nhập, vào danh sách task | Phải xác thực email trước, form bị khóa và hiện hướng dẫn (do backend `auth-core` bắt buộc xác thực email) | TC-REG-01, 02 |
| 2 | FR-02: mở lại app khi phiên còn hiệu lực thì vào thẳng danh sách task | `startDestination = LOGIN`, không kiểm tra phiên lúc khởi động | TC-LOGIN-09 |
| 3 | FR-03: có dialog "Bạn có chắc muốn đăng xuất?" | Nhấn **Đăng xuất** là thoát ngay | TC-LOGOUT-01 |
| 4 | FR-05: dùng mã OTP | Dùng **token** lấy từ email | TC-PWD-02, 06–08 |
| 5 | FR-05: thông báo "Nếu email tồn tại…" sau khi gửi; "Đặt lại mật khẩu thành công" sau khi reset | Gửi xong chuyển màn ngay. Reset xong về Login, không thấy thông báo thành công | TC-PWD-02, 06 |
| 6 | FR-11/FR-12: tiêu đề trống thì có lỗi inline "Tiêu đề không được để trống" | Nút **Lưu** bị disable nên không thấy lỗi inline | TC-CREATE-03, 04; TC-EDIT-03 |
| 7 | FR-11/12/14/16: có snackbar báo thành công sau khi tạo / sửa / xóa | Không thấy thông báo thành công trong code, chỉ có thông báo lỗi | TC-CREATE-01, TC-EDIT-02, TC-DEL-03 |
| 8 | SRS dùng JWT (`Authorization: Bearer`) | Dùng session cookie `sid` + CSRF (theo `auth-core`) | Không ảnh hưởng UI. Cần cập nhật SRS |

---

## 5. Phạm vi test tự động đã triển khai (2026-11-14)

Đã viết thêm test code và chạy thật trên emulator (`./gradlew :app:testDebugUnitTest` +
`./gradlew :app:connectedDebugAndroidTest`, cả hai **BUILD SUCCESSFUL**, không còn tình trạng
"chưa chạy thật được" như trước) cho các nhóm còn thiếu coverage:

- **Unit test (JVM, `TaskListViewModelTest.kt`)**: `onDeleteTaskConfirmed` (thành công/404/403/mất
  mạng/double-confirm), `onToggleTask` 403, `onFilterSelected` lọc đúng nội dung (không chỉ
  empty-state), `onPullToRefresh` thành công giữ nguyên filter.
- **Unit test mới (`NavGraphSessionExpiredMessageTest.kt`)**: tách logic chọn message khi 401
  (`sessionExpiredMessageFor` trong `NavGraph.kt`) thành hàm thuần để test — xác nhận đúng message
  phân biệt TC-SESS-01 (route thường) và TC-SESS-02 (route Tạo/Sửa task). **Chưa** test được phần
  điều hướng/xóa backstack thật (`navController.navigate(...)`) vì `NavGraph` dùng `hiltViewModel()`
  và project chưa có Hilt test module — phần đó (back không quay lại danh sách) vẫn cần làm thủ công.
- **Instrumented test (Compose UI, `TaskListScreenTest.kt`)**: thêm swipe-xóa xác nhận/hủy
  (TC-DEL-01/02/03), lọc theo chip + empty-state riêng theo filter (TC-FILTER-01→04), chạm checkbox
  không mở form Sửa (TC-TOGGLE-06), đăng xuất gọi callback ngay không có dialog xác nhận
  (TC-LOGOUT-01).

**Cố tình để lại thủ công** (không tự động hóa ở vòng này) vì lý do cụ thể:

| Nhóm | Lý do |
|---|---|
| TC-OWN-01/03 | Gọi API chéo user bằng curl/Postman — thuộc phạm vi e2e test của `auth-core` (đã có, xem `backend/test/todo-task-api.e2e-spec.ts`), không phải test app Android |
| TC-PERF-01/02 | Cuộn mượt/giữ trạng thái khi app vào background cần quan sát trực quan hoặc công cụ profiling (Perfetto), không có tiêu chí pass/fail rõ ràng để assert |
| TC-LOGIN-09, TC-LOGOUT-03, TC-SESS (phần điều hướng) | Cần kill process thật/thu hồi session phía server/NavController thật — vượt ngoài phạm vi Compose UI test dựng bằng fake repository hiện tại (TC-LOGOUT-02 đã verify thật, xem mục 6) |
| TC-REFRESH-03/04, TC-CREATE-06/07, TC-EDIT-06/07, TC-TOGGLE-03/04, TC-DEL-04/05 | Cần giả lập mất mạng/mạng chậm thật trên thiết bị (`adb shell svc wifi disable`) — phần logic rollback/giữ dữ liệu tương ứng **đã** được test ở tầng ViewModel (unit test, dùng `ApiResult.NetworkError`/`CompletableDeferred` giả lập), chỉ riêng phần "thao tác tay + quan sát UI thật" là còn thủ công |

---

## 6. Thực thi thật trên emulator + backend thật (2026-11-14)

Cài APK debug lên emulator thật (`SafeCall_API34`), chạy `auth-core` thật ở `localhost:8081`,
thao tác qua `adb` (tap/input/screenshot) thay vì đọc email thì tạo token xác thực trực tiếp trong
DB (tương đương "mở link xác thực trong email", xem mục 1.1) để kiểm tra golden-path đầy đủ:
đăng ký → xác thực → đăng nhập → tạo task → toggle → lọc → sửa → xóa (hủy rồi xác nhận) → đăng xuất
→ Back sau đăng xuất. Toàn bộ các case đã đổi KQ thành `✅ Pass (thật, 2026-11-14)` ở trên đều được
verify bằng cách này, không chỉ suy luận từ code.

**2 bug thật được phát hiện qua quá trình này (cả hai đã sửa và verify lại bằng cách tái hiện qua
API/app thật, không chỉ đọc code):**

1. **Backend `auth-core`** — `PrismaUserRepository.markEmailVerified()` chỉ ghi `emailVerifiedAt`,
   không chuyển `status` từ `PENDING_VERIFICATION` sang `ACTIVE`. Hệ quả: **mọi** user xác thực
   email xong vẫn bị `login()` chặn vĩnh viễn với `403 ACCOUNT_EMAIL_NOT_VERIFIED` — chặn đứng toàn
   bộ TC-REG-02 và mọi case cần đăng nhập. Sửa ở `auth-core` commit `44c3be8`
   (`fix(auth): activate user on email verification, not just mark timestamp`), bump submodule ở
   ToDo commit `571d860`.
2. **Android app** — `TaskListViewModel.removeTaskFromState()` và 2 nhánh 404 (của
   `onDeleteTaskConfirmed`/`onToggleTask`) chỉ `filterNot` khỏi `tasks`, không gọi lại
   `computeEmptyState()`. Hệ quả: xóa/loại nốt task cuối cùng khớp filter hiện tại → `tasks` rỗng
   nhưng `emptyStateType` vẫn giữ giá trị `null` cũ → UI không khớp nhánh list lẫn nhánh
   empty-state → **màn hình trắng trơn** thay vì thông điệp đúng (ảnh hưởng TC-FILTER-04,
   TC-DEL-03, TC-TOGGLE-05). Sửa bằng hàm `stateAfterRemovingTask()` mới trong
   `TaskListViewModel.kt`, có thêm 2 unit test tái hiện đúng kịch bản này trong
   `TaskListViewModelTest.kt`.
