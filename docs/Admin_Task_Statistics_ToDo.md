# Thống kê công việc cho ADMIN (ToDo feature extension)

| | |
|---|---|
| **Phiên bản** | 1.0 |
| **Ngày tạo** | 2026-11-05 |
| **Trạng thái** | Đã implement (backend + web admin trong `auth-core`) |
| **Code** | `auth-core/backend/src/features/todo-task-module/` · `auth-core/frontend/src/features/admin-task-stats/` |
| **Liên quan** | `auth-core/docs/requirements/srs-multi-user-system.md` (A-3, NFR-004, REQ-ADMIN-004) · `auth-core/docs/design/api-spec-multi-user-system.md` (mục 1, 5.5) |

Tài liệu này đặc tả phần **thống kê task** dành cho ADMIN trên web admin
của `auth-core`. Phần này thuộc nghiệp vụ ToDo, nên được đặc tả ở repo
ToDo; `auth-core` chỉ đặc tả phần chung (chi tiết tài khoản, REQ-ADMIN-004)
và cung cấp chỗ gắn mở rộng.

---

## 1. Nguyên tắc bảo mật dữ liệu (bắt buộc)

Task là dữ liệu cá nhân của người dùng. Theo `auth-core` SRS **A-3** và
**NFR-004**, ADMIN không có đặc quyền đọc dữ liệu cá nhân ở các module
nghiệp vụ. Vì vậy:

- ADMIN **chỉ** xem **số liệu tổng hợp**: tổng số task, số task theo trạng
  thái, mốc tạo task gần nhất.
- ADMIN **không bao giờ** xem `title`, `description` hay danh sách task cụ
  thể của người khác — qua API, giao diện, hay bất kỳ đường nào khác.
- Ở tầng dữ liệu, repository thống kê chỉ dùng `groupBy`/`aggregate`/
  `count`, không bao giờ đọc bản ghi task. Unit test kiểm tra điều này.
- API task của người dùng (`/api/v1/tasks`) giữ nguyên: mọi thao tác luôn
  lọc theo `userId` của người đang đăng nhập, kể cả khi người đó là ADMIN.

Tra cứu nội dung task trực tiếp trong database (khi xử lý sự cố) là hoạt
động vận hành ngoài ứng dụng, phải có lý do rõ ràng, chỉ đọc, và nên dùng
tài khoản DB chỉ có quyền `SELECT`.

---

## 2. Yêu cầu chức năng

### REQ-TODO-ADMIN-001 — Thống kê công việc của một người dùng
**User Story**: Là ADMIN, khi xem chi tiết một tài khoản, tôi muốn biết
mức độ sử dụng ứng dụng của người đó mà không xem nội dung công việc.

**Acceptance Criteria**:
- AC1: Trang chi tiết người dùng (`auth-core` S8b) hiển thị card "Thống kê
  công việc": tổng số task, số đang mở, số hoàn thành, thời điểm tạo task
  gần nhất, và tỉ lệ hoàn thành.
- AC2: Người dùng chưa có task → hiển thị "Người dùng chưa tạo công việc nào."
- AC3: Card không hiển thị và API không trả về `title`/`description` (mục 1).
- AC4: Card lỗi không làm hỏng phần còn lại của trang chi tiết; có nút "Thử lại".
- AC5: Người gọi role USER bị từ chối (403).

### REQ-TODO-ADMIN-002 — Tổng quan công việc toàn hệ thống
**User Story**: Là ADMIN, tôi muốn xem tổng quan số lượng công việc của
cả hệ thống và của từng người dùng.

**Acceptance Criteria**:
- AC1: Trang `/admin/task-stats` hiển thị tổng toàn hệ thống (tổng, đang
  mở, hoàn thành, tỉ lệ hoàn thành). Số liệu này không phụ thuộc bộ lọc.
- AC2: Bảng người dùng gồm email, tổng, đang mở, hoàn thành, tỉ lệ hoàn
  thành; sắp theo số task giảm dần, rồi email tăng dần; gồm cả người dùng
  chưa có task.
- AC3: Tìm theo email (chuỗi con, không phân biệt hoa thường) và phân
  trang 20 dòng/trang, giống trang quản trị người dùng.
- AC4: Click email → trang chi tiết người dùng (S8b).
- AC5: Không hiển thị nội dung task (mục 1). USER bị từ chối (403).

---

## 3. API

Quy ước chung (envelope, lỗi, phân trang, xác thực bằng cookie session) theo
`auth-core/docs/design/api-spec-multi-user-system.md` mục 1. Cả hai
endpoint chỉ đọc, chỉ dành cho ADMIN, không yêu cầu CSRF.

### 3.1 `GET /api/v1/admin/users/{id}/task-stats`

**Path param**: `id` — UUID người dùng.

**Response `200 OK`**
```json
{
  "data": {
    "userId": "uuid",
    "total": 12,
    "byStatus": { "open": 7, "done": 5 },
    "lastCreatedAt": "2026-11-04T09:00:00Z"
  }
}
```
- `byStatus` luôn có đủ khóa của mọi trạng thái task hợp lệ (mặc định `0`).
  Thêm trạng thái mới cho task sẽ tự thêm khóa tương ứng.
- `lastCreatedAt` là `null` nếu người dùng chưa có task.

**Response lỗi**
| Status | code | Tình huống |
|---|---|---|
| 400 | `VALIDATION_ERROR` | `id` không đúng định dạng UUID |
| 401 | `UNAUTHENTICATED` | không có session hợp lệ |
| 403 | `FORBIDDEN` | người gọi role `USER` |
| 404 | `USER_NOT_FOUND` | không tìm thấy người dùng (cùng mã với `auth-core` để FE xử lý thống nhất) |

### 3.2 `GET /api/v1/admin/task-stats`

**Query param**
- `page`: integer, optional, default `1`, min `1`.
- `limit`: integer, optional, default `20`, max `100`.
- `email`: string, optional, tối đa 255 ký tự — lọc theo chuỗi con, không phân biệt hoa thường.

**Response `200 OK`**
```json
{
  "data": {
    "summary": { "total": 14, "byStatus": { "open": 10, "done": 4 } },
    "items": [
      { "userId": "uuid-a", "email": "a@example.com", "total": 5, "byStatus": { "open": 3, "done": 2 } },
      { "userId": "uuid-b", "email": "b@example.com", "total": 0, "byStatus": { "open": 0, "done": 0 } }
    ]
  },
  "meta": { "page": 1, "limit": 20, "totalItems": 41, "totalPages": 3 }
}
```
- `summary`: tổng toàn hệ thống, không phụ thuộc `email`/`page`.
- `items`: người dùng khớp bộ lọc, sắp theo số task giảm dần rồi email tăng dần.
- `meta.totalItems`: số người dùng khớp bộ lọc.

**Response lỗi**
| Status | code | Tình huống |
|---|---|---|
| 400 | `VALIDATION_ERROR` | `page`/`limit`/`email` không hợp lệ |
| 401 | `UNAUTHENTICATED` | không có session hợp lệ |
| 403 | `FORBIDDEN` | người gọi role `USER` |

---

## 4. Giao diện (web admin của `auth-core`)

Theo nguyên tắc thị giác của `auth-core` (dark theme, glassmorphism, neon).

### 4.1 Card "Thống kê công việc" trên trang chi tiết người dùng
Gắn vào vùng mở rộng `extraSections` của S8b; file route
`app/(authenticated)/admin/users/[id]/page.tsx` ghép card vào trang.

```
┌──────────────────────────────────────────────────────────────┐
│ THỐNG KÊ CÔNG VIỆC                                           │
│  ┌────────┐ ┌──────────┐ ┌────────────┐ ┌──────────────────┐ │
│  │ TỔNG   │ │ ĐANG MỞ  │ │ HOÀN THÀNH │ │ TẠO GẦN NHẤT     │ │
│  │  12    │ │    7     │ │     5      │ │ 04/11/2026 09:00 │ │
│  └────────┘ └──────────┘ └────────────┘ └──────────────────┘ │
│  [████████░░░░░░░] 42% hoàn thành                            │
│  ⓘ Nội dung công việc là dữ liệu riêng tư của người dùng,    │
│    quản trị viên không xem được.                             │
└──────────────────────────────────────────────────────────────┘
```

### 4.2 Trang `/admin/task-stats`
Mục "Thống kê công việc" trong Sidebar, chỉ hiện với ADMIN.

```
┌──────────────────────────────────────────────────────────────┐
│ Thống kê công việc                                           │
│ ┌ TOÀN HỆ THỐNG ───────────────────────────────────────────┐ │
│ │  TỔNG 14      ĐANG MỞ 10      HOÀN THÀNH 4               │ │
│ │  [█████░░░░░░░░] 29% hoàn thành                          │ │
│ └──────────────────────────────────────────────────────────┘ │
│ 🔍 [ tìm theo email...        ]                              │
│ Email           Tổng  Đang mở  Hoàn thành  Tỉ lệ hoàn thành  │
│ a@example.com     5        3           2   [███░░] 40%       │
│ b@example.com     0        0           0   —                 │
│                     ‹ Trang 1 / 3 ›                          │
│ ⓘ Chỉ hiển thị số liệu tổng hợp...                           │
└──────────────────────────────────────────────────────────────┘
```

| Trạng thái | Hiển thị |
|---|---|
| Loading | Bảng skeleton; card tổng hiện "—" để giữ khung, không nháy layout. |
| Empty | "Chưa có người dùng nào." hoặc "Không tìm thấy người dùng nào khớp với …" + "Xóa tìm kiếm". |
| Error | Card lỗi "Không thể tải thống kê công việc" + "Thử lại". |
| Forbidden | "Không đủ quyền truy cập" + "Về trang hồ sơ cá nhân". |

---

## 5. Thiết kế kỹ thuật (tóm tắt)

- **Ranh giới module**: toàn bộ code thống kê nằm trong feature module
  ToDo. `AdminModule` lõi và trang admin lõi của `auth-core` không import
  gì từ Task; frontend ghép card ở tầng route qua prop `extraSections`.
- **Backend** (`todo-task-module/`): `AdminTaskStatisticsController`
  (guard `AuthGuard` + `RolesGuard` + `@Roles(ADMIN)`),
  `TaskStatisticsService`, `TaskStatisticsRepository` (interface riêng,
  tách khỏi `TaskRepository` CRUD) và `PrismaTaskStatisticsRepository`,
  hàm dùng chung `summarizeTaskStatusCounts`. Không có migration mới.
- **Frontend**: `lib/admin-task-stats/admin-task-stats-api.ts`,
  `features/admin-task-stats/` (`UserTaskStatsCard`,
  `TaskStatsOverviewPage`, `TaskStatsUsersTable`, `TaskCompletionBar`,
  `TaskStatTile`, các hook), route `app/(authenticated)/admin/task-stats`.

## 6. Kiểm thử

- Unit test backend: `task-statistics.service.spec.ts`,
  `prisma-task-statistics.repository.spec.ts`, `task-status-summary.spec.ts`
  — gồm kiểm tra không đọc bản ghi task và không lộ `title`/`description`.
- Đã chạy truy vấn thật trên Postgres local (chỉ đọc) cho cả hai endpoint.
- Chưa có: e2e qua HTTP với tài khoản ADMIN đã đăng nhập, `next build`, và
  kiểm tra trực quan trên trình duyệt.
