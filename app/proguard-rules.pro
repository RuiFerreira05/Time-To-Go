# This file is intentionally left empty.
# Add project specific ProGuard rules here.
-keep class com.timetogo.app.data.remote.dto.** { *; }
-keepclassmembers class com.timetogo.app.data.remote.dto.** { *; }

# Moshi
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep @com.squareup.moshi.JsonQualifier interface *

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
