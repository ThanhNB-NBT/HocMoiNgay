package com.thanhnb.hocmoingay.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Cold start tới khung hình đầu: không profile và có baseline profile (tiêu chí xong của e). */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule val rule = MacrobenchmarkRule()

    @Test fun khongProfile() = startup(CompilationMode.None())
    @Test fun coProfile() = startup(CompilationMode.Partial(BaselineProfileMode.Require))

    private fun startup(mode: CompilationMode) = rule.measureRepeated(
        packageName = PKG, metrics = listOf(StartupTimingMetric()), compilationMode = mode,
        iterations = 10, startupMode = StartupMode.COLD,
        // đăng nhập dev một lần (cài mới thì chưa đăng nhập), để mỗi lần đo đều mở vào Hôm nay như spec §7.5
        setupBlock = { pressHome(); startActivityAndWait(); loginIfNeeded(); pressHome() },
    ) {
        pressHome()
        startActivityAndWait()
    }
}
