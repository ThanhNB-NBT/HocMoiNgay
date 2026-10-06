import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.test")
    id("org.jetbrains.kotlin.android") // gradle.properties: android.builtInKotlin=false
}

android {
    namespace = "com.thanhnb.hocmoingay.baselineprofile"
    compileSdk = 37
    defaultConfig {
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // AVD gac_truyen là máy ảo: chấp nhận số đo kém chính xác hơn máy thật
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
    }
    targetProjectPath = ":app"
    // macrobenchmark kill app đích: test phải chạy ở tiến trình riêng (plugin baselineprofile vốn tự bật cờ này)
    experimentalProperties["android.experimental.self-instrumenting"] = true
    // khớp build type devRelease của :app (trỏ server dev): chỉ sinh/đo trên variant này, không bao giờ trên release (production)
    buildTypes {
        create("devRelease") { signingConfig = signingConfigs.getByName("debug") }
        create("devBench") { signingConfig = signingConfigs.getByName("debug") }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}


kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

dependencies {
    implementation("androidx.test.ext:junit:1.3.0")
    implementation("androidx.test.uiautomator:uiautomator:2.4.0")
    implementation("androidx.benchmark:benchmark-macro-junit4:1.5.0")
}
