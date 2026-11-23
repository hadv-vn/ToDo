# 0003. Dùng chung backend qua git submodule (`auth-core` / Multi-user-System) thay vì tự viết backend riêng

## Status

Accepted (đã triển khai ở commit `4eacf29`, 2026-11-01; mô tả thêm ở
README tại commit `22461c6`; code backend thực tế nằm trong submodule
`auth-core` → repo [hadv-vn/Multi-user-System](https://github.com/hadv-vn/Multi-user-System)).

> Ghi lại sau khi code đã chạy (retroactive) — ADR thứ 3 của repo ToDo,
> tiếp theo [0001](0001-task-manual-reorder-sort-key.md) và
> [0002](0002-android-session-cookie-storage.md), để tiếp tục lấp finding
> `c6 Decision Coverage` trong `health-check/index.html` (còn 2/5 thay đổi
> `feat:` lớn chưa có ADR sau ADR này). Đây là quyết định **ảnh hưởng
> rộng nhất** trong 5 — mọi quyết định ở ADR 0001/0002 là hệ quả trực
> tiếp của quyết định này.

## Context

ToDo ban đầu có 1 folder `backend/` tự viết (Express + Prisma) để phục
vụ CRUD Task + xác thực cơ bản — folder này **chưa từng được commit**
nên không còn lịch sử git để đối chiếu chi tiết, chỉ còn nhắc lại trong
message của chính commit `4eacf29`.

Cùng thời điểm, đã có sẵn 1 hệ thống khác — **Multi-user-System** (repo
riêng, NestJS + Prisma + PostgreSQL) — với đầy đủ Auth (đăng ký + xác
thực email + đăng nhập + khóa tài khoản sau đăng nhập sai), Session
(server-side session, đã có ADR riêng), RBAC (USER/ADMIN), Account,
Admin. Repo đó được thiết kế **có chủ đích để làm nền cho nhiều hệ
thống khác** — nguyên văn `auth-core/CLAUDE.md`: *"the repo is also
meant to serve as a base for future systems, so keep the core modules
generic and add new business features as new modules"*.

Câu hỏi kiến trúc cần quyết định: ToDo tiếp tục tự duy trì backend
auth riêng, hay dùng lại backend đã có sẵn ở Multi-user-System — và nếu
dùng lại, **tích hợp bằng cơ chế nào**?

**Các phương án:**

- **A — Tiếp tục tự viết/duy trì `backend/` (Express + Prisma) riêng
  cho ToDo.** Đây là hiện trạng trước commit này. Bị loại — lý do nêu
  trực tiếp trong commit message: *"chuyển sang dùng chung hệ thống
  backend/auth từ repo Multi-user-System thay vì tự duy trì backend
  riêng trong project này"*. Tự viết lại nghĩa là trùng lặp toàn bộ
  logic Auth/Session/RBAC đã có sẵn, kiểm chứng sẵn (test, ADR riêng)
  ở Multi-user-System, và phải tự bảo trì song song 2 nơi.
- **B — Dùng lại qua git submodule** trỏ tới Multi-user-System, pin ở
  1 commit cụ thể. **Chọn.**
- **C — Git subtree** (copy lịch sử/code Multi-user-System trực tiếp
  vào cây commit của ToDo) — *không thấy nhắc tới trong commit/README,
  suy luận lại để đủ bức tranh, không phải trích dẫn thảo luận gốc.*
  Nếu dùng subtree, code bị nhân bản vào lịch sử ToDo, mất khả năng
  "1 nguồn sự thật" — sửa ở Multi-user-System không tự có ở ToDo mà
  phải merge lại thủ công, dễ phân nhánh (fork drift) hơn submodule
  (submodule chỉ lưu **con trỏ** commit, không nhân bản code).
- **D — Backend-as-a-service** (chỉ gọi API của 1 instance Multi-user-
  System đã deploy sẵn, không nhúng source code vào repo ToDo) — *cũng
  không thấy nhắc tới, suy luận thêm.* Phương án này không khả thi vì
  ToDo cần **thêm code nghiệp vụ Task module mới** vào chính backend đó
  (`todo-task-module`, xem `53d6d66`/`72cbd3b`) — cần quyền sửa source,
  không chỉ gọi API của 1 bản đã deploy cố định.

## Decision

Chọn **B — git submodule**:

- `.gitmodules` khai báo submodule `auth-core` trỏ tới
  `https://github.com/hadv-vn/Multi-user-System.git`, pin tại 1 commit
  hash cụ thể (version ban đầu `fdb5b11...`, cập nhật dần qua các lần
  "bump auth-core" sau này — `571d860`, `8465c01`, `ccf971f`, `53d6d66`,
  `eb94e26`, `72cbd3b`).
- Xóa hoàn toàn `backend/` (Express + Prisma) tự viết trước đó.
- **Mọi code backend/auth, kể cả tính năng nghiệp vụ riêng của ToDo**
  (Task CRUD, Category, DueDate, Priority, Reorder, Admin Task
  Statistics) **được thêm vào chính repo `auth-core`**, cụ thể tại
  `auth-core/backend/src/features/todo-task-module/` — không tạo 1
  backend thứ 2 riêng cho ToDo. ToDo app (Android) chỉ là **client**
  gọi API `api/v1/*` của backend trong submodule.
- Cập nhật submodule lên commit mới = quy trình 2 bước ghi trong
  README: (1) `cd auth-core && git pull` (hoặc checkout commit cụ thể)
  ở phía repo `auth-core`, (2) `git add auth-core && git commit` ở
  phía ToDo để chốt lại con trỏ mới.

## Consequences

**Tích cực:**

- Không trùng lặp logic Auth/Session/RBAC/Account/Admin đã có sẵn, đã
  kiểm chứng (unit test, e2e test, ADR riêng tại `auth-core/docs/
  architecture/adr/`) — tiết kiệm effort lớn hơn nhiều so với tự viết
  lại từ đầu (đăng ký + xác thực email + session + lockout + RBAC +
  khóa tài khoản...).
- **1 nguồn sự thật duy nhất** cho backend: 1 bản fix bug/bảo mật ở
  `auth-core` tự động sẵn có cho ToDo (và mọi hệ thống khác dùng chung
  submodule này trong tương lai) chỉ bằng cách bump con trỏ, không cần
  đồng bộ tay 2 codebase như phương án subtree/copy.
- `auth-core` được thiết kế theo đúng mô hình "core generic + feature
  module" (`auth-core/CLAUDE.md`) nên Task module của ToDo nằm gọn
  trong 1 thư mục feature riêng, không làm "bẩn" core Auth/RBAC —
  module Task vẫn có thể tái dùng cho hệ thống khác sau này, đúng mục
  tiêu ban đầu của Multi-user-System.

**Đánh đổi / rủi ro chấp nhận:**

- Git submodule là cơ chế dễ dùng sai: quên `--recursive` khi clone,
  quên `git submodule update --init --recursive` sau khi pull, hoặc
  commit "con trỏ cũ" nếu quên pull trong `auth-core/` trước — README
  đã phải viết riêng hướng dẫn để giảm rủi ro này, nhưng vẫn là gánh
  nặng vận hành thêm so với 1 repo liền mạch.
- ToDo phụ thuộc hoàn toàn vào vòng đời phát triển của `auth-core`: 1
  thay đổi breaking ở backend (ví dụ đổi hẳn cơ chế xác thực JWT →
  session cookie, xem [ADR 0002](0002-android-session-cookie-storage.md))
  buộc Android phải refactor theo ngay, không nằm trong tầm kiểm soát
  riêng của ToDo.
- Về mặt tổ chức code, "ToDo" bị chia thành **2 repo vật lý** — người
  đọc lần đầu phải biết nhảy sang `auth-core/backend/src/features/
  todo-task-module/` mới thấy đủ backend logic của chính app này. Bản
  thân `auth-core/CLAUDE.md` đã tự ghi nhận độ lệch quy ước phát sinh
  từ đây: *"module task nằm ở `backend/src/features/todo-task-module/`
  trong khi CLAUDE.md hướng dẫn `backend/src/<feature>/` — cần thống
  nhất một quy ước cho module mới"* — vẫn là việc chưa xử lý.
- Không có bằng chứng ghi lại rằng phương án C/D từng được viết ra so
  sánh tại thời điểm quyết định — khác ADR 0001/0002 (có sẵn spec
  doc/ADR gốc ghi rõ từng phương án), ADR này **phục dựng lại** các
  phương án hợp lý nhất có thể suy luận được từ bối cảnh, không phải
  trích dẫn nguyên văn một cuộc thảo luận đã có.

**Ảnh hưởng tới các bước tiếp theo:**

- Mọi tính năng backend mới của ToDo tiếp tục đặt trong `auth-core/
  backend/src/features/todo-task-module/`, không tạo thêm backend thứ
  2 — vi phạm nguyên tắc này sẽ tái tạo lại đúng vấn đề mà ADR này đã
  giải quyết (trùng lặp logic).
- Pipeline/kiểm thử của ToDo phải tính cả bước "bump + test submodule"
  (`auth-core` có CI riêng — lint + unit test + e2e + Docker smoke
  test) như 1 phần của quy trình release ToDo, không thể coi backend
  là "đã xong" chỉ vì Android build xanh.
- Việc thống nhất quy ước đặt tên module (`features/<feature>/` so với
  `features/todo-task-module/`) nêu ở `auth-core/CLAUDE.md` vẫn là nợ
  kỹ thuật mở, nên đưa vào `tech-debt-tracker` hoặc xử lý ở 1 ADR/commit
  `refactor` riêng, không gộp vào đây.

## Liên quan

- ADR trước trong repo ToDo: [0001](0001-task-manual-reorder-sort-key.md),
  [0002](0002-android-session-cookie-storage.md) — cả hai là hệ quả kỹ
  thuật trực tiếp của quyết định dùng `auth-core` ở ADR này.
- `auth-core/CLAUDE.md` — mục Overview (*"base for future systems"*) và
  mục 9 (*lưu ý lệch quy ước `todo-task-module`*).
- README (`README.md` mục "1. auth-core (submodule)") — hướng dẫn
  clone/update/bump submodule.
- Commit triển khai: `4eacf29` (thêm submodule), `22461c6` (mô tả trong
  README).
- Dashboard tham chiếu: `health-check/index.html` — chỉ số `c6 Decision
  Coverage`.
