# ToDo — AI Transformation Health Check

Dashboard tĩnh (`index.html`) đo "sức khoẻ kho kiến thức" của project ToDo
theo tiêu chí tại `D:\MyStartUp\AI Transformation\docs\A-khoi-dong\AI-Transformation-Program.md`
§8.1 (KPI dự án) và §8.2 (Knowledge Health c1–c7). Mở trực tiếp `index.html`
bằng browser, không cần server.

## Dữ liệu lấy từ đâu

Toàn bộ số liệu trong `index.html` được tính tay từ `git log` + `grep` trên
chính repo này tại commit `c55147a` (2026-11-23), **không suy diễn/bịa số**
— điều Program nhắc riêng ở EX-05. 4 KPI trong §8.1 (Review Time, Estimation
Accuracy, Nén, Hiệu quả token) không có nguồn dữ liệu thật (không có file
telemetry/`DECISION-LOG-*.md` nào trong repo) nên để trống thay vì ước lượng.

## Cách tính lại từng chỉ số (để kiểm chứng hoặc cập nhật)

```bash
# Traceability (c1): liệt kê mã FR/NFR trong docs/, rồi tìm lại trong
# Test_Scenarios + Admin_Task_Statistics + KDoc trong android/app/src
grep -rhoE "FR-[0-9]+|NFR-[0-9]+" docs/*.md | sort -u -V
grep -rhoE "FR-[0-9]+|NFR-[0-9]+" docs/Test_Scenarios_ToDo_Android.md \
  docs/Admin_Task_Statistics_ToDo.md android/app/src | sort -u -V

# Change Coupling (c2): với mỗi commit đổi android/app/src/main, có đổi docs/ không
git log --format=%H | while read h; do
  git show --name-only --format="" "$h" | grep -q "^android/app/src/main/" && \
  echo "$h: $(git show --name-only --format='' "$h" | grep -c '^docs/') doc-file(s)"
done

# Orphan Rate (c4): UseCase/ViewModel/RepositoryImpl không có *Test.kt tương ứng
for f in $(find android/app/src/main -name "*ViewModel.kt" -o -name "*UseCase.kt" \
  -o -iname "*RepositoryImpl.kt"); do
  base=$(basename "$f" .kt)
  find android/app/src/test -name "${base}Test.kt" | grep -q . || echo "NO TEST: $base"
done

# Decision Coverage (c6): có ADR/Decision Log nào không
find . -iname "*adr*" -o -iname "*decision-log*" | grep -v node_modules

# AI Adoption / Rework Rate (proxy, §8.1): từ git log
git log --format=%s | grep -c "^fix:"
git log --format=%H | while read h; do git log -1 --format=%B "$h" | \
  grep -qi "Co-Authored-By: Claude" && echo "$h"; done | wc -l
```

## Cập nhật

- **2026-11-23**: thêm `docs/adr/0001-task-manual-reorder-sort-key.md` (ADR
  đầu tiên của repo ToDo, theo đúng khuôn `auth-core` đang dùng) để sửa một
  phần finding `c6 Decision Coverage` — từ 0/5 lên 1/5 (0% → 20%).
- **2026-11-23**: thêm `docs/adr/0002-android-session-cookie-storage.md`
  (quyết định CookieJar mã hóa AndroidKeyStore thay JWT Bearer, commit
  `53d6d66`) — `c6` lên 2/5 (40%).
- **2026-11-23**: thêm `docs/adr/0003-auth-core-shared-backend-submodule.md`
  (quyết định dùng chung backend qua git submodule thay vì tự viết, commit
  `4eacf29` — quyết định ảnh hưởng rộng nhất, gốc của cả ADR 0001/0002) —
  `c6` lên 3/5 (60%).
- **2026-11-23**: thêm `docs/adr/0004-android-clean-architecture-foundation.md`
  (Clean Architecture 3 layer + MVVM + Hilt + Compose, online-only, commit
  `659a35b`) và `docs/adr/0005-admin-task-statistics-aggregate-only.md`
  (ADMIN chỉ xem số liệu tổng hợp, không bao giờ đọc nội dung task, commit
  `eb94e26`) — `c6` lên **5/5 (100%)**, đã phủ hết 5 thay đổi `feat:` lớn
  ban đầu. Lưu ý: cả 5 ADR đều viết retroactive (sau khi code đã chạy) —
  bước tiếp theo là viết ADR *trước khi* code cho quyết định kế tiếp.
- **2026-11-23**: viết 9 unit test còn thiếu (`LoginUseCaseTest`,
  `LogoutUseCaseTest`, `ForgotPasswordUseCaseTest`, `ResetPasswordUseCaseTest`,
  `ResendVerificationUseCaseTest`, `GetTasksUseCaseTest`,
  `GetTaskByIdUseCaseTest`, `DeleteTaskUseCaseTest`,
  `ToggleTaskStatusUseCaseTest`), `./gradlew testDebugUnitTest` xanh toàn
  bộ — `c4 Orphan Rate` 25.0% (17/68) → **11.8% (8/68)**, dưới mốc 15%;
  `c7 Retrieval Quality` 80% (4/5) → **100% (5/5)** (bộ ba spec+code+test
  của `ToggleTaskStatusUseCase` nay đủ). Phần "orphan req" còn lại (8 NFR,
  xem `c1`) chưa xử lý — là việc review tay, không phải viết test.
- **2026-11-23**: cập nhật 5 doc stale (`SRS`, `HLD`, `LLD`,
  `API_Contract`, `Test_Scenarios`) thêm mục "CẬP NHẬT 2026-11-16" trỏ
  sang 4 doc tính năng (Category/DueDate/Priority/Reorder) + mục 7 mới
  trong `Test_Scenarios` (20 dòng TC-CAT/DUE/PRI/REORDER-*) — `c3
  Freshness` 50% (5/10) → **100% (10/10)**. Không vẽ lại mermaid diagram
  gốc của HLD (chỉ thêm bảng text) — ghi rõ trong doc. Mục 7 mới cũng tự
  khai 4 kịch bản chưa có test (TC-CAT-07, TC-PRI-03, TC-REORDER-05,
  gesture UI thật) — nợ test đó vẫn còn, chưa xử lý ở lần sửa này.

## Giới hạn đã biết

- `c1`/`c4` đếm FR/NFR bằng grep theo mã số — NFR khó gắn mã trong code hơn
  FR nên 8 mã "mồ côi" phát hiện được đều là NFR, có thể là false negative
  (NFR vẫn được tuân thủ nhưng không chú thích mã).
- `c7` (Retrieval Quality) dựa trên spot-check 5 tính năng, không phải một
  bộ test tự động — lặp lại có thể ra số khác nếu chọn tính năng khác.
- Dashboard là file tĩnh, số liệu đứng yên tại thời điểm tạo. Muốn cập nhật:
  chạy lại các lệnh trên, sửa mảng `knowledgeHealth` / `proxyKPI` /
  `findings` trong `index.html`.
