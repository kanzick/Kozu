# Kozu Application Proguard / R8 Rules

# Keep Data Transfer Objects (DTOs) and Models for Gson / DataStore / Reflection
-keep class vn.kozu.data.remote.osu.** { *; }
-keepclassmembers class vn.kozu.data.remote.osu.** { <fields>; }
-keep class vn.kozu.data.local.** { *; }
-keepclassmembers class vn.kozu.data.local.** { <fields>; }
-keep class vn.kozu.domain.model.** { *; }
-keepclassmembers class vn.kozu.domain.model.** { <fields>; }

# Keep App Components and Services
-keep class vn.kozu.KozuApplication { *; }
-keep class vn.kozu.player.PlaybackService { *; }
-keep class vn.kozu.data.download.DownloadService { *; }

# Attributes required for Retrofit / Reflection / Annotations
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, AnnotationDefault

# Retrofit
-dontwarn retrofit2.**
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Coroutines
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
