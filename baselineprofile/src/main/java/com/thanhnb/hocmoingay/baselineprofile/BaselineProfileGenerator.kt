package com.thanhnb.hocmoingay.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

val PKG: String get() = InstrumentationRegistry.getArguments().getString("targetAppId") ?: "com.thanhnb.hocmoingay"

/** Đăng nhập tài khoản dev (chỉ variant trỏ server dev mới có nút này). */
fun MacrobenchmarkScope.loginIfNeeded() {
    val fill = device.wait(Until.findObject(By.text("Điền tài khoản test (dev)")), 3_000) ?: return
    fill.click()
    device.findObject(By.text("Bắt đầu học"))?.click()
    device.wait(Until.hasObject(By.text("Việc hôm nay")), 20_000)
}

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule val rule = BaselineProfileRule()

    /** 3 luồng của spec §7.5: mở app vào Hôm nay, mở bài học, mở editor. */
    @Test fun generate() = rule.collect(packageName = PKG, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()
        loginIfNeeded()
        device.wait(Until.hasObject(By.text("Việc hôm nay")), 15_000)
        // Mở bài học: mục "Luyện code" (bài problem: explain → code), nếu không có thì "Bài tiếp theo"
        (device.findObject(By.text("Luyện code")) ?: device.findObject(By.text("Bài tiếp theo")))?.click()
        device.wait(Until.findObject(By.text("Bắt đầu")), 10_000)?.click()
        // Mở editor: đi qua các card tới khi thấy "Mở editor" (tối đa 5 card)
        repeat(5) {
            device.wait(Until.findObject(By.text("Mở editor")), 2_000)?.let { it.click(); device.waitForIdle(); return@collect }
            device.findObject(By.text("Tiếp"))?.click()
        }
    }
}
