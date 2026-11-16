# Hạn chót cho Task (Due Date) — ToDo feature extension

| | |
|---|---|
| **Phiên bản** | 1.0 |
| **Ngày tạo** | 2026-11-16 |
| **Trạng thái** | Đề xuất — chưa implement (HLD + LLD, chờ duyệt trước khi code) |
| **Code dự kiến** | `auth-core/backend/src/features/todo-task-module/` (backend) · `android/app/src/main/java/com/example/todo/` (Android) |
| **Liên quan** | `docs/SRS_ToDo_Android.md` FR-06/FR-11/FR-12 · `docs/Task_Priority_ToDo.md` (cùng nhóm 🟡, độc lập — không phụ thuộc lẫn nhau) |

Tài liệu này đặc tả việc thêm **hạn chót** (due date, không kèm giờ) cho
Task — tăng trưởng 🟡 thứ 2 sau mức ưu tiên (`Task_Priority_ToDo.md`).
Thiết kế lặp lại đúng pattern đã dùng cho Priority: mở rộng field trên
Task hiện có, không thêm entity/endpoint mới.

---

## 1. Mục tiêu & phạm vi của tăng trưởng này

**Trong phạm vi:**
- Thêm field `dueDate` (ngày, không có giờ; nullable — mặc định "không
  có hạn") vào Task.
- Đặt/xóa hạn chót khi **tạo** hoặc **sửa** task — cùng 1 form với
  title/description/priority, không phải thao tác riêng.
- Hiển thị hạn chót trong danh sách; cảnh báo trực quan khi **task đang
  mở (OPEN) và đã quá hạn**.

**Ngoài phạm vi của tăng trưởng này:**
- Nhắc nhở/thông báo (notification) khi gần tới hoặc quá hạn — thuộc
  nhóm 🔴 "Nhắc nhở & thông báo", cần hạ tầng `WorkManager`/permission
  hoàn toàn khác, không làm chung.
- Sắp xếp/lọc danh sách theo hạn chót — để tăng trưởng sau (cùng loại
  quyết định như đã hoãn "sort theo priority" ở `Task_Priority_ToDo.md`).
- "My Day" (nhóm 🔴) — phụ thuộc due date nhưng cần thêm logic gợi ý
  riêng, không phải chỉ thêm field.
- Giờ cụ thể trong ngày (chỉ có ngày, giống hầu hết to-do app đơn giản;
  nếu sau này cần nhắc theo giờ thì làm cùng lúc với tính năng Reminder).

---

## 2. Yêu cầu chức năng

Tiếp nối đánh số FR — `Task_Priority_ToDo.md` dùng FR-18/FR-19, nên tài
liệu này dùng FR-20/FR-21.

### FR-20 — Đặt/xóa hạn chót cho task (US-20)
**User Story**: Là người dùng, khi tạo hoặc sửa task, tôi muốn chọn một
ngày làm hạn chót (hoặc không chọn gì) để biết việc gì cần xong trước
ngày nào.

**Acceptance Criteria**:
- AC1: Form Tạo/Sửa task có ô "Hạn chót" mở `DatePickerDialog`
  (Material3), mặc định **không có hạn** (ô hiển thị "Chưa đặt hạn").
- AC2: Sau khi chọn ngày, hiện thêm nút nhỏ "Xóa hạn" để quay lại trạng
  thái không có hạn — AC "xóa" này áp dụng cho cả Create (bỏ lựa chọn
  vừa chọn) và Edit (xóa hạn đã lưu trước đó, gửi tường minh `null`,
  cùng cơ chế `description: null` đã dùng).
- AC3: Không chặn chọn ngày trong quá khứ — người dùng có thể ghi nhận
  một việc đã trễ hạn từ lúc tạo (không có yêu cầu nghiệp vụ nào cấm).
- AC4: Giữ giá trị đã chọn khi xoay màn hình/process death (cùng cơ chế
  `SavedStateHandle` đang dùng cho title/description/priority).
- AC5: Màn hình Sửa hiển thị đúng hạn chót hiện tại của task (tải từ
  server); task chưa có hạn hiển thị "Chưa đặt hạn".

### FR-21 — Hiển thị hạn chót & cảnh báo quá hạn trong danh sách (US-21)
**User Story**: Là người dùng, khi xem danh sách task, tôi muốn thấy
ngay hạn chót và nhận biết task nào đã quá hạn mà chưa làm.

**Acceptance Criteria**:
- AC1: Task có `dueDate` hiển thị 1 dòng phụ dưới title/description,
  dạng "Hạn: dd/MM/yyyy".
- AC2: Task **đang mở (OPEN)** và `dueDate` < ngày hôm nay (theo giờ
  thiết bị) → dòng "Hạn: ..." đổi màu cảnh báo (đỏ, dùng
  `MaterialTheme.colorScheme.error`) để nổi bật.
- AC3: Task **đã xong (DONE)** không hiển thị cảnh báo đỏ dù `dueDate`
  đã qua — chỉ hiển thị ngày bình thường (hoàn thành thì không còn là
  vấn đề "quá hạn").
- AC4: Task không có `dueDate` → không hiển thị dòng phụ này, giữ mật
  độ danh sách gọn (NFR-1, ~50 task phải cuộn mượt).
- AC5: Badge/màu áp dụng đúng ở cả 3 filter All/Open/Done đã có, không
  ảnh hưởng logic filter hiện tại.

---

## 3. Mô hình dữ liệu (Backend)

### 3.1 Migration (additive)

```sql
-- Migration ToDo feature extension — thêm cột "due_date" vào "tasks"
-- Chỉ NGÀY, không giờ -> dùng DATE (không TIMESTAMPTZ) để tránh lệch
-- timezone khi so sánh "quá hạn" ở client theo ngày lịch, không theo mốc
-- thời gian tuyệt đối.

ALTER TABLE "tasks"
  ADD COLUMN "due_date" DATE;
```

Không có `CHECK`/`NOT NULL` — "không có hạn" là trạng thái hợp lệ mặc
định (`NULL`), không cần default khác `NULL`. Không thêm index: không
có sort/filter theo `due_date` trong tăng trưởng này (mục 1).

### 3.2 Thay đổi các file hiện có

| File | Thay đổi |
|---|---|
| `task-record.interface.ts` | thêm `dueDate: Date \| null` |
| `task-result.interface.ts` | thêm `dueDate: string \| null` (xem mục 3.3 — serialize dạng `YYYY-MM-DD`, không phải ISO datetime đầy đủ như `createdAt`) |
| `task-repository.interface.ts` | `CreateTaskData`/`UpdateTaskDetailsData` thêm `dueDate?: Date \| null` |
| `prisma-task.repository.ts` | `create()`/`updateDetails()` truyền thêm `dueDate: data.dueDate` vào `data` Prisma |
| `dto/create-task.dto.ts` | thêm `@IsOptional() @IsDateString() dueDate?: string` |
| `dto/update-task.dto.ts` | thêm `@IsOptional() @IsDateString() dueDate?: string \| null` (cùng kiểu `@IsOptional` cho phép `null` tường minh như `description`) |
| `task.service.ts` | nhánh `updateDetails`: `dueDate: dto.dueDate === undefined ? task.dueDate : parseDateOrNull(dto.dueDate)` — cùng pattern `description` đã có cho "không gửi = giữ nguyên, gửi `null` = xóa" |

### 3.3 Serialize ngày — tránh bug lệch timezone

`toResult()` trong `task.service.ts` **không** dùng
`date.toISOString()` (trả cả phần giờ `T00:00:00.000Z`, gây hiểu lầm là
có giờ) mà dùng `date.toISOString().slice(0, 10)` để chỉ lấy
`YYYY-MM-DD`. Vì cột là `DATE` (Postgres không lưu giờ/timezone), giá
trị ngày luôn đúng theo lịch, không có rủi ro lệch ngày khi slice theo
cách này.

---

## 4. API (REST)

Không thêm endpoint mới — mở rộng request/response body của
`api/v1/tasks` đã có.

### 4.1 `POST /api/v1/tasks`
```json
{ "title": "Nộp báo cáo", "description": null, "priority": "medium", "dueDate": "2026-11-20" }
```
`dueDate` optional — không gửi = không có hạn.

### 4.2 `PUT /api/v1/tasks/:id`
```json
{ "title": "Nộp báo cáo", "description": null, "priority": "medium", "dueDate": null }
```
`dueDate: null` tường minh = xóa hạn đã đặt trước đó (cùng quy ước
`description: null`).

### 4.3 Response
```json
{
  "data": {
    "id": "uuid", "title": "...", "description": null,
    "status": "open", "priority": "medium", "dueDate": "2026-11-20",
    "createdAt": "...", "updatedAt": "..."
  }
}
```
`dueDate` là `null` khi task không có hạn.

### 4.4 Lỗi mới
| Status | code | Tình huống |
|---|---|---|
| 400 | `VALIDATION_ERROR` | `dueDate` không đúng định dạng ngày ISO (`@IsDateString`) |

---

## 5. Thiết kế Android

### 5.1 Domain
- `domain/model/Task.kt`: thêm `val dueDate: LocalDate?` (JDK thuần,
  cùng quyết định Phase 0 dùng `java.time.*` thay `kotlinx-datetime` —
  xem comment gốc trong `Task.kt`).
- `domain/repository/TaskRepository.kt`: `createTask`/`editTask` thêm
  tham số `dueDate: LocalDate?`.
- `CreateTaskUseCase.kt`/`UpdateTaskUseCase.kt`: `invoke()` thêm tham số
  `dueDate: LocalDate?`, truyền thẳng xuống repository — không có rule
  validate (AC3: không chặn ngày quá khứ).

### 5.2 Data
- `TaskDto.kt`: thêm `val dueDate: String? = null` (dạng `yyyy-MM-dd`).
- `CreateTaskRequestDto.kt`: thêm `val dueDate: String? = null` (có
  default — Create không có giá trị cũ cần phân biệt "không gửi" vs
  "gửi null", giống `description` ở DTO này).
- `UpdateTaskDetailsRequestDto.kt`: thêm `val dueDate: String?`
  **không có default** — cùng lý do đã ghi chú trong file này cho
  `description` (`encodeDefaults = false` sẽ lược field bằng default
  khỏi JSON, làm mất khả năng gửi `null` tường minh để xóa hạn).
- `TaskMapper.kt`: `dueDate?.let { LocalDate.parse(it) }` khi map vào
  domain; `dueDate?.toString()` khi map ra DTO (`LocalDate.toString()`
  mặc định đúng định dạng ISO `yyyy-MM-dd`, không cần `DateTimeFormatter`
  riêng).

### 5.3 Presentation — Form (Create/Edit)
- `TaskFormUiState.kt`: thêm `val dueDate: LocalDate? = null`.
- `TaskFormViewModel.kt`: thêm `onDueDateChange(value: LocalDate?)`
  (ghi `SavedStateHandle` bằng `value?.toEpochDay()` — kiểu `Long?`,
  SavedStateHandle hỗ trợ trực tiếp, không cần serialize chuỗi);
  `loadTaskForEdit()` set thêm `dueDate = task.dueDate`; `onSubmit()`
  truyền `state.dueDate` vào use case.
- `CreateTaskScreen.kt`/`EditTaskScreen.kt`: thêm 1 dòng dưới mức ưu
  tiên:
  - Chưa chọn: `TextButton` "+ Thêm hạn chót" → mở `DatePickerDialog`.
  - Đã chọn: `Text` hiển thị `dd/MM/yyyy` + icon "x" nhỏ để xóa (gọi
    `onDueDateChange(null)`).
  - Dùng `rememberDatePickerState()` + `DatePickerDialog` của
    `androidx.compose.material3` — đã có sẵn từ Compose BOM hiện tại
    (cùng BOM đang cấp `PullToRefreshBox`), **không** cần thêm
    dependency mới (nguyên tắc #3/#9).

### 5.4 Presentation — List
- `TaskUiModel.kt`: thêm `val dueDate: LocalDate?` và `val isOverdue:
  Boolean` — tính sẵn ở mapper (Presentation), **không** tính lại trong
  Composable, để `TaskRow` thuần hiển thị (giữ nguyên tắc Carmack: state
  đi một chiều, không suy luận lại trong UI).
- Mapper `Task.toUiModel()` (trong `TaskListViewModel.kt`):
  ```kotlin
  isOverdue = dueDate != null && status == TaskStatus.OPEN && dueDate.isBefore(LocalDate.now())
  ```
- `TaskListScreen.kt` (`TaskRow`): sau `descriptionPreview`, thêm
  ```kotlin
  task.dueDate?.let { date ->
      Text(
          text = "Hạn: " + date.format(dateFormatter),
          style = MaterialTheme.typography.bodySmall,
          color = if (task.isOverdue) MaterialTheme.colorScheme.error
                  else MaterialTheme.colorScheme.onSurfaceVariant
      )
  }
  ```
  (`dateFormatter` dùng `DateTimeFormatter.ofPattern("dd/MM/yyyy")`,
  khai báo `remember { }` 1 lần ở `TaskListScreen`, không tạo lại mỗi
  item).

---

## 6. Giao diện (mockup)

### 6.1 Create/Edit — thêm ô hạn chót
```
┌──────────────────────────────────────────┐
│ Tiêu đề: [ Nộp báo cáo                ]   │
│ Mô tả:   [ ...                        ]   │
│ Mức ưu tiên:  ( Cao ) [●TB] ( Thấp )      │
│ Hạn chót:  [ 20/11/2026 ]  [x Xóa hạn]    │
│                                            │
│              [  Lưu  ]                    │
└──────────────────────────────────────────┘
```
(Khi chưa chọn: `Hạn chót:  [+ Thêm hạn chót]`)

### 6.2 Danh sách — dòng phụ + cảnh báo quá hạn
```
┌──────────────────────────────────────────┐
│ ☐ 🔴 Họp review sprint                    │
│      Hạn: 18/11/2026  ← (màu đỏ, đã qua)  │
│ ☑ 🟡 Viết báo cáo tuần          (gạch)    │
│      Hạn: 10/11/2026  ← (màu thường,      │
│                           vì đã DONE)     │
│ ☐ 🟢 Dọn inbox                            │
│      (không có dòng Hạn — chưa đặt)       │
└──────────────────────────────────────────┘
```

---

## 7. Kiểm thử

**Backend** (Vitest):
- `task.service.spec.ts`: tạo task có/không `dueDate`; sửa task gửi
  `dueDate: null` → xóa hạn; sửa task không gửi `dueDate` → giữ nguyên;
  `dueDate` sai định dạng → `400 VALIDATION_ERROR`.
- Serialize: kiểm tra response trả đúng `YYYY-MM-DD`, không có phần giờ.

**Android**:
- `CreateTaskUseCaseTest`/`UpdateTaskUseCaseTest`: truyền `dueDate`,
  xác nhận gọi repository đúng tham số (gồm case `null`).
- `TaskFormViewModelTest`: chọn ngày → state cập nhật; xóa hạn → state
  về `null`; giữ giá trị sau rotation (SavedStateHandle); load Edit set
  đúng `dueDate` ban đầu.
- `TaskListViewModelTest` (mapper): 4 case `isOverdue` — OPEN quá hạn
  (true), OPEN chưa tới hạn (false), DONE quá hạn (false, theo AC3),
  không có `dueDate` (false).
- Instrumented: chọn ngày quá khứ ở Create → task mới hiện ngay cảnh
  báo đỏ trong danh sách.

---

## 8. Ngoài phạm vi (nhắc lại, tránh lấn phạm vi khi code)

Không làm trong tăng trưởng này: reminder/notification, sort/filter
theo hạn chót, "My Day", giờ cụ thể trong ngày. Xem mục 1.
