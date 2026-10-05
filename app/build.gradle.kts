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
// Môi trường dev: server Supabase local do server/dev-up.sh dựng, secret + tài khoản test ở server/.env.dev (gitignore)
val devEnv = Properties().apply {
    rootProject.file("server/.env.dev").takeIf { it.exists() }?.inputStream()?.use(::load)
}
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
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SUPABASE_URL", "\"https://hoc-api.120203.xyz\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${local.getProperty("ANON_KEY", "")}\"")
        // Production (release) luôn rỗng → không có nút điền tài khoản test
        buildConfigField("String", "DEV_EMAIL", "\"\"")
        buildConfigField("String", "DEV_PASSWORD", "\"\"")
        buildConfigField("boolean", "SHOW_SAMPLES", "false")
        manifestPlaceholders["cleartext"] = "false"
    }

    buildTypes {
        debug { useDevServer() }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // ponytail: ký bằng debug key để chạy thử bản R8 trên AVD; keystore thật khi phát hành
            signingConfig = signingConfigs.getByName("debug")
        }
        // Giống hệt release (R8 + shrink) nhưng trỏ server dev: kiểm R8/serializer mà không đụng production
        create("devRelease") {
            initWith(getByName("release"))
            matchingFallbacks += "release"
            useDevServer()
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
    implementation("androidx.room:room-runtime:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")
    implementation("androidx.work:work-runtime-ktx:2.12.0")
    implementation(platform("io.github.jan-tennert.supabase:bom:3.8.0"))
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation("io.ktor:ktor-client-okhttp:3.5.1") // khớp ktor 3.5.1 mà supabase-kt 3.8.0 dùng
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}
