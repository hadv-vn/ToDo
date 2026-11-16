# Danh mục cho Task (Category) — ToDo feature extension

| | |
|---|---|
| **Phiên bản** | 1.0 |
| **Ngày tạo** | 2026-11-16 |
| **Trạng thái** | Đề xuất — chưa implement (HLD + LLD, chờ duyệt trước khi code) |
| **Code dự kiến** | `auth-core/backend/src/features/todo-task-module/` (module mới trong cùng feature) · `android/app/src/main/java/com/example/todo/` |
| **Liên quan** | `docs/SRS_ToDo_Android.md` FR-06/FR-11/FR-12 · `docs/Task_Priority_ToDo.md` (FR-18/19) · `docs/Task_DueDate_ToDo.md` (FR-20/21) |

Tài liệu này đặc tả **Danh mục** (category) do người dùng tự tạo để gán
cho task — tăng trưởng 🟡 thứ 3. Khác 2 tăng trưởng trước (chỉ thêm
field trên Task), đây là lần đầu cần **entity mới + CRUD riêng**, nên
tài liệu dài hơn và có thêm mục 3 (thiết kế module Category) trước khi
tới phần tích hợp vào Task.

---

## 1. Mục tiêu & phạm vi của tăng trưởng này

**Trong phạm vi:**
- Người dùng tạo/sửa/xóa danh mục của riêng mình: tên, màu, icon (emoji
  từ bộ cố định).
- Mỗi task thuộc **tối đa 1 danh mục** (nullable, không phải nhiều
  nhãn/tag) — xem quyết định ở mục 2.
- Gán/đổi/bỏ danh mục khi tạo hoặc sửa task.
- Hiển thị badge danh mục (màu + icon + tên) trong danh sách task.
- Xóa danh mục đang được task dùng → task đó chuyển về "Không danh mục"
  (không xóa task, không chặn xóa danh mục).

**Ngoài phạm vi của tăng trưởng này:**
- Gắn **nhiều** danh mục/nhãn cho 1 task (multi-tag) — nếu cần, đó là
  một mô hình dữ liệu khác (bảng nối N-N), để tăng trưởng riêng sau khi
  xác nhận thực sự cần.
- Lọc danh sách task theo danh mục — cùng lý do đã hoãn ở
  `Task_Priority_ToDo.md`/`Task_DueDate_ToDo.md`: nên làm 1 lần sau khi
  có đủ priority/dueDate/category để lọc kết hợp, không lặp lại 3 lần.
- "Nhóm task theo dự án" (Project, nhóm 🔴 trong đánh giá 2026-11-15) —
  Category ở đây **không** phải Project: Category chỉ là nhãn phân loại
  phẳng (flat), không có khái niệm cha-con/hierarchy, không đổi mô hình
  quyền sở hữu. Không nhầm 2 khái niệm này khi code.
- Icon picker tự do / color picker tự do — dùng bộ cố định (mục 3.1) để
  không cần thêm thư viện.

---

## 2. Quyết định mô hình dữ liệu (quan trọng, chốt trước khi code)

- **1 task → 0 hoặc 1 category** (không phải N-N). Lý do: input gốc chỉ
  nói "Danh mục hoặc nhãn tự tạo" ở mức đơn giản nhất; thêm bảng nối
  N-N (`task_categories`) là một quyết định kiến trúc lớn hơn (ảnh hưởng
  query, UI chọn nhiều, API shape) — theo nguyên tắc #3 (đơn giản tối
  đa), chọn phương án nhỏ nhất đáp ứng đúng yêu cầu đã cho, mở rộng lên
  N-N sau nếu người dùng thực sự cần gắn nhiều nhãn.
- **Xóa category → `SET NULL` trên `tasks.category_id`**, không
  `CASCADE` xóa task, không chặn xóa (`RESTRICT`). Lý do: dữ liệu task
  là của người dùng, một hành động quản lý danh mục không nên âm thầm
  xóa task (vi phạm "taste" — không có edge case ngầm nguy hiểm) và
  cũng không nên bị chặn xóa chỉ vì đang gắn với task cũ (trải nghiệm
  khó chịu, user phải gỡ từng task trước).
- **Category thuộc về 1 user** (không chia sẻ giữa user) — nhất quán
  ownership model hiện có của Task, không mở ra khái niệm dữ liệu dùng
  chung mới.

---

## 3. Yêu cầu chức năng

Tiếp nối FR — `Task_DueDate_ToDo.md` kết thúc ở FR-21.

### FR-22 — Quản lý danh mục: tạo, sửa, xóa (US-22)
**User Story**: Là người dùng, tôi muốn tự tạo các danh mục (ví dụ
"Công việc", "Cá nhân") với màu và icon riêng để phân loại task.

**Acceptance Criteria**:
- AC1: Màn hình "Quản lý danh mục" (vào từ icon trên `TaskListScreen`)
  liệt kê danh mục của người dùng, mỗi dòng có icon + tên + màu.
- AC2: Tạo danh mục: bắt buộc tên (≤ 50 ký tự, không rỗng), chọn 1 màu
  từ bộ 8 màu cố định, chọn 1 icon từ bộ emoji cố định (mục 4.1) — cả
  màu và icon có giá trị mặc định nếu người dùng không chọn gì.
- AC3: Sửa danh mục: đổi tên/màu/icon, task đang gán danh mục đó tự
  động hiển thị giá trị mới (vì chỉ lưu `categoryId`, không copy dữ
  liệu — xem mục 6.2).
- AC4: Xóa danh mục: có xác nhận (AlertDialog, cùng pattern dialog xóa
  task); sau khi xóa, mọi task đang gán danh mục đó hiển thị "Không
  danh mục" (mục 2).
- AC5: Trùng tên danh mục (trong cùng user) — **cho phép**, không có
  ràng buộc unique (không có yêu cầu nghiệp vụ nào cần tên duy nhất,
  không tự thêm ràng buộc không được yêu cầu — nguyên tắc #8).

### FR-23 — Gán danh mục cho task khi tạo/sửa (US-23)
**User Story**: Là người dùng, khi tạo hoặc sửa task, tôi muốn chọn 1
danh mục đã tạo (hoặc không chọn).

**Acceptance Criteria**:
- AC1: Form Tạo/Sửa task có hàng `FilterChip` ngang liệt kê danh mục
  của người dùng + 1 chip "Không danh mục" (mặc định được chọn).
- AC2: Chưa có danh mục nào → hàng chip chỉ có "Không danh mục" + 1 nút
  "+ Tạo danh mục" dẫn sang FR-22.
- AC3: Giữ lựa chọn khi xoay màn hình (SavedStateHandle, cùng cơ chế
  đã dùng cho priority/dueDate).
- AC4: Task đang gán 1 category **đã bị xóa** ở nơi khác → mở Edit vẫn
  hoạt động bình thường, hiển thị "Không danh mục" (vì server đã
  `SET NULL`, xem mục 2) — không có lỗi 404 riêng cho trường hợp này.

### FR-24 — Hiển thị danh mục trong danh sách task (US-24)
**User Story**: Là người dùng, khi xem danh sách, tôi muốn nhận ra
ngay task thuộc danh mục nào.

**Acceptance Criteria**:
- AC1: Task có category hiển thị 1 badge nhỏ (icon + tên, nền màu của
  category) ở cuối dòng tiêu đề.
- AC2: Task không có category → không hiển thị badge (giữ mật độ danh
  sách, NFR-1).
- AC3: Badge hiển thị đúng ở cả 3 filter All/Open/Done.

---

## 4. Thiết kế module Category (Backend)

### 4.1 Bộ màu & icon cố định

```ts
// category-appearance.constants.ts
export const CATEGORY_COLOR_VALUES = [
  '#E53935', '#FB8C00', '#FDD835', '#43A047',
  '#1E88E5', '#5E35B1', '#8D6E63', '#757575',
] as const;
export const CATEGORY_DEFAULT_COLOR = '#1E88E5';

export const CATEGORY_ICON_VALUES = [
  '📁', '💼', '🏠', '🛒', '📚', '⭐', '❤️', '🎯',
] as const;
export const CATEGORY_DEFAULT_ICON = '📁';
```
Validate bằng `@IsIn(...)` ở DTO — cùng pattern `TASK_STATUS_VALUES`.
Lưu icon dạng chuỗi emoji trực tiếp trong DB (không cần bảng tra cứu
riêng) — Android hiển thị emoji bằng `Text`, không cần icon font/thư
viện mới.

### 4.2 Migration (additive)

```sql
-- Migration ToDo feature extension — thêm bảng "categories" + cột
-- "category_id" trên "tasks".

CREATE TABLE "categories" (
    "id" UUID NOT NULL,
    "user_id" UUID NOT NULL,
    "name" VARCHAR(50) NOT NULL,
    "color" VARCHAR(10) NOT NULL DEFAULT '#1E88E5',
    "icon" VARCHAR(8) NOT NULL DEFAULT '📁',
    "created_at" TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "categories_pkey" PRIMARY KEY ("id")
);

CREATE INDEX "categories_user_id_idx" ON "categories"("user_id");

ALTER TABLE "categories" ADD CONSTRAINT "categories_user_id_fkey"
  FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE "tasks" ADD COLUMN "category_id" UUID;

ALTER TABLE "tasks" ADD CONSTRAINT "tasks_category_id_fkey"
  FOREIGN KEY ("category_id") REFERENCES "categories"("id")
  ON DELETE SET NULL ON UPDATE CASCADE;

CREATE INDEX "tasks_category_id_idx" ON "tasks"("category_id");
```
`ON DELETE SET NULL` thực hiện đúng quyết định mục 2 **ở tầng DB**,
không phải xử lý bằng code service (nguyên tắc #1 — để DB loại bỏ
trạng thái sai thay vì rải `if` kiểm tra).

### 4.3 File mới (mirror cấu trúc `todo-task-module` hiện có)

| File | Nội dung |
|---|---|
| `category-appearance.constants.ts` | mục 4.1 |
| `category-record.interface.ts` | `{ id, userId, name, color, icon, createdAt }` |
| `category-result.interface.ts` | giống `TaskResult` — không có `userId` |
| `dto/create-category.dto.ts` | `name` (`@IsNotEmpty @MaxLength(50)`), `color?`/`icon?` (`@IsOptional @IsIn(...)`) |
| `dto/update-category.dto.ts` | 3 field, tất cả `@IsOptional` (không có khái niệm "xóa" cho name/color/icon — luôn phải có giá trị, khác `description` của Task) |
| `exceptions/category-not-found.exception.ts` | 404 `CATEGORY_NOT_FOUND` |
| `exceptions/category-forbidden.exception.ts` | 403 `CATEGORY_FORBIDDEN` |
| `repositories/category-repository.interface.ts` | `findById`, `findManyByUser`, `create`, `update`, `delete` |
| `repositories/prisma-category.repository.ts` | implementation |
| `category.controller.ts` | `Controller('api/v1/categories')`, guard giống `TaskController` |
| `category.service.ts` | CRUD + `findOwnedCategoryOrThrow` (403/404, cùng pattern `TaskService.findOwnedTaskOrThrow`) |

### 4.4 Wiring vào `task.module.ts`
```ts
@Module({
  imports: [SessionModule, RbacModule],
  controllers: [TaskController, AdminTaskStatisticsController, CategoryController],
  providers: [
    TaskService, TaskStatisticsService, CategoryService,
    { provide: TASK_REPOSITORY, useClass: PrismaTaskRepository },
    { provide: TASK_STATISTICS_REPOSITORY, useClass: PrismaTaskStatisticsRepository },
    { provide: CATEGORY_REPOSITORY, useClass: PrismaCategoryRepository },
  ],
})
export class TaskModule {}
```
Category nằm **cùng module** `TaskModule`, không tách module riêng —
đây là 1 bounded context với Task (danh mục không có ý nghĩa gì ngoài
ngữ cảnh phân loại task), tách module riêng sẽ là lớp chia nhỏ không
cần thiết (nguyên tắc #9 — majestic monolith, không chia vì đang thịnh
hành).

### 4.5 Tích hợp vào Task hiện có

| File | Thay đổi |
|---|---|
| `task-record.interface.ts`/`task-result.interface.ts` | thêm `categoryId: string \| null` |
| `dto/create-task.dto.ts`/`update-task.dto.ts` | thêm `@IsOptional() @IsUUID() categoryId?: string \| null` |
| `task-repository.interface.ts` | `CreateTaskData`/`UpdateTaskDetailsData` thêm `categoryId?: string \| null` |
| `task.service.ts` | **Trước khi** gán `categoryId` (create hoặc update), nếu `categoryId` khác `null`/`undefined` → gọi `categoryService.findOwnedCategoryOrThrow(userId, categoryId)`. Không tồn tại hoặc thuộc user khác → ném lỗi (map thành `400 VALIDATION_ERROR` ở tầng Task, **không** phải 403/404 — vì đây là validate input "categoryId có hợp lệ với user này không", không phải thao tác trực tiếp lên resource category qua URL) |

Không cần endpoint "category kèm task" lồng nhau — Task chỉ lưu
`categoryId`; client tự tra cứu tên/màu/icon từ danh sách category đã
tải riêng (mục 5.4) — tách đúng trách nhiệm, tránh N+1/over-fetch.

---

## 5. Thiết kế Android

### 5.1 Domain
- **Mới** `domain/model/Category.kt`:
  ```kotlin
  data class Category(val id: String, val name: String, val color: String, val icon: String)
  ```
- **Mới** `domain/repository/CategoryRepository.kt`: `getCategories()`,
  `createCategory(name, color, icon)`, `updateCategory(id, name, color,
  icon)`, `deleteCategory(id)` — trả `ApiResult<...>`, cùng pattern
  `TaskRepository`.
- **Mới** `domain/usecase/category/`: `GetCategoriesUseCase`,
  `CreateCategoryUseCase`, `UpdateCategoryUseCase`,
  `DeleteCategoryUseCase` — mỗi UseCase 1 việc (nguyên tắc #3).
- `domain/model/Task.kt`: thêm `val categoryId: String?`.
- `TaskRepository.kt`/`CreateTaskUseCase.kt`/`UpdateTaskUseCase.kt`:
  thêm tham số `categoryId: String?`.

### 5.2 Data
- **Mới** `data/remote/dto/category/CategoryDto.kt`,
  `CreateCategoryRequestDto.kt`, `UpdateCategoryRequestDto.kt`,
  `CategoryEnvelopeDto.kt`, `CategoryListEnvelopeDto.kt` — mirror cấu
  trúc DTO của Task.
- **Mới** `data/remote/api/CategoryApiService.kt` (Retrofit, 4 method:
  GET/POST/PUT/DELETE `api/v1/categories`).
- **Mới** `data/repository/CategoryRepositoryImpl.kt`.
- **Mới** `data/mapper/CategoryMapper.kt`.
- `TaskDto.kt`/`CreateTaskRequestDto.kt`/`UpdateTaskDetailsRequestDto.kt`:
  thêm `categoryId: String?` (không có default ở 2 DTO update, cùng lý
  do `description`/`dueDate` đã ghi chú).
- `di/RepositoryModule.kt`: thêm
  `@Binds abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository`.

### 5.3 Presentation — màn hình Quản lý danh mục (mới)
Theo đúng convention hiện có (bộ đôi `*Screen.kt`/`*ViewModel.kt`/
`*UiState.kt` cho mỗi màn hình, nguyên tắc #9):
- `CategoryListUiState.kt`, `CategoryListViewModel.kt`,
  `CategoryListScreen.kt` — load danh sách, xóa (có dialog xác nhận,
  tái dùng đúng pattern `TaskListScreen` swipe/dialog), FAB "+" điều
  hướng sang tạo.
- `CategoryFormUiState.kt`, `CategoryFormViewModel.kt`,
  `CreateCategoryScreen.kt`, `EditCategoryScreen.kt` — mirror
  `TaskFormViewModel`/`CreateTaskScreen`/`EditTaskScreen` (dùng chung 1
  ViewModel cho Create/Edit qua `SavedStateHandle` đọc `categoryId`
  nav-arg, giống cách `TaskFormViewModel` đọc `taskId`). Form: ô tên +
  hàng 8 ô tròn màu (chọn 1) + hàng 8 emoji (chọn 1).

### 5.4 Presentation — tích hợp vào Task
- `Routes.kt`: thêm `CATEGORY_LIST = "category_list"`,
  `CREATE_CATEGORY = "create_category"`,
  `EDIT_CATEGORY = "edit_category/{categoryId}"` +
  `EDIT_CATEGORY_ARG_CATEGORY_ID`. `TaskListScreen` `TopAppBar` thêm
  `IconButton` điều hướng `CATEGORY_LIST` (cạnh nút "Đăng xuất").
- `TaskListViewModel.kt`: tải thêm danh sách category (song song với
  `loadTasks()`, qua `GetCategoriesUseCase`) để tra cứu tên/màu/icon khi
  map `TaskUiModel` — lưu vào 1 field `categories: List<Category>` của
  `TaskListUiState` (không lồng vào từng `TaskUiModel` để tránh copy dữ
  liệu trùng lặp cho nhiều task cùng category — tra cứu bằng
  `categoryId` tại thời điểm render).
- `TaskUiModel.kt`: thêm `val categoryId: String?` — **không** thêm
  `categoryName`/`categoryColor` trực tiếp vào đây (giữ UI model mỏng,
  tra cứu ở Composable qua `uiState.categories`, cùng tinh thần mục 4.5
  "không lồng category vào response Task").
- `TaskFormUiState.kt`/`TaskFormViewModel.kt`: thêm `val categoryId:
  String?`, `val availableCategories: List<Category>` (tải 1 lần khi
  mở form, qua `GetCategoriesUseCase`), `onCategorySelected(id: String?)`.
- `CreateTaskScreen.kt`/`EditTaskScreen.kt`: thêm hàng `FilterChip`
  ngang các category + chip "Không danh mục" (chip đầu, mặc định chọn).
- `TaskListScreen.kt` (`TaskRow`): thêm badge nhỏ cuối dòng title —
  `Box` nền màu `Color(category.color.toColorInt())` (dùng
  `androidx.core.graphics.toColorInt()`, đã có sẵn qua `core-ktx`,
  không cần parser hex tự viết) chứa `Text("${category.icon} ${category.name}")`.

---

## 6. Giao diện (mockup)

### 6.1 Màn hình Quản lý danh mục
```
┌──────────────────────────────────────────┐
│ ← Quản lý danh mục                        │
│ 💼 Công việc                        [🗑]  │
│ 🏠 Cá nhân                          [🗑]  │
│ 🛒 Mua sắm                          [🗑]  │
│                                       [+] │
└──────────────────────────────────────────┘
```

### 6.2 Tạo/sửa danh mục
```
┌──────────────────────────────────────────┐
│ Tên: [ Công việc                     ]    │
│ Màu:  🔴 🟠 🟡 🟢 🔵 🟣 🟤 ⚪ (chọn 1)      │
│ Icon: 📁 💼 🏠 🛒 📚 ⭐ ❤️ 🎯 (chọn 1)     │
│              [  Lưu  ]                    │
└──────────────────────────────────────────┘
```

### 6.3 Create/Edit Task — thêm hàng chọn danh mục
```
│ Danh mục:  [Không danh mục] [💼Công việc] [🏠Cá nhân]  │
```

### 6.4 Danh sách — badge danh mục
```
┌──────────────────────────────────────────┐
│ ☐ 🔴 Họp review sprint      [💼 Công việc]│
│ ☑ 🟡 Viết báo cáo tuần                    │
│ ☐ 🟢 Dọn inbox              [🏠 Cá nhân]  │
└──────────────────────────────────────────┘
```

---

## 7. Kiểm thử

**Backend** (Vitest):
- `category.service.spec.ts`: CRUD cơ bản; sửa/xóa category của user
  khác → 403/404; xóa category đang gắn task → task còn lại với
  `categoryId = null` (test qua `TaskRepository`, xác nhận hành vi
  `ON DELETE SET NULL` hoạt động đúng — test tích hợp với DB thật, không
  mock được FK behavior).
- `task.service.spec.ts`: tạo/sửa task với `categoryId` hợp lệ; `categoryId`
  thuộc user khác hoặc không tồn tại → `400 VALIDATION_ERROR`.

**Android**:
- `CategoryFormViewModelTest`, `CategoryListViewModelTest`: theo đúng
  khuôn các test ViewModel hiện có (MockK + Turbine/state trực tiếp).
- `TaskFormViewModelTest`: chọn category → state đúng; chọn "Không danh
  mục" → `categoryId = null`.
- `TaskListViewModelTest`: mapper gán đúng badge cho task dựa trên
  `categories` đã tải.
- Instrumented: tạo danh mục mới → quay lại Create Task → danh mục mới
  xuất hiện trong hàng chip ngay (không cần thoát app).

---

## 8. Ngoài phạm vi (nhắc lại, tránh lấn phạm vi khi code)

Không làm trong tăng trưởng này: multi-tag (N-N), lọc theo category,
Project/nhóm theo dự án (khác Category — xem mục 1), color/icon picker
tự do. Xem mục 1/2.
