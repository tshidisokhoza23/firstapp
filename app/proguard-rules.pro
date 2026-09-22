# Project specific ProGuard rules

# Keep Room generated classes
-keep class androidx.room.RoomDatabase { *; }
-keep class com.tpsk.tj.data.local.** { *; }

# Keep Retrofit and Gson classes
-keep class retrofit2.** { *; }
-keep class com.google.gson.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep WebRTC
-keep class org.webrtc.** { *; }

# Keep Firebase
-keep class com.google.firebase.** { *; }

# Keep AndroidX lifecycle
-keep class androidx.lifecycle.** { *; }

# AppLovin / Amazon PrivacyPass
-dontwarn com.amazon.privacypass.**
-dontwarn com.iab.omid.library.applovin.**
-dontwarn com.applovin.**

# Optimization for performance
-optimizationpasses 5
-allowaccessmodification
-dontpreverify
