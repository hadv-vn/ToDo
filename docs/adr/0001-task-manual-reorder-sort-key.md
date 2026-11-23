# 0001. Lưu thứ tự thủ công của Task — Fractional Sort Key qua API dựa trên hàng xóm

## Status

Accepted (đã triển khai ở commit `72cbd3b`, 2026-11-16; backend tại
`auth-core/backend/src/features/todo-task-module/task.service.ts`
method `reorder()`, Android tại
`android/app/src/main/java/com/example/todo/domain/usecase/task/ReorderTaskUseCase.kt`).

> Ghi lại sau khi code đã chạy (retroactive) — ADR đầu tiên của repo
> ToDo, viết để lấp khoảng trống Decision Coverage (c6) phát hiện ở
> `health-check/index.html`: 5 thay đổi `feat:` lớn trước đó không có
> ADR/Decision Log nào, trong khi submodule `auth-core` đã có thói quen
> này (`auth-core/docs/architecture/adr/0001-session-management-strategy.md`).
> Nội dung bên dưới tái cấu trúc lại quyết định đã nằm rải trong
> `docs/Task_Reorder_ToDo.md` mục 2 thành dạng ADR độc lập, dễ truy vết.

## Context

ToDo cần 2 năng lực (FR-25, FR-26 — `docs/Task_Reorder_ToDo.md`):
kéo-thả đổi thứ tự 1 task trong danh sách, và chọn vị trí chèn task mới
(đầu/cuối danh sách). Thứ tự phải **toàn cục theo user** (không theo
từng filter All/Open/Done) và phải **tồn tại trên server** (không mất
khi tải lại/đổi thiết bị).

Vấn đề kỹ thuật cần quyết định trước khi code: lưu "thứ tự thủ công"
bằng cấu trúc dữ liệu nào, để 1 lần kéo-thả không tốn chi phí tăng theo
độ dài danh sách.

**Các phương án đã cân nhắc** (gốc tại `Task_Reorder_ToDo.md` mục 2):

- **A — Số nguyên thứ tự tuần tự (1, 2, 3, …)**: đơn giản, dễ hiểu
  nhất. Bị loại vì chèn 1 task vào giữa 2 task khác đòi hỏi re-index
  (ghi lại) **N task phía sau** mỗi lần kéo-thả — O(N) write cho 1 thao
  tác, và vi phạm AC2 (FR-25: UI phải phản hồi tức thời bằng optimistic
  update, không thể chờ ghi lại cả danh sách mỗi lần kéo).
- **B — Fractional index (`sortKey: double`)**: di chuyển 1 task chỉ
  cần tính `sortKey` mới = trung điểm của 2 `sortKey` liền kề — **1
  write duy nhất**, không phụ thuộc độ dài danh sách.
- **C — Client tự tính và gửi thẳng giá trị `sortKey`** (không ghi
  thành phương án riêng trong spec vì bị loại ngay, nêu lại ở đây để
  không ai hỏi lại): bị loại vì client có thể đang cache dữ liệu cũ,
  tính `sortKey` sai sẽ ghi sai trực tiếp xuống DB mà server không có
  cách phát hiện.

Chọn B, nhưng B có 1 câu hỏi thiết kế con cần chốt kèm: **client gửi
gì để server tính `sortKey`?** Quyết định: client không gửi giá trị
`sortKey`, chỉ gửi **id của 2 "hàng xóm"** (`afterTaskId`/
`beforeTaskId`) — server tự đọc `sortKey` thật của 2 hàng xóm đó từ DB
tại thời điểm xử lý rồi tính trung điểm. Điều này loại bỏ hẳn rủi ro
của phương án C mà vẫn giữ được chi phí O(1) của phương án B.

## Decision

Chọn **B + server tự tính theo hàng xóm**: thứ tự thủ công được lưu
bằng cột `sort_key: double` trên bảng `tasks`, cập nhật qua API
`PATCH /api/v1/tasks/:id/reorder` nhận `{ afterTaskId, beforeTaskId }`
(cả hai optional, `null` tường minh có ý nghĩa "đầu/cuối danh sách" —
xem comment tại `ReorderTaskRequestDto.kt`).

Chi tiết đã chốt (triển khai tại `docs/Task_Reorder_ToDo.md` mục 4.2):

- **Tính `newSortKey`**: cả 2 hàng xóm có giá trị → trung điểm
  `(after + before) / 2`; chỉ có 1 bên → `± GAP` (khoảng cách mặc định
  `1000`, hằng số `insert-position.constants.ts`); không có hàng xóm
  nào (danh sách 1 task) → giữ `sortKey` hiện tại.
- **Khoảng cách mặc định `GAP = 1000`** giữa 2 `sortKey` liền kề ngay
  từ đầu (migration backfill dùng `ROW_NUMBER() * 1000`) — chủ động
  chừa khoảng trống lớn để làm chậm tốc độ "cạn" độ chính xác `double`
  khi chèn lặp lại nhiều lần vào đúng 1 khoảng.
- **Validate ownership ở backend**: `afterTaskId`/`beforeTaskId`
  không tồn tại hoặc thuộc user khác → `400 VALIDATION_ERROR`. Vì vậy
  tầng domain Android (`ReorderTaskUseCase`) **không cần rule nghiệp
  vụ nào** — chỉ gọi thẳng `TaskRepository.reorderTask(...)`, cùng mẫu
  đã dùng cho `DeleteTaskUseCase`.
- **Không rebalance tự động ở v1** — giới hạn lý thuyết (độ chính xác
  `double` ~15–17 chữ số thập phân) đã biết trước nhưng không xử lý
  ngay khi chưa đo được vấn đề thật ở quy mô hiện tại (~50 task/user,
  NFR-1) — theo nguyên tắc #6 (không tối ưu sớm) của `CLAUDE.md`.
- **Đổi `orderBy` mặc định** của danh sách task từ `createdAt desc`
  sang `sortKey asc` — đây là thay đổi hành vi hiện có, không thuần
  additive như các tăng trưởng trước, cần chạy lại toàn bộ
  `TC-LIST-*` trong `Test_Scenarios_ToDo_Android.md` sau migrate.

## Consequences

**Tích cực:**

- Chi phí 1 lần kéo-thả là **O(1)** (1 UPDATE), không phụ thuộc độ dài
  danh sách — khớp yêu cầu optimistic UI phản hồi tức thời (AC2 FR-25).
- Server luôn đọc `sortKey` thật tại thời điểm xử lý thay vì tin giá
  trị client tính — loại bỏ lớp rủi ro của phương án C mà không cần
  thêm cơ chế lock/versioning phức tạp.
- Validate ownership nằm hẳn ở backend → `ReorderTaskUseCase` ở Android
  giữ được nguyên tắc "mỗi UseCase một việc" (`CLAUDE.md` #3) mà không
  phải nhân đôi logic kiểm tra ở client.
- Test backend (`task.service.spec.ts`) và Android
  (`ReorderTaskUseCaseTest.kt`) đều cover đủ 4 nhánh tính `sortKey`
  (cả 2 hàng xóm / chỉ sau / chỉ trước / không hàng xóm nào).

**Đánh đổi / rủi ro chấp nhận:**

- **Giới hạn lý thuyết chưa xử lý**: kéo lặp lại rất nhiều lần vào
  đúng 1 khoảng trống (ví dụ luôn thả giữa đúng 2 task A/B) sẽ làm
  khoảng cách giữa 2 `sortKey` nhỏ dần, về lý thuyết có thể chạm độ
  chính xác `double` sau **rất nhiều** lần — ghi nhận là nợ kỹ thuật
  đã biết (`Task_Reorder_ToDo.md` mục 8), chưa có cơ chế rebalance.
  Nếu phát sinh thật, chỉ cần thêm 1 job backend tính lại `sortKey`
  theo khoảng đều cho 1 user, **không cần đổi API `reorder` hiện có**.
- **Đổi hành vi sắp xếp mặc định** (`createdAt desc` → `sortKey asc`)
  là rủi ro triển khai thật trên dữ liệu production đang có — migration
  phải backfill đúng, nếu sai thứ tự sẽ nhảy loạn ngay sau deploy.
- **Liên kết tới Finding c3 (Freshness) trong health-check**: tại thời
  điểm viết ADR này, `Test_Scenarios_ToDo_Android.md` **chưa** được bổ
  sung kịch bản cho thay đổi `orderBy` nói trên — đây là việc còn thiếu,
  không phải đã làm, ghi rõ ra đây để không bị hiểu nhầm là đã xong.

**Ảnh hưởng tới các bước tiếp theo:**

- Bất kỳ tính năng sau này cần thêm 1 "thứ tự thủ công" khác (ví dụ nếu
  sau này làm thứ tự riêng theo category/dự án — hiện đang ngoài phạm
  vi) nên tái dùng đúng mô hình fractional `sortKey` + API theo hàng
  xóm này, không phát sinh cơ chế lưu thứ tự thứ 2 trong cùng project.
- Khi thêm ADR mới cho quyết định kiến trúc tiếp theo, đặt tại
  `docs/adr/000X-<slug>.md`, đánh số tiếp theo ADR này.

## Liên quan

- Spec gốc: `docs/Task_Reorder_ToDo.md` (mục 2 — quyết định mô hình dữ
  liệu; mục 4.2 — API `reorder`; mục 8 — giới hạn đã biết).
- Code: `auth-core/backend/src/features/todo-task-module/task.service.ts`
  (`reorder()`, `create()`), `insert-position.constants.ts`;
  `android/app/.../domain/usecase/task/ReorderTaskUseCase.kt`,
  `domain/model/InsertPosition.kt`,
  `data/remote/dto/task/ReorderTaskRequestDto.kt`.
- Test: `auth-core/backend/src/features/todo-task-module/task.service.spec.ts`
  (`describe('reorder', …)`), `android/app/src/test/.../ReorderTaskUseCaseTest.kt`.
- Commit triển khai: `72cbd3b` (feat: thêm kéo-thả sắp xếp, danh mục,
  hạn chót và mức ưu tiên cho task).
- Dashboard tham chiếu: `health-check/index.html` — chỉ số `c6 Decision
  Coverage`.
