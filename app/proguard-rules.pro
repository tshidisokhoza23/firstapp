# Project specific ProGuard / R8 rules for FirstApp

# 1. Preserve All Application Classes, Interfaces, Models, Entities, DTOs, ViewModels, and Enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keep class com.tpsk.tj.** { *; }
-keep interface com.tpsk.tj.** { *; }
-keepclassmembers class com.tpsk.tj.** { *; }
-keepclassmembers interface com.tpsk.tj.** { *; }

# 2. Preserve Annotations and Generic Signatures for Reflection / Retrofit / Gson / Room
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, RuntimeVisibleTypeAnnotations, AnnotationDefault, *Annotation*

# 3. Retrofit & OkHttp
# Every API is a Kotlin suspend function. R8 must keep Continuation<T>, or Retrofit
# throws "Class cannot be cast to ParameterizedType" on every service call.
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keepclassmembers class retrofit2.** { *; }
-keepclassmembers interface retrofit2.** { *; }
-keep,includedescriptorclasses,allowshrinking interface com.tpsk.tj.** { *; }
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation,includedescriptorclasses interface <1>
-keepclassmembers,allowobfuscation,includedescriptorclasses interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**

# 4. Gson, SerializedName Fields & TypeToken Subclasses
-keep class com.google.gson.** { *; }
-keepclassmembers class com.google.gson.** { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }
-keepclassmembers class * extends com.google.gson.reflect.TypeToken { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 5. Room Database, DAOs, Entities & TypeConverters
-keep class androidx.room.RoomDatabase { *; }
-keepclassmembers class * {
    @androidx.room.TypeConverter <methods>;
    @androidx.room.Dao <methods>;
}

# 6. Hilt & Dagger Dependency Injection
-keep class * extends javax.inject.Provider
-keep class dagger.hilt.** { *; }
-keep class hilt_aggregated_deps.** { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @dagger.Provides <methods>;
}

# 7. Media3 / ExoPlayer & Coil Image Loader
-keep class coil.** { *; }
-dontwarn coil.**
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# 8. WebRTC & Call Services
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# 9. Firebase & Google Play Services / AdMob
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.ads.** { *; }

# 10. AndroidX Lifecycle & Security
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.security.crypto.**

# 11. AppLovin / Amazon PrivacyPass
-dontwarn com.amazon.privacypass.**
-dontwarn com.iab.omid.library.applovin.**
-dontwarn com.applovin.**

# 12. Kotlin Coroutines & Retrofit Suspend Function Continuation Signatures
-keep class kotlin.coroutines.** { *; }
-keep class kotlin.coroutines.jvm.internal.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class * extends kotlin.coroutines.jvm.internal.ContinuationImpl { *; }
-keepclassmembers class * extends kotlin.coroutines.jvm.internal.BaseContinuationImpl { *; }

# 13. Reflection used by Gson, Retrofit, Room, and Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclasseswithmembers class **$$serializer { *; }
-keep class net.sqlcipher.** { *; }
-keep class net.zetetic.** { *; }
-dontwarn net.sqlcipher.**
-dontwarn net.zetetic.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn javax.annotation.**
-dontwarn org.slf4j.**

# Do not obfuscate. Renaming Kotlin suspend signatures makes Continuation raw,
# and every Retrofit service call then crashes in release only.
-dontoptimize
-dontobfuscate
