# Học Mỗi Ngày

App Android học **lập trình** (đọc, viết và chạy code thật — Python, JavaScript, Kotlin, Java, Go, Rust, C++, C#, SQL)
và **tiếng Anh công việc** (họp, email, phỏng vấn: nghe rồi nói to), mỗi ngày 10–30 phút.

- Kotlin + Jetpack Compose (Material 3), Navigation 3, Room, WorkManager, supabase-kt.
- Lịch ôn FSRS-6, hàng đợi "Hôm nay", chuỗi ngày, nhắc học.
- Editor code: Sora Editor + grammar TextMate (nguồn và giấy phép: `app/src/main/assets/textmate/NOTICE.txt`).
- Server (Supabase tự host, chạy code bằng Piston) và giáo trình nằm ở repo riêng (private).

## Build

Cần JDK 17+ và Android SDK (compileSdk 37).

```bash
./gradlew :app:testDebugUnitTest      # unit test
./gradlew :app:assembleRelease        # APK release (R8)
```

`local.properties` (không commit):

```properties
sdk.dir=…
ANON_KEY=…            # anon key của server production (cho bản release)
DEV_ENV_FILE=…        # tuỳ chọn: đường dẫn server/.env.dev của server dev local (cho debug/devRelease)
```

Build type: `debug` và `devRelease` trỏ server dev local (`10.0.2.2:8100`), `release` trỏ production.

## Phát hành

Đẩy tag `v*` (ví dụ `git tag v0.2.0 && git push origin v0.2.0`): GitHub Actions chạy test, build APK ký keystore
và tạo Release kèm APK. Secrets cần có: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `SUPABASE_ANON_KEY`.

## Baseline profile

`app/src/main/baselineProfiles/baseline-prof.txt` sinh bằng module `:baselineprofile` trên máy ảo (variant `devBench`):

```bash
./gradlew :baselineprofile:connectedDevBenchAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.thanhnb.hocmoingay.baselineprofile.BaselineProfileGenerator
```

rồi chép file `*-startup-prof.txt` trong `baselineprofile/build/outputs/connected_android_test_additional_output/` vào đó.
