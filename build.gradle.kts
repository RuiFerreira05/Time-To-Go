// Top-level build file for Time to Go
// AGP 9.0.1 has built-in Kotlin support — no separate kotlin-android plugin needed.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
}
