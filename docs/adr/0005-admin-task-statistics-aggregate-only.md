# 0005. Thống kê task cho ADMIN — chỉ số liệu tổng hợp, không bao giờ đọc nội dung task

## Status

Accepted (đã triển khai; bump submodule ở commit `eb94e26`, 2026-11-05,
trỏ `auth-core` sang branch `feature/admin-user-detail`; đặc tả tại
`docs/Admin_Task_Statistics_ToDo.md`; code tại
`auth-core/backend/src/features/todo-task-module/` —
`AdminTaskStatisticsController`, `TaskStatisticsService`,
`TaskStatisticsRepository`/`PrismaTaskStatisticsRepository`).

> Ghi lại sau khi code đã chạy (retroactive) — ADR thứ 5 và **cuối
> cùng** trong 5 thay đổi `feat:` lớn được health-check phát hiện
> thiếu ADR (`health-check/index.html`, chỉ số `c6 Decision Coverage`).
> Sau ADR này, cả 5/5 thay đổi lớn ban đầu đều có ADR tương ứng (xem
> [0001](0001-task-manual-reorder-sort-key.md),
> [0002](0002-android-session-cookie-storage.md),
> [0003](0003-auth-core-shared-backend-submodule.md),
> [0004](0004-android-clean-architecture-foundation.md)).

## Context

ToDo cần cho ADMIN khả năng xem **mức độ sử dụng app** của từng người
dùng (REQ-TODO-ADMIN-001, REQ-TODO-ADMIN-002 —
`docs/Admin_Task_Statistics_ToDo.md`): tổng số task, số đang mở/hoàn
thành, mốc tạo gần nhất, tỉ lệ hoàn thành — cho 1 user cụ thể (trang
chi tiết tài khoản) và cho toàn hệ thống (trang `/admin/task-stats`).

Ràng buộc đã chốt từ trước ở tầng `auth-core` (SRS chung, không phải
riêng ToDo): **A-3** và **NFR-004** — *"ADMIN không có đặc quyền đọc dữ
liệu cá nhân ở các module nghiệp vụ"*. Task (`title`, `description`)
là dữ liệu cá nhân của người dùng, không phải dữ liệu vận hành hệ
thống. Câu hỏi kiến trúc cần quyết định: làm sao cho ADMIN thấy được
*mức độ dùng* mà không vi phạm ràng buộc đó?

**Các phương án:**

- **A — ADMIN xem được danh sách/nội dung task đầy đủ của bất kỳ
  user** (mẫu phổ biến ở nhiều admin panel: "xem như user đó"). Bị
  loại ngay — vi phạm trực tiếp A-3/NFR-004, không phải vấn đề thẩm mỹ
  mà là yêu cầu bảo mật đã chốt ở tầng SRS chung của `auth-core`.
- **B — ADMIN có thể xem nội dung task nhưng có audit log mọi lần
  xem** (mô hình "break-glass access" — cho phép truy cập nhưng ghi
  vết để truy cứu sau). *Không thấy nêu trong spec, suy luận thêm để
  đủ bức tranh:* vẫn bị loại vì A-3/NFR-004 không đặt điều kiện "có
  audit thì được xem" — ràng buộc là **cấm truy cập**, không phải
  "truy cập có kiểm soát".
- **C — ADMIN chỉ xem số liệu tổng hợp (aggregate-only)**: tổng số,
  đếm theo trạng thái, mốc tạo gần nhất, tỉ lệ hoàn thành — không bao
  giờ trả về `title`/`description`, và ở tầng dữ liệu **không bao giờ
  đọc bản ghi task** (chỉ `groupBy`/`aggregate`/`count`). **Chọn.**

## Decision

Chọn **C**, với 2 lớp chặn độc lập (defense in depth) để nguyên tắc
"aggregate-only" không thể bị vi phạm bởi 1 lỗi code đơn lẻ:

1. **Tầng API/response — không có field nội dung**: `TaskStatsDto`
   chỉ có `userId`, `total`, `byStatus` (map trạng thái → số lượng,
   luôn đủ khóa mặc định `0`), `lastCreatedAt`. Không có `title`,
   `description`, hay danh sách task nào trong response, ở cả 2
   endpoint:
   - `GET /api/v1/admin/users/{id}/task-stats` (1 user).
   - `GET /api/v1/admin/task-stats` (toàn hệ thống, phân trang, lọc
     theo email).
2. **Tầng dữ liệu — tách repository riêng cho thống kê**:
   `TaskStatisticsRepository`/`PrismaTaskStatisticsRepository` là
   interface **tách biệt hoàn toàn** khỏi `TaskRepository` (CRUD task
   của user). Repository thống kê chỉ gọi `groupBy`/`count`, không có
   method nào trả về bản ghi task đầy đủ — nghĩa là về mặt cấu trúc
   code, **không có đường nào** để tầng Controller/Service của thống
   kê vô tình `SELECT *` ra nội dung task, vì class đó không hề có khả
   năng đó.
3. **API task gốc của user giữ nguyên hành vi lọc theo `userId`**,
   *kể cả khi người gọi là ADMIN* — ADMIN không có đường tắt nào (role
   check) để bypass filter này trên API `/api/v1/tasks`.
4. **Ranh giới module**: toàn bộ code thống kê nằm trong
   `todo-task-module/` (nghiệp vụ ToDo), không chạm vào `AdminModule`
   lõi của `auth-core` — `auth-core` chỉ cung cấp chỗ gắn mở rộng
   (`extraSections` ở frontend, route `/admin/task-stats` riêng), giữ
   đúng mô hình "core generic + feature module" đã quyết định ở
   [ADR 0003](0003-auth-core-shared-backend-submodule.md).
5. **Kiểm chứng bằng test, không chỉ bằng quy ước**: unit test
   (`task-statistics.service.spec.ts`,
   `prisma-task-statistics.repository.spec.ts`) kiểm tra rõ "không đọc
   bản ghi task" và "không lộ `title`/`description`" — biến nguyên tắc
   bảo mật thành assertion chạy được, không chỉ là ghi chú trong docs.

## Consequences

**Tích cực:**

- Tuân thủ đúng A-3/NFR-004 bằng **cấu trúc code** (interface tách
  biệt, không có method nào có khả năng trả nội dung) thay vì chỉ bằng
  quy ước/code review — khó vi phạm nhầm hơn so với việc dựa vào
  `TaskRepository` chung rồi "nhớ" không select `title`.
  (Ghi chú: cách làm "cấu trúc dữ liệu loại bỏ trường hợp sai" đúng
  nguyên tắc #1 của `CLAUDE.md` — Linus Torvalds "taste" — áp cho
  Kotlin/Android, ở đây áp đúng cùng tinh thần cho TypeScript backend.)
- ADMIN vẫn có đủ thông tin để đánh giá "mức độ dùng app" (phục vụ hỗ
  trợ/vận hành) mà không cần quyền truy cập cao hơn mức cần thiết
  (nguyên tắc least privilege).
- UI chủ động thông báo giới hạn này cho ADMIN ngay trên giao diện
  (*"Nội dung công việc là dữ liệu riêng tư của người dùng, quản trị
  viên không xem được"*) — tránh ADMIN hiểu nhầm là thiếu tính năng.

**Đánh đổi / rủi ro chấp nhận:**

- ADMIN **không thể** dùng tính năng này để hỗ trợ user theo kiểu "xem
  đúng task bị lỗi của bạn" — mọi hỗ trợ ở mức nội dung task phải qua
  kênh khác (user tự cung cấp thông tin), không qua admin panel. Đây
  là đánh đổi có chủ đích, không phải thiếu sót.
- Tra cứu nội dung task trực tiếp trong database (khi xử lý sự cố) vẫn
  khả thi về mặt kỹ thuật (ai có quyền truy cập DB) — spec đã ghi rõ
  đây là "hoạt động vận hành **ngoài** ứng dụng, phải có lý do rõ ràng,
  chỉ đọc, nên dùng tài khoản DB chỉ có quyền `SELECT`" — nghĩa là ranh
  giới này được enforce ở tầng ứng dụng, **không** ở tầng hạ tầng/DB
  (ngoài phạm vi ADR này).
- Thêm 1 trạng thái task mới trong tương lai yêu cầu cập nhật đồng bộ
  `byStatus` ở cả `TaskStatisticsRepository` và enum trạng thái chính —
  2 nơi phải khớp nhau, chưa có cơ chế tự kiểm tra lệch giữa 2 chỗ này
  ngoài test hiện có.
- e2e test qua HTTP với tài khoản ADMIN thật và kiểm tra trực quan
  trên browser **chưa chạy** tại thời điểm viết spec (ghi rõ trong mục
  6 "Kiểm thử" của `Admin_Task_Statistics_ToDo.md`) — chỉ có unit test
  + truy vấn thủ công trên Postgres local.

**Ảnh hưởng tới các bước tiếp theo:**

- Mọi tính năng admin-facing khác chạm tới dữ liệu nghiệp vụ của user
  (không chỉ Task) nên áp đúng mẫu này: repository tổng hợp tách biệt
  khỏi repository CRUD, test khẳng định "không đọc bản ghi gốc" — không
  tái phát minh lại cách giải quyết mỗi lần có module mới.
- Nếu sau này cần ADMIN xem nội dung task trong 1 tình huống đặc biệt
  (ví dụ điều tra lạm dụng), đó là thay đổi **ràng buộc bảo mật đã chốt
  ở A-3/NFR-004**, không phải thay đổi kỹ thuật nhỏ — cần 1 ADR mới ở
  tầng `auth-core` (không phải ToDo) xác nhận lại nguyên tắc, kèm mô
  hình audit/break-glass access nếu được chấp thuận.

## Liên quan

- Đặc tả: `docs/Admin_Task_Statistics_ToDo.md` (mục 1 — nguyên tắc bảo
  mật dữ liệu; mục 3 — API; mục 5 — thiết kế kỹ thuật; mục 6 — kiểm
  thử).
- Ràng buộc gốc (tầng `auth-core`, áp dụng chung mọi module nghiệp
  vụ): `auth-core/docs/requirements/srs-multi-user-system.md` — A-3,
  NFR-004, REQ-ADMIN-004.
- ADR liên quan trong repo ToDo: [0003](0003-auth-core-shared-backend-submodule.md)
  (mô hình "core generic + feature module" mà ranh giới module ở ADR
  này tuân theo).
- Code: `auth-core/backend/src/features/todo-task-module/
  task-statistics.service.ts`, `prisma-task-statistics.repository.ts`,
  `admin-task-statistics.controller.ts` (tên file tham khảo theo mô tả
  spec, xem đúng tên thật trong submodule `auth-core`).
- Test: `task-statistics.service.spec.ts`,
  `prisma-task-statistics.repository.spec.ts`,
  `task-status-summary.spec.ts`.
- Commit triển khai: `eb94e26` (bump submodule sang
  `feature/admin-user-detail`).
- Dashboard tham chiếu: `health-check/index.html` — chỉ số `c6 Decision
  Coverage`.
