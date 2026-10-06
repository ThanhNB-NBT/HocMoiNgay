import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("androidx.room")
}

// local.properties (gitignore): sdk.dir + ANON_KEY=... (Task 6 lấy từ .env trên box)
val local = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
// Môi trường dev: server Supabase local do server/dev-up.sh (repo server riêng) dựng; secret + tài khoản test ở server/.env.dev.
// Đường dẫn đặt trong local.properties: DEV_ENV_FILE=…/hoc_moi_ngay/server/.env.dev. Thiếu file thì bản dev không có key/tài khoản điền sẵn.
val devEnv = Properties().apply {
    local.getProperty("DEV_ENV_FILE")?.let(::file)?.takeIf { it.exists() }?.inputStream()?.use(::load)
}
// Bản release: ANON_KEY ở local.properties (máy dev) hoặc biến môi trường SUPABASE_ANON_KEY (CI, từ GitHub Secrets)
val anonKey: String = local.getProperty("ANON_KEY") ?: System.getenv("SUPABASE_ANON_KEY").orEmpty()
// Ký phát hành: CI giải keystore từ secret ra file rồi đặt ANDROID_KEYSTORE_FILE/ANDROID_KEYSTORE_PASSWORD (alias "upload", như Novel_Persona)
val keystoreFile: String? = System.getenv("ANDROID_KEYSTORE_FILE")
fun javaStr(v: String) = "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
fun com.android.build.api.dsl.ApplicationBuildType.useDevServer() {
    buildConfigField("String", "SUPABASE_URL", javaStr("http://10.0.2.2:8100")) // localhost của laptop nhìn từ emulator
    buildConfigField("String", "SUPABASE_ANON_KEY", javaStr(devEnv.getProperty("ANON_KEY", "")))
    buildConfigField("String", "DEV_EMAIL", javaStr(devEnv.getProperty("DEV_TEST_EMAIL", "")))
    buildConfigField("String", "DEV_PASSWORD", javaStr(devEnv.getProperty("DEV_TEST_PASSWORD", "")))
    buildConfigField("boolean", "SHOW_SAMPLES", "true")
    manifestPlaceholders["cleartext"] = "true" // server dev chạy http
}

android {
    namespace = "com.thanhnb.hocmoingay"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.thanhnb.hocmoingay"
        minSdk = 26
        targetSdk = 36
        // CI phát hành truyền -PversionName=<tag bỏ v> -PversionCode=<số lần chạy>
        versionCode = (findProperty("versionCode") as String?)?.toInt() ?: 1
        versionName = (findProperty("versionName") as String?) ?: "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SUPABASE_URL", "\"https://hoc-api.120203.xyz\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", javaStr(anonKey))
        // Production (release) luôn rỗng → không có nút điền tài khoản test
        buildConfigField("String", "DEV_EMAIL", "\"\"")
        buildConfigField("String", "DEV_PASSWORD", "\"\"")
        buildConfigField("boolean", "SHOW_SAMPLES", "false")
        manifestPlaceholders["cleartext"] = "false"
    }

    signingConfigs {
        create("upload") {
            keystoreFile?.let { storeFile = file(it) }
            storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            keyAlias = "upload"
            keyPassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
        }
    }

    buildTypes {
        debug { useDevServer() }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Máy dev: ký debug để chạy thử bản R8 trên AVD; CI phát hành: keystore thật
            signingConfig = signingConfigs.getByName(if (keystoreFile != null) "upload" else "debug")
        }
        // Giống hệt release (R8 + shrink) nhưng trỏ server dev: kiểm R8/serializer mà không đụng production
        create("devRelease") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
            useDevServer()
            isProfileable = true // macrobenchmark đo cold start (Task 8 của e)
        }
        // Chỉ để sinh baseline profile: như devRelease nhưng không R8 (profile cần tên lớp thật; AGP tự ánh xạ qua R8 khi build release).
        // File kết quả nằm ở src/main/baselineProfiles và dùng cho mọi variant.
        create("devBench") {
            initWith(getByName("devRelease"))
            matchingFallbacks += listOf("devRelease", "release")
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // android.util.Log trong code được test JVM gọi tới
    testOptions { unitTests.isReturnDefaultValues = true }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

room { schemaDirectory("$projectDir/schemas") }

dependencies {
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.navigation3:navigation3-runtime:1.2.0")
    implementation("androidx.navigation3:navigation3-ui:1.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-navigation3:2.11.0")
    implementation("androidx.room:room-runtime:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")
    implementation("androidx.work:work-runtime-ktx:2.12.0")
    implementation(platform("io.github.jan-tennert.supabase:bom:3.8.0"))
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation("io.github.jan-tennert.supabase:functions-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    implementation(platform("io.github.rosemoe:editor-bom:0.24.6"))
    implementation("io.github.rosemoe:editor")
    implementation("io.github.rosemoe:language-textmate")
    implementation("io.ktor:ktor-client-okhttp:3.5.1") // khớp ktor 3.5.1 mà supabase-kt 3.8.0 dùng
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}
