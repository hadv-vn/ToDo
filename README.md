# To-Do CRUD — Android App + Backend API

Ứng dụng quản lý công việc (Task/To-Do) theo từng người dùng, gồm 2 phần:

- **`auth-core/`** — **git submodule**, trỏ tới repo [hadv-vn/Multi-user-System](https://github.com/hadv-vn/Multi-user-System) — cung cấp toàn bộ backend dùng chung (NestJS + Prisma + PostgreSQL, có sẵn Auth/Session/RBAC/Account/Admin) và một frontend Next.js riêng của hệ thống đó. Project này **không tự viết backend** mà dùng lại nguyên khối backend trong submodule này — xem chi tiết ở [mục auth-core](#auth-core-submodule) bên dưới.
- **`android/`** — Ứng dụng Android native (Kotlin + Jetpack Compose + Clean Architecture), gọi API từ backend trong `auth-core/`.

Toàn bộ tài liệu thiết kế (yêu cầu, kiến trúc, API contract) nằm ở [`docs/`](docs/):

| Tài liệu | Nội dung |
|---|---|
| [`docs/SRS_ToDo_Android.md`](docs/SRS_ToDo_Android.md) | Yêu cầu chức năng/phi chức năng, Data Model |
| [`docs/HLD_ToDo_Android.md`](docs/HLD_ToDo_Android.md) | Kiến trúc hệ thống, lựa chọn thư viện |
| [`docs/LLD_ToDo_Android.md`](docs/LLD_ToDo_Android.md) | Thiết kế class/module chi tiết, sequence diagram |
| [`docs/API_Contract_ToDo_Android.md`](docs/API_Contract_ToDo_Android.md) | Đặc tả request/response từng endpoint |

## Yêu cầu môi trường

| Công cụ | Phiên bản | Dùng cho |
|---|---|---|
| Node.js | 20.19+, 22.12+ hoặc 24+ (xem `.nvmrc` trong `auth-core/`) | Backend/frontend trong `auth-core/` |
| PostgreSQL | 14+ | Backend trong `auth-core/` (database) |
| Android Studio | bản mới nhất (Ladybug trở lên) | Android — tự tải Android SDK/Gradle khi mở project lần đầu |
| JDK | 17+ (Android Studio có sẵn) | Android |

## 1. auth-core (submodule)

`auth-core/` **không phải code của project này** — đây là một [git submodule](https://git-scm.com/book/en/v2/Git-Tools-Submodules) trỏ tới repo riêng [hadv-vn/Multi-user-System](https://github.com/hadv-vn/Multi-user-System). Project ToDo dùng lại nguyên khối backend (và frontend quản trị) của hệ thống đó thay vì tự viết, thay cho `backend/` (Express) cũ đã bị loại bỏ.

Khai báo trong [`.gitmodules`](.gitmodules):

```ini
[submodule "auth-core"]
	path = auth-core
	url = https://github.com/hadv-vn/Multi-user-System.git
```

### Lấy code submodule

```bash
# Khi clone project lần đầu — thêm --recursive để lấy luôn auth-core
git clone --recursive https://github.com/hadv-vn/ToDo.git

# Nếu đã clone trước đó mà chưa có nội dung trong auth-core/
git submodule update --init --recursive
```

### Cập nhật submodule lên commit mới nhất

```bash
cd auth-core
git pull origin main        # hoặc branch tương ứng của Multi-user-System
cd ..
git add auth-core
git commit -m "chore: bump auth-core submodule"
```

### Cài đặt & chạy

Toàn bộ hướng dẫn cài đặt, chạy, cấu hình `.env`, scripts build/run/stop, tài khoản admin mặc định, v.v. nằm trong **README của chính submodule**: [`auth-core/README.md`](auth-core/README.md). Tóm tắt kiến trúc:

- **Backend**: NestJS + Prisma + PostgreSQL, modular monolith (`auth`, `session`, `rbac`, `account`, `admin`, `email`...), API tại `/api/v1`, chạy ở cổng **8081**.
- **Frontend** (của riêng `auth-core`, không phải app Android): Next.js + Tailwind, chạy ở cổng **8080**.
- Session lưu phía server trong PostgreSQL (không dùng JWT) — xem ADR trong `auth-core/docs/`.

Chạy nhanh (Linux, theo scripts có sẵn trong submodule):

```bash
cd auth-core
./scripts/build-all.sh
./scripts/run-all.sh
```

> Android app trong `android/` gọi API từ backend này — nếu cổng/API base URL khác mặc định (`8081`), cập nhật `BASE_URL` trong `android/app/build.gradle.kts` cho khớp (xem mục [2. Chạy Android](#2-chạy-android)).

## 2. Chạy Android

1. Mở Android Studio → **Open** → chọn thư mục `android/`.
2. Đợi Android Studio tự đồng bộ Gradle và tải Android SDK còn thiếu (nếu có) trong lần mở đầu tiên.
3. Cấu hình địa chỉ Backend API nếu cần — mặc định ở `app/build.gradle.kts` (build type `debug`), `BASE_URL` trỏ tới `http://10.0.2.2:8081/` (cổng **8081** của backend NestJS trong `auth-core/`, nhìn từ Android Emulator tới máy host). Nếu chạy trên thiết bị thật, đổi thành địa chỉ IP LAN thật của máy chạy backend, ví dụ `http://192.168.1.10:8081/`.
4. Chạy app: chọn thiết bị ảo (emulator) hoặc thiết bị thật đã bật USB debugging → nhấn **Run**.
5. Đảm bảo backend trong `auth-core/` đang chạy (mục [1. auth-core (submodule)](#1-auth-core-submodule)) trước khi dùng app — app không có chế độ offline.

### Chạy test Android

- Unit test (JVM, không cần thiết bị): `./gradlew test` (Windows: `gradlew.bat test`)
- Compose UI test (cần emulator/thiết bị): `./gradlew connectedAndroidTest`

## Luồng sử dụng chính

1. Đăng ký tài khoản (email + mật khẩu) → tự động đăng nhập.
2. Tạo/xem/sửa/xóa task, đánh dấu hoàn thành (toggle), lọc theo trạng thái (Tất cả/Đang mở/Đã xong).
3. Quên mật khẩu → nhận OTP qua email (hoặc xem trong console log backend nếu chưa cấu hình SMTP) → đặt lại mật khẩu.
4. Đăng xuất.

## Cấu trúc thư mục

```
ToDo/
├── docs/              # Tài liệu thiết kế (SRS, HLD, LLD, API Contract) của app Android
├── .gitmodules        # Khai báo submodule auth-core
├── auth-core/         # GIT SUBMODULE → github.com/hadv-vn/Multi-user-System
│   ├── backend/           # NestJS + Prisma — Auth/Session/RBAC/Account/Admin, API /api/v1 (cổng 8081)
│   ├── frontend/          # Next.js + Tailwind — UI quản trị riêng của Multi-user-System (cổng 8080)
│   ├── docs/              # Tài liệu thiết kế/ADR của riêng auth-core
│   └── scripts/           # build-all.sh / run-all.sh / stop-all.sh / setup-database.sh
└── android/            # App Android — Kotlin + Jetpack Compose
    └── app/src/main/java/com/example/todo/
        ├── domain/         # Entity, UseCase, Repository interface (Kotlin thuần)
        ├── data/           # DTO, API service (Retrofit), Repository impl, local storage
        ├── presentation/   # Composable screens, ViewModel, Navigation
        └── di/             # Hilt modules
```
