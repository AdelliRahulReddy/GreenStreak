# Add project specific ProGuard rules here.

# Keep Moshi models
-keep class com.rahul.githubwallpaper.data.** { *; }
-keepclassmembers class com.rahul.githubwallpaper.data.** { *; }

# Keep Retrofit interfaces
-keep interface com.rahul.githubwallpaper.data.GitHubApi { *; }

# Moshi
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep @com.squareup.moshi.JsonQualifier @interface *

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
