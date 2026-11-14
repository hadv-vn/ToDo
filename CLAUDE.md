# ToDo — Quy tắc & phong cách code

> Soạn bởi `/my-style:init` — 9 nguyên tắc nền + áp dụng cụ thể theo
> stack: Android (Kotlin + Jetpack Compose + Hilt, Clean Architecture,
> JUnit4/MockK/Turbine/Compose UI test). Backend/web nằm trong submodule
> `auth-core/` và theo quy tắc riêng tại `auth-core/CLAUDE.md`.

## Git — an toàn

- Không tự tạo branch mới, không tự `git commit`, không tự `git push` nếu
  người dùng chưa yêu cầu hoặc chưa đồng ý rõ ràng. Luôn hỏi xác nhận trước
  khi thực hiện các hành động này, ở MỌI project có plugin `my-style`.

## 1. "Taste" và cấu trúc dữ liệu — Linus Torvalds (Linux, Git)

Developer kém lo về code, developer giỏi lo về cấu trúc dữ liệu và quan hệ
giữa chúng. "Taste" là viết code sao cho trường hợp đặc biệt (edge case)
biến mất, thay vì thêm nhiều `if` để xử lý từng trường hợp riêng. Thẳng
thắn trong review, giữ chuẩn mực cao, không ngại nói thẳng khi thiết kế tệ.

**Áp dụng vào project này:** Mọi kết quả từ data layer đi qua sealed class
`ApiResult` (`Success`/`Error`/`NetworkError`) — xử lý lỗi bằng `when`
exhaustive trên kiểu đó, không rải `try/catch` hay check null ở ViewModel.
Phân biệt lỗi nghiệp vụ bằng `errorCode` ổn định của backend (vd
`TASK_FORBIDDEN`, `EMAIL_NOT_VERIFIED`), không so khớp chuỗi `message`.

## 2. Tập trung sâu và thực dụng — John Carmack (Doom, Quake)

Làm việc theo những khoảng tập trung dài, đào sâu đến tận gốc vấn đề thay vì
vá lỗi hời hợt. Chạy từng bước code trong debugger để hiểu chính xác máy
đang làm gì, không đoán. Ưu tiên code đơn giản, dễ đoán, hạn chế trạng thái
thay đổi lung tung (mutable state) — đặc biệt quan trọng ở nơi hiệu năng và
sự ổn định là tối quan trọng.

**Áp dụng vào project này:** State màn hình chỉ có một nguồn: `private
MutableStateFlow` trong ViewModel, expose ra ngoài dạng `StateFlow` bất biến
(`*UiState` là data class). Bug liên quan tới session/cookie/CSRF thì tái
hiện bằng request thật tới backend `auth-core` (hoặc test e2e ở đó) trước khi
sửa — không đoán từ triệu chứng trên UI.

## 3. Đơn giản tối đa — Ken Thompson & Rob Pike (Unix, Go)

Triết lý Unix: mỗi chương trình làm một việc và làm thật tốt, rồi ghép các
chương trình nhỏ lại với nhau. "Rõ ràng hơn khôn lỏi" (clear is better than
clever) — Go được thiết kế ít tính năng một cách có chủ đích, không thêm
cơ chế chỉ vì nó khả thi.

**Áp dụng vào project này:** Mỗi UseCase làm đúng một việc
(`CreateTaskUseCase`, `ToggleTaskStatusUseCase`...); mỗi interceptor OkHttp
một trách nhiệm (`CsrfHeaderInterceptor`, `SessionExpiredInterceptor`).
`domain/` là Kotlin thuần, không import Android/Retrofit. Không thêm thư
viện mới khi Compose/Coroutines/Hilt hiện có đã giải quyết được.

## 4. Test và bước nhỏ — Kent Beck (TDD, Extreme Programming)

Viết test trước, code sau, rồi refactor. "Make it work, make it right, make
it fast" — đúng theo thứ tự đó, không nhảy cóc sang tối ưu khi chưa đúng và
chưa rõ ràng. Thay đổi code theo từng bước nhỏ, an toàn, luôn giữ code ở
trạng thái chạy được giữa các bước.

**Áp dụng vào project này:** Unit test (JVM) ở `app/src/test/` với JUnit4 +
MockK + Turbine + `kotlinx-coroutines-test`, chạy `./gradlew test`; UI test
Compose ở `app/src/androidTest/`, chạy `./gradlew connectedAndroidTest` (cần
emulator). Mỗi ViewModel/UseCase/Repository mới đi kèm test cùng tên
`*Test.kt`. Thay đổi contract API thì chạy cả test e2e của backend
(`auth-core/backend/test/todo-task-api.e2e-spec.ts`).

## 5. Refactor liên tục — Martin Fowler

Coi code như vườn cây cần chăm sóc thường xuyên, nhận diện "code smell" để
dọn dẹp ngay khi thấy, không để tích tụ. "Ai cũng viết được code máy hiểu.
Developer giỏi viết code con người hiểu."

**Áp dụng vào project này:** Theo dõi các file đang phình to
(`TaskListViewModel.kt`, `TaskListScreen.kt` ~380 dòng) — khi thêm tính năng
vào đó, tách composable con / logic phụ ra trước. Mỗi refactor là commit
riêng, không trộn với thay đổi hành vi.

## 6. Code là để đọc — Donald Knuth & Guido van Rossum

"Literate programming": viết chương trình như một bài văn giải thích cho
người đọc. "Tối ưu hóa sớm là gốc rễ của mọi tội lỗi" (premature
optimization is the root of all evil) — đừng tối ưu trước khi đo. Python
đặt "Readability counts" làm giá trị cốt lõi.

**Áp dụng vào project này:** Tên class/file theo Kotlin style
(`PascalCase.kt` khớp tên class, package lowercase). KDoc dẫn chiếu tới tài
liệu thiết kế khi quyết định đến từ đó (vd "theo LLD mục 3.3",
`docs/SRS_ToDo_Android.md` FR-xx) — giữ thói quen hiện có này.

## 7. Ước lượng bằng con số — Jeff Dean (Google)

Trước khi thiết kế, ước lượng nhanh trên giấy (back-of-the-envelope) về độ
trễ, băng thông, bộ nhớ. Thuộc các con số cơ bản (đọc RAM, đọc SSD, round-trip
mạng mất bao lâu...) để biết ngay thiết kế nào khả thi trước khi code.

**Áp dụng vào project này:** App thuần CRUD, mục này giữ ở mức nguyên tắc
chung. Mốc duy nhất đã có trong SRS: danh sách ~50 task phải cuộn mượt
(NFR-1) — dùng `LazyColumn` với `key` ổn định theo `task.id`.

## 8. Code như nghệ thuật — Salvatore Sanfilippo "antirez" (Redis)

Code có cá tính và có thẩm mỹ, không chỉ là công cụ vô hồn. Viết comment để
giải thích TẠI SAO, không phải chỉ làm gì (cái "làm gì" đã nằm trong code).
Dám bỏ bớt tính năng để giữ thiết kế gọn, thay vì ôm đồm mọi yêu cầu.

**Áp dụng vào project này:** Comment giải thích lý do của các quyết định
không hiển nhiên, ví dụ: toggle dùng optimistic update còn create/edit/delete
thì không (SRS 10.1); cookie session lưu bằng AndroidKeyStore. Tính năng
ngoài phạm vi v1 (offline, dark mode, đa ngôn ngữ — SRS 8.2/NFR-5) không tự
thêm khi chưa được yêu cầu.

## 9. Quy ước thay cho cấu hình — David Heinemeier Hansson (Ruby on Rails)

Convention over configuration: theo quy ước chung thì không cần cấu hình
từng thứ một. Ưu tiên niềm vui và năng suất của developer, chọn giải pháp
đơn giản (ví dụ "majestic monolith") thay vì kiến trúc phức tạp chỉ vì đang
thịnh hành.

**Áp dụng vào project này:** Theo cấu trúc package có sẵn
`domain/ data/ presentation/ di/ core/` và bộ đôi `*Screen.kt` /
`*ViewModel.kt` / `*UiState.kt` cho mỗi màn hình. Không dựng backend riêng —
mọi API mới thêm vào `auth-core/backend/src/features/todo-task-module/`
theo `auth-core/CLAUDE.md`. Commit theo Conventional Commits
(`feat:`/`fix:`/`test:`/`docs:`...), có body nêu cái gì, tại sao, ảnh hưởng;
không dùng message kiểu "Update".
