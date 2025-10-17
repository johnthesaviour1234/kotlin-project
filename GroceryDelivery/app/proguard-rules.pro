# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Retrofit interfaces
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items).
-keep,allowobfuscation,allowshrinking class * extends retrofit2.Call
-keep,allowobfuscation,allowshrinking class * extends retrofit2.Response

# With R8 full mode generic signatures are stripped for classes that are not kept.
# Suspend functions are wrapped in continuations where the type argument is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# Room database rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Hilt rules
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp

# Keep data classes and DTOs
-keep class com.grocery.customer.data.remote.dto.** { *; }
-keep class com.grocery.customer.domain.model.** { *; }

# Keep ViewBinding classes
-keep class * implements androidx.viewbinding.ViewBinding {
    public static *** inflate(...);
    public static *** bind(...);
}

# Common Android rules
-keep class * extends java.lang.Enum {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepattributes *Annotation*