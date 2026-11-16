# Mức ưu tiên cho Task (Priority) — ToDo feature extension

| | |
|---|---|
| **Phiên bản** | 1.0 |
| **Ngày tạo** | 2026-11-15 |
| **Trạng thái** | Đề xuất — chưa implement (HLD + LLD, chờ duyệt trước khi code) |
| **Code dự kiến** | `auth-core/backend/src/features/todo-task-module/` (backend) · `android/app/src/main/java/com/example/todo/` (Android) |
| **Liên quan** | `docs/SRS_ToDo_Android.md` FR-06/FR-11/FR-12 · `docs/HLD_ToDo_Android.md` · `docs/LLD_ToDo_Android.md` mục 3.2/3.4/3.5/4.1/4.2 |

Tài liệu này đặc tả việc thêm **mức ưu tiên** (Cao/Trung bình/Thấp) cho
Task — mục được đánh giá khả thi ở mức 🟡 "Vừa" trong đánh giá tính năng
2026-11-15 (fit thẳng vào pattern `status` đã có sẵn, không cần entity
mới, không đổi mô hình quyền sở hữu).

---

## 1. Mục tiêu & phạm vi của tăng trưởng này

**Trong phạm vi:**
- Thêm field `priority` (giá trị: `low` | `medium` | `high`, mặc định
  `medium`) vào Task.
- Gán priority khi **tạo** task; sửa priority khi **sửa** task — cùng 1
  form với title/description, không phải thao tác riêng.
- Hiển thị priority bằng badge màu trong danh sách task.

**Ngoài phạm vi của tăng trưởng này** (để làm tăng trưởng kế tiếp,
không trộn vào đây theo nguyên tắc Kent Beck "bước nhỏ"):
- Tự động sắp xếp danh sách theo priority (thuộc nhóm 🟡 "kéo thả sắp
  xếp" — sẽ cần field `order` riêng, xử lý cùng lúc hợp lý hơn).
- Lọc danh sách theo priority ("bộ lọc thông minh" — thuộc nhóm 🔴, nên
  làm sau khi có cả category/due date để lọc kết hợp một lần).
- Due date, "My Day", category/label — các mục 🟡/🔴 khác, không phụ
  thuộc lẫn nhau với priority.

---

## 2. Yêu cầu chức năng

Tiếp nối đánh số FR của SRS (FR-17 là FR cuối hiện có).

### FR-18 — Gán/sửa mức ưu tiên cho task (US-18)
**User Story**: Là người dùng, khi tạo hoặc sửa task, tôi muốn chọn mức
độ quan trọng của nó để biết việc nào cần làm trước.

**Acceptance Criteria**:
- AC1: Màn hình Tạo task có bộ chọn 3 mức Cao/Trung bình/Thấp, mặc định
  chọn **Trung bình**.
- AC2: Màn hình Sửa task hiển thị đúng mức priority hiện tại của task
  (tải từ server), cho phép đổi sang mức khác.
- AC3: Không chọn gì ở màn hình Tạo (giữ mặc định) → server lưu
  `medium`, giống hành vi `status` hiện có (client không gửi, server tự
  mặc định).
- AC4: Đổi priority không ảnh hưởng `status` (open/done) — hai field độc
  lập, cùng pattern cách `editTask` không đụng `status` hiện nay.
- AC5: Giữ priority đã chọn khi xoay màn hình (cùng cơ chế
  `SavedStateHandle` đang dùng cho title/description).

### FR-19 — Hiển thị mức ưu tiên trong danh sách task (US-19)
**User Story**: Là người dùng, khi xem danh sách task, tôi muốn nhận ra
ngay task nào ưu tiên cao mà không cần mở từng task.

**Acceptance Criteria**:
- AC1: Mỗi dòng task trong danh sách hiển thị 1 badge màu tương ứng
  priority (Cao = đỏ, Trung bình = vàng cam, Thấp = xanh lá — xem mục 5).
- AC2: Thứ tự hiển thị danh sách **không đổi** so với hiện tại
  (`createdAt desc`) — xem mục 1, sắp xếp theo priority là tăng trưởng
  khác.
- AC3: Badge hiển thị đúng ở cả 3 trạng thái filter (All/Open/Done) đã
  có.

---

## 3. Mô hình dữ liệu (Backend)

### 3.1 Migration (additive, không sửa migration cũ)

```sql
-- Migration ToDo feature extension — thêm cột "priority" vào "tasks"

ALTER TABLE "tasks"
  ADD COLUMN "priority" VARCHAR(10) NOT NULL DEFAULT 'medium';

ALTER TABLE "tasks"
  ADD CONSTRAINT "tasks_priority_check" CHECK ("priority" IN ('low', 'medium', 'high'));
```

Không thêm index mới: NFR-1 của SRS chỉ cam kết mượt với ~50 task/người
dùng, danh sách đã luôn lọc theo `user_id` qua `tasks_user_id_idx` có
sẵn; thêm index trên cột ít phân biệt giá trị (3 giá trị) không có lợi
ích đo được ở quy mô này (nguyên tắc #7 — không tối ưu khi chưa đo).

### 3.2 Constants (mirror `task-status.constants.ts`)

```ts
// task-priority.constants.ts
export const TASK_PRIORITY_VALUES = ['low', 'medium', 'high'] as const;
export type TaskPriority = (typeof TASK_PRIORITY_VALUES)[number];
export const TASK_DEFAULT_PRIORITY: TaskPriority = 'medium';
```

### 3.3 Thay đổi các file hiện có

| File | Thay đổi |
|---|---|
| `task-record.interface.ts` | thêm `priority: string` |
| `task-result.interface.ts` | thêm `priority: string` |
| `task-repository.interface.ts` | `CreateTaskData`/`UpdateTaskDetailsData` thêm `priority?: string` (optional — service tự điền default khi thiếu) |
| `prisma-task.repository.ts` | `create()` dùng `data.priority ?? TASK_DEFAULT_PRIORITY` thay vì hard-code; `updateDetails()` thêm `priority: data.priority` vào `data` |
| `dto/create-task.dto.ts` | thêm `@IsOptional() @IsIn(TASK_PRIORITY_VALUES) priority?: string` |
| `dto/update-task.dto.ts` | thêm `@IsOptional() @IsIn(TASK_PRIORITY_VALUES) priority?: string` |
| `task.service.ts` | `create()`: truyền `dto.priority ?? undefined` xuống repository (repository tự default); `update()`: nhánh `typeof dto.title === 'string'` truyền thêm `priority: dto.priority ?? task.priority` vào `updateDetails` — **không** thêm nhánh thứ 3, vì priority luôn đi cùng title/description trong cùng 1 submit form (xem mục 1 — không phải thao tác độc lập như `status`) |

**Không thêm method repository mới** — tái dùng `updateDetails`, giữ
đúng nguyên tắc #3 (đơn giản tối đa): priority là một phần nội dung
task, không phải một hành động nghiệp vụ riêng như toggle status.

---

## 4. API (REST)

Không thêm endpoint mới — mở rộng response/request body của các
endpoint `api/v1/tasks` đã có.

### 4.1 `POST /api/v1/tasks`
**Request** (field mới, optional):
```json
{ "title": "Việc cần làm", "description": null, "priority": "high" }
```

### 4.2 `PUT /api/v1/tasks/:id` (nhánh sửa nội dung)
**Request** (field mới, optional — nếu không gửi, giữ priority hiện tại,
giống cách `description` giữ nguyên khi không gửi):
```json
{ "title": "Việc cần làm", "description": null, "priority": "low" }
```

### 4.3 Response (mọi endpoint trả Task)
```json
{
  "data": {
    "id": "uuid", "title": "...", "description": null,
    "status": "open", "priority": "medium",
    "createdAt": "...", "updatedAt": "..."
  }
}
```

### 4.4 Lỗi mới
| Status | code | Tình huống |
|---|---|---|
| 400 | `VALIDATION_ERROR` | `priority` không thuộc `low`/`medium`/`high` |

Không có lỗi mới khác — 401/403/404 giữ nguyên hành vi đã có (ownership
enforcement không đổi).

---

## 5. Thiết kế Android

### 5.1 Domain (Kotlin thuần)
- **Mới**: `domain/model/TaskPriority.kt`
  ```kotlin
  enum class TaskPriority { LOW, MEDIUM, HIGH }
  ```
  (mirror `TaskStatus.kt` — comment giải thích mapping wire value thuộc
  tầng Data, không phải Domain).
- `domain/model/Task.kt`: thêm `val priority: TaskPriority`.
- `domain/repository/TaskRepository.kt`: `createTask`/`editTask` thêm
  tham số `priority: TaskPriority`.
- `domain/usecase/task/CreateTaskUseCase.kt`,
  `UpdateTaskUseCase.kt`: `invoke()` thêm tham số `priority:
  TaskPriority`, truyền thẳng xuống repository (không có rule nghiệp vụ
  nào cần validate — mọi giá trị enum đều hợp lệ, khác validate
  `title.isEmpty()`).

### 5.2 Data
- `data/remote/dto/task/TaskDto.kt`: thêm `val priority: String`.
- `data/remote/dto/task/CreateTaskRequestDto.kt`: thêm `val priority:
  String` (luôn gửi tường minh giá trị wire của `TaskPriority.MEDIUM`
  khi người dùng không đổi gì — đơn giản hơn làm field optional ở
  Android, vì UI luôn có giá trị mặc định sẵn, không có khái niệm "chưa
  chọn").
- `data/remote/dto/task/UpdateTaskDetailsRequestDto.kt`: thêm `val
  priority: String` — cùng lý do field `title`/`description` hiện tại
  không có default: luôn serialize tường minh, tránh đúng cái bug
  `encodeDefaults=false` đã ghi chú trong file này.
- `data/mapper/TaskMapper.kt`: thêm `toDomainTaskPriority()` / `
  TaskPriority.toWireValue()` cùng pattern với `TaskStatus`.

### 5.3 Presentation — Form (Create/Edit)
- `TaskFormUiState.kt`: thêm `val priority: TaskPriority =
  TaskPriority.MEDIUM`.
- `TaskFormViewModel.kt`: thêm `onPriorityChange(value: TaskPriority)`
  (cùng pattern `onTitleChange`/`onDescriptionChange`, ghi vào
  `SavedStateHandle` bằng key mới `KEY_PRIORITY` lưu `.name` dạng
  String); `loadTaskForEdit()` set thêm `priority = task.priority`;
  `onSubmit()` truyền `state.priority` vào use case.
- `CreateTaskScreen.kt`/`EditTaskScreen.kt`: thêm 1 dòng 3
  `FilterChip`/`SegmentedButton` Cao/Trung bình/Thấp ngay dưới
  description — tái dùng đúng component `FilterChip` đã dùng cho
  All/Open/Done ở `TaskListScreen.kt` (nguyên tắc #9 — quy ước thay cho
  cấu hình, không nhập component mới).

### 5.4 Presentation — List
- `TaskUiModel.kt`: thêm `val priority: TaskPriority`.
- Mapper `Task.toUiModel()` (trong `TaskListViewModel.kt`): map thêm
  field.
- `TaskListScreen.kt`: mỗi item thêm 1 `Box` tròn nhỏ màu theo priority
  cạnh checkbox (không thêm text để giữ mật độ danh sách — NFR-1, danh
  sách ~50 task phải gọn).
- `presentation/theme/Color.kt`: thêm 3 màu mới
  `PriorityHigh = Color(0xFFE53935)`,
  `PriorityMedium = Color(0xFFFFA000)`,
  `PriorityLow = Color(0xFF43A047)`.

---

## 6. Giao diện (mockup)

### 6.1 Create/Edit — thêm bộ chọn priority
```
┌──────────────────────────────────────────┐
│ Tiêu đề: [ Họp review sprint          ]   │
│ Mô tả:   [ ...                        ]   │
│ Mức ưu tiên:                              │
│   ( Cao )  [●Trung bình]  ( Thấp )        │
│                                            │
│              [  Lưu  ]                    │
└──────────────────────────────────────────┘
```

### 6.2 Danh sách — badge màu
```
┌──────────────────────────────────────────┐
│ ☐ 🔴 Họp review sprint                    │
│ ☑ 🟡 Viết báo cáo tuần          (gạch)    │
│ ☐ 🟢 Dọn inbox                            │
└──────────────────────────────────────────┘
```

---

## 7. Kiểm thử

**Backend** (Vitest, cùng vị trí file hiện có):
- `task.service.spec.ts`: bổ sung case tạo task không gửi `priority` →
  mặc định `medium`; sửa task có gửi `priority` → cập nhật đúng; sửa task
  không gửi `priority` → giữ nguyên giá trị cũ.
- DTO validation: `priority` sai giá trị → `400 VALIDATION_ERROR`
  (test ở tầng e2e hoặc unit DTO nếu project có thói quen test riêng DTO).

**Android**:
- `CreateTaskUseCaseTest`/`UpdateTaskUseCaseTest`: truyền priority, xác
  nhận gọi repository với đúng tham số.
- `TaskFormViewModelTest`: `onPriorityChange` cập nhật state đúng; giữ
  giá trị sau "rotation" (SavedStateHandle); submit gửi đúng priority
  đang chọn; load Edit set đúng priority ban đầu.
- `TaskListViewModelTest`: mapper gán đúng `priority` vào `TaskUiModel`.
- Instrumented (`TaskListScreenTest`/`TaskFormScreenTest` nếu có): chọn
  chip priority khác Trung bình → submit → task mới hiện đúng badge màu.

---

## 8. Ngoài phạm vi (nhắc lại, tránh lấn phạm vi khi code)

Không làm trong tăng trưởng này: sắp xếp/lọc theo priority, due date,
"My Day", kéo-thả, category/label. Xem mục 1.
