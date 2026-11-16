# Sắp xếp thủ công cho Task (Kéo-thả + vị trí thêm mới) — ToDo feature extension

| | |
|---|---|
| **Phiên bản** | 1.0 |
| **Ngày tạo** | 2026-11-16 |
| **Trạng thái** | Đề xuất — chưa implement (HLD + LLD, chờ duyệt trước khi code) |
| **Code dự kiến** | `auth-core/backend/src/features/todo-task-module/` · `android/app/src/main/java/com/example/todo/` |
| **Liên quan** | `docs/SRS_ToDo_Android.md` FR-06 · `docs/Task_Priority_ToDo.md` (FR-18/19) · `docs/Task_DueDate_ToDo.md` (FR-20/21) · `docs/Task_Category_ToDo.md` (FR-22/23/24) |

Tài liệu này đặc tả 2 mục trong nhóm 🟡 còn lại của mục "Tạo và quản lý
task": **kéo-thả sắp xếp lại task** và **chọn vị trí thêm task mới**
(đầu/cuối danh sách). Hai mục này dùng **chung 1 cơ chế dữ liệu**
(`sortKey`), nên gộp vào 1 tài liệu thay vì tách riêng.

Đây là tăng trưởng 🟡 **phức tạp nhất về UI** trong 4 tài liệu đã làm —
không phải vì mô hình dữ liệu khó, mà vì Compose hiện tại (xem mục 5.3)
không có sẵn "reorderable LazyColumn" built-in, cần tự dựng gesture kéo
thả — có 1 quyết định cần người dùng/team xác nhận trước khi code (mục
5.3).

---

## 1. Mục tiêu & phạm vi của tăng trưởng này

**Trong phạm vi:**
- Kéo-thả 1 task trong danh sách để đổi vị trí; vị trí mới được lưu lại
  trên server (không mất khi tải lại/đổi filter).
- Thứ tự là **toàn cục** theo mỗi user (không phải riêng theo từng
  filter All/Open/Done) — đổi filter chỉ ẩn/hiện task, không đổi thứ tự
  tương đối giữa các task còn hiển thị.
- 1 cài đặt (trên thiết bị, không đồng bộ server) chọn task mới tạo
  được chèn **lên đầu** hoặc **xuống cuối** danh sách — mặc định: đầu
  (giống hành vi phổ biến của các app to-do khác).

**Ngoài phạm vi của tăng trưởng này:**
- Thứ tự riêng theo từng category/dự án (chưa có khái niệm "dự án" —
  nhóm 🔴 trong đánh giá trước; category ở `Task_Category_ToDo.md`
  không ảnh hưởng thứ tự).
- Đồng bộ real-time thứ tự giữa nhiều thiết bị đang mở cùng lúc (ngoài
  phạm vi "online-only" hiện có — mỗi lần tải lại list sẽ thấy thứ tự
  mới nhất, không cần live-update).
- Tự động "rebalance" `sortKey` định kỳ (mục 2 — biết trước giới hạn,
  không xử lý trong v1, xem mục 8).

---

## 2. Quyết định mô hình dữ liệu (quan trọng, chốt trước khi code)

- **Fractional index (`sortKey: double`)**, không phải số nguyên thứ
  tự 1,2,3,... Lý do: với số nguyên, chuyển 1 task vào giữa 2 task khác
  đòi hỏi re-index (ghi lại) **N task phía sau** mỗi lần kéo-thả — O(N)
  write cho 1 thao tác kéo. Với fractional index, di chuyển 1 task chỉ
  cần tính `sortKey` mới = trung điểm 2 `sortKey` liền kề — **1 write
  duy nhất**, bất kể danh sách dài bao nhiêu (vẫn hợp lý ở quy mô NFR-1
  ~50 task, nhưng cách làm đúng không phụ thuộc quy mô).
- **Giới hạn đã biết của fractional index**: lặp lại việc chèn vào cùng
  1 khoảng trống rất nhiều lần (ví dụ luôn kéo task vào giữa đúng 2 task
  A/B) sẽ làm khoảng cách giữa 2 số càng nhỏ dần, về lý thuyết có thể
  chạm độ chính xác `double` (~15-17 chữ số thập phân) sau **rất nhiều**
  lần. Ở quy mô v1 (~50 task/user, thao tác thủ công bằng tay), không
  đáng để xử lý rebalance ngay — ghi nhận là giới hạn đã biết (mục 8),
  không tự thêm cơ chế rebalance khi chưa đo được vấn đề thật (nguyên
  tắc #6 — không tối ưu sớm).
- **Thứ tự tính qua "2 hàng xóm" do client cung cấp, server tự tính
  `sortKey`** (không nhận trực tiếp số `sortKey` từ client) — tránh
  phải tin tưởng giá trị float client tự tính (client có thể tính sai
  do đang cache dữ liệu cũ), server luôn đọc `sortKey` thật của 2 hàng
  xóm từ DB tại thời điểm xử lý.
- **Migration backfill**: task cũ (tạo trước migration) cần có
  `sortKey` phản ánh đúng thứ tự `createdAt desc` hiện tại — nếu không,
  thứ tự hiển thị sẽ nhảy loạn ngay sau khi deploy. Backfill 1 lần bằng
  `ROW_NUMBER()` trong chính migration (mục 3.1), không cần script riêng.

---

## 3. Yêu cầu chức năng

Tiếp nối FR — `Task_Category_ToDo.md` kết thúc ở FR-24.

### FR-25 — Kéo-thả sắp xếp lại thứ tự task (US-25)
**User Story**: Là người dùng, tôi muốn kéo-thả 1 task trong danh sách
để đặt nó lên trên hoặc xuống dưới task khác, phản ánh đúng mức độ tôi
muốn làm trước/sau.

**Acceptance Criteria**:
- AC1: Nhấn giữ (long-press) 1 dòng task → dòng đó "nhô lên" (elevation/
  shadow) và có thể kéo theo chiều dọc; các dòng khác tự dịch chuyển để
  nhường chỗ theo thời gian thực khi kéo đi qua.
- AC2: Thả tay → vị trí mới được gửi lên server; trong lúc chờ phản hồi,
  UI **giữ nguyên vị trí mới** (optimistic, giống tinh thần Toggle ở
  SRS 10.1) — khác AC khác (Create/Edit/Delete) vì bản chất kéo-thả bắt
  buộc phải phản hồi tức thời, không có cách nào làm khác mà vẫn dùng
  được.
- AC3: Server lỗi (mất mạng/404 — task bị xóa ở nơi khác trong lúc kéo)
  → **rollback** về thứ tự trước khi kéo + Snackbar lỗi (cùng pattern
  rollback đã có ở `ToggleTaskStatusUseCase`/LLD mục 4.4a).
- AC4: Kéo-thả hoạt động đúng trong cả 3 filter (All/Open/Done) — thứ
  tự sau khi kéo trong 1 filter cũng đúng khi xem lại filter khác (mục
  2 — thứ tự toàn cục).
- AC5: Danh sách < 2 task → không có gì để kéo-thả có ý nghĩa (không
  cần AC riêng, tự nhiên không có hiệu ứng).

### FR-26 — Chọn vị trí thêm task mới: đầu hoặc cuối danh sách (US-26)
**User Story**: Là người dùng, tôi muốn chọn task mới tạo luôn xuất
hiện ở đầu (để làm trước) hoặc cuối (để xếp hàng) danh sách, theo thói
quen của tôi.

**Acceptance Criteria**:
- AC1: Có 1 cài đặt 2 lựa chọn "Thêm task mới vào: Đầu danh sách / Cuối
  danh sách" — mặc định **Đầu danh sách**.
- AC2: Cài đặt lưu trên thiết bị (không đồng bộ server/nhiều thiết bị —
  mục 1), áp dụng ngay cho lần tạo task kế tiếp.
- AC3: Task mới tạo xuất hiện đúng vị trí đã chọn ngay khi quay lại
  `TaskListScreen` (không cần kéo tay sau khi tạo).

---

## 4. Thiết kế Backend

### 4.1 Migration (additive + backfill)

```sql
-- Migration ToDo feature extension — thêm "sort_key" vào "tasks",
-- backfill giữ đúng thứ tự created_at desc hiện tại.

ALTER TABLE "tasks" ADD COLUMN "sort_key" DOUBLE PRECISION;

UPDATE "tasks" AS t
SET "sort_key" = ranked.rn
FROM (
  SELECT "id", ROW_NUMBER() OVER (
    PARTITION BY "user_id" ORDER BY "created_at" DESC
  ) * 1000 AS rn
  FROM "tasks"
) AS ranked
WHERE t."id" = ranked."id";

ALTER TABLE "tasks" ALTER COLUMN "sort_key" SET NOT NULL;

CREATE INDEX "tasks_user_id_sort_key_idx" ON "tasks"("user_id", "sort_key");
```
`* 1000` để chừa khoảng trống lớn giữa các task liền kề ngay từ đầu —
giảm tốc độ "cạn" độ chính xác float đã nói ở mục 2. Đổi `orderBy` mặc
định của `findManyByUser` từ `createdAt: 'desc'` sang `sortKey: 'asc'`
(nhỏ hơn = lên trên) — **đây là thay đổi hành vi sắp xếp mặc định của
danh sách**, cần ghi rõ trong PR vì nó không "additive" thuần như các
tăng trưởng trước (xem mục 8).

### 4.2 API mới — reorder

`PATCH /api/v1/tasks/:id/reorder`

**Request**:
```json
{ "afterTaskId": "uuid-hoac-null", "beforeTaskId": "uuid-hoac-null" }
```
Ngữ nghĩa: "đặt task này ngay sau `afterTaskId` và ngay trước
`beforeTaskId`". Cả 2 đều `null` → kéo vào danh sách chỉ có 1 task
(không xảy ra thực tế nhưng vẫn hợp lệ). `afterTaskId = null` → kéo
lên đầu danh sách. `beforeTaskId = null` → kéo xuống cuối danh sách.

**Xử lý** (`task.service.ts`, method mới `reorder()`):
1. `findOwnedTaskOrThrow(userId, taskId)` — task đang kéo.
2. Nếu `afterTaskId` khác null: `findOwnedTaskOrThrow(userId, afterTaskId)`
   lấy `sortKey` thật; tương tự `beforeTaskId`. Không tồn tại/không
   thuộc user này → `400 VALIDATION_ERROR` (cùng quyết định đã dùng cho
   `categoryId` ở `Task_Category_ToDo.md` mục 4.5 — tham chiếu resource
   khác qua body, không phải path, nên xử lý như lỗi input).
3. Tính `newSortKey`:
   - cả 2 có giá trị: `(afterSortKey + beforeSortKey) / 2`.
   - chỉ có `before`: `beforeSortKey - 1000`.
   - chỉ có `after`: `afterSortKey + 1000`.
   - không có cái nào (danh sách còn 1 task): giữ `sortKey` hiện tại.
4. `taskRepository.updateSortKey(taskId, newSortKey)` — 1 UPDATE duy
   nhất (xem mục 2).

**Response**: `{ "data": <TaskResult> }` — trả lại `TaskResult` đầy đủ
(không cần field `sortKey` lộ ra ngoài — client chỉ cần biết thao tác
thành công, tự giữ thứ tự optimistic đã áp dụng ở UI, xem mục 5.3 AC2).

**Lỗi**:
| Status | code | Tình huống |
|---|---|---|
| 400 | `VALIDATION_ERROR` | `afterTaskId`/`beforeTaskId` không tồn tại hoặc thuộc user khác |
| 404 | `TASK_NOT_FOUND` | task đang kéo không tồn tại/đã bị xóa |
| 403 | `TASK_FORBIDDEN` | task đang kéo thuộc user khác |

### 4.3 Vị trí thêm task mới (FR-26)

`POST /api/v1/tasks` thêm field optional `insertPosition: 'top' |
'bottom'` (mặc định `'top'` nếu không gửi — khớp AC1 FR-26).

`task.service.create()`: đọc `sortKey` nhỏ nhất/lớn nhất hiện có của
user (`taskRepository.findMinMaxSortKey(userId)`), rồi:
- `'top'` → `sortKey = min - 1000` (hoặc `1000` nếu chưa có task nào).
- `'bottom'` → `sortKey = max + 1000` (hoặc `1000` nếu chưa có task nào).

### 4.4 File thay đổi/mới

| File | Thay đổi |
|---|---|
| `task-record.interface.ts`/`task-result.interface.ts` | thêm `sortKey: number` (record) — **không** thêm vào `TaskResult`/response (mục 4.2, client không cần) |
| `task-repository.interface.ts` | thêm `updateSortKey(id, sortKey)`, `findMinMaxSortKey(userId)` |
| `prisma-task.repository.ts` | implement 2 method trên; `findManyByUser` đổi `orderBy` sang `{ sortKey: 'asc' }` |
| `dto/create-task.dto.ts` | thêm `@IsOptional() @IsIn(['top', 'bottom']) insertPosition?: string` |
| **Mới** `dto/reorder-task.dto.ts` | `afterTaskId?: string \| null`, `beforeTaskId?: string \| null` (`@IsOptional @IsUUID`) |
| `task.controller.ts` | thêm `@Patch(':id/reorder')` |
| `task.service.ts` | thêm `reorder()` (mục 4.2), sửa `create()` (mục 4.3) |

---

## 5. Thiết kế Android

### 5.1 Domain
- `domain/repository/TaskRepository.kt`: thêm
  `suspend fun reorderTask(id: String, afterTaskId: String?, beforeTaskId: String?): ApiResult<Task>`;
  `createTask(...)` thêm tham số `insertPosition: InsertPosition`
  (enum `TOP, BOTTOM`, mới tại `domain/model/InsertPosition.kt`).
- **Mới** `domain/usecase/task/ReorderTaskUseCase.kt` — không có rule
  nghiệp vụ cần validate (ownership đã enforce ở backend), chỉ gọi
  thẳng repository.

### 5.2 Presentation — cài đặt vị trí thêm mới (FR-26)
- Cách lưu: `DataStore<Preferences>` đã có sẵn cho `SessionCookieStore`
  (xem `data/local/SessionCookieStore.kt`) — thêm 1 key boolean mới
  **trong cùng cơ chế DataStore đang dùng**, không tạo thêm hệ thống
  lưu trữ khác (nguyên tắc #9).
- UI: 1 `IconButton` "⚙" trên `TaskListScreen` `TopAppBar` mở
  `AlertDialog` 2 `RadioButton` — đủ nhỏ để không cần 1 "SettingsScreen"
  riêng (khác Category — Category cần CRUD thật, cài đặt này chỉ là 1
  switch).
- `TaskFormViewModel.onSubmit()` (Create mode): đọc preference này qua
  1 use case nhỏ `GetInsertPositionPreferenceUseCase`, truyền vào
  `CreateTaskUseCase`.

### 5.3 Presentation — kéo-thả (FR-25) — **quyết định cần xác nhận**

Compose hiện tại (BOM đang dùng, xem `Task_DueDate_ToDo.md` mục 5.3 đã
xác nhận có `DatePickerDialog`) **không có** API chính thức cho "kéo-thả
sắp xếp lại item trong `LazyColumn`" — khác date picker (đã có sẵn),
đây là năng lực hoàn toàn chưa có trong stack. 2 phương án:

| Phương án | Ưu điểm | Nhược điểm |
|---|---|---|
| **A. Tự viết** bằng `Modifier.pointerInput` + `detectDragGesturesAfterLongPress` trên `LazyListState.layoutInfo.visibleItemsInfo` để tính vị trí/offset, `Modifier.animateItem()` cho hiệu ứng dịch chuyển (API Compose Foundation hiện có, không cần thêm gì) | Không thêm dependency (đúng nguyên tắc #3/#9); toàn quyền kiểm soát hành vi | Code tự viết phức tạp hơn, phải tự xử lý: tính item đang ở dưới điểm thả, auto-scroll khi kéo tới mép màn hình, đồng bộ `pendingDeleteTaskIds`/swipe-to-delete đang dùng chung `LazyColumn` này |
| **B. Thêm thư viện** reorder cho Compose (ví dụ `org.burnoutcrew.reorderable`/kế nhiệm) | Ít code tự viết, đã xử lý sẵn auto-scroll/edge-case | Thêm 1 dependency ngoài — vi phạm nguyên tắc #3 ("không thêm thư viện mới khi cơ chế sẵn có đã đủ") NẾU việc tự viết vẫn khả thi (nó khả thi — xem cột A) |

**Khuyến nghị**: Phương án **A** (tự viết) — vì nguyên tắc #3/#9 của
project ưu tiên rõ, và quy mô danh sách nhỏ (~50 task, NFR-1) không cần
tối ưu phức tạp mà 1 thư viện chuyên dụng mới đáng giá. Cần xác nhận
của người dùng/team trước khi code vì đây là lựa chọn tốn effort hơn
hẳn các tăng trưởng 🟡 khác đã làm — không phải quyết định nhỏ.

**Thiết kế chi tiết nếu chọn A**:
- `TaskListScreen.kt` (`TaskRow`/vùng `LazyColumn`): thêm state
  `draggedTaskId: String?` + `dragOffsetY: Float` (local `remember`,
  KHÔNG đưa vào `TaskListUiState` — đây là trạng thái gesture thuần
  UI, không phải dữ liệu nghiệp vụ, đúng tinh thần Carmack "state đi 1
  nguồn" áp dụng cho dữ liệu, không áp dụng cho animation tạm thời).
- **Tách 3 hàm ở `TaskListViewModel`, KHÔNG dùng 1 hàm
  `onTaskReordered` duy nhất** — phát hiện khi triển khai thật (Carmack
  "chạy từng bước, hiểu chính xác máy đang làm gì" thay vì đoán): 1
  lần kéo đi qua N hàng xóm sẽ kích hoạt callback `onDrag` của
  `pointerInput` N lần LIÊN TỤC trong lúc kéo — nếu mỗi lần đó đều gọi
  `ReorderTaskUseCase` (network), 1 lần kéo tay sẽ spam N request lên
  server. Thiết kế đúng tách rời "di chuyển cục bộ" (nhiều lần, không
  mạng) khỏi "commit lên server" (đúng 1 lần khi thả tay):
  - `onTaskDragStarted()` — gọi khi long-press bắt đầu, lưu snapshot
    `tasks` hiện tại làm mốc rollback.
  - `onTaskDragStepped(fromIndex, toIndex)` — gọi MỖI LẦN item đang
    kéo đi qua 1 hàng xóm trong `onDrag`; chỉ `_uiState.update` cục bộ
    (`tasks.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }`),
    không gọi `ReorderTaskUseCase`.
  - `onTaskDragEnded(taskId)` — gọi 1 LẦN DUY NHẤT ở `onDragEnd`/
    `onDragCancel`; đọc 2 hàng xóm hiện tại của `taskId` trong
    `uiState.tasks` (đã đúng vị trí cuối cùng nhờ các lần `Stepped`
    trước đó), gọi `ReorderTaskUseCase` đúng 1 lần.
- Lỗi/rollback (AC3 FR-25): `onTaskDragEnded` rollback về snapshot đã
  lưu ở `onTaskDragStarted` (thứ tự LÚC BẮT ĐẦU kéo, không phải thứ tự
  ngay trước bước lỗi) + Snackbar — cùng khuôn `onToggleTask` rollback
  hiện có.

### 5.4 Thay đổi file

| File | Thay đổi |
|---|---|
| `TaskListUiState.kt` | không đổi struct (mục 5.3 — gesture state ở ngoài) |
| `TaskListViewModel.kt` | thêm `onTaskDragStarted()`/`onTaskDragStepped(fromIndex, toIndex)`/`onTaskDragEnded(taskId)` |
| `TaskListScreen.kt` | `TaskRow`/`LazyColumn`: thêm gesture kéo-thả + `Modifier.animateItem()` |
| `TaskFormViewModel.kt` | Create mode: đọc `insertPosition`, truyền vào `CreateTaskUseCase` |
| `data/local/SessionCookieStore.kt` hoặc file DataStore mới cùng chỗ | thêm key preference `insertPosition` |

---

## 6. Giao diện (mockup)

### 6.1 Kéo-thả (đang kéo "Dọn inbox" lên trên "Họp review sprint")
```
┌──────────────────────────────────────────┐
│ ☐ 🔴 Họp review sprint                    │
│ ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ │ ← khoảng trống đang nhường chỗ
│ ☐ 🟢 Dọn inbox              (đang kéo, nhô lên, theo tay) │
│ ☑ 🟡 Viết báo cáo tuần                    │
└──────────────────────────────────────────┘
```

### 6.2 Cài đặt vị trí thêm mới
```
┌──────────────────────────────────────────┐
│ Thêm task mới vào:                        │
│  ( ● ) Đầu danh sách                      │
│  (   ) Cuối danh sách                     │
│              [  Đóng  ]                   │
└──────────────────────────────────────────┘
```

---

## 7. Kiểm thử

**Backend** (Vitest):
- `task.service.spec.ts`: `reorder()` — cả 4 trường hợp tính `newSortKey`
  (giữa 2 task, chỉ có after, chỉ có before, cả 2 null); `afterTaskId`
  thuộc user khác → `400`; task đang kéo không tồn tại → `404`.
- `create()`: `insertPosition: 'top'`/`'bottom'`/không gửi, kiểm tra
  `sortKey` tính đúng so với min/max hiện có, kể cả case chưa có task
  nào (user mới).
- Migration backfill: test riêng (hoặc kiểm tra thủ công trên DB test)
  xác nhận thứ tự sau backfill khớp đúng `created_at desc` cũ.

**Android**:
- `ReorderTaskUseCaseTest`: gọi đúng tham số repository.
- `TaskListViewModelTest`: `onTaskDragStepped` cập nhật đúng vị trí cục
  bộ, KHÔNG gọi `ReorderTaskUseCase`; `onTaskDragEnded` gọi
  `ReorderTaskUseCase` đúng 1 lần với đúng hàng xóm cuối cùng; lỗi từ
  `ReorderTaskUseCase` → rollback đúng thứ tự LÚC BẮT ĐẦU kéo (giống
  khuôn test `onToggleTask` lỗi hiện có); không đổi vị trí → không gọi
  server.
- `TaskFormViewModelTest`: Create gửi đúng `insertPosition` theo
  preference đã lưu.
- Instrumented: kéo-thả 1 item bằng `performTouchInput { swipe... }`
  (hoặc `down`/`moveBy`/`up` theo bước) → xác nhận thứ tự hiển thị mới;
  tạo task mới với từng preference → xuất hiện đúng đầu/cuối danh sách.

---

## 8. Ngoài phạm vi & giới hạn đã biết

- Rebalance `sortKey` định kỳ khi khoảng cách giữa 2 số quá nhỏ — giới
  hạn lý thuyết đã ghi ở mục 2, không xử lý ở v1.
- Đồng bộ real-time thứ tự giữa nhiều thiết bị đang mở cùng lúc.
- Thứ tự riêng theo category/dự án.
- **Lưu ý triển khai**: đổi `orderBy` mặc định (mục 4.1) là thay đổi
  hành vi hiện có (không thuần additive như 3 tăng trưởng 🟡 trước) —
  cần chạy lại `Test_Scenarios_ToDo_Android.md` TC-LIST-* sau khi
  migrate để xác nhận không phá vỡ kỳ vọng thứ tự hiện tại của người
  dùng đang có task thật trên production.
