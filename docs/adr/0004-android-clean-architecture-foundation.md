# 0004. Kiến trúc nền Android — Clean Architecture 3 layer + MVVM + Hilt + Compose, online-only

## Status

Accepted (triển khai từ trước khi có git, nhập vào repo ở commit
`659a35b`, 2026-11-01; đặc tả gốc tại `docs/HLD_ToDo_Android.md` mục
"Phương án kiến trúc đã chốt" + mục 2, ngày tạo 2026-10-14).

> Ghi lại sau khi code đã chạy (retroactive) — ADR thứ 4 của repo ToDo,
> tiếp theo [0001](0001-task-manual-reorder-sort-key.md),
> [0002](0002-android-session-cookie-storage.md),
> [0003](0003-auth-core-shared-backend-submodule.md) — để tiếp tục lấp
> finding `c6 Decision Coverage` (còn 1/5 thay đổi lớn chưa có ADR sau
> ADR này). Đây là quyết định **nền tảng nhất về code Android** — mọi
> ADR khác trong repo (0001, 0002) đều nằm *bên trong* khung kiến trúc
> mà ADR này mô tả (domain/data/presentation/di).

## Context

Commit `659a35b` nhập toàn bộ mã nguồn Android **đã được phát triển từ
trước** (chưa có git) vào repo mới tạo — bản thân commit đó không phải
lúc ra quyết định kiến trúc, mà là lúc **chốt lại bằng văn bản** một
quyết định đã có sẵn trong `docs/HLD_ToDo_Android.md`. ADR này phục
dựng lại quyết định đó ở dạng truy vết được, vì HLD là tài liệu thiết
kế dài (558 dòng), không phải nơi người đọc nhanh tìm thấy "quyết định
kiến trúc nền + lý do" ở một chỗ.

HLD ghi rõ nguồn gốc quyết định (mục "Phương án kiến trúc đã chốt"):
*"Người dùng tự chốt trực tiếp, chỉ định thư viện cụ thể cho từng
phần. Các chỉ định thư viện này được giữ nguyên, không tự đổi."* — khác
ADR 0001/0002/0003 (có phương án A/B/C đối lập rõ do AI đề xuất), phần
lớn quyết định ở ADR này là **lựa chọn có sẵn của người dùng**, ADR chỉ
ghi lại cùng những alternative mà HLD **có nêu rõ** cho vài điểm cụ
thể (JSON parser, test framework, cơ chế bắt lỗi 401).

**Phạm vi quyết định** (không phải 1 lựa chọn đơn, mà 1 bộ quyết định
đi cùng nhau):

1. **Phong cách kiến trúc tổng thể**: Clean Architecture 3 layer
   (Presentation / Domain / Data), tách bằng package
   `presentation/ domain/ data/ di/` — so với lựa chọn thay thế hay
   gặp là MVVM không tách Domain (ViewModel gọi trực tiếp Repository,
   không qua UseCase) hoặc MVI/Redux-style. HLD không ghi rõ lý do loại
   MVVM-phẳng, nhưng hệ quả quan trọng nhất **có ghi rõ**: Domain Layer
   là **Kotlin thuần, không phụ thuộc Android framework** — mục đích để
   unit test nhanh không cần Android instrumented (mục 2, "Android —
   Domain Layer").
2. **UI**: Jetpack Compose, **không dùng XML/View system**.
3. **DI**: Hilt (`@HiltAndroidApp`, `@HiltViewModel`, `@Module`).
4. **Chiến lược dữ liệu**: **online-only** — không Room, không cache
   Task cục bộ; chỉ lưu JWT (sau đó đổi thành session cookie, xem ADR
   0002) cục bộ.
5. **Networking**: Retrofit2 + OkHttp3.
6. **JSON parsing — có ghi rõ 2 phương án**:
   - **kotlinx.serialization (chọn)** — tích hợp sẵn converter chính
     thức cho Retrofit, không cần reflection (code-gen tại compile-time
     qua Kotlin compiler plugin), đồng bộ tốt với `data class`/
     `sealed class` Kotlin thuần ở Domain.
   - **Moshi (phương án thay thế hợp lệ, không chọn)** — sinh thái lâu
     đời hơn, nhiều adapter custom sẵn có; nhược điểm cần thêm
     annotation `@JsonClass` trên model.
7. **Lưu dữ liệu nhạy cảm cục bộ**: Jetpack `DataStore (Preferences)` +
   `androidx.security:security-crypto` (Android Keystore) — không lưu
   plaintext (đáp ứng NFR-3 SRS). Cách triển khai cụ thể cho thế hệ
   session cookie được tách riêng ở [ADR 0002](0002-android-session-cookie-storage.md).
8. **Phát hiện lỗi xác thực toàn cục (401) — có ghi rõ 2 phương án**:
   - **OkHttp `Interceptor` (chọn)** — đơn giản, đủ dùng vì SRS chưa có
     refresh token ở v1.
   - **OkHttp `Authenticator` (không chọn ở v1)** — cần nếu sau này có
     logic refresh-token, HLD ghi rõ để dành làm lựa chọn nâng cấp.
9. **Testing — có ghi rõ 2 phương án**:
   - **JUnit4 + MockK + Turbine + Compose UI Testing (chọn)** — hệ sinh
     thái AndroidX Test và Compose UI Testing hỗ trợ JUnit4
     Rule-based testing là first-class.
   - **JUnit5 (`android-junit5` plugin, không chọn)** — khả dụng nhưng
     cần cấu hình thêm cho instrumented test, không cần thiết cho scope
     dự án này.

## Decision

Chốt nguyên khối kiến trúc Android theo 9 điểm ở Context, cụ thể hóa
thành cấu trúc package `android/app/src/main/java/com/example/todo/`:

```
domain/        Kotlin thuần: model (Task, Session…), usecase/<nhóm>,
               repository/ (interface)
data/          remote/ (Retrofit service + DTO), local/ (DataStore +
               Keystore), repository/ (impl các interface ở domain),
               mapper/
presentation/  <feature>/ (Screen.kt + ViewModel.kt + UiState.kt),
               navigation/ (NavGraph, SessionObserverViewModel),
               theme/
di/            Hilt @Module theo từng nhóm (Network, DataStore,
               Repository…)
```

Ràng buộc đi kèm (ghi trong HLD mục 1.2 và mục 2, áp dụng xuyên suốt
mọi tính năng thêm sau — bao gồm cả Category/DueDate/Priority/Reorder ở
`72cbd3b`):

- Domain **không bao giờ** import `android.*`, không biết Retrofit/
  DataStore là gì — chỉ biết interface Repository.
- Presentation **không** gọi trực tiếp Retrofit/API, không chứa
  business logic — mọi quyết định nghiệp vụ nằm ở UseCase.
- Data **không** chứa UI state, không quyết định điều hướng màn hình.
- `SessionEventBus`/sự kiện 401 toàn cục là Kotlin thuần đặt ở lớp dùng
  chung, tiêm qua Hilt dưới dạng Singleton — cầu nối giữa Data và
  Presentation mà không phá vỡ ranh giới layer.

## Consequences

**Tích cực:**

- Domain Kotlin thuần → unit test nhanh, không cần thiết bị/emulator
  cho phần lớn logic nghiệp vụ (ViewModel/UseCase test chạy JVM thuần
  qua MockK, chỉ phần đụng `AndroidKeyStore` thật mới cần
  `androidTest` — đúng như thực tế: `SessionCookieStoreTest.kt` là
  instrumented, còn lại gần hết (`*ViewModelTest`, `*UseCaseTest`) là
  JVM unit test).
- Ranh giới rõ giữa 3 layer giúp thêm tính năng mới (Category, Reorder,
  Priority, DueDate ở `72cbd3b`) lặp lại đúng 1 khuôn
  (`UseCase → Repository interface → RepositoryImpl`) mà không cần
  bàn lại kiến trúc mỗi lần — chi phí quyết định kiến trúc chỉ trả 1
  lần ở ADR này.
- online-only giữ app đơn giản đúng mức cần (CRUD ~50 task/user,
  NFR-1) — không phải xây đồng bộ offline/conflict-resolution mà SRS
  không yêu cầu.

**Đánh đổi / rủi ro chấp nhận:**

- **online-only** nghĩa là mất mạng = không dùng được app, không có
  fallback đọc dữ liệu cũ — chấp nhận được vì ngoài phạm vi SRS (ghi rõ
  là scope quyết định, không phải bị bỏ sót).
- Không ghi lại trong HLD lý do **loại** các kiến trúc khác (MVVM
  phẳng không Domain, MVI) — quyết định này đến từ người dùng trực
  tiếp ("tự chốt, chỉ định thư viện cụ thể"), nên ADR này không thể
  phục dựng đầy đủ *quá trình cân nhắc* như đã làm được cho 3 ADR
  trước (nơi AI đề xuất và so sánh phương án). Đây là giới hạn của một
  ADR retroactive — không phải giới hạn của quyết định kiến trúc.
- Chọn Interceptor (không phải Authenticator) cho lỗi 401 là quyết
  định **tạm đủ cho v1** — nếu SRS sau này thêm refresh token, cần
  quay lại đúng điểm quyết định này để nâng cấp, không phải thiết kế
  lại từ đầu.

**Ảnh hưởng tới các bước tiếp theo:**

- Mọi ADR/feature sau này về Android (0001, 0002, và các tính năng
  tương lai) đặt *bên trong* khung 4 package này — nếu một thay đổi
  cần vi phạm ranh giới layer (ví dụ Presentation gọi thẳng Retrofit),
  đó là tín hiệu cần 1 ADR mới để thay đổi chính quyết định này, không
  nên âm thầm phá ranh giới trong 1 commit `feat`/`fix` thông thường.
- Nếu sau này cần offline/cache (phá vỡ giả định "online-only"), đây
  là thay đổi đủ lớn để cần ADR riêng, không phải mở rộng ngầm trong
  Data Layer hiện tại.

## Liên quan

- Đặc tả gốc: `docs/HLD_ToDo_Android.md` (mục "Phương án kiến trúc đã
  chốt", mục 1 — sơ đồ thành phần, mục 2 — trách nhiệm + thư viện từng
  layer).
- ADR liên quan trong repo ToDo: [0001](0001-task-manual-reorder-sort-key.md),
  [0002](0002-android-session-cookie-storage.md) — cả hai là quyết định
  *bên trong* khung kiến trúc này (0001 ở Domain/Data layer cho 1
  tính năng cụ thể, 0002 thay thế cụ thể hóa điểm 7 ở Context khi
  backend đổi cơ chế xác thực).
- Commit triển khai: `659a35b` (nhập mã nguồn Android + docs vào git).
- Dashboard tham chiếu: `health-check/index.html` — chỉ số `c6 Decision
  Coverage`.
